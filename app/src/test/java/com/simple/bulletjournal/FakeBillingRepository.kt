package com.simple.bulletjournal

import android.app.Activity
import com.simple.bulletjournal.data.BillingNotice
import com.simple.bulletjournal.data.BillingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class FakeBillingRepository : BillingRepository {
    val noticesFlow = MutableSharedFlow<BillingNotice>(extraBufferCapacity = 1)
    override val notices: Flow<BillingNotice> = noticesFlow

    var purchaseAdRemovalCallCount = 0
        private set
    var restorePurchasesCallCount = 0
        private set

    override fun purchaseAdRemoval(activity: Activity) {
        purchaseAdRemovalCallCount++
    }

    override fun restorePurchases() {
        restorePurchasesCallCount++
    }
}
