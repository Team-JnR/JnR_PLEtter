package com.jnr.pletter.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jnr.pletter.data.LetterStore
import com.jnr.pletter.data.Profile

/**
 * 시동 후 첫 화면. 차량만으로 사람을 추정하지 않고, 프로필 선택 + PIN으로 본인 확인을 거칩니다.
 */
@Composable
fun DriverScreen(store: LetterStore, parked: Boolean, onVerified: (Profile) -> Unit) {
    var selected by remember { mutableStateOf<Profile?>(null) }
    var adding by remember { mutableStateOf(false) }

    val target = selected
    when {
        adding -> AddProfileForm(
            onDone = { name, pin ->
                store.addProfile(name, pin)
                adding = false
            },
            onCancel = { adding = false },
        )
        target != null -> PinForm(
            profile = target,
            verify = { pin -> store.verifyPin(target, pin) },
            onVerified = { onVerified(target) },
            onCancel = { selected = null },
        )
        else -> ScreenFrame("누가 운전하시나요?") {
            Note("본인 확인을 마친 사람에게만 편지가 전달됩니다.")
            Spacer(Modifier.height(24.dp))
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (store.profiles.isEmpty()) {
                    Note("아직 프로필이 없습니다. 이 차를 함께 쓰는 사람의 프로필을 만들어 주세요.")
                }
                store.profiles.forEach { profile ->
                    BigButton(
                        text = profile.name,
                        onClick = { selected = profile },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                BigButton(
                    text = "+ 프로필 추가",
                    onClick = { adding = true },
                    enabled = parked,
                    primary = false,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (!parked) Note("프로필 추가는 주차 중에만 할 수 있습니다.")
            }
        }
    }
}

@Composable
private fun PinForm(
    profile: Profile,
    verify: (String) -> Boolean,
    onVerified: () -> Unit,
    onCancel: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var failed by remember { mutableStateOf(false) }

    ScreenFrame("${profile.name}님, PIN을 입력해 주세요", onBack = onCancel) {
        PinPad(
            pin = pin,
            onChange = { value ->
                failed = false
                pin = value
                if (value.length == 4) {
                    if (verify(value)) {
                        onVerified()
                    } else {
                        failed = true
                        pin = ""
                    }
                }
            },
        )
        if (failed) {
            Spacer(Modifier.height(12.dp))
            Note("PIN이 맞지 않습니다. 다시 입력해 주세요.", color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun AddProfileForm(onDone: (String, String) -> Unit, onCancel: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }

    ScreenFrame("프로필 추가", onBack = onCancel) {
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(40.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 12) name = it },
                    label = { Text("이름 (예: 엄마, 아빠)") },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 26.sp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Note("PIN 4자리를 오른쪽에서 정해 주세요. 편지를 받을 때 본인 확인에 씁니다.")
                BigButton(
                    text = "만들기",
                    onClick = { onDone(name, pin) },
                    enabled = name.isNotBlank() && pin.length == 4,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Column(Modifier.weight(1f)) {
                PinPad(pin = pin, onChange = { pin = it })
            }
        }
    }
}

/** IME 없이 쓰는 숫자 패드. 입력한 숫자는 화면에 드러내지 않습니다. */
@Composable
fun PinPad(pin: String, onChange: (String) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "●".repeat(pin.length) + "○".repeat(4 - pin.length),
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 12.sp,
        )
        Spacer(Modifier.height(16.dp))
        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("", "0", "지움"),
        )
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { key ->
                    if (key.isEmpty()) {
                        Spacer(Modifier.size(width = 120.dp, height = 68.dp))
                    } else {
                        Button(
                            onClick = {
                                if (key == "지움") {
                                    onChange(pin.dropLast(1))
                                } else if (pin.length < 4) {
                                    onChange(pin + key)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                            ),
                            modifier = Modifier.size(width = 120.dp, height = 68.dp),
                        ) {
                            Text(key, fontSize = 24.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
