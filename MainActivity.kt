package com.example.uchat

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Minta izin notifikasi untuk Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101
                )
            }
        }

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = WA_Bg
                ) {
                    UchatApp()
                }
            }
        }
    }
}

// ============================================
//           DATA CLASS TAB BAWAH
// ============================================

data class BottomTab(
    val title: String,
    val iconSelected: ImageVector,
    val iconUnselected: ImageVector
)

// ============================================
//          COMPOSABLE UTAMA
// ============================================

@Composable
fun UchatApp(
    chatViewModel: ChatViewModel = viewModel(),
    statusViewModel: StatusViewModel = viewModel()
) {
    val userEmail by chatViewModel.userEmail.collectAsStateSafe()

    // ==== STATE NAVIGASI GLOBAL ====
    var showTambahTeman by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showMyProfile by remember { mutableStateOf(false) }
    var showEditProfile by remember { mutableStateOf(false) }
    var showAddStatus by remember { mutableStateOf(false) }
    var viewingStatusGroup by remember { mutableStateOf<UserStatusGroup?>(null) }
    var isViewingMyStatus by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

    // ==== STATE DATA ====
    val error by chatViewModel.error.collectAsStateSafe()
    val chatRooms by chatViewModel.chatRooms.collectAsStateSafe()
    val messages by chatViewModel.messages.collectAsStateSafe()
    val isUploading by chatViewModel.isUploading.collectAsStateSafe()
    val activePartner by chatViewModel.activeChatPartner.collectAsStateSafe()
    val isPartnerTyping by chatViewModel.isPartnerTyping.collectAsStateSafe()

    // Minta izin POST_NOTIFICATIONS di dalam compose (opsional, sudah di onCreate)
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* granted or not, no action */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    if (userEmail == null) {
        // ============ LAYAR LOGIN / REGISTER ============
        AuthScreen(
            error = error,
            viewModel = chatViewModel,
            onSignIn = { e, p -> chatViewModel.signIn(e, p) }
        )
    } else {
        // ============ APLIKASI UTAMA ============
        when {
            // 1. Chat aktif (private chat)
            activePartner != null -> {
                ActiveChatScreen(
                    partner = activePartner!!,
                    userEmail = userEmail!!,
                    messages = messages,
                    isUploading = isUploading,
                    isPartnerTyping = isPartnerTyping,
                    onSend = {
                        chatViewModel.sendMessage(it)
                        chatViewModel.stopTyping()
                    },
                    onSendImage = { chatViewModel.sendImage(it) },
                    onDeleteMessage = { id, forAll ->
                        chatViewModel.deleteMessage(id, forAll)
                    },
                    onUserTyping = { chatViewModel.onUserTyping() },
                    onBack = { chatViewModel.closeActiveChat() }
                )
            }

            // 2. Tambah Teman
            showTambahTeman -> {
                TambahTemanScreen(
                    viewModel = chatViewModel,
                    onBack = { showTambahTeman = false },
                    onChatWithUser = { user ->
                        showTambahTeman = false
                        chatViewModel.openChatWith(user)
                    }
                )
            }

            // 3. Edit Profil
            showEditProfile -> {
                EditProfileScreen(
                    viewModel = chatViewModel,
                    onBack = {
                        showEditProfile = false
                        showMyProfile = true
                    }
                )
            }

            // 4. Lihat Profil Sendiri
            showMyProfile -> {
                MyProfileScreen(
                    viewModel = chatViewModel,
                    onEditProfile = {
                        showMyProfile = false
                        showEditProfile = true
                    },
                    onBack = {
                        showMyProfile = false
                        showSettings = true
                    }
                )
            }

            // 5. Pengaturan
            showSettings -> {
                SettingsScreen(
                    viewModel = chatViewModel,
                    onBack = { showSettings = false },
                    onOpenProfile = {
                        showSettings = false
                        showMyProfile = true
                    },
                    onSignOut = { chatViewModel.signOut() }
                )
            }

            // 6. Tambah Status
            showAddStatus -> {
                AddStatusScreen(
                    viewModel = statusViewModel,
                    onBack = { showAddStatus = false }
                )
            }

            // 7. Lihat Status (viewer)
            viewingStatusGroup != null -> {
                StatusViewerScreen(
                    group = viewingStatusGroup!!,
                    isMyStatus = isViewingMyStatus,
                    onMarkViewed = { id -> statusViewModel.markAsViewed(id) },
                    onDeleteMyStatus = { id -> statusViewModel.deleteMyStatus(id) },
                    onClose = { viewingStatusGroup = null }
                )
            }

            // 8. Tab Utama (3 tab)
            else -> {
                val tabs = listOf(
                    BottomTab("Pesan", Icons.Filled.Chat, Icons.Outlined.Chat),
                    BottomTab("Pembaruan", Icons.Filled.Update, Icons.Outlined.Update),
                    BottomTab("Panggilan", Icons.Filled.Phone, Icons.Outlined.Phone)
                )

                Scaffold(
                    containerColor = WA_Bg,
                    bottomBar = {
                        NavigationBar(
                            containerColor = WA_Dark,
                            tonalElevation = 0.dp
                        ) {
                            tabs.forEachIndexed { index, tab ->
                                val isSelected = selectedTab == index
                                NavigationBarItem(
                                    icon = {
                                        Icon(
                                            imageVector = if (isSelected) tab.iconSelected else tab.iconUnselected,
                                            contentDescription = tab.title
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tab.title,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    selected = isSelected,
                                    onClick = { selectedTab = index },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color.Black,
                                        selectedTextColor = WA_Green,
                                        indicatorColor = WA_Green,
                                        unselectedIconColor = WA_TextSecondary,
                                        unselectedTextColor = WA_TextSecondary
                                    )
                                )
                            }
                        }
                    }
                ) { padding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    ) {
                        when (selectedTab) {
                            0 -> DaftarChatScreen(
                                chatRooms = chatRooms,
                                onOpenChat = { room ->
                                    val partner = User(
                                        uid = room.otherUid,
                                        email = room.otherEmail,
                                        displayName = room.otherName,
                                        username = room.otherUsername,
                                        photoUrl = room.otherPhoto
                                    )
                                    chatViewModel.openChatWith(partner)
                                },
                                onOpenTambahTeman = { showTambahTeman = true },
                                onOpenSettings = { showSettings = true }
                            )
                            1 -> PembaruanScreen(
                                statusViewModel = statusViewModel,
                                onOpenStatusViewer = { group ->
                                    viewingStatusGroup = group
                                    isViewingMyStatus = false
                                },
                                onOpenMyStatusViewer = { group ->
                                    viewingStatusGroup = group
                                    isViewingMyStatus = true
                                },
                                onOpenAddStatus = { showAddStatus = true }
                            )
                            2 -> PanggilanScreen()
                        }
                    }
                }
            }
        }
    }
}

// ============================================
//     HELPER: collectAsState (safety wrapper)
// ============================================

@Composable
fun <T> kotlinx.coroutines.flow.StateFlow<T>.collectAsStateSafe() =
    androidx.lifecycle.compose.collectAsStateWithLifecycle(this)

@Composable
fun <T> androidx.lifecycle.compose.LocalLifecycleOwnerExt(): androidx.lifecycle.LifecycleOwner =
    androidx.lifecycle.compose.LocalLifecycleOwner.current

// ============================================
//     EXTENSION: agar bisa panggil collectAsStateSafe
// ============================================

private fun <T> kotlinx.coroutines.flow.StateFlow<T>.asState() =
    this

// ============================================
//     IMPORT TAMBAHAN (letakkan di atas file)
// ============================================

// import androidx.lifecycle.compose.collectAsStateWithLifecycle
// Jika belum bisa, ganti semua collectAsStateSafe() jadi collectAsState()
