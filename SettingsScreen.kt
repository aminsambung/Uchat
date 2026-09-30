package com.example.uchat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

data class SettingItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ChatViewModel,
    onBack: () -> Unit,
    onOpenProfile: () -> Unit,
    onSignOut: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()

    val settingItems = listOf(
        SettingItem("Langganan", "Jelajahi keuntungan premium", Icons.Outlined.Star),
        SettingItem("Perangkat tertaut", "Gunakan Uchat di perangkat lain", Icons.Outlined.Devices),
        SettingItem("Akun", "Notifikasi keamanan, ganti nomor", Icons.Outlined.VpnKey),
        SettingItem("Privasi", "Akun diblokir, pesan sementara", Icons.Outlined.Lock),
        SettingItem("Daftar", "Kelola orang dan grup", Icons.Outlined.Contacts),
        SettingItem("Chat", "Riwayat obrolan, cadangan", Icons.Outlined.Chat),
        SettingItem("Tampilan", "Tema obrolan, ikon aplikasi", Icons.Outlined.Palette),
        SettingItem("Notifikasi", "Pesan, grup & nada dering", Icons.Outlined.Notifications),
        SettingItem("Penyimpanan dan data", "Penggunaan jaringan, unduh otomatis", Icons.Outlined.Storage),
        SettingItem("Aksesibilitas", "Tingkatkan kontras, animasi", Icons.Outlined.Accessibility),
        SettingItem("Bahasa Aplikasi", "Indonesia (bahasa perangkat)", Icons.Outlined.Language),
        SettingItem("Bantuan dan masukan", "Pusat Bantuan, hubungi kami", Icons.Outlined.HelpOutline),
        SettingItem("Undang teman", "", Icons.Outlined.PersonAdd),
        SettingItem("Pembaruan aplikasi", "", Icons.Outlined.SystemUpdate)
    )

    Scaffold(
        containerColor = WA_Bg,
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan", color = WA_TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = WA_TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = {}) { Icon(Icons.Default.Search, "Cari", tint = WA_TextPrimary) }
                    IconButton(onClick = {}) { Icon(Icons.Default.QrCodeScanner, "QR", tint = WA_TextPrimary) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WA_Dark)
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                ProfileHeader(user = currentUser, onProfileClick = onOpenProfile)
            }

            items(settingItems) { item ->
                SettingsRow(item)
            }

            item {
                HorizontalDivider(Modifier.padding(vertical = 8.dp), color = WA_Dark)
            }

            item {
                SettingsRow(
                    SettingItem(
                        "Pusat Akun",
                        "Kendalikan pengalaman Anda di Uchat,\nFacebook, Instagram, dan lainnya.",
                        Icons.Outlined.AllInclusive
                    )
                )
            }

            item {
                HorizontalDivider(Modifier.padding(vertical = 8.dp), color = WA_Dark)
            }

            item {
                Row(
                    Modifier.fillMaxWidth().clickable(onClick = onSignOut).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Logout, null, tint = Color(0xFFFF5252))
                    Spacer(Modifier.width(20.dp))
                    Text("Keluar", color = Color(0xFFFF5252), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun ProfileHeader(user: User?, onProfileClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clickable(onClick = onProfileClick).padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(100.dp).clip(CircleShape).background(Color.Gray),
            contentAlignment = Alignment.Center
        ) {
            if (!user?.photoUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = user!!.photoUrl, contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Default.Person, null, modifier = Modifier.size(60.dp), tint = Color.White)
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            user?.displayName ?: "Pengguna",
            color = WA_TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Normal
        )
        Text(
            "@${user?.username ?: "-"}",
            color = WA_TextSecondary, fontSize = 14.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            user?.about ?: "",
            color = WA_TextSecondary, fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}

@Composable
fun SettingsRow(item: SettingItem) {
    Row(
        Modifier.fillMaxWidth().clickable {}.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(item.icon, null, tint = WA_TextSecondary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(20.dp))
        Column(Modifier.weight(1f)) {
            Text(item.title, color = WA_TextPrimary, fontSize = 16.sp)
            if (item.subtitle.isNotEmpty()) {
                Text(item.subtitle, color = WA_TextSecondary, fontSize = 13.sp, lineHeight = 16.sp)
            }
        }
    }
}
