package com.example.uchat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStatusScreen(
    viewModel: StatusViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    var text by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#25D366") }
    var mode by remember { mutableStateOf("text") }
    var pickedImageUri by remember { mutableStateOf<Uri?>(null) }
    var pickedVideoUri by remember { mutableStateOf<Uri?>(null) }
    val isUploading by viewModel.isUploading.collectAsState()
    val error by viewModel.error.collectAsState()

    val colors = listOf(
        "#25D366", "#075E54", "#34B7F1", "#54656F",
        "#D32F2F", "#7B1FA2", "#F57C00", "#C2185B"
    )

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            pickedImageUri = uri
            pickedVideoUri = null
            mode = "image"
        }
    }

    val videoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            pickedVideoUri = uri
            pickedImageUri = null
            mode = "video"
        }
    }

    Scaffold(
        containerColor = if (mode == "text") Color(android.graphics.Color.parseColor(selectedColor)) else WA_Bg,
        topBar = {
            TopAppBar(
                title = { Text("Status Baru", color = WA_TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = WA_TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { imagePicker.launch("image/*") }) {
                        Icon(Icons.Default.Image, "Pilih Gambar", tint = WA_TextPrimary)
                    }
                    IconButton(onClick = { videoPicker.launch("video/*") }) {
                        Icon(Icons.Default.Videocam, "Pilih Video", tint = WA_TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (mode == "text") Color.Transparent else WA_Dark
                )
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {

            if (error != null) {
                Surface(color = Color(0xFF7F1D1D), modifier = Modifier.fillMaxWidth()) {
                    Text(error!!, color = Color.White, fontSize = 13.sp, modifier = Modifier.padding(12.dp))
                }
            }

            if (mode == "text") {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TextField(
                        value = text,
                        onValueChange = { text = it },
                        placeholder = {
                            Text("Ketik status Anda...", fontSize = 24.sp, color = Color.White.copy(alpha = 0.6f))
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 24.sp, color = Color.White),
                        modifier = Modifier.fillMaxWidth().padding(24.dp)
                    )
                }

                LazyRow(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(colors) { colorHex ->
                        Box(
                            Modifier.size(40.dp).clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(colorHex)))
                                .border(
                                    width = if (selectedColor == colorHex) 3.dp else 0.dp,
                                    color = Color.White, shape = CircleShape
                                )
                                .clickable { selectedColor = colorHex }
                        )
                    }
                }
            } else if (mode == "image") {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (pickedImageUri != null) {
                        coil.compose.AsyncImage(
                            model = pickedImageUri, contentDescription = null,
                            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Image, null, tint = WA_TextSecondary, modifier = Modifier.size(64.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("Pilih gambar dari galeri", color = WA_TextSecondary)
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = { imagePicker.launch("image/*") },
                                colors = ButtonDefaults.buttonColors(WA_Green, Color.Black)
                            ) { Text("Pilih Gambar") }
                        }
                    }
                }
            } else {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (pickedVideoUri != null) {
                        VideoPreviewPlayer(uri = pickedVideoUri!!)
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Videocam, null, tint = WA_TextSecondary, modifier = Modifier.size(64.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("Pilih video dari galeri", color = WA_TextSecondary)
                            Spacer(Modifier.height(4.dp))
                            Text("Maks 30 detik • Maks 50MB", color = WA_TextSecondary, fontSize = 12.sp)
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = { videoPicker.launch("video/*") },
                                colors = ButtonDefaults.buttonColors(WA_Green, Color.Black)
                            ) { Text("Pilih Video") }
                        }
                    }
                }
            }

            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.CenterEnd) {
                FloatingActionButton(
                    onClick = {
                        when (mode) {
                            "text" -> viewModel.uploadTextStatus(text, selectedColor)
                            "image" -> pickedImageUri?.let { viewModel.uploadImageStatus(it) }
                            "video" -> pickedVideoUri?.let { viewModel.uploadVideoStatus(it, context) }
                        }
                        onBack()
                    },
                    containerColor = WA_Green,
                    contentColor = Color.Black,
                    modifier = Modifier.size(56.dp)
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(Modifier.size(24.dp), color = Color.Black, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Send, "Kirim")
                    }
                }
            }
        }
    }
}
