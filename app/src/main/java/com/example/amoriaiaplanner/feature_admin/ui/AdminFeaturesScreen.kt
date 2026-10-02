package com.example.amoriaiaplanner.feature_admin.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.amoriaiaplanner.core.ui.AmoriaPrimaryButton
import com.example.amoriaiaplanner.feature_admin.model.AdminFeatureSettings
import com.example.amoriaiaplanner.feature_admin.model.AdminUiState

@Composable
fun AdminFeaturesScreen(
    ui: AdminUiState,
    onSettingsChange: (AdminFeatureSettings) -> Unit,
    onSave: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Application functionalities",
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Enable or pause user-facing modules. Changes are stored in Firestore.",
            color = Color.White.copy(alpha = 0.78f),
            fontSize = 13.sp
        )
        Text(
            text = if (ui.hasUnsavedFeatureChanges) {
                "Unsaved changes"
            } else {
                "All controls are synchronized"
            },
            color = if (ui.hasUnsavedFeatureChanges) {
                Color(0xFFFFD180)
            } else {
                Color(0xFFA5D6A7)
            },
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )

        FeatureToggleCard(
            title = "AI suggestions",
            subtitle = "Personal place suggestions in Default mode",
            icon = Icons.Default.Psychology,
            checked = ui.featureSettings.defaultSuggestionsEnabled,
            enabled = !ui.isBusy,
            onCheckedChange = {
                onSettingsChange(
                    ui.featureSettings.copy(defaultSuggestionsEnabled = it)
                )
            }
        )

        FeatureToggleCard(
            title = "Friends Rooms",
            subtitle = "Room creation, joining and group suggestions",
            icon = Icons.Default.Groups,
            checked = ui.featureSettings.friendsRoomsEnabled,
            enabled = !ui.isBusy,
            onCheckedChange = {
                onSettingsChange(
                    ui.featureSettings.copy(friendsRoomsEnabled = it)
                )
            }
        )

        FeatureToggleCard(
            title = "Location suggestions",
            subtitle = "Nearby places based on the user's location",
            icon = Icons.Default.LocationOn,
            checked = ui.featureSettings.locationSuggestionsEnabled,
            enabled = !ui.isBusy,
            onCheckedChange = {
                onSettingsChange(
                    ui.featureSettings.copy(locationSuggestionsEnabled = it)
                )
            }
        )

        Spacer(Modifier.height(2.dp))

        AmoriaPrimaryButton(
            text = if (ui.isBusy) "Saving..." else "Save functionalities",
            onClick = onSave,
            enabled = !ui.isBusy && ui.hasUnsavedFeatureChanges
        )
    }
}

@Composable
private fun FeatureToggleCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.10f)
        ),
        shape = RoundedCornerShape(22.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 12.sp
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFFE14D72),
                    uncheckedThumbColor = Color.White.copy(alpha = 0.85f),
                    uncheckedTrackColor = Color.White.copy(alpha = 0.20f)
                )
            )
        }
    }
}
