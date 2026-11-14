package com.jnr.pletter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.jnr.pletter.data.LetterStore
import com.jnr.pletter.ui.PletterApp
import com.jnr.pletter.ui.PletterTheme
import com.jnr.pletter.vehicle.MockVehicleState

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val store = LetterStore(applicationContext)
        // 기획서의 대체 흐름: 자동 실행이 제한되면 "시동 후 첫 앱 실행"을 새 시동 세션으로 봅니다.
        if (savedInstanceState == null) store.startNewSession()

        // Pleos Vehicle SDK 연동 전까지는 모의 차량 상태를 사용합니다.
        val vehicle = MockVehicleState()

        setContent {
            PletterTheme {
                PletterApp(store = store, vehicle = vehicle)
            }
        }
    }
}
