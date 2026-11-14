package com.jnr.pletter.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.jnr.pletter.audio.LetterPlayer
import com.jnr.pletter.audio.VoiceRecorder
import com.jnr.pletter.data.LetterStore
import com.jnr.pletter.data.Profile
import kotlinx.coroutines.delay

/** ① 녹음. 사용자가 버튼을 누른 동안만 녹음하며, 녹음 중임을 화면에 표시합니다. */
@Composable
fun RecordScreen(parked: Boolean, onBack: () -> Unit, onRecorded: (String) -> Unit) {
    val context = LocalContext.current
    val recorder = remember { VoiceRecorder(context.applicationContext) }
    var recording by remember { mutableStateOf(false) }
    var seconds by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { result -> granted = result }

    DisposableEffect(Unit) {
        onDispose { recorder.cancel() }
    }

    LaunchedEffect(recording) {
        if (recording) {
            seconds = 0
            while (true) {
                delay(1000)
                seconds += 1
            }
        }
    }

    // 녹음 중 주행을 시작하면 녹음을 버리고 멈춥니다(MVP: 녹음은 주차 중에만).
    LaunchedEffect(parked) {
        if (!parked && recording) {
            recorder.cancel()
            recording = false
            error = "주행이 시작되어 녹음을 멈췄습니다."
        }
    }

    ScreenFrame(
        title = "편지 남기기",
        onBack = {
            recorder.cancel()
            onBack()
        },
    ) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            when {
                !parked -> Note("주행 중에는 녹음할 수 없어요. 안전하게 멈춘 뒤 남겨 주세요.")
                !granted -> {
                    Note("목소리를 담으려면 마이크 권한이 필요합니다.")
                    Spacer(Modifier.height(20.dp))
                    BigButton(
                        "마이크 권한 허용",
                        onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                    )
                }
                recording -> {
                    Text(
                        "● 녹음 중",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "%d:%02d".format(seconds / 60, seconds % 60),
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(24.dp))
                    BigButton(
                        "녹음 마치기",
                        onClick = {
                            val file = recorder.stop()
                            recording = false
                            if (file != null) {
                                onRecorded(file.absolutePath)
                            } else {
                                error = "녹음이 너무 짧습니다. 다시 시도해 주세요."
                            }
                        },
                        modifier = Modifier.width(360.dp),
                    )
                }
                else -> {
                    Note("전하고 싶은 마음을 목소리 그대로 남겨 보세요.")
                    Spacer(Modifier.height(8.dp))
                    Note("동승자가 있다면 녹음한다는 것을 먼저 알려 주세요.")
                    Spacer(Modifier.height(24.dp))
                    BigButton(
                        "녹음 시작",
                        onClick = {
                            error = null
                            try {
                                recorder.start()
                                recording = true
                            } catch (e: Exception) {
                                error = "녹음을 시작할 수 없습니다. 마이크 상태를 확인해 주세요."
                            }
                        },
                        modifier = Modifier.width(360.dp),
                    )
                }
            }
            error?.let { message ->
                Spacer(Modifier.height(16.dp))
                Note(message, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

/** ② 원본·전사본 확인. */
@Composable
fun ReviewScreen(
    audioPath: String,
    parked: Boolean,
    onRetake: () -> Unit,
    onNext: (String) -> Unit,
) {
    val context = LocalContext.current
    val player = remember { LetterPlayer(context.applicationContext) }
    var transcript by remember { mutableStateOf("") }

    DisposableEffect(Unit) {
        onDispose { player.stop() }
    }

    ScreenFrame("녹음 확인") {
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Note("녹음한 목소리가 원본 그대로 전달됩니다.")
                BigButton(
                    text = if (player.isPlaying) "■ 멈춤" else "▶ 다시 듣기",
                    onClick = { if (player.isPlaying) player.stop() else player.play(audioPath) },
                    primary = false,
                    modifier = Modifier.fillMaxWidth(),
                )
                BigButton(
                    text = "다시 녹음",
                    onClick = onRetake,
                    primary = false,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Column(Modifier.weight(1.4f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = transcript,
                    onValueChange = { transcript = it },
                    label = { Text("글로도 함께 전하기 (선택)") },
                    enabled = parked,
                    textStyle = TextStyle(fontSize = 22.sp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
                // TODO(Pleos SpeechToText SDK): 제공 상태 확인 후 자동 전사로 교체.
                Note("자동 전사는 아직 연동 전입니다. 이 MVP에서는 직접 입력한 글이 함께 전달됩니다.")
            }
        }
        Spacer(Modifier.height(16.dp))
        BigButton(
            text = "받을 사람 고르기 ›",
            onClick = {
                player.stop()
                onNext(transcript)
            },
            enabled = parked,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** ③ 수신자·전달 조건 선택 → ④ 예약. MVP의 전달 조건은 '다음 시동 때' 하나입니다. */
@Composable
fun ScheduleScreen(
    store: LetterStore,
    driver: Profile,
    onBack: () -> Unit,
    onSchedule: (String) -> Unit,
) {
    val candidates = store.profiles.filter { it.id != driver.id }
    var recipientId by remember { mutableStateOf<String?>(null) }

    ScreenFrame("누구에게 전할까요?", onBack = onBack) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (candidates.isEmpty()) {
                Note("받을 사람의 프로필이 없습니다. '운전자 바꾸기'에서 이 차를 함께 쓰는 사람의 프로필을 먼저 추가해 주세요.")
            }
            candidates.forEach { profile ->
                BigButton(
                    text = if (recipientId == profile.id) "✓ ${profile.name}" else profile.name,
                    onClick = { recipientId = profile.id },
                    primary = recipientId == profile.id,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            InfoCard {
                Text("전달 순간", fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text("받는 사람이 다음에 시동을 걸 때", fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Note("본인 확인을 마친 뒤에만 전달되고, 다른 사람이 먼저 타면 편지는 기다립니다. 전달 전에는 언제든 취소할 수 있습니다.")
            }
        }
        Spacer(Modifier.height(16.dp))
        BigButton(
            text = "예약하기",
            onClick = { recipientId?.let(onSchedule) },
            enabled = recipientId != null,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
