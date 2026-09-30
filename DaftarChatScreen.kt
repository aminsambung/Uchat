package com.example.uchat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DaftarChatScreen(
    chatRooms: List<ChatRoom>,
    onOpenChat: (ChatRoom) -> Unit,
    onOpenTambahTeman: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = WA_Bg,
        topBar = {
            TopAppBar(
                title = { Text("Uchat", color = WA_TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = onOpenTambahTeman) {
                        Icon(Icons.Default.PersonAdd, "Tambah Teman", tint = WA_TextPrimary)
                    }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, "Menu", tint = WA_TextPrimary)
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Pengaturan") },
                            onClick = { showMenu = false; onOpenSettings() },
                            leadingIcon = { Icon(Icons.Default.Settings, null) }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WA_Bg)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenTambahTeman,
                containerColor = WA_Green,
                contentColor = Color.Black
            ) { Icon(Icons.Default.Chat, "Chat Baru") }
        }
    ) { padding ->
        if (chatRooms.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ChatBubbleOutline, null, tint = WA_TextSecondary, modifier = Modifier.size(80.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("Belum ada chat", color = WA_TextPrimary, fontSize = 18.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Tekan tombol + untuk mulai chat", color = WA_TextSecondary, fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(chatRooms, key = { it.chatId }) { room ->
                    ChatRoomRow(room) { onOpenChat(room) }
                    HorizontalDivider(color = WA_Dark, thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
fun ChatRoomRow(room: ChatRoom, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(52.dp).clip(CircleShape).background(Color.DarkGray),
            contentAlignment = Alignment.Center
        ) {
            if (room.otherPhoto.isNotEmpty()) {
                AsyncImage(
                    model = room.otherPhoto, contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Default.Person, null, tint = Color.LightGray, modifier = Modifier.size(30.dp))
            }
        }

        Spacer(Modifier.width(16.dp))

        Column(Modifier.weight(1f)) {
            Text(room.otherName, color = WA_TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            if (room.isOtherTyping) {
                Text("sedang mengetik...", color = WA_Green, fontSize = 14.sp, fontStyle = FontStyle.Italic, maxLines = 1)
            } else {
                Text(
                    room.lastMessage.ifEmpty { "Mulai obrolan..." },
                    color = WA_TextSecondary, fontSize = 14.sp, maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                formatTime(room.lastMessageTime),
                color = if (room.unreadCount > 0) WA_Green else WA_TextSecondary,
                fontSize = 12.sp
            )
            if (room.unreadCount > 0) {
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier.size(22.dp).clip(CircleShape).background(WA_Green),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (room.unreadCount > 99) "99+" else room.unreadCount.toString(),
                        color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    if (timestamp == 0L) return ""
    val now = Calendar.getInstance()
    val msgTime = Calendar.getInstance().apply { timeInMillis = timestamp }
    return when {
        now.get(Calendar.DATE) == msgTime.get(Calendar.DATE) ->
            SimpleDateFormat("HH.mm", Locale.getDefault()).format(Date(timestamp))
        else -> SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(timestamp))
    }
}
