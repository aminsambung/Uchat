package com.example.uchat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ActiveChatScreen(
    partner: User,
    userEmail: String,
    messages: List<Message>,
    isUploading: Boolean,
    isPartnerTyping: Boolean,
    onSend: (String) -> Unit,
    onSendImage: (Uri) -> Unit,
    onDeleteMessage: (String, Boolean) -> Unit,
    onUserTyping: () -> Unit,
    onBack: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    var messageToDelete by remember { mutableStateOf<Message?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> if (uri != null) onSendImage(uri) }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    if (showDeleteDialog && messageToDelete != null) {
        val msg = messageToDelete!!
        val isMe = msg.sender == userEmail
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = WA_Dark,
            title = { Text("Hapus pesan?", color = WA_TextPrimary) },
            text = { Text("Pilih cara menghapus pesan ini.", color = WA_TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteMessage(msg.id, isMe)
                    showDeleteDialog = false
                }) {
                    Text(
                        if (isMe) "Hapus untuk semua" else "Hapus untuk saya",
                        color = Color(0xFFFF5252)
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Batal", color = WA_TextSecondary)
                }
            }
        )
    }

    Column(Modifier.fillMaxSize().background(WA_Bg)) {
        // ===== TOP BAR =====
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(Color.DarkGray),
                        contentAlignment = Alignment.Center
                    ) {
                        if (partner.photoUrl.isNotEmpty()) {
                            AsyncImage(
                                model = partner.photoUrl, contentDescription = null,
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Default.Person, null, tint = Color.LightGray, modifier = Modifier.size(24.dp))
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(partner.displayName, color = WA_TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Text(
                            text = if (isPartnerTyping) "sedang mengetik..." else "@${partner.username}",
                            color = if (isPartnerTyping) WA_Green else WA_TextSecondary,
                            fontSize = 12.sp,
                            fontStyle = if (isPartnerTyping) FontStyle.Italic else FontStyle.Normal
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = WA_TextPrimary)
                }
            },
            actions = {
                IconButton(onClick = {}) { Icon(Icons.Default.Call, "Telepon", tint = WA_TextPrimary) }
                IconButton(onClick = {}) { Icon(Icons.Default.Videocam, "Video", tint = WA_TextPrimary) }
                IconButton(onClick = {}) { Icon(Icons.Default.MoreVert, "Menu", tint = WA_TextPrimary) }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = WA_Dark)
        )

        // ===== DAFTAR PESAN =====
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                val isMe = msg.sender == userEmail
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isMe) WA_SentBubble else WA_ReceivedBubble
                        ),
                        modifier = Modifier.combinedClickable(
                            onClick = {},
                            onLongClick = {
                                messageToDelete = msg
                                showDeleteDialog = true
                            }
                        )
                    ) {
                        Column(Modifier.padding(8.dp)) {
                            if (msg.type == "image" && msg.imageUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = msg.imageUrl, contentDescription = "Gambar",
                                    modifier = Modifier.width(200.dp).height(200.dp).clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(msg.text, color = WA_TextPrimary, style = MaterialTheme.typography.bodyLarge)
                            }

                            Spacer(Modifier.height(4.dp))
                            Row(
                                Modifier.align(Alignment.End),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(formatTimeShort(msg.timestamp), color = WA_TextSecondary, fontSize = 10.sp)
                                if (isMe) {
                                    Spacer(Modifier.width(4.dp))
                                    Icon(
                                        imageVector = if (msg.status == "read") Icons.Default.DoneAll else Icons.Default.Done,
                                        contentDescription = "Status",
                                        tint = if (msg.status == "read") WA_BlueTick else WA_TextSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (isUploading) {
            LinearProgressIndicator(Modifier.fillMaxWidth(), color = WA_Green, trackColor = WA_Bg)
        }

        // ===== INPUT BAR =====
        Row(
            Modifier.fillMaxWidth().padding(8.dp).background(WA_Bg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { imagePicker.launch("image/*") }, enabled = !isUploading) {
                Icon(Icons.Default.AttachFile, "Gambar", tint = WA_TextSecondary)
            }

            OutlinedTextField(
                value = input,
                onValueChange = { input = it; onUserTyping() },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Tulis pesan...", color = WA_TextSecondary) },
                enabled = !isUploading,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = WA_TextPrimary,
                    unfocusedTextColor = WA_TextPrimary,
                    focusedBorderColor = WA_Green,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = WA_Dark,
                    unfocusedContainerColor = WA_Dark,
                    cursorColor = WA_Green
                )
            )
            Spacer(Modifier.width(8.dp))
            FloatingActionButton(
                onClick = { onSend(input); input = "" },
                containerColor = WA_Green,
                contentColor = Color.Black,
                modifier = Modifier.size(48.dp)
            ) { Icon(Icons.Default.Send, "Kirim") }
        }
    }
}

private fun formatTimeShort(timestamp: Long): String {
    if (timestamp == 0L) return ""
    return SimpleDateFormat("HH.mm", Locale.getDefault()).format(Date(timestamp))
}
