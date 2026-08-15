package com.goalnudge.app.overlay

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.goalnudge.app.domain.model.NudgeContent
import com.goalnudge.app.domain.model.Tone
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue

private data class ToneStyle(val accent: Color, val onAccent: Color)

private fun styleFor(tone: Tone): ToneStyle = when (tone) {
    Tone.LEMBUT -> ToneStyle(Color(0xFF5C9EAD), Color.White)
    Tone.NETRAL -> ToneStyle(Color(0xFF3F51B5), Color.White)
    Tone.KERAS -> ToneStyle(Color(0xFFD7263D), Color.White)
}

/**
 * Kartu nudge full-screen — PLAN.md §1: overlay ringan 3-5 detik, dismissible via tombol
 * atau swipe. Auto-timeout supaya tidak "nyangkut" kalau user mengabaikannya.
 */
@Composable
fun NudgeOverlayCard(
    content: NudgeContent,
    tone: Tone,
    autoDismissMillis: Long,
    onCheckIn: () -> Unit,
    onLater: () -> Unit,
    onSwipedAway: () -> Unit,
    onTimeout: () -> Unit
) {
    val style = styleFor(tone)
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var dismissed by remember { mutableStateOf(false) }
    val animatedOffset by animateFloatAsState(targetValue = dragOffset, label = "drag")

    LaunchedEffect(Unit) {
        delay(autoDismissMillis)
        if (!dismissed) {
            dismissed = true
            onTimeout()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (dragOffset.absoluteValue > 300f && !dismissed) {
                            dismissed = true
                            onSwipedAway()
                        } else {
                            dragOffset = 0f
                        }
                    },
                    onVerticalDrag = { _, dragAmount -> dragOffset += dragAmount }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .graphicsLayer { translationY = animatedOffset },
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Surface(color = style.accent, shape = RoundedCornerShape(8.dp)) {
                    Text(
                        text = content.title,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = style.onAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = content.body)
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = content.checkInLine, color = Color.Gray)
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { if (!dismissed) { dismissed = true; onCheckIn() } },
                        colors = ButtonDefaults.buttonColors(containerColor = style.accent),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Kerjakan sekarang")
                    }
                    OutlinedButton(
                        onClick = { if (!dismissed) { dismissed = true; onLater() } },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Nanti")
                    }
                }
            }
        }
    }
}
