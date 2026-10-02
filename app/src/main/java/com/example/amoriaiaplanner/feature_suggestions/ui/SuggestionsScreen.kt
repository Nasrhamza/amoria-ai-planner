package com.example.amoriaiaplanner.feature_suggestions.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.amoriaiaplanner.feature_ai.model.PlaceSuggestion
import com.example.amoriaiaplanner.feature_suggestions.data.SuggestionMemoryStore
import androidx.compose.foundation.layout.ColumnScope
import com.example.amoriaiaplanner.core.ui.AmoriaPrimaryButton
import com.example.amoriaiaplanner.core.ui.AmoriaSecondaryButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuggestionsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val suggestions = SuggestionMemoryStore.latestSuggestions
    val modeLabel = SuggestionMemoryStore.latestModeLabel
    val extraPreference = SuggestionMemoryStore.latestExtraPreference

    var selectedSuggestion by remember { mutableStateOf<PlaceSuggestion?>(null) }
    val likedMap = remember { mutableStateMapOf<String, Boolean>() }

    val bg = Brush.verticalGradient(
        listOf(
            Color(0xFFB3202E),
            Color(0xFF5D1F6E),
            Color(0xFF172F84)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Suggestions",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineSmall
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
                        containerColor = Color(0xFF351038),
                        titleContentColor = Color.White
                    )
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(
                    start = 18.dp,
                    end = 18.dp,
                    top = 16.dp,
                    bottom = 105.dp // ✅ Space for Bottom Bar
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    PremiumSectionCard {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Session",
                                color = Color(0xFFFFD4E3),
                                style = MaterialTheme.typography.labelLarge
                            )
                            if (modeLabel.isNotBlank()) {
                                Text(
                                    text = "Mode: $modeLabel",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            if (extraPreference.isNotBlank()) {
                                Text(
                                    text = "Extra preference: $extraPreference",
                                    color = Color(0xFFFFE7F0),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }

                itemsIndexed(
                    items = suggestions,
                    key = { _, item -> item.title + item.vibe }
                ) { index, suggestion ->
                    val isLiked = likedMap[suggestion.title] == true

                    PremiumSuggestionCard(
                        rank = index + 1,
                        suggestion = suggestion,
                        isLiked = isLiked,
                        onToggleLike = {
                            likedMap[suggestion.title] = !isLiked
                        },
                        onOpenDetails = {
                            selectedSuggestion = suggestion
                        },
                        onOpenMaps = {
                            val query = Uri.encode("${suggestion.title} Tunisia")
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://www.google.com/maps/search/?api=1&query=$query")
                            )
                            context.startActivity(intent)
                        }
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = selectedSuggestion != null,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            selectedSuggestion?.let { suggestion ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF4E296A),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = suggestion.title,
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.weight(1f)
                                )

                                IconButton(onClick = { selectedSuggestion = null }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = Color.White
                                    )
                                }
                            }

                            BadgeRow(suggestion = suggestion)

                            Text(
                                text = suggestion.reason,
                                color = Color(0xFFFFEEF5),
                                style = MaterialTheme.typography.bodyLarge
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                AmoriaPrimaryButton(
                                    text = "Maps",
                                    onClick = {
                                        val query = Uri.encode("${suggestion.title} Tunisia")
                                        val intent = Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("https://www.google.com/maps/search/?api=1&query=$query")
                                        )
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.weight(1f),
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.Place,
                                            contentDescription = null
                                        )
                                    }
                                )

                                AmoriaSecondaryButton(
                                    text = "Close",
                                    onClick = { selectedSuggestion = null },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumSectionCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White.copy(alpha = 0.10f),
        shape = RoundedCornerShape(26.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        listOf(Color(0x22FFFFFF), Color(0x10FFFFFF))
                    ),
                    shape = RoundedCornerShape(26.dp)
                )
                .border(
                    width = 1.dp,
                    color = Color(0x22FFD8E8),
                    shape = RoundedCornerShape(26.dp)
                )
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                content = content
            )
        }
    }
}

@Composable
private fun PremiumSuggestionCard(
    rank: Int,
    suggestion: PlaceSuggestion,
    isLiked: Boolean,
    onToggleLike: () -> Unit,
    onOpenDetails: () -> Unit,
    onOpenMaps: () -> Unit
) {
    PremiumSectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "$rank. ${suggestion.title}",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge
                )

                BadgeRow(suggestion = suggestion)

                Text(
                    text = suggestion.reason,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = Color(0xFFFFEEF5),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircleIconButton(
                    onClick = onOpenDetails,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Details",
                            tint = Color(0xFFFFD6E6)
                        )
                    }
                )

                CircleIconButton(
                    onClick = onToggleLike,
                    icon = {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (isLiked) Color(0xFFFF7AA2) else Color(0xFFFFD6E6)
                        )
                    }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AmoriaPrimaryButton(
                text = "View on Maps",
                onClick = onOpenMaps,
                modifier = Modifier.weight(1f),
                icon = {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null
                    )
                }
            )

            AmoriaSecondaryButton(
                text = "Details",
                onClick = onOpenDetails,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BadgeRow(
    suggestion: PlaceSuggestion
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TinyBadge("Suggestion")
        if (suggestion.vibe.contains("outdoor", ignoreCase = true)) {
            TinyBadge("Outdoor")
        } else if (suggestion.vibe.contains("indoor", ignoreCase = true)) {
            TinyBadge("Indoor")
        }
        TinyBadge(suggestion.vibe.take(18))
    }
}

@Composable
private fun TinyBadge(text: String) {
    Box(
        modifier = Modifier
            .background(
                color = Color(0x33FFD6E6),
                shape = RoundedCornerShape(999.dp)
            )
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = text,
            color = Color(0xFFFFD6E6),
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun CircleIconButton(
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color(0x28FFFFFF),
        modifier = Modifier.size(42.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            icon()
        }
    }
}
