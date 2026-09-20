package com.simple.bulletjournal.data

import android.app.Activity

interface BillingRepository {
    /** 결제 다이얼로그를 띄워 "광고 제거" 상품 구매를 시작합니다. */
    fun purchaseAdRemoval(activity: Activity)

    /** 이미 구매한 이력이 있는지 다시 조회해 로컬 상태를 동기화합니다 (재설치·기기 변경 대응). */
    fun restorePurchases()
}
