package com.example.uchat

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

// ============================================
//                 MODEL
// ============================================

data class Status(
    val id: String = "",
    val uid: String = "",
    val userName: String = "",
    val userPhoto: String = "",
    val type: String = "image",          // "image" | "text" | "video"
    val imageUrl: String = "",
    val videoUrl: String = "",
    val thumbnailUrl: String = "",
    val videoDuration: Long = 0L,
    val text: String = "",
    val backgroundColor: String = "#25D366",
    val createdAt: Long = 0L,
    val expiresAt: Long = 0L,
    val viewedBy: List<String> = emptyList()
)

data class UserStatusGroup(
    val uid: String = "",
    val userName: String = "",
    val userPhoto: String = "",
    val statuses: List<Status> = emptyList(),
    val latestTime: Long = 0L,
    val hasUnviewed: Boolean = false
)

// ============================================
//              VIEWMODEL
// ============================================

class StatusViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val statusesRef = db.collection("statuses")

    private val _statusGroups = MutableStateFlow<List<UserStatusGroup>>(emptyList())
    val statusGroups: StateFlow<List<UserStatusGroup>> = _statusGroups.asStateFlow()

    private val _myStatuses = MutableStateFlow<List<Status>>(emptyList())
    val myStatuses: StateFlow<List<Status>> = _myStatuses.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var listener: ListenerRegistration? = null

    init {
        auth.addAuthStateListener { firebaseAuth ->
            firebaseAuth.currentUser?.let { listenStatuses() }
                ?: run { _statusGroups.value = emptyList(); _myStatuses.value = emptyList() }
        }
    }

    private fun listenStatuses() {
        listener?.remove()
        val now = System.currentTimeMillis()

        listener = statusesRef
            .whereGreaterThan("expiresAt", now)
            .orderBy("expiresAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) { Log.e("Uchat", "listenStatuses error", e); return@addSnapshotListener }

                val myUid = auth.currentUser?.uid ?: return@addSnapshotListener
                val allStatuses = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Status::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                _myStatuses.value = allStatuses
                    .filter { it.uid == myUid }
                    .sortedBy { it.createdAt }

                val others = allStatuses.filter { it.uid != myUid }

                _statusGroups.value = others
                    .groupBy { it.uid }
                    .map { (uid, list) ->
                        val sortedList = list.sortedBy { it.createdAt }
                        UserStatusGroup(
                            uid = uid,
                            userName = sortedList.first().userName,
                            userPhoto = sortedList.first().userPhoto,
                            statuses = sortedList,
                            latestTime = sortedList.maxOf { it.createdAt },
                            hasUnviewed = sortedList.any { s -> !s.viewedBy.contains(myUid) }
                        )
                    }
                    .sortedByDescending { it.latestTime }
            }
    }

    // ============ UPLOAD GAMBAR ============
    fun uploadImageStatus(uri: Uri) {
        val uid = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            _isUploading.value = true
            try {
                val userData = db.collection("users").document(uid).get().await()
                    .toObject(User::class.java)

                val fileName = "statuses/$uid/${UUID.randomUUID()}.jpg"
                val ref = storage.reference.child(fileName)
                ref.putFile(uri).await()
                val downloadUrl = ref.downloadUrl.await().toString()

                val now = System.currentTimeMillis()
                val newStatus = hashMapOf(
                    "uid" to uid,
                    "userName" to (userData?.displayName ?: "Pengguna"),
                    "userPhoto" to (userData?.photoUrl ?: ""),
                    "type" to "image",
                    "imageUrl" to downloadUrl,
                    "videoUrl" to "",
                    "thumbnailUrl" to "",
                    "videoDuration" to 0L,
                    "text" to "",
                    "backgroundColor" to "#25D366",
                    "createdAt" to now,
                    "expiresAt" to (now + 24 * 60 * 60 * 1000L),
                    "viewedBy" to listOf(uid)
                )
                statusesRef.add(newStatus).await()
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Gagal upload status: ${e.message}"
            } finally {
                _isUploading.value = false
            }
        }
    }

    // ============ UPLOAD TEKS ============
    fun uploadTextStatus(text: String, backgroundColor: String) {
        if (text.isBlank()) return
        val uid = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            _isUploading.value = true
            try {
                val userData = db.collection("users").document(uid).get().await()
                    .toObject(User::class.java)

                val now = System.currentTimeMillis()
                val newStatus = hashMapOf(
                    "uid" to uid,
                    "userName" to (userData?.displayName ?: "Pengguna"),
                    "userPhoto" to (userData?.photoUrl ?: ""),
                    "type" to "text",
                    "imageUrl" to "",
                    "videoUrl" to "",
                    "thumbnailUrl" to "",
                    "videoDuration" to 0L,
                    "text" to text.trim(),
                    "backgroundColor" to backgroundColor,
                    "createdAt" to now,
                    "expiresAt" to (now + 24 * 60 * 60 * 1000L),
                    "viewedBy" to listOf(uid)
                )
                statusesRef.add(newStatus).await()
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Gagal upload status: ${e.message}"
            } finally {
                _isUploading.value = false
            }
        }
    }

    // ============ UPLOAD VIDEO ============
    fun uploadVideoStatus(uri: Uri, context: Context) {
        val uid = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            _isUploading.value = true
            try {
                // Validasi ukuran
                val sizeBytes = context.contentResolver.openInputStream(uri)
                    ?.use { it.available().toLong() } ?: 0L
                if (sizeBytes > 50 * 1024 * 1024) {
                    _error.value = "Video terlalu besar (maks 50MB)"
                    return@launch
                }

                // Ambil durasi
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, uri)
                val durationMs = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_DURATION
                )?.toLongOrNull() ?: 0L
                retriever.release()

                if (durationMs > 30_000) {
                    _error.value = "Video maksimal 30 detik"
                    return@launch
                }

                // Generate thumbnail
                val thumbnailUri = generateThumbnail(uri, context)

                // Upload video
                val videoFileName = "statuses/$uid/${UUID.randomUUID()}.mp4"
                val videoRef = storage.reference.child(videoFileName)
                videoRef.putFile(uri).await()
                val videoUrl = videoRef.downloadUrl.await().toString()

                // Upload thumbnail
                var thumbnailUrl = ""
                if (thumbnailUri != null) {
                    val thumbFileName = "statuses/$uid/thumbs/${UUID.randomUUID()}.jpg"
                    val thumbRef = storage.reference.child(thumbFileName)
                    thumbRef.putFile(thumbnailUri).await()
                    thumbnailUrl = thumbRef.downloadUrl.await().toString()
                }

                val userData = db.collection("users").document(uid).get().await()
                    .toObject(User::class.java)

                val now = System.currentTimeMillis()
                val newStatus = hashMapOf(
                    "uid" to uid,
                    "userName" to (userData?.displayName ?: "Pengguna"),
                    "userPhoto" to (userData?.photoUrl ?: ""),
                    "type" to "video",
                    "imageUrl" to "",
                    "videoUrl" to videoUrl,
                    "thumbnailUrl" to thumbnailUrl,
                    "videoDuration" to durationMs,
                    "text" to "",
                    "backgroundColor" to "#25D366",
                    "createdAt" to now,
                    "expiresAt" to (now + 24 * 60 * 60 * 1000L),
                    "viewedBy" to listOf(uid)
                )

                statusesRef.add(newStatus).await()
                _error.value = null
            } catch (e: Exception) {
                Log.e("Uchat", "upload video gagal", e)
                _error.value = "Gagal upload video: ${e.message}"
            } finally {
                _isUploading.value = false
            }
        }
    }

    private suspend fun generateThumbnail(videoUri: Uri, context: Context): Uri? {
        return withContext(Dispatchers.IO) {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, videoUri)
                val bitmap = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                retriever.release()

                if (bitmap == null) return@withContext null

                val thumbFile = File(context.cacheDir, "thumb_${UUID.randomUUID()}.jpg")
                val outputStream = FileOutputStream(thumbFile)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                outputStream.flush()
                outputStream.close()
                bitmap.recycle()

                Uri.fromFile(thumbFile)
            } catch (e: Exception) {
                Log.e("Uchat", "Gagal generate thumbnail", e)
                null
            }
        }
    }

    fun markAsViewed(statusId: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                statusesRef.document(statusId)
                    .update("viewedBy", FieldValue.arrayUnion(uid)).await()
            } catch (_: Exception) {}
        }
    }

    fun deleteMyStatus(statusId: String) {
        viewModelScope.launch {
            try { statusesRef.document(statusId).delete().await() }
            catch (e: Exception) { _error.value = e.message }
        }
    }

    fun clearError() { _error.value = null }

    override fun onCleared() {
        super.onCleared()
        listener?.remove()
    }
}
