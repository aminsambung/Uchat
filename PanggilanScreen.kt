package com.example.uchat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class CallLog(
    val name: String,
    val time: String,
    val isMissed: Boolean = false,
    val isOutgoing: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanggilanScreen() {
    val callLogs = listOf(
        CallLog("Eka Duta", "Hari ini, 20.15", isOutgoing = true),
        CallLog("Lek Andi", "Hari ini, 18.42", isMissed = true),
        CallLog("Aminsambung", "Kemarin, 21.30", isOutgoing = true),
        CallLog("Rent Dslr Dmk", "Kemarin, 14.20", isMissed = true),
        CallLog("Muna Zahra", "26/09/26, 10.05", isOutgoing = true),
        CallLog("Zulfa", "25/09/26, 19.48", isMissed = true)
    )

    Scaffold(
        containerColor = WA_Bg,
        topBar = {
            TopAppBar(
                title = {
                    Text("Panggilan", color = WA_TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                },
                actions = {
                    IconButton(onClick = {}) { Icon(Icons.Default.Search, "Cari", tint = WA_TextPrimary) }
                    IconButton(onClick = {}) { Icon(Icons.Default.MoreVert, "Menu", tint = WA_TextPrimary) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WA_Bg)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {},
                containerColor = WA_Green,
                contentColor = Color.Black
            ) { Icon(Icons.Default.AddCall, "Panggilan Baru") }
        }
    ) { padding ->
        if (callLogs.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Phone, null, tint = WA_TextSecondary, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("Belum ada panggilan", color = WA_TextPrimary, fontSize = 18.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Mulai panggilan dengan menekan tombol +",
                        color = WA_TextSecondary, fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                item {
                    Text(
                        "Terbaru", color = WA_TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
                items(callLogs) { log -> CallLogRow(log) }
            }
        }
    }
}

@Composable
fun CallLogRow(log: CallLog) {
    Row(
        Modifier.fillMaxWidth().clickable {}.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(52.dp).clip(CircleShape).background(Color.DarkGray),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Person, null, tint = Color.LightGray, modifier = Modifier.size(30.dp))
        }

        Spacer(Modifier.width(16.dp))

        Column(Modifier.weight(1f)) {
            Text(
                log.name,
                color = if (log.isMissed) Color(0xFFFF5252) else WA_TextPrimary,
                fontSize = 16.sp, fontWeight = FontWeight.Medium
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when {
                        log.isMissed -> Icons.Default.CallMissed
                        log.isOutgoing -> Icons.Default.CallMade
                        else -> Icons.Default.CallReceived
                    },
                    contentDescription = null,
                    tint = if (log.isMissed) Color(0xFFFF5252) else WA_TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(log.time, color = WA_TextSecondary, fontSize = 13.sp)
            }
        }

        IconButton(onClick = {}) {
            Icon(Icons.Default.Call, "Panggil", tint = WA_Green)
        }
    }
}
