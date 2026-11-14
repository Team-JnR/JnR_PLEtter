package com.jnr.pletter.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jnr.pletter.data.LetterStore
import com.jnr.pletter.data.Profile

@Composable
fun HomeScreen(
    store: LetterStore,
    driver: Profile,
    parked: Boolean,
    newlyArrived: Int,
    onRecord: () -> Unit,
    onInbox: () -> Unit,
    onOutbox: () -> Unit,
    onSwitchDriver: () -> Unit,
) {
    val inboxCount = store.inboxOf(driver.id).size

    ScreenFrame("${driver.name}님, 안녕하세요") {
        if (newlyArrived > 0) {
            // 전달과 재생의 분리: 도착만 알리고, 자동으로 읽거나 재생하지 않습니다.
            InfoCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "새 편지 ${newlyArrived}통이 도착했어요",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(4.dp))
                        Note(
                            if (parked) "편할 때 편지함에서 직접 열어 보세요."
                            else "주행 중에는 열 수 없어요. 안전하게 멈춘 뒤 열어 보세요."
                        )
                    }
                    Spacer(Modifier.width(24.dp))
                    BigButton("편지함", onInbox)
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            BigButton(
                text = "편지 남기기",
                onClick = onRecord,
                enabled = parked,
                modifier = Modifier
                    .weight(1f)
                    .height(140.dp),
            )
            BigButton(
                text = if (inboxCount > 0) "받은 편지함 ($inboxCount)" else "받은 편지함",
                onClick = onInbox,
                primary = false,
                modifier = Modifier
                    .weight(1f)
                    .height(140.dp),
            )
        }
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            BigButton(
                text = "예약한 편지",
                onClick = onOutbox,
                primary = false,
                modifier = Modifier.weight(1f),
            )
            BigButton(
                text = "운전자 바꾸기",
                onClick = onSwitchDriver,
                primary = false,
                modifier = Modifier.weight(1f),
            )
        }
        if (!parked) {
            Spacer(Modifier.height(16.dp))
            Note("주행 중에는 편지 남기기와 편지 열람이 보류됩니다.")
        }
    }
}
