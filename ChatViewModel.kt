package com.example.uchat

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ChatViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val usersRef = db.collection("users")
    private val chatsRef = db.collection("chats")

    // ============ STATE ============
    private val _userEmail = MutableStateFlow<String?>(null)
    val userEmail: StateFlow<String?> = _userEmail.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val _isUpdatingProfile = MutableStateFlow(false)
    val isUpdatingProfile: StateFlow<Boolean> = _isUpdatingProfile.asStateFlow()

    private val _profileUpdateSuccess = MutableStateFlow(false)
    val profileUpdateSuccess: StateFlow<Boolean> = _profileUpdateSuccess.asStateFlow()

    private val _chatRooms = MutableStateFlow<List<ChatRoom>>(emptyList())
    val chatRooms: StateFlow<List<ChatRoom>> = _chatRooms.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _activeChatPartner = MutableStateFlow<User?>(null)
    val activeChatPartner: StateFlow<User?> = _activeChatPartner.asStateFlow()

    private val _isPartnerTyping = MutableStateFlow(false)
    val isPartnerTyping: StateFlow<Boolean> = _isPartnerTyping.asStateFlow()

    // ============ LISTENERS ============
    private var messagesListener: ListenerRegistration? = null
    private var chatRoomsListener: ListenerRegistration? = null
    private var chatDocListener: ListenerRegistration? = null
    private var userDataListener: ListenerRegistration? = null

    private var typingJob: Job? = null
    private var activeChatId: String? = null

    init {
        auth.addAuthStateListener { firebaseAuth ->
            _userEmail.value = firebaseAuth.currentUser?.email
            firebaseAuth.currentUser?.uid?.let { uid ->
                loadUserData(uid)
                listenChatRooms(uid)
                saveFcmToken(uid)
            } ?: run {
                _currentUser.value = null
                _chatRooms.value = emptyList()
            }
        }
    }

    // ============================================
    //          BAGIAN 1: AUTENTIKASI
    // ============================================

    private fun generatePin(): String {
        val chars = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
        return (1..8).map { chars.random() }.joinToString("")
    }

    suspend fun isUsernameAvailable(username: String): Boolean {
        return try {
            val result = usersRef.whereEqualTo("username", username.lowercase().trim())
                .limit(1).get().await()
            result.isEmpty
        } catch (e: Exception) { false }
    }

    private suspend fun isPinUnique(pin: String): Boolean {
        val result = usersRef.whereEqualTo("pin", pin).limit(1).get().await()
        return result.isEmpty
    }

    private suspend fun generateUniquePin(): String {
        var pin: String
        var attempts = 0
        do {
            pin = generatePin()
            attempts++
            if (attempts > 10) break
        } while (!isPinUnique(pin))
        return pin
    }

    fun signUp(
        email: String, password: String, displayName: String, username: String,
        onSuccess: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                if (username.length < 3) {
                    _error.value = "Username minimal 3 karakter"
                    return@launch
                }
                if (!username.matches(Regex("^[a-z0-9_]+$"))) {
                    _error.value = "Username hanya boleh huruf kecil, angka, dan underscore"
                    return@launch
                }
                if (displayName.isBlank()) {
                    _error.value = "Nama tidak boleh kosong"
                    return@launch
                }

                val cleanUsername = username.lowercase().trim()
                if (!isUsernameAvailable(cleanUsername)) {
                    _error.value = "Username @$cleanUsername sudah dipakai"
                    return@launch
                }

                val authResult = auth.createUserWithEmailAndPassword(email.trim(), password).await()
                val uid = authResult.user?.uid ?: run {
                    _error.value = "Gagal membuat akun"
                    return@launch
                }

                val pin = generateUniquePin()
                val fcmToken = try { FirebaseMessaging.getInstance().token.await() } catch (e: Exception) { "" }

                val newUser = User(
                    uid = uid, email = email.trim(), username = cleanUsername, pin = pin,
                    displayName = displayName.trim(), fcmToken = fcmToken,
                    createdAt = System.currentTimeMillis(), lastSeen = System.currentTimeMillis()
                )

                usersRef.document(uid).set(newUser).await()
                db.collection("usernames").document(cleanUsername).set(mapOf("uid" to uid)).await()

                _error.value = null
                onSuccess(pin)
            } catch (e: Exception) {
                _error.value = e.message ?: "Terjadi kesalahan"
            }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            try {
                auth.signInWithEmailAndPassword(email.trim(), password).await()
                _error.value = null
            } catch (e: Exception) { _error.value = e.message }
        }
    }

    fun signOut() {
        messagesListener?.remove()
        chatRoomsListener?.remove()
        chatDocListener?.remove()
        userDataListener?.remove()
        auth.signOut()
    }

    private fun loadUserData(uid: String) {
        userDataListener?.remove()
        userDataListener = usersRef.document(uid).addSnapshotListener { snap, _ ->
            _currentUser.value = snap?.toObject(User::class.java)
        }
    }

    private fun saveFcmToken(uid: String) {
        viewModelScope.launch {
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                usersRef.document(uid).update("fcmToken", token)
            } catch (_: Exception) {}
        }
    }

    suspend fun findUserByPin(pin: String): User? = try {
        val r = usersRef.whereEqualTo("pin", pin.uppercase().trim()).limit(1).get().await()
        r.documents.firstOrNull()?.toObject(User::class.java)
    } catch (e: Exception) { null }

    suspend fun findUserByUsername(username: String): User? = try {
        val clean = username.lowercase().trim().removePrefix("@")
        val r = usersRef.whereEqualTo("username", clean).limit(1).get().await()
        r.documents.firstOrNull()?.toObject(User::class.java)
    } catch (e: Exception) { null }

    // ============================================
    //          BAGIAN 2: CHAT ROOMS
    // ============================================

    private fun buildChatId(uid1: String, uid2: String): String =
        listOf(uid1, uid2).sorted().joinToString("_")

    private fun listenChatRooms(myUid: String) {
        chatRoomsListener?.remove()
        chatRoomsListener = chatsRef
            .whereArrayContains("members", myUid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) { Log.e("Uchat", "listenChatRooms err", e); return@addSnapshotListener }
                viewModelScope.launch {
                    val rooms = mutableListOf<ChatRoom>()
                    snapshot?.documents?.forEach { doc ->
                        val members = doc.get("members") as? List<*> ?: return@forEach
                        val otherUid = members.firstOrNull { it != myUid } as? String ?: return@forEach

                        val otherUser = try {
                            usersRef.document(otherUid).get().await().toObject(User::class.java)
                        } catch (e: Exception) { null }

                        val unreadMap = doc.get("unreadCount") as? Map<*, *>
                        val myUnread = (unreadMap?.get(myUid) as? Long)?.toInt() ?: 0

                        val typingMap = doc.get("typing") as? Map<*, *>
                        val isOtherTyping = (typingMap?.get(otherUid) as? Boolean) ?: false

                        rooms.add(
                            ChatRoom(
                                chatId = doc.id,
                                otherUid = otherUid,
                                otherEmail = otherUser?.email ?: "",
                                otherName = otherUser?.displayName ?: "Pengguna",
                                otherUsername = otherUser?.username ?: "",
                                otherPhoto = otherUser?.photoUrl ?: "",
                                lastMessage = doc.getString("lastMessage") ?: "",
                                lastMessageTime = doc.getLong("lastMessageTime") ?: 0L,
                                unreadCount = myUnread,
                                isOtherTyping = isOtherTyping
                            )
                        )
                    }
                    _chatRooms.value = rooms.sortedByDescending { it.lastMessageTime }
                }
            }
    }

    fun openChatWith(otherUser: User) {
        val myUid = auth.currentUser?.uid ?: return
        val myEmail = auth.currentUser?.email ?: return
        val chatId = buildChatId(myUid, otherUser.uid)

        viewModelScope.launch {
            try {
                val existing = chatsRef.document(chatId).get().await()
                if (!existing.exists()) {
                    val newChat = hashMapOf(
                        "members" to listOf(myUid, otherUser.uid),
                        "memberEmails" to listOf(myEmail, otherUser.email),
                        "lastMessage" to "",
                        "lastMessageSender" to "",
                        "lastMessageTime" to System.currentTimeMillis(),
                        "createdAt" to System.currentTimeMillis(),
                        "unreadCount" to mapOf(myUid to 0L, otherUser.uid to 0L)
                    )
                    chatsRef.document(chatId).set(newChat).await()
                }

                _activeChatPartner.value = otherUser
                activeChatId = chatId
                listenMessages(chatId)
                listenChatDoc(chatId, otherUser.uid)
                markAllAsRead(chatId)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    private fun listenChatDoc(chatId: String, otherUid: String) {
        chatDocListener?.remove()
        chatDocListener = chatsRef.document(chatId).addSnapshotListener { snap, _ ->
            val typingMap = snap?.get("typing") as? Map<*, *>
            _isPartnerTyping.value = (typingMap?.get(otherUid) as? Boolean) ?: false
        }
    }

    // ============================================
    //          BAGIAN 3: PESAN
    // ============================================

    private fun listenMessages(chatId: String) {
        messagesListener?.remove()
        _messages.value = emptyList()
        val myUid = auth.currentUser?.uid ?: return

        messagesListener = chatsRef.document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) { Log.e("Uchat", "listenMessages err", e); return@addSnapshotListener }

                viewModelScope.launch {
                    val list = mutableListOf<Message>()
                    snapshot?.documents?.forEach { doc ->
                        val deletedFor = doc.get("deletedFor") as? List<*> ?: emptyList<String>()
                        if (deletedFor.contains(myUid)) return@forEach

                        list.add(
                            Message(
                                id = doc.id,
                                text = doc.getString("text") ?: "",
                                imageUrl = doc.getString("imageUrl") ?: "",
                                type = doc.getString("type") ?: "text",
                                sender = doc.getString("sender") ?: "",
                                timestamp = doc.getLong("timestamp") ?: 0L,
                                status = doc.getString("status") ?: "sent",
                                deletedFor = deletedFor.map { it as String }
                            )
                        )
                    }
                    _messages.value = list
                    markIncomingAsRead(chatId)
                }
            }
    }

    fun closeActiveChat() {
        val chatId = activeChatId
        val myUid = auth.currentUser?.uid
        if (chatId != null && myUid != null) {
            viewModelScope.launch {
                try { chatsRef.document(chatId).update("typing.$myUid", false).await() } catch (_: Exception) {}
            }
        }
        messagesListener?.remove()
        chatDocListener?.remove()
        messagesListener = null
        chatDocListener = null
        activeChatId = null
        _messages.value = emptyList()
        _activeChatPartner.value = null
        _isPartnerTyping.value = false
        typingJob?.cancel()
    }

    fun sendMessage(text: String) {
        val myEmail = auth.currentUser?.email ?: return
        val myUid = auth.currentUser?.uid ?: return
        val partner = _activeChatPartner.value ?: return
        if (text.isBlank()) return

        val chatId = buildChatId(myUid, partner.uid)

        viewModelScope.launch {
            try {
                val message = hashMapOf(
                    "text" to text.trim(),
                    "type" to "text",
                    "sender" to myEmail,
                    "timestamp" to System.currentTimeMillis(),
                    "status" to "sent",
                    "deletedFor" to emptyList<String>()
                )
                chatsRef.document(chatId).collection("messages").add(message).await()

                chatsRef.document(chatId).set(
                    hashMapOf<String, Any>(
                        "lastMessage" to text.trim(),
                        "lastMessageSender" to myEmail,
                        "lastMessageTime" to System.currentTimeMillis(),
                        "typing.$myUid" to false
                    ), SetOptions.merge()
                ).await()

                chatsRef.document(chatId).update(
                    "unreadCount.${partner.uid}", FieldValue.increment(1)
                ).await()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun sendImage(uri: Uri) {
        val myEmail = auth.currentUser?.email ?: return
        val myUid = auth.currentUser?.uid ?: return
        val partner = _activeChatPartner.value ?: return
        val chatId = buildChatId(myUid, partner.uid)

        viewModelScope.launch {
            _isUploading.value = true
            try {
                val fileName = "chat_images/${UUID.randomUUID()}.jpg"
                val imageRef = storage.reference.child(fileName)
                imageRef.putFile(uri).await()
                val downloadUrl = imageRef.downloadUrl.await().toString()

                val message = hashMapOf(
                    "imageUrl" to downloadUrl,
                    "type" to "image",
                    "sender" to myEmail,
                    "timestamp" to System.currentTimeMillis(),
                    "status" to "sent",
                    "deletedFor" to emptyList<String>()
                )
                chatsRef.document(chatId).collection("messages").add(message).await()

                chatsRef.document(chatId).set(
                    hashMapOf<String, Any>(
                        "lastMessage" to "📷 Foto",
                        "lastMessageSender" to myEmail,
                        "lastMessageTime" to System.currentTimeMillis(),
                        "typing.$myUid" to false
                    ), SetOptions.merge()
                ).await()

                chatsRef.document(chatId).update(
                    "unreadCount.${partner.uid}", FieldValue.increment(1)
                ).await()
            } catch (e: Exception) {
                _error.value = "Gagal kirim gambar: ${e.message}"
            } finally {
                _isUploading.value = false
            }
        }
    }

    fun deleteMessage(messageId: String, forEveryone: Boolean) {
        val myUid = auth.currentUser?.uid ?: return
        val chatId = activeChatId ?: return

        viewModelScope.launch {
            try {
                val msgRef = chatsRef.document(chatId).collection("messages").document(messageId)
                if (forEveryone) {
                    msgRef.update(
                        mapOf(
                            "text" to "🚫 Pesan ini telah dihapus",
                            "imageUrl" to "",
                            "type" to "text"
                        )
                    ).await()
                } else {
                    msgRef.update("deletedFor", FieldValue.arrayUnion(myUid)).await()
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    // ============================================
    //    BAGIAN 4: UNREAD / TYPING / READ RECEIPT
    // ============================================

    private suspend fun markIncomingAsRead(chatId: String) {
        val myUid = auth.currentUser?.uid ?: return
        val myEmail = auth.currentUser?.email ?: return
        try {
            val snapshot = chatsRef.document(chatId).collection("messages")
                .whereEqualTo("status", "sent")
                .get().await()

            snapshot.documents.forEach { doc ->
                val sender = doc.getString("sender") ?: ""
                if (sender != myEmail) {
                    doc.reference.update("status", "read")
                }
            }
            chatsRef.document(chatId).update("unreadCount.$myUid", 0L)
        } catch (_: Exception) {}
    }

    private fun markAllAsRead(chatId: String) {
        val myUid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try { chatsRef.document(chatId).update("unreadCount.$myUid", 0L) } catch (_: Exception) {}
        }
    }

    fun onUserTyping() {
        val myUid = auth.currentUser?.uid ?: return
        val chatId = activeChatId ?: return

        viewModelScope.launch {
            try { chatsRef.document(chatId).update("typing.$myUid", true) } catch (_: Exception) {}
        }

        typingJob?.cancel()
        typingJob = viewModelScope.launch {
            delay(1500)
            try { chatsRef.document(chatId).update("typing.$myUid", false) } catch (_: Exception) {}
        }
    }

    fun stopTyping() {
        val myUid = auth.currentUser?.uid ?: return
        val chatId = activeChatId ?: return
        typingJob?.cancel()
        viewModelScope.launch {
            try { chatsRef.document(chatId).update("typing.$myUid", false) } catch (_: Exception) {}
        }
    }

    // ============================================
    //          BAGIAN 5: EDIT PROFIL
    // ============================================

    fun updateDisplayName(newName: String) {
        val uid = auth.currentUser?.uid ?: return
        if (newName.isBlank()) { _error.value = "Nama tidak boleh kosong"; return }

        viewModelScope.launch {
            _isUpdatingProfile.value = true
            try {
                usersRef.document(uid).update("displayName", newName.trim()).await()
                _profileUpdateSuccess.value = true
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Gagal update nama: ${e.message}"
            } finally {
                _isUpdatingProfile.value = false
            }
        }
    }

    fun updateAbout(newAbout: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isUpdatingProfile.value = true
            try {
                usersRef.document(uid).update("about", newAbout.trim()).await()
                _profileUpdateSuccess.value = true
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Gagal update tentang: ${e.message}"
            } finally {
                _isUpdatingProfile.value = false
            }
        }
    }

    fun updateProfilePhoto(uri: Uri) {
        val uid = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            _isUpdatingProfile.value = true
            try {
                val oldPhotoUrl = usersRef.document(uid).get().await().getString("photoUrl") ?: ""

                val fileName = "profile_photos/$uid/${UUID.randomUUID()}.jpg"
                val ref = storage.reference.child(fileName)
                ref.putFile(uri).await()
                val newPhotoUrl = ref.downloadUrl.await().toString()

                usersRef.document(uid).update("photoUrl", newPhotoUrl).await()
                updateUserPhotoInStatuses(uid, newPhotoUrl)

                if (oldPhotoUrl.isNotEmpty()) {
                    try { storage.getReferenceFromUrl(oldPhotoUrl).delete().await() } catch (_: Exception) {}
                }

                _profileUpdateSuccess.value = true
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Gagal update foto: ${e.message}"
            } finally {
                _isUpdatingProfile.value = false
            }
        }
    }

    private suspend fun updateUserPhotoInStatuses(uid: String, newPhotoUrl: String) {
        try {
            val statuses = db.collection("statuses").whereEqualTo("uid", uid).get().await()
            statuses.documents.forEach { doc ->
                doc.reference.update("userPhoto", newPhotoUrl).await()
            }
        } catch (_: Exception) {}
    }

    fun resetProfileSuccessFlag() { _profileUpdateSuccess.value = false }
    fun clearError() { _error.value = null }
}
