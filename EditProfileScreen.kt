package com.example.uchat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    viewModel: ChatViewModel,
    onBack: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val isUpdating by viewModel.isUpdatingProfile.collectAsState()
    val updateSuccess by viewModel.profileUpdateSuccess.collectAsState()
    val error by viewModel.error.collectAsState()

    var displayName by remember { mutableStateOf("") }
    var about by remember { mutableStateOf("") }
    var pickedImageUri by remember { mutableStateOf<Uri?>(null) }
    var initialized by remember { mutableStateOf(false) }

    LaunchedEffect(currentUser) {
        if (currentUser != null && !initialized) {
            displayName = currentUser!!.displayName
            about = currentUser!!.about
            initialized = true
        }
    }

    LaunchedEffect(updateSuccess) {
        if (updateSuccess) {
            delay(2000)
            viewModel.resetProfileSuccessFlag()
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> if (uri != null) pickedImageUri = uri }

    Scaffold(
        containerColor = WA_Bg,
        topBar = {
            TopAppBar(
                title = { Text("Edit Profil", color = WA_TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = WA_TextPrimary)
                    }
                },
                actions = {
                    if (isUpdating) {
                        CircularProgressIndicator(
                            Modifier.size(24.dp).padding(end = 12.dp),
                            color = WA_Green, strokeWidth = 2.dp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WA_Dark)
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
        ) {
            if (error != null) {
                Surface(color = Color(0xFF7F1D1D), modifier = Modifier.fillMaxWidth()) {
                    Text(error!!, color = Color.White, fontSize = 13.sp, modifier = Modifier.padding(12.dp))
                }
            }

            if (updateSuccess) {
                Surface(color = WA_Green, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = Color.Black)
                        Spacer(Modifier.width(8.dp))
                        Text("Perubahan berhasil disimpan", color = Color.Black, fontSize = 13.sp)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(140.dp), contentAlignment = Alignment.BottomEnd) {
                    Box(
                        Modifier.size(140.dp).clip(CircleShape).background(Color.DarkGray)
                            .clickable { imagePicker.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            pickedImageUri != null -> AsyncImage(
                                model = pickedImageUri, contentDescription = "Preview",
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            !currentUser?.photoUrl.isNullOrEmpty() -> AsyncImage(
                                model = currentUser!!.photoUrl, contentDescription = null,
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            else -> Icon(
                                Icons.Default.Person, null,
                                tint = Color.LightGray, modifier = Modifier.size(80.dp)
                            )
                        }
                    }

                    Box(
                        Modifier.size(44.dp).clip(CircleShape).background(WA_Green)
                            .border(3.dp, WA_Bg, CircleShape)
                            .clickable { imagePicker.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CameraAlt, "Ganti Foto", tint = Color.Black, modifier = Modifier.size(22.dp))
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (pickedImageUri != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = { pickedImageUri = null },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = WA_TextPrimary)
                        ) { Text("Batal") }
                        Button(
                            onClick = {
                                viewModel.updateProfilePhoto(pickedImageUri!!)
                                pickedImageUri = null
                            },
                            colors = ButtonDefaults.buttonColors(WA_Green, Color.Black),
                            enabled = !isUpdating
                        ) { Text("Simpan Foto", fontWeight = FontWeight.Bold) }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            EditableField(
                label = "Nama",
                value = displayName,
                onValueChange = { displayName = it },
                icon = Icons.Default.Person,
                maxLength = 25,
                enabled = !isUpdating,
                helperText = "Nama ini akan tampil di chat dan status Anda"
            )

            Spacer(Modifier.height(16.dp))

            EditableField(
                label = "Tentang",
                value = about,
                onValueChange = { about = it },
                icon = Icons.Default.Info,
                maxLength = 139,
                enabled = !isUpdating,
                helperText = "Status singkat yang tampil di profil Anda"
            )

            Spacer(Modifier.height(16.dp))

            ReadOnlyField(
                "Username", "@${currentUser?.username ?: "-"}",
                Icons.Default.AlternateEmail, "Username tidak bisa diubah"
            )
            Spacer(Modifier.height(12.dp))
            ReadOnlyField(
                "PIN", currentUser?.pin ?: "-",
                Icons.Default.Pin, "PIN unik Anda (tidak bisa diubah)"
            )
            Spacer(Modifier.height(12.dp))
            ReadOnlyField(
                "Email", currentUser?.email ?: "-",
                Icons.Default.Email, "Email tidak bisa diubah"
            )

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = {
                    if (displayName.trim() != currentUser?.displayName) {
                        viewModel.updateDisplayName(displayName.trim())
                    }
                    if (about.trim() != currentUser?.about) {
                        viewModel.updateAbout(about.trim())
                    }
                },
                enabled = !isUpdating && (
                    displayName.trim() != currentUser?.displayName ||
                    about.trim() != currentUser?.about
                ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WA_Green, contentColor = Color.Black,
                    disabledContainerColor = WA_Dark, disabledContentColor = WA_TextSecondary
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(50.dp)
            ) {
                if (isUpdating) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Save, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Simpan Perubahan", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun EditableField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: ImageVector,
    maxLength: Int,
    enabled: Boolean,
    helperText: String
) {
    var showDialog by remember { mutableStateOf(false) }
    var tempValue by remember { mutableStateOf(value) }

    Column(Modifier.padding(horizontal = 16.dp)) {
        Text(label, color = WA_Green, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(6.dp))

        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(WA_Dark)
                .clickable(enabled = enabled) {
                    tempValue = value
                    showDialog = true
                }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = WA_TextSecondary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(16.dp))
            Text(value.ifEmpty { "Belum diisi" }, color = WA_TextPrimary, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Icon(Icons.Default.Edit, "Edit", tint = WA_Green, modifier = Modifier.size(20.dp))
        }

        Spacer(Modifier.height(4.dp))
        Text(helperText, color = WA_TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp))
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            containerColor = WA_Dark,
            title = { Text("Edit $label", color = WA_TextPrimary) },
            text = {
                Column {
                    OutlinedTextField(
                        value = tempValue,
                        onValueChange = { if (it.length <= maxLength) tempValue = it },
                        label = { Text(label) },
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
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${tempValue.length} / $maxLength karakter",
                        color = WA_TextSecondary, fontSize = 11.sp,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { onValueChange(tempValue.trim()); showDialog = false },
                    enabled = tempValue.isNotBlank()
                ) { Text("Simpan", color = WA_Green, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Batal", color = WA_TextSecondary)
                }
            }
        )
    }
}

@Composable
fun ReadOnlyField(
    label: String,
    value: String,
    icon: ImageVector,
    helperText: String
) {
    Column(Modifier.padding(horizontal = 16.dp)) {
        Text(label, color = WA_TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(6.dp))

        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(WA_Bg)
                .border(1.dp, WA_Dark, RoundedCornerShape(8.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = WA_TextSecondary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(16.dp))
            Text(value, color = WA_TextSecondary, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Icon(Icons.Default.Lock, "Terkunci", tint = WA_TextSecondary, modifier = Modifier.size(16.dp))
        }

        Spacer(Modifier.height(4.dp))
        Text(helperText, color = WA_TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp))
    }
}
