package com.simple.bulletjournal

import android.app.Activity
import com.simple.bulletjournal.data.BillingRepository

class FakeBillingRepository : BillingRepository {
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
