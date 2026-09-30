package com.example.uchat

import androidx.compose.foundation.background
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyProfileScreen(
    viewModel: ChatViewModel,
    onEditProfile: () -> Unit,
    onBack: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()

    Scaffold(
        containerColor = WA_Bg,
        topBar = {
            TopAppBar(
                title = { Text("Profil Saya", color = WA_TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = WA_TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = onEditProfile) {
                        Icon(Icons.Default.Edit, "Edit", tint = WA_Green)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WA_Dark)
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            Box(
                Modifier.size(160.dp).clip(CircleShape).background(Color.DarkGray),
                contentAlignment = Alignment.Center
            ) {
                if (!currentUser?.photoUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = currentUser!!.photoUrl, contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Person, null, tint = Color.LightGray, modifier = Modifier.size(90.dp))
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(
                currentUser?.displayName ?: "-",
                color = WA_TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "@${currentUser?.username ?: "-"}",
                color = WA_TextSecondary, fontSize = 15.sp
            )

            Spacer(Modifier.height(24.dp))

            ProfileInfoCard(
                icon = Icons.Default.Info,
                label = "Tentang",
                value = currentUser?.about ?: "Hai! Saya pakai Uchat"
            )

            ProfileInfoCard(
                icon = Icons.Default.Pin,
                label = "PIN",
                value = currentUser?.pin ?: "-",
                highlight = true
            )

            ProfileInfoCard(
                icon = Icons.Default.AlternateEmail,
                label = "Email",
                value = currentUser?.email ?: "-"
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onEditProfile,
                colors = ButtonDefaults.buttonColors(WA_Green, Color.Black),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(50.dp)
            ) {
                Icon(Icons.Default.Edit, null)
                Spacer(Modifier.width(8.dp))
                Text("Edit Profil", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun ProfileInfoCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    highlight: Boolean = false
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(WA_Dark)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon, null,
            tint = if (highlight) WA_Green else WA_TextSecondary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(label, color = WA_TextSecondary, fontSize = 12.sp)
            Spacer(Modifier.height(2.dp))
            Text(
                value,
                color = if (highlight) WA_Green else WA_TextPrimary,
                fontSize = 15.sp,
                fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
