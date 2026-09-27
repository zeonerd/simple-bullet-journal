package com.simple.bulletjournal

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

// 광고 동의(UMP) + MobileAds 초기화는 data/AdsConsentRepositoryImpl로 옮겼습니다(설정 화면에서도 재사용하기 위함).
@HiltAndroidApp
class BulletJournalApp : Application()
