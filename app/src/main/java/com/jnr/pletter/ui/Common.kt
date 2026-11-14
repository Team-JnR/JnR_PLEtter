package com.jnr.pletter.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val PletterColors = darkColorScheme(
    primary = Color(0xFFFFB68A),
    onPrimary = Color(0xFF3A1D0B),
    background = Color(0xFF14110F),
    onBackground = Color(0xFFF3ECE6),
    surface = Color(0xFF211C19),
    onSurface = Color(0xFFF3ECE6),
    surfaceVariant = Color(0xFF2E2723),
    onSurfaceVariant = Color(0xFFCFC3BA),
    error = Color(0xFFFF8A80),
)

@Composable
fun PletterTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = PletterColors, content = content)
}

/** 차량 화면용 큰 버튼. */
@Composable
fun BigButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    primary: Boolean = true,
) {
    val colors = if (primary) {
        ButtonDefaults.buttonColors()
    } else {
        ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface,
        )
    }
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = colors,
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp),
        modifier = modifier.heightIn(min = 76.dp),
    ) {
        Text(text, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun ScreenFrame(
    title: String,
    onBack: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp, vertical = 24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                BigButton("‹ 뒤로", onBack, primary = false)
                Spacer(Modifier.width(24.dp))
            }
            Text(title, fontSize = 34.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(24.dp))
        content()
    }
}

@Composable
fun Note(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(text, fontSize = 20.sp, lineHeight = 28.sp, color = color, modifier = modifier)
}

@Composable
fun InfoCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(24.dp), content = content)
    }
}

fun formatTime(millis: Long): String =
    SimpleDateFormat("M월 d일 HH:mm", Locale.KOREA).format(Date(millis))
