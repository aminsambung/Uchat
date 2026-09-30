package com.example.uchat

// ============================================
//              MODEL DATA
// ============================================

data class User(
    val uid: String = "",
    val email: String = "",
    val username: String = "",
    val pin: String = "",
    val displayName: String = "",
    val about: String = "Hai! Saya pakai Uchat",
    val photoUrl: String = "",
    val fcmToken: String = "",
    val createdAt: Long = 0L,
    val lastSeen: Long = 0L
)

data class Message(
    val id: String = "",
    val text: String = "",
    val imageUrl: String = "",
    val type: String = "text",      // "text" | "image"
    val sender: String = "",
    val timestamp: Long = 0L,
    val status: String = "sent",    // "sent" | "read"
    val deletedFor: List<String> = emptyList()
)

data class ChatRoom(
    val chatId: String = "",
    val otherUid: String = "",
    val otherEmail: String = "",
    val otherName: String = "",
    val otherUsername: String = "",
    val otherPhoto: String = "",
    val lastMessage: String = "",
    val lastMessageTime: Long = 0L,
    val unreadCount: Int = 0,
    val isOtherTyping: Boolean = false
)

// ============================================
//         WARNA GLOBAL (tema WhatsApp)
// ============================================

val WA_Bg = androidx.compose.ui.graphics.Color(0xFF0B141A)
val WA_Green = androidx.compose.ui.graphics.Color(0xFF25D366)
val WA_TextPrimary = androidx.compose.ui.graphics.Color(0xFFE9EDEF)
val WA_TextSecondary = androidx.compose.ui.graphics.Color(0xFF8696A0)
val WA_Dark = androidx.compose.ui.graphics.Color(0xFF1F2C34)
val WA_SentBubble = androidx.compose.ui.graphics.Color(0xFF005C4B)
val WA_ReceivedBubble = androidx.compose.ui.graphics.Color(0xFF1F2C34)
val WA_BlueTick = androidx.compose.ui.graphics.Color(0xFF53BDEB)
