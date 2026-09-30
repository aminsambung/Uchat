package com.example.uchat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AuthScreen(
    error: String?,
    viewModel: ChatViewModel,
    onSignIn: (String, String) -> Unit
) {
    var isRegisterMode by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var generatedPin by remember { mutableStateOf("") }

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            containerColor = WA_Dark,
            title = { Text("Registrasi Berhasil! 🎉", color = WA_TextPrimary) },
            text = {
                Column {
                    Text(
                        "Ini PIN unik Anda. Simpan dan bagikan ke teman agar mereka bisa menambahkan Anda.",
                        color = WA_TextSecondary
                    )
                    Spacer(Modifier.height(16.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = WA_Green),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                generatedPin,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                letterSpacing = 4.sp
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Tips: Catat PIN ini di tempat aman.",
                        fontSize = 12.sp, color = WA_TextSecondary
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text("Mengerti", color = WA_Green)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WA_Bg)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))

        Text("Uchat", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = WA_Green)
        Spacer(Modifier.height(8.dp))
        Text(
            if (isRegisterMode) "Buat akun baru" else "Masuk ke akun Anda",
            color = WA_TextSecondary, fontSize = 14.sp
        )

        Spacer(Modifier.height(32.dp))

        if (isRegisterMode) {
            RegisterForm(viewModel) { pin ->
                generatedPin = pin
                showPinDialog = true
            }
        } else {
            LoginForm(error, onSignIn)
        }

        Spacer(Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (isRegisterMode) "Sudah punya akun?" else "Belum punya akun?",
                color = WA_TextSecondary, fontSize = 14.sp
            )
            TextButton(onClick = { isRegisterMode = !isRegisterMode }) {
                Text(
                    if (isRegisterMode) "Masuk" else "Daftar",
                    color = WA_Green, fontWeight = FontWeight.Bold
                )
            }
        }

        if (error != null && !isRegisterMode) {
            Spacer(Modifier.height(8.dp))
            Text(error, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        }
    }
}

@Composable
private fun waFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = WA_TextPrimary,
    unfocusedTextColor = WA_TextPrimary,
    focusedBorderColor = WA_Green,
    unfocusedBorderColor = WA_TextSecondary,
    focusedLabelColor = WA_Green,
    unfocusedLabelColor = WA_TextSecondary,
    cursorColor = WA_Green
)

@Composable
fun LoginForm(error: String?, onSignIn: (String, String) -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = email, onValueChange = { email = it },
            label = { Text("Email") }, singleLine = true,
            keyboardOptions = KeyboardOptions(KeyboardType.Email, imeAction = ImeAction.Next),
            colors = waFieldColors(), modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = password, onValueChange = { password = it },
            label = { Text("Password") }, singleLine = true,
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(KeyboardType.Password, imeAction = ImeAction.Done),
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        "Toggle", tint = WA_TextSecondary
                    )
                }
            },
            colors = waFieldColors(), modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { onSignIn(email, password) },
            colors = ButtonDefaults.buttonColors(WA_Green, Color.Black),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) { Text("Masuk", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
    }
}

@Composable
fun RegisterForm(viewModel: ChatViewModel, onRegisterSuccess: (String) -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = displayName, onValueChange = { displayName = it },
            label = { Text("Nama Tampilan") }, placeholder = { Text("Contoh: Aminsambung") },
            singleLine = true, colors = waFieldColors(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it.lowercase().filter { c -> c.isLetterOrDigit() || c == '_' } },
            label = { Text("Username") }, placeholder = { Text("aminsambung") },
            prefix = { Text("@", color = WA_TextSecondary) },
            singleLine = true, colors = waFieldColors(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            supportingText = {
                Text("Hanya huruf kecil, angka, dan _", fontSize = 11.sp, color = WA_TextSecondary)
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = email, onValueChange = { email = it },
            label = { Text("Email") }, singleLine = true,
            keyboardOptions = KeyboardOptions(KeyboardType.Email, imeAction = ImeAction.Next),
            colors = waFieldColors(), modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = password, onValueChange = { password = it },
            label = { Text("Password") }, singleLine = true,
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(KeyboardType.Password, imeAction = ImeAction.Done),
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        "Toggle", tint = WA_TextSecondary
                    )
                }
            },
            supportingText = { Text("Minimal 6 karakter", fontSize = 11.sp, color = WA_TextSecondary) },
            colors = waFieldColors(), modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                localError = null
                when {
                    displayName.isBlank() -> localError = "Nama tidak boleh kosong"
                    username.length < 3 -> localError = "Username minimal 3 karakter"
                    !email.contains("@") -> localError = "Email tidak valid"
                    password.length < 6 -> localError = "Password minimal 6 karakter"
                    else -> {
                        isLoading = true
                        viewModel.signUp(email, password, displayName, username) { pin ->
                            isLoading = false
                            onRegisterSuccess(pin)
                        }
                    }
                }
            },
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(WA_Green, Color.Black),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(Modifier.size(24.dp), color = Color.Black, strokeWidth = 2.dp)
            } else {
                Text("Daftar", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }

        val vmError by viewModel.error.collectAsState()
        val displayError = localError ?: vmError
        if (displayError != null) {
            Spacer(Modifier.height(8.dp))
            Text(displayError, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        }
    }
}
