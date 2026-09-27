package com.simple.bulletjournal.data

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow

interface AdsConsentRepository {
    /** 동의 절차가 끝나 MobileAds SDK가 초기화되었고 광고를 요청해도 되는 상태인지 */
    val canShowAds: StateFlow<Boolean>

    /**
     * 사용자가 광고 동의를 다시 바꿀 수 있는 "개인정보 옵션" 진입점을 노출해야 하는지.
     * EEA/영국 등 UMP가 요구하는 지역에서만 true가 되며, 설정 화면이 이 값으로 메뉴 노출을 결정합니다.
     */
    val isPrivacyOptionsRequired: StateFlow<Boolean>

    /** 앱 시작 시 호출: 동의 정보 갱신 → 필요하면 동의 폼 표시 → 광고 SDK 초기화 */
    fun gatherConsent(activity: Activity)

    /** 설정 화면에서 호출: UMP 개인정보 옵션 폼을 띄워 동의를 변경/철회할 수 있게 함 */
    fun showPrivacyOptionsForm(activity: Activity)
}
