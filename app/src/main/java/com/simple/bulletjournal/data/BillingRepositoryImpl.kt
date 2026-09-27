package com.simple.bulletjournal.data

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

// Google Play Console에서 생성할 비소모성(non-consumable) 인앱상품 ID. 반드시 이 값과 동일하게 등록할 것.
const val REMOVE_ADS_PRODUCT_ID = "remove_ads_sbj"

class BillingRepositoryImpl(
    context: Context,
    private val userPreferencesRepository: UserPreferencesRepository
) : BillingRepository, PurchasesUpdatedListener {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var removeAdsProductDetails: ProductDetails? = null

    private val _notices = MutableSharedFlow<BillingNotice>(extraBufferCapacity = 1)
    override val notices: Flow<BillingNotice> = _notices.asSharedFlow()

    // Billing Library 8부터 enableAutoServiceReconnection()을 켜면 연결이 끊긴 상태에서 API를 호출해도
    // 라이브러리가 알아서 재연결 후 실행합니다. 예전처럼 onBillingServiceDisconnected에서 수동 재연결(무한 재귀 위험)할 필요가 없습니다.
    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .enableAutoServiceReconnection()
        .build()

    init {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryRemoveAdsProductDetails()
                    syncPurchases(notifyResult = false)
                }
            }

            override fun onBillingServiceDisconnected() = Unit
        })
    }

    private fun queryRemoveAdsProductDetails() {
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(REMOVE_ADS_PRODUCT_ID)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(product))
            .build()

        billingClient.queryProductDetailsAsync(params) { _, result ->
            removeAdsProductDetails = result.productDetailsList.firstOrNull()
        }
    }

    override fun purchaseAdRemoval(activity: Activity) {
        val details = removeAdsProductDetails
        if (details == null) {
            Log.w(TAG, "상품 정보가 아직 준비되지 않았습니다. 재조회를 시도합니다.")
            _notices.tryEmit(BillingNotice.PRODUCT_UNAVAILABLE)
            queryRemoveAdsProductDetails()
            return
        }

        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .build()
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        val result = billingClient.launchBillingFlow(activity, flowParams)
        handleFailure(result)
    }

    override fun restorePurchases() {
        syncPurchases(notifyResult = true)
    }

    /**
     * Play에 기록된 구매 내역을 기준으로 로컬 [UserPreferences.isAdRemoved]를 맞춥니다.
     * 조회에 성공했는데 구매 내역이 없으면(환불·취소 포함) 광고 제거 상태를 해제합니다.
     * 조회 자체가 실패하면(오프라인 등) 기존 상태를 그대로 둡니다 — 잘못 광고를 되살리지 않기 위함.
     */
    private fun syncPurchases(notifyResult: Boolean) {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                if (notifyResult) _notices.tryEmit(BillingNotice.RESTORE_FAILED)
                return@queryPurchasesAsync
            }

            val owned = purchases.filter { it.isOwnedRemoveAds() }
            owned.forEach(::grantAndAcknowledge)
            if (owned.isEmpty()) {
                repositoryScope.launch { userPreferencesRepository.setAdRemoved(false) }
            }

            if (notifyResult) {
                _notices.tryEmit(if (owned.isEmpty()) BillingNotice.NOTHING_TO_RESTORE else BillingNotice.RESTORED)
            }
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            handleFailure(result)
            return
        }
        purchases.orEmpty()
            .filter { REMOVE_ADS_PRODUCT_ID in it.products }
            .forEach { purchase ->
                when (purchase.purchaseState) {
                    Purchase.PurchaseState.PURCHASED -> grantAndAcknowledge(purchase)
                    Purchase.PurchaseState.PENDING -> _notices.tryEmit(BillingNotice.PURCHASE_PENDING)
                    else -> Unit
                }
            }
    }

    private fun handleFailure(result: BillingResult) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK,
            BillingClient.BillingResponseCode.USER_CANCELED -> Unit
            // 다른 기기에서 이미 샀거나 로컬 상태가 어긋난 경우 — 구매 내역을 다시 맞춘다
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> syncPurchases(notifyResult = true)
            else -> {
                Log.w(TAG, "결제 실패: ${result.responseCode} ${result.debugMessage}")
                _notices.tryEmit(BillingNotice.PURCHASE_FAILED)
            }
        }
    }

    private fun grantAndAcknowledge(purchase: Purchase) {
        repositoryScope.launch {
            userPreferencesRepository.setAdRemoved(true)
        }

        if (!purchase.isAcknowledged) {
            val ackParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient.acknowledgePurchase(ackParams) { }
        }
    }

    private fun Purchase.isOwnedRemoveAds(): Boolean =
        REMOVE_ADS_PRODUCT_ID in products && purchaseState == Purchase.PurchaseState.PURCHASED

    companion object {
        private const val TAG = "BillingRepository"
    }
}
