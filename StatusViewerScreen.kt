package com.example.uchat

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

@Composable
fun StatusViewerScreen(
    group: UserStatusGroup,
    isMyStatus: Boolean,
    onMarkViewed: (String) -> Unit,
    onDeleteMyStatus: (String) -> Unit,
    onClose: () -> Unit
) {
    var currentIndex by remember { mutableIntStateOf(0) }
    var isPaused by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val currentStatus = group.statuses.getOrNull(currentIndex)
    val progress = remember { Animatable(0f) }

    val totalDuration = when (currentStatus?.type) {
        "video" -> currentStatus.videoDuration.coerceAtLeast(1000L)
        else -> 5000L
    }

    LaunchedEffect(currentIndex, isPaused) {
        if (isPaused || currentStatus == null) return@LaunchedEffect
        onMarkViewed(currentStatus.id)
        progress.snapTo(0f)

        if (currentStatus.type != "video") {
            val steps = 100
            val stepDuration = totalDuration / steps
            repeat(steps) {
                if (isPaused) return@LaunchedEffect
                delay(stepDuration)
                progress.snapTo((it + 1) / steps.toFloat())
            }
            if (currentIndex < group.statuses.size - 1) currentIndex++ else onClose()
        }
    }

    if (showDeleteConfirm && currentStatus != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = WA_Dark,
            title = { Text("Hapus status ini?", color = WA_TextPrimary) },
            text = { Text("Status akan dihapus permanen.", color = WA_TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteMyStatus(currentStatus.id)
                    showDeleteConfirm = false
                    if (group.statuses.size <= 1) onClose()
                    else if (currentIndex >= group.statuses.size - 1) currentIndex--
                }) { Text("Hapus", color = Color(0xFFFF5252)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Batal", color = WA_TextSecondary)
                }
            }
        )
    }

    Box(
        Modifier.fillMaxSize().background(Color.Black)
            .pointerInput(currentIndex) {
                detectTapGestures(
                    onTap = { offset ->
                        val screenWidth = size.width
                        if (offset.x < screenWidth / 3f) {
                            if (currentIndex > 0) currentIndex-- else onClose()
                        } else {
                            if (currentIndex < group.statuses.size - 1) currentIndex++ else onClose()
                        }
                    },
                    onPress = {
                        isPaused = true
                        tryAwaitRelease()
                        isPaused = false
                    }
                )
            }
    ) {
        if (currentStatus == null) {
            onClose()
            return@Box
        }

        when (currentStatus.type) {
            "video" -> {
                if (currentStatus.videoUrl.isNotEmpty()) {
                    VideoStatusPlayer(
                        url = currentStatus.videoUrl,
                        isPaused = isPaused,
                        onVideoEnd = {
                            if (currentIndex < group.statuses.size - 1) currentIndex++
                            else onClose()
                        }
                    )
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Video tidak tersedia", color = Color.White)
                    }
                }
            }
            "image" -> {
                if (currentStatus.imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = currentStatus.imageUrl, contentDescription = null,
                        modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit
                    )
                }
            }
            else -> {
                Box(
                    Modifier.fillMaxSize()
                        .background(Color(android.graphics.Color.parseColor(currentStatus.backgroundColor))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        currentStatus.text, color = Color.White,
                        fontSize = 28.sp, fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(32.dp)
                    )
                }
            }
        }

        Column(
            Modifier.fillMaxWidth().padding(top = 12.dp, start = 12.dp, end = 12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                group.statuses.forEachIndexed { index, _ ->
                    LinearProgressIndicator(
                        progress = {
                            when {
                                index < currentIndex -> 1f
                                index == currentIndex -> progress.value
                                else -> 0f
                            }
                        },
                        modifier = Modifier.weight(1f).height(3.dp).clip(RoundedCornerShape(2.dp)),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.3f)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(Color.Gray),
                    contentAlignment = Alignment.Center
                ) {
                    if (group.userPhoto.isNotEmpty()) {
                        AsyncImage(
                            model = group.userPhoto, contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(group.userName, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(
                        formatTimeAgo(currentStatus.createdAt),
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }

                if (isMyStatus) {
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Default.Delete, "Hapus", tint = Color.White)
                    }
                }

                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, "Tutup", tint = Color.White)
                }
            }
        }
    }
}

private fun formatTimeAgo(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val minutes = diff / 60000
    val hours = diff / 3600000
    return when {
        minutes < 1 -> "Baru saja"
        minutes < 60 -> "$minutes menit yang lalu"
        hours < 24 -> "$hours jam yang lalu"
        else -> "Lebih dari sehari"
    }
}
