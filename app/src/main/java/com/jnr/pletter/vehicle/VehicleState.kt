package com.jnr.pletter.vehicle

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 앱이 차량에서 알아야 하는 최소 상태.
 * 실제 연동 시 Pleos Vehicle SDK(기어·시동 상태 구독)로 구현한 클래스를 만들어
 * MainActivity에서 MockVehicleState 대신 주입합니다.
 */
interface VehicleStateProvider {
    /** 기어 P(주차) 여부. false면 주행 중으로 보고 녹음·열람을 제한합니다. */
    val isParked: Boolean
}

/** 화면 하단 '모의 차량 상태' 바에서 직접 바꾸는 시연·개발용 구현. */
class MockVehicleState : VehicleStateProvider {
    override var isParked by mutableStateOf(true)
}
