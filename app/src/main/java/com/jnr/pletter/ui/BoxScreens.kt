package com.jnr.pletter.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jnr.pletter.audio.LetterPlayer
import com.jnr.pletter.data.LetterStatus
import com.jnr.pletter.data.LetterStore
import com.jnr.pletter.data.Profile

/** 내가 예약한 편지. 전달 전에는 취소할 수 있고, 상대의 열람 여부는 보여 주지 않습니다. */
@Composable
fun OutboxScreen(store: LetterStore, driver: Profile, onBack: () -> Unit) {
    val letters = store.outboxOf(driver.id)

    ScreenFrame("예약한 편지", onBack = onBack) {
        if (letters.isEmpty()) {
            Note("예약한 편지가 없습니다.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(letters, key = { it.id }) { letter ->
                    InfoCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "${store.profileName(letter.recipientId)}님에게",
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Spacer(Modifier.height(4.dp))
                                Note(
                                    when (letter.status) {
                                        LetterStatus.SCHEDULED ->
                                            "${formatTime(letter.createdAt)} 예약 · 다음 시동을 기다리는 중"
                                        LetterStatus.DELIVERED ->
                                            "${formatTime(letter.deliveredAt ?: letter.createdAt)} 전달됨"
                                    }
                                )
                            }
                            if (letter.status == LetterStatus.SCHEDULED) {
                                Spacer(Modifier.width(16.dp))
                                BigButton(
                                    "예약 취소",
                                    onClick = { store.deleteLetter(letter.id) },
                                    primary = false,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 받은 편지함. 목록에는 내용을 드러내지 않고, 주행 중에는 열람을 보류합니다. */
@Composable
fun InboxScreen(
    store: LetterStore,
    driver: Profile,
    parked: Boolean,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
) {
    val letters = store.inboxOf(driver.id)

    ScreenFrame("받은 편지함", onBack = onBack) {
        if (!parked) {
            Note(
                "주행 중에는 편지를 열 수 없어요. 안전하게 멈춘 뒤 열어 보세요.",
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(16.dp))
        }
        if (letters.isEmpty()) {
            Note("아직 받은 편지가 없습니다.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(letters, key = { it.id }) { letter ->
                    InfoCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "${store.profileName(letter.senderId)}님이 보낸 편지",
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Spacer(Modifier.height(4.dp))
                                Note("${formatTime(letter.deliveredAt ?: letter.createdAt)} 도착")
                            }
                            Spacer(Modifier.width(16.dp))
                            BigButton("열기", onClick = { onOpen(letter.id) }, enabled = parked)
                        }
                    }
                }
            }
        }
    }
}

/** ⑥ 수신자가 듣기·읽기를 직접 선택합니다. 화면에 들어와도 자동으로 재생하거나 글을 펼치지 않습니다. */
@Composable
fun LetterDetailScreen(store: LetterStore, letterId: String, parked: Boolean, onBack: () -> Unit) {
    val context = LocalContext.current
    val player = remember { LetterPlayer(context.applicationContext) }
    var showText by remember { mutableStateOf(false) }
    val letter = store.letters.firstOrNull { it.id == letterId }

    DisposableEffect(Unit) {
        onDispose { player.stop() }
    }

    // 열람 중 주행을 시작하면 재생을 멈추고 내용을 가립니다.
    LaunchedEffect(parked) {
        if (!parked) {
            player.stop()
            showText = false
        }
    }

    if (letter == null) {
        ScreenFrame("편지", onBack = onBack) { Note("편지를 찾을 수 없습니다.") }
        return
    }

    ScreenFrame("${store.profileName(letter.senderId)}님이 보낸 편지", onBack = onBack) {
        if (!parked) {
            Note(
                "주행 중에는 편지를 열 수 없어요. 안전하게 멈춘 뒤 이어서 열어 보세요.",
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            Note("${formatTime(letter.createdAt)}에 남긴 편지")
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                BigButton(
                    text = if (player.isPlaying) "■ 멈춤" else "▶ 목소리로 듣기",
                    onClick = {
                        if (player.isPlaying) player.stop() else player.play(letter.audioPath)
                    },
                    modifier = Modifier.weight(1f),
                )
                BigButton(
                    text = if (showText) "글 접기" else "글로 읽기",
                    onClick = { showText = !showText },
                    enabled = letter.transcript.isNotBlank(),
                    primary = false,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(20.dp))
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                if (showText) {
                    InfoCard {
                        Text(letter.transcript, fontSize = 26.sp, lineHeight = 38.sp)
                    }
                } else if (letter.transcript.isBlank()) {
                    Note("이 편지는 목소리로만 남겨졌습니다.")
                }
            }
            Spacer(Modifier.height(16.dp))
            BigButton(
                text = "편지 삭제",
                onClick = {
                    player.stop()
                    store.deleteLetter(letter.id)
                    onBack()
                },
                primary = false,
            )
        }
    }
}
