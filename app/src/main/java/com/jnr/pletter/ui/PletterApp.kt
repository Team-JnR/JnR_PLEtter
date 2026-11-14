package com.jnr.pletter.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jnr.pletter.data.LetterStore
import com.jnr.pletter.vehicle.MockVehicleState

sealed interface Screen {
    data object Driver : Screen
    data object Home : Screen
    data object Record : Screen
    data class Review(val audioPath: String) : Screen
    data class Schedule(val audioPath: String, val transcript: String) : Screen
    data object Outbox : Screen
    data object Inbox : Screen
    data class Detail(val letterId: String) : Screen
}

@Composable
fun PletterApp(store: LetterStore, vehicle: MockVehicleState) {
    var screen by remember { mutableStateOf<Screen>(Screen.Driver) }
    var driverId by remember { mutableStateOf<String?>(null) }
    var newlyArrived by remember { mutableIntStateOf(0) }

    val driver = store.profiles.firstOrNull { it.id == driverId }
    val parked = vehicle.isParked

    // 임시 녹음을 들고 있는 화면에서 벗어날 때 파일을 정리합니다.
    fun discardDraft() {
        when (val s = screen) {
            is Screen.Review -> store.discardAudio(s.audioPath)
            is Screen.Schedule -> store.discardAudio(s.audioPath)
            else -> Unit
        }
    }

    fun signOut() {
        discardDraft()
        driverId = null
        newlyArrived = 0
        screen = Screen.Driver
    }

    BackHandler(enabled = screen != Screen.Driver && screen != Screen.Home) {
        screen = when (val s = screen) {
            is Screen.Review -> {
                store.discardAudio(s.audioPath)
                Screen.Record
            }
            is Screen.Schedule -> Screen.Review(s.audioPath)
            is Screen.Detail -> Screen.Inbox
            else -> Screen.Home
        }
    }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                val current = screen
                if (driver == null || current == Screen.Driver) {
                    // 운전자가 확인되지 않으면 어떤 편지도 보여 주지 않습니다.
                    DriverScreen(
                        store = store,
                        parked = parked,
                        onVerified = { profile ->
                            driverId = profile.id
                            newlyArrived = store.deliverPendingFor(profile.id)
                            screen = Screen.Home
                        },
                    )
                } else {
                    when (current) {
                        Screen.Driver -> Unit
                        Screen.Home -> HomeScreen(
                            store = store,
                            driver = driver,
                            parked = parked,
                            newlyArrived = newlyArrived,
                            onRecord = { screen = Screen.Record },
                            onInbox = {
                                newlyArrived = 0
                                screen = Screen.Inbox
                            },
                            onOutbox = { screen = Screen.Outbox },
                            onSwitchDriver = { signOut() },
                        )
                        Screen.Record -> RecordScreen(
                            parked = parked,
                            onBack = { screen = Screen.Home },
                            onRecorded = { path -> screen = Screen.Review(path) },
                        )
                        is Screen.Review -> ReviewScreen(
                            audioPath = current.audioPath,
                            parked = parked,
                            onRetake = {
                                store.discardAudio(current.audioPath)
                                screen = Screen.Record
                            },
                            onNext = { transcript ->
                                screen = Screen.Schedule(current.audioPath, transcript)
                            },
                        )
                        is Screen.Schedule -> ScheduleScreen(
                            store = store,
                            driver = driver,
                            onBack = { screen = Screen.Review(current.audioPath) },
                            onSchedule = { recipientId ->
                                store.schedule(
                                    senderId = driver.id,
                                    recipientId = recipientId,
                                    audioPath = current.audioPath,
                                    transcript = current.transcript,
                                )
                                screen = Screen.Outbox
                            },
                        )
                        Screen.Outbox -> OutboxScreen(
                            store = store,
                            driver = driver,
                            onBack = { screen = Screen.Home },
                        )
                        Screen.Inbox -> InboxScreen(
                            store = store,
                            driver = driver,
                            parked = parked,
                            onBack = { screen = Screen.Home },
                            onOpen = { letterId -> screen = Screen.Detail(letterId) },
                        )
                        is Screen.Detail -> LetterDetailScreen(
                            store = store,
                            letterId = current.letterId,
                            parked = parked,
                            onBack = { screen = Screen.Inbox },
                        )
                    }
                }
            }

            MockVehicleBar(
                vehicle = vehicle,
                sessionId = store.sessionId,
                onNewIgnition = {
                    signOut()
                    vehicle.isParked = true
                    store.startNewSession()
                },
            )
        }
    }
}

/**
 * 시연·개발용 모의 차량 상태 바. 실제 차량 신호가 아니라 손으로 바꾸는 값임을 화면에 명시합니다.
 * Vehicle SDK 연동 후에는 제거합니다.
 */
@Composable
private fun MockVehicleBar(vehicle: MockVehicleState, sessionId: Int, onNewIgnition: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "모의 차량 상태 · 시동 세션 #$sessionId",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (vehicle.isParked) {
                Button(onClick = {}) { Text("P 주차", fontSize = 16.sp) }
                OutlinedButton(onClick = { vehicle.isParked = false }) { Text("D 주행", fontSize = 16.sp) }
            } else {
                OutlinedButton(onClick = { vehicle.isParked = true }) { Text("P 주차", fontSize = 16.sp) }
                Button(onClick = {}) { Text("D 주행", fontSize = 16.sp) }
            }
            OutlinedButton(onClick = onNewIgnition) {
                Text("새 시동(모의)", fontSize = 16.sp)
            }
        }
    }
}
