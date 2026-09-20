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
import kotlinx.coroutines.launch

// Google Play Console에서 생성할 비소모성(non-consumable) 인앱상품 ID. 반드시 이 값과 동일하게 등록할 것.
const val REMOVE_ADS_PRODUCT_ID = "remove_ads_sbj"

class BillingRepositoryImpl(
    context: Context,
    private val userPreferencesRepository: UserPreferencesRepository
) : BillingRepository, PurchasesUpdatedListener {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var removeAdsProductDetails: ProductDetails? = null

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .build()

    init {
        connect()
    }

    private fun connect() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryRemoveAdsProductDetails()
                    restorePurchases()
                }
            }

            override fun onBillingServiceDisconnected() {
                connect()
            }
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

        billingClient.queryProductDetailsAsync(params) { _, productDetailsList ->
            removeAdsProductDetails = productDetailsList.firstOrNull()
        }
    }

    override fun purchaseAdRemoval(activity: Activity) {
        val details = removeAdsProductDetails
        if (details == null) {
            Log.w(TAG, "상품 정보가 아직 준비되지 않았습니다. 재조회를 시도합니다.")
            queryRemoveAdsProductDetails()
            return
        }

        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .build()
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        billingClient.launchBillingFlow(activity, flowParams)
    }

    override fun restorePurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(params) { _, purchases ->
            purchases.forEach(::handlePurchase)
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
            purchases?.forEach(::handlePurchase)
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (!purchase.products.contains(REMOVE_ADS_PRODUCT_ID)) return
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return

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

    companion object {
        private const val TAG = "BillingRepository"
    }
}
