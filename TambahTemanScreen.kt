package com.example.uchat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TambahTemanScreen(
    viewModel: ChatViewModel,
    onBack: () -> Unit,
    onChatWithUser: (User) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var foundUser by remember { mutableStateOf<User?>(null) }
    var isSearching by remember { mutableStateOf(false) }
    var notFound by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val currentUser by viewModel.currentUser.collectAsState()

    Scaffold(
        containerColor = WA_Bg,
        topBar = {
            TopAppBar(
                title = { Text("Tambah Teman", color = WA_TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = WA_TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WA_Dark)
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp)
        ) {
            Text(
                "Cari teman dengan PIN atau @username",
                color = WA_TextSecondary, fontSize = 14.sp
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    foundUser = null
                    notFound = false
                },
                label = { Text("PIN atau @username") },
                placeholder = { Text("Contoh: A7BX9M2K atau @aminsambung") },
                singleLine = true,
                trailingIcon = {
                    IconButton(
                        onClick = {
                            if (query.isBlank()) return@IconButton
                            scope.launch {
                                isSearching = true
                                foundUser = null
                                notFound = false

                                val clean = query.trim().removePrefix("@")
                                val user = if (clean.length == 8 && clean.all {
                                        it.isUpperCase() || it.isDigit()
                                    }) {
                                    viewModel.findUserByPin(clean)
                                } else {
                                    viewModel.findUserByUsername(clean)
                                }

                                // Jangan tampilkan diri sendiri
                                foundUser = if (user != null && user.uid != currentUser?.uid) user else null
                                notFound = foundUser == null
                                isSearching = false
                            }
                        },
                        enabled = !isSearching && query.isNotBlank()
                    ) {
                        Icon(Icons.Default.Search, "Cari", tint = WA_Green)
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = WA_TextPrimary,
                    unfocusedTextColor = WA_TextPrimary,
                    focusedBorderColor = WA_Green,
                    unfocusedBorderColor = WA_TextSecondary,
                    focusedLabelColor = WA_Green,
                    unfocusedLabelColor = WA_TextSecondary,
                    cursorColor = WA_Green
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))

            when {
                isSearching -> {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = WA_Green)
                    }
                }
                foundUser != null -> {
                    UserFoundCard(foundUser!!) { onChatWithUser(foundUser!!) }
                }
                notFound -> {
                    Text(
                        "Pengguna tidak ditemukan. Periksa PIN atau username.",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun UserFoundCard(user: User, onChat: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = WA_Dark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier.size(80.dp).clip(CircleShape).background(Color.DarkGray),
                contentAlignment = Alignment.Center
            ) {
                if (user.photoUrl.isNotEmpty()) {
                    AsyncImage(
                        model = user.photoUrl, contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Person, null, tint = Color.LightGray, modifier = Modifier.size(40.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(user.displayName, color = WA_TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("@${user.username}", color = WA_TextSecondary, fontSize = 14.sp)
            Spacer(Modifier.height(4.dp))
            Text("PIN: ${user.pin}", color = WA_TextSecondary, fontSize = 12.sp)
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onChat,
                colors = ButtonDefaults.buttonColors(WA_Green, Color.Black),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Mulai Chat", fontWeight = FontWeight.Bold) }
        }
    }
}
