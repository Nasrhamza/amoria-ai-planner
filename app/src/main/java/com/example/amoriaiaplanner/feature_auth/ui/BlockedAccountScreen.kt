package com.example.amoriaiaplanner.feature_auth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.amoriaiaplanner.core.ui.AmoriaPrimaryButton

@Composable
fun BlockedAccountScreen(
    onLogout: () -> Unit
) {
    val bg = Brush.linearGradient(
        listOf(Color(0xFFB3202E), Color(0xFF4B1E6D), Color(0xFF0B2B6B))
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.10f)),
            shape = RoundedCornerShape(26.dp)
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Block,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .padding(top = 6.dp)
                )

                Spacer(Modifier.height(14.dp))

                Text(
                    text = "Account blocked",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Your access to Amoria is currently blocked by an administrator. Please contact support or log out.",
                    color = Color.White.copy(alpha = 0.90f),
                    fontSize = 14.sp
                )

                Spacer(Modifier.height(18.dp))

                AmoriaPrimaryButton(
                    text = "Logout",
                    onClick = onLogout,
                    icon = {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = Color.White)
                    }
                )
            }
        }
    }
}
