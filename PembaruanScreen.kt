package com.example.uchat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PembaruanScreen(
    statusViewModel: StatusViewModel,
    onOpenStatusViewer: (UserStatusGroup) -> Unit,
    onOpenMyStatusViewer: (UserStatusGroup) -> Unit,
    onOpenAddStatus: () -> Unit
) {
    val groups by statusViewModel.statusGroups.collectAsState()
    val myStatuses by statusViewModel.myStatuses.collectAsState()
    val currentUser by statusViewModel.myStatuses.collectAsState() // fallback
    val userPhoto = myStatuses.firstOrNull()?.userPhoto ?: ""

    Scaffold(
        containerColor = WA_Bg,
        topBar = {
            TopAppBar(
                title = { Text("Pembaruan", color = WA_TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = {}) { Icon(Icons.Default.Search, "Cari", tint = WA_TextPrimary) }
                    IconButton(onClick = {}) { Icon(Icons.Default.MoreVert, "Menu", tint = WA_TextPrimary) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WA_Bg)
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SmallFloatingActionButton(
                    onClick = onOpenAddStatus,
                    containerColor = WA_Dark,
                    contentColor = WA_TextPrimary
                ) { Icon(Icons.Outlined.Edit, "Tulis Status") }

                FloatingActionButton(
                    onClick = onOpenAddStatus,
                    containerColor = WA_Green,
                    contentColor = Color.Black
                ) { Icon(Icons.Default.CameraAlt, "Kamera") }
            }
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                Text(
                    "Status",
                    color = WA_TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                Row(
                    Modifier.fillMaxWidth().clickable {
                        if (myStatuses.isNotEmpty()) {
                            onOpenMyStatusViewer(
                                UserStatusGroup(
                                    uid = myStatuses.first().uid,
                                    userName = "Status Saya",
                                    userPhoto = myStatuses.first().userPhoto,
                                    statuses = myStatuses,
                                    latestTime = myStatuses.maxOf { it.createdAt },
                                    hasUnviewed = false
                                )
                            )
                        } else {
                            onOpenAddStatus()
                        }
                    }.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                            if (myStatuses.isNotEmpty()) {
                                Box(Modifier.matchParentSize().border(2.dp, WA_Green, CircleShape))
                            }
                            Box(
                                Modifier.size(48.dp).clip(CircleShape).background(Color.Gray),
                                contentAlignment = Alignment.Center
                            ) {
                                if (userPhoto.isNotEmpty()) {
                                    AsyncImage(
                                        model = userPhoto, contentDescription = null,
                                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(28.dp))
                                }
                            }
                        }

                        if (myStatuses.isEmpty()) {
                            Box(
                                Modifier.size(22.dp).clip(CircleShape).background(WA_Green)
                                    .border(2.dp, WA_Bg, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Add, "Tambah", tint = Color.Black, modifier = Modifier.size(14.dp))
                            }
                        }
                    }

                    Spacer(Modifier.width(16.dp))

                    Column {
                        Text(
                            if (myStatuses.isEmpty()) "Tambah Status" else "Status Saya",
                            color = WA_TextPrimary, fontSize = 16.sp
                        )
                        Text(
                            if (myStatuses.isEmpty()) "Akan hilang setelah 24 jam"
                            else "${myStatuses.size} pembaruan • Ketuk untuk lihat",
                            color = WA_TextSecondary, fontSize = 13.sp
                        )
                    }
                }
            }

            if (groups.isNotEmpty()) {
                item {
                    Text(
                        "Pembaruan terkini",
                        color = WA_TextSecondary, fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
                items(groups, key = { it.uid }) { group ->
                    StatusRow(group) { onOpenStatusViewer(group) }
                }
            } else {
                item {
                    Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Update, null, tint = WA_TextSecondary, modifier = Modifier.size(60.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("Belum ada pembaruan terbaru", color = WA_TextPrimary, fontSize = 16.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Status dari teman Anda akan muncul di sini",
                                color = WA_TextSecondary, fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusRow(group: UserStatusGroup, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier.matchParentSize().border(
                    width = 2.dp,
                    color = if (group.hasUnviewed) WA_Green else Color.Gray,
                    shape = CircleShape
                )
            )
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(Color.DarkGray),
                contentAlignment = Alignment.Center
            ) {
                if (group.userPhoto.isNotEmpty()) {
                    AsyncImage(
                        model = group.userPhoto, contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Person, null, tint = Color.LightGray, modifier = Modifier.size(28.dp))
                }
            }
        }

        Spacer(Modifier.width(16.dp))

        Column(Modifier.weight(1f)) {
            Text(group.userName, color = WA_TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text(formatRelativeTime(group.latestTime), color = WA_TextSecondary, fontSize = 14.sp)
        }
    }
}

private fun formatRelativeTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val minutes = diff / 60000
    val hours = diff / 3600000
    return when {
        minutes < 1 -> "Baru saja"
        minutes < 60 -> "$minutes menit yang lalu"
        hours < 24 -> "$hours jam yang lalu"
        else -> SimpleDateFormat("HH.mm", Locale.getDefault()).format(Date(timestamp))
    }
}
