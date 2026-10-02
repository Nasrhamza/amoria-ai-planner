package com.example.amoriaiaplanner.feature_friends_room.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.amoriaiaplanner.core.ui.AmoriaPrimaryButton
import com.example.amoriaiaplanner.feature_ai.model.PlaceSuggestion
import com.example.amoriaiaplanner.feature_friends_room.vm.FriendsRoomSuggestionsViewModel
import com.example.amoriaiaplanner.feature_friends_room.vm.FriendsRoomViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsRoomSuggestionsRoute(
    onBack: () -> Unit,
    roomVm: FriendsRoomViewModel = viewModel(),
    suggestionsVm: FriendsRoomSuggestionsViewModel = viewModel()
) {
    val roomUi by roomVm.ui.collectAsState()
    val room = roomUi.activeRoom

    LaunchedEffect(Unit) {
        roomVm.load()
    }

    LaunchedEffect(room?.roomId) {
        if (room != null) {
            suggestionsVm.observeSuggestions(room.roomId)
        }
    }

    FriendsRoomSuggestionsScreen(
        roomCode = room?.code.orEmpty(),
        onBack = onBack,
        vm = suggestionsVm
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsRoomSuggestionsScreen(
    roomCode: String,
    onBack: () -> Unit,
    vm: FriendsRoomSuggestionsViewModel = viewModel()
) {
    val ui by vm.ui.collectAsState()

    val bg = Brush.linearGradient(
        listOf(
            Color(0xFFB3202E),
            Color(0xFF4B1E6D),
            Color(0xFF0B2B6B)
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Group suggestions",
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF3A1639)
                )
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bg)
                .padding(padding)
                .padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 105.dp)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Friends room results",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge
                    )

                    if (roomCode.isNotBlank()) {
                        Text(
                            text = "Room code: $roomCode",
                            color = Color(0xFFFFD9E6),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }

                    Text(
                        text = "These suggestions are generated from the group quiz answers.",
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            when {
                ui.loading -> {
                    Text(
                        text = "Loading group suggestions...",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                ui.suggestions.isEmpty() -> {
                    Text(
                        text = ui.message ?: "No group suggestions yet.",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                else -> {
                    ui.suggestions.forEachIndexed { index, suggestion ->
                        GroupSuggestionCard(
                            index = index + 1,
                            suggestion = suggestion
                        )
                    }
                }
            }

            ui.message?.let { message ->
                if (ui.suggestions.isNotEmpty()) {
                    Text(
                        text = message,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupSuggestionCard(
    index: Int,
    suggestion: PlaceSuggestion
) {
    val context = LocalContext.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White.copy(alpha = 0.12f),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "$index. ${suggestion.title}",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "Vibe: ${suggestion.vibe}",
                color = Color(0xFFFFD9E6),
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = suggestion.reason,
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge
            )

            AmoriaPrimaryButton(
                text = "Open in Maps",
                onClick = {
                    val query = Uri.encode("${suggestion.title} Tunisia")
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://www.google.com/maps/search/?api=1&query=$query")
                    )
                    context.startActivity(intent)
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null
                    )
                }
            )
        }
    }
}
