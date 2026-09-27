package com.simple.bulletjournal.data

import android.app.Activity
import kotlinx.coroutines.flow.Flow

/** 결제/복원 결과 중 사용자에게 알려야 하는 것들. 사용자가 직접 취소한 경우는 알리지 않습니다. */
enum class BillingNotice {
    /** 상품 정보를 아직 못 불러옴(Play 연결 실패, 상품 미등록, 오프라인 등) */
    PRODUCT_UNAVAILABLE,
    /** 결제가 보류 중(예: 편의점 결제 등 지연 결제 수단) — 완료되면 자동으로 반영됨 */
    PURCHASE_PENDING,
    PURCHASE_FAILED,
    RESTORED,
    NOTHING_TO_RESTORE,
    RESTORE_FAILED
}

interface BillingRepository {
    /** 설정 화면에서 스낵바로 보여줄 일회성 알림 */
    val notices: Flow<BillingNotice>

    /** 결제 다이얼로그를 띄워 "광고 제거" 상품 구매를 시작합니다. */
    fun purchaseAdRemoval(activity: Activity)

    /** 이미 구매한 이력이 있는지 다시 조회해 로컬 상태를 동기화합니다 (재설치·기기 변경 대응). 결과를 [notices]로 알립니다. */
    fun restorePurchases()
}
