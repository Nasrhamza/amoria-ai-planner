package com.example.amoriaiaplanner.feature_home.ui

import androidx.lifecycle.Lifecycle
import androidx.compose.runtime.DisposableEffect
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.amoriaiaplanner.R
import com.example.amoriaiaplanner.core.config.AppFeatureConfigViewModel
import com.example.amoriaiaplanner.core.ui.*
import com.example.amoriaiaplanner.feature_ai.model.PlaceSuggestion
import com.example.amoriaiaplanner.feature_friends_room.model.RoomStatus
import com.example.amoriaiaplanner.feature_friends_room.vm.FriendsRoomViewModel
import com.example.amoriaiaplanner.feature_home.vm.HomeSuggestionViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class SuggestionMode {
    DEFAULT, FRIENDS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenFriendsQuiz: () -> Unit,
    onOpenFriendsSuggestions: () -> Unit,
    onOpenQuiz: () -> Unit,
    onOpenSuggestions: () -> Unit,
    friendsRoomVm: FriendsRoomViewModel = viewModel(),
    aiVm: HomeSuggestionViewModel = viewModel(),
    featureConfigVm: AppFeatureConfigViewModel = viewModel()
) {
    var refreshing by remember { mutableStateOf(false) }
    var selectedMode by remember { mutableStateOf(SuggestionMode.DEFAULT) }
    var userManuallySelectedMode by remember { mutableStateOf(false) }
    var selectedReason by remember { mutableStateOf<PlaceSuggestion?>(null) }
    val scope = rememberCoroutineScope()

    val friendsRoomUi by friendsRoomVm.ui.collectAsState()
    val aiUi by aiVm.ui.collectAsState()
    val featureConfigUi by featureConfigVm.ui.collectAsState()
    val enabledFeatures = featureConfigUi.settings
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms.values.any { it }
        aiVm.onLocationPermissionResult(context, granted)
    }

    val bg = Brush.linearGradient(
        listOf(
            Color(0xFFB3202E),
            Color(0xFF4B1E6D),
            Color(0xFF0B2B6B)
        )
    )

    LaunchedEffect(Unit) {
        friendsRoomVm.load()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                aiVm.refreshLocationAvailability(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(friendsRoomUi.activeRoom?.roomId) {
        if (
            !userManuallySelectedMode &&
            friendsRoomUi.activeRoom != null &&
            enabledFeatures.friendsRoomsEnabled
        ) {
            selectedMode = SuggestionMode.FRIENDS
        }
    }

    LaunchedEffect(enabledFeatures) {
        when {
            !enabledFeatures.defaultSuggestionsEnabled && enabledFeatures.friendsRoomsEnabled -> {
                selectedMode = SuggestionMode.FRIENDS
            }
            !enabledFeatures.friendsRoomsEnabled && enabledFeatures.defaultSuggestionsEnabled -> {
                selectedMode = SuggestionMode.DEFAULT
            }
        }

        if (!enabledFeatures.locationSuggestionsEnabled && aiUi.useLocation) {
            aiVm.disableLocation()
        }
    }

    if (selectedReason != null) {
        AlertDialog(
            onDismissRequest = { selectedReason = null },
            title = { Text(selectedReason!!.title) },
            text = { Text(selectedReason!!.reason) },
            confirmButton = {
                TextButton(onClick = { selectedReason = null }) {
                    Text("Close")
                }
            }
        )
    }

    if (aiUi.showEnableLocationDialog) {
        AlertDialog(
            onDismissRequest = { aiVm.dismissLocationDialog() },
            title = { Text("Enable location") },
            text = { Text("Turn on phone location to improve nearby place suggestions.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        aiVm.dismissLocationDialog()
                        context.startActivity(
                            android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                        )
                    }
                ) {
                    Text("Open settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { aiVm.dismissLocationDialog() }) {
                    Text("Not now")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.logo_1),
                            contentDescription = "Amoria logo",
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "Amoria AI",
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF3A1639),
                    titleContentColor = Color.White
                ),
                actions = {
                    IconButton(
                        onClick = {
                            if (aiUi.useLocation) {
                                aiVm.disableLocation()
                            } else {
                                permissionLauncher.launch(
                                    arrayOf(
                                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        },
                        enabled = enabledFeatures.locationSuggestionsEnabled
                    ) {
                        Icon(
                            imageVector = if (aiUi.useLocation) Icons.Default.LocationOn else Icons.Default.LocationOff,
                            contentDescription = "Toggle location",
                            tint = when {
                                !enabledFeatures.locationSuggestionsEnabled -> Color.White.copy(alpha = 0.25f)
                                aiUi.useLocation -> Color(0xFF4CAF50)
                                else -> Color.White.copy(alpha = 0.6f)
                            }
                        )
                    }

                    IconButton(onClick = onOpenAccount) {
                        Icon(
                            imageVector = Icons.Default.ManageAccounts,
                            contentDescription = "Manage account",
                            tint = Color.White
                        )
                    }

                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Logout",
                            tint = Color.White
                        )
                    }
                }
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = {
                scope.launch {
                    refreshing = true
                    friendsRoomVm.load()
                    featureConfigVm.refresh()
                    delay(700)
                    refreshing = false
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .background(bg)
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 18.dp, end = 18.dp, top = 28.dp, bottom = 105.dp)
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ModeRow(
                            text = "Default mode",
                            selected = selectedMode == SuggestionMode.DEFAULT,
                            enabled = enabledFeatures.defaultSuggestionsEnabled,
                            onClick = {
                                userManuallySelectedMode = true
                                selectedMode = SuggestionMode.DEFAULT
                                aiVm.clearSuggestions()
                            }
                        )

                        ModeRow(
                            text = "Friends mode",
                            selected = selectedMode == SuggestionMode.FRIENDS,
                            enabled = enabledFeatures.friendsRoomsEnabled,
                            onClick = {
                                userManuallySelectedMode = true
                                selectedMode = SuggestionMode.FRIENDS
                                aiVm.clearSuggestions()
                            }
                        )
                    }
                }

                if (selectedMode == SuggestionMode.FRIENDS && enabledFeatures.friendsRoomsEnabled) {
                    FriendsRoomCard(
                        friendsRoomUi = friendsRoomUi,
                        friendsRoomVm = friendsRoomVm,
                        onOpenFriendsQuiz = onOpenFriendsQuiz,
                        onOpenFriendsSuggestions = onOpenFriendsSuggestions
                    )
                }

                if (selectedMode == SuggestionMode.DEFAULT && enabledFeatures.defaultSuggestionsEnabled) {
                    DefaultSuggestionsSection(
                        aiUi = aiUi,
                        aiVm = aiVm,
                        onOpenSuggestions = onOpenSuggestions,
                        onOpenReason = { selectedReason = it }
                    )
                }

                if (
                    !enabledFeatures.defaultSuggestionsEnabled &&
                    !enabledFeatures.friendsRoomsEnabled
                ) {
                    FeatureUnavailableCard(
                        "The interactive modules are temporarily disabled by the administrator."
                    )
                }

                friendsRoomUi.message?.let { message ->
                    Text(
                        text = message,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun FriendsRoomCard(
    friendsRoomUi: com.example.amoriaiaplanner.feature_friends_room.vm.FriendsRoomUiState,
    friendsRoomVm: FriendsRoomViewModel,
    onOpenFriendsQuiz: () -> Unit,
    onOpenFriendsSuggestions: () -> Unit
) {
    val context = LocalContext.current
    val room = friendsRoomUi.activeRoom
    val members = friendsRoomUi.members
    val isHost = room?.hostUid == friendsRoomUi.myUid

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White.copy(alpha = 0.10f),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Friends room",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )

            if (room == null) {
                Text(
                    text = "Create a room and share the code with friends, or join a room using a code.",
                    color = Color(0xFFFFE6F0),
                    style = MaterialTheme.typography.bodyMedium
                )

                AmoriaPrimaryButton(
                    text = if (friendsRoomUi.loading) "Creating..." else "Create friends room",
                    onClick = { friendsRoomVm.createRoom() },
                    enabled = !friendsRoomUi.loading
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "OR Join a room",
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall
                )

                OutlinedTextField(
                    value = friendsRoomUi.enteredRoomCode,
                    onValueChange = friendsRoomVm::onRoomCodeChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Enter room code", color = Color.White.copy(alpha = 0.7f)) },
                    placeholder = { Text("6-digit code", color = Color.White.copy(alpha = 0.5f)) },
                    trailingIcon = {
                        if (friendsRoomUi.enteredRoomCode.isNotEmpty()) {
                            IconButton(onClick = { friendsRoomVm.onRoomCodeChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.White)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color.White
                    )
                )

                AmoriaSecondaryButton(
                    text = if (friendsRoomUi.loading) "Joining..." else "Join room",
                    onClick = { friendsRoomVm.joinRoom() },
                    enabled = friendsRoomUi.enteredRoomCode.length == 6 && !friendsRoomUi.loading
                )
            } else {
                Text(
                    text = "Room code: ${room.code}",
                    color = Color(0xFFFFD9E6),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Members: ${members.size}/${room.maxMembers}",
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    members.forEach { member ->
                        val nameToShow = when {
                            member.displayName.isNotBlank() -> member.displayName
                            member.email.isNotBlank() -> member.email.substringBefore("@")
                            else -> "Guest"
                        }
                        Text(
                            text = "• $nameToShow ${if (member.isHost) "(Host)" else ""}",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                if (room.status == RoomStatus.WAITING) {
                    if (isHost) {
                        AmoriaPrimaryButton(
                            text = if (members.size < 2) "Need at least 2 friends" else "Start group quiz",
                            onClick = { friendsRoomVm.startRoom() },
                            enabled = members.size >= 2 && !friendsRoomUi.loading
                        )
                    } else {
                        Text(
                            text = "Waiting for the host to start...",
                            color = Color(0xFFFFE6F0),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                if (room.status == RoomStatus.STARTED) {
                    val myMember = members.firstOrNull { it.uid == friendsRoomUi.myUid }
                    if (myMember?.hasFinishedQuiz == true) {
                        Text(
                            text = "Quiz submitted! Waiting for others to finish...",
                            color = Color(0xFFFFD9E6),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        AmoriaPrimaryButton(
                            text = "Open group quiz",
                            onClick = onOpenFriendsQuiz
                        )
                    }

                    if (isHost) {
                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                        Spacer(Modifier.height(16.dp))

                        Text(
                            text = "Group suggestions settings",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            text = "Add a specific preference or vibe for the AI",
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall
                        )

                        OutlinedTextField(
                            value = friendsRoomUi.extraPreference,
                            onValueChange = friendsRoomVm::onExtraPreferenceChange,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Example: specific area, quiet place, low budget...", color = Color.White.copy(alpha = 0.5f)) },
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFFF7AA2),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.22f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Color.White
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = friendsRoomUi.useLocation,
                                onCheckedChange = friendsRoomVm::setUseLocation,
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFFE14D72))
                            )
                            Text(
                                text = "Use host location for suggestions",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        AmoriaSecondaryButton(
                            text = if (friendsRoomUi.loading && friendsRoomUi.message?.contains("Generating") == true) "Generating..." else "Generate group suggestions",
                            onClick = { friendsRoomVm.startGroupSuggestionGeneration(context) },
                            enabled = !friendsRoomUi.loading
                        )
                    }
                }

                if (room.status == RoomStatus.COMPLETED) {
                    AmoriaPrimaryButton(
                        text = "View results",
                        onClick = onOpenFriendsSuggestions
                    )
                }

                Spacer(Modifier.height(8.dp))

                AmoriaGlassButton(
                    text = if (isHost) "Delete room" else "Leave room",
                    onClick = {
                        if (isHost) friendsRoomVm.deleteRoomCompletely()
                        else friendsRoomVm.leaveRoom()
                    },
                    icon = {
                        Icon(
                            imageVector = if (isHost) Icons.Default.Delete else Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun DefaultSuggestionsSection(
    aiUi: com.example.amoriaiaplanner.feature_home.vm.HomeSuggestionUiState,
    aiVm: HomeSuggestionViewModel,
    onOpenSuggestions: () -> Unit,
    onOpenReason: (PlaceSuggestion) -> Unit
) {
    val context = LocalContext.current
    ExtraPreferenceCard(
        value = aiUi.extraPreference,
        onValueChange = aiVm::onExtraPreferenceChange,
        title = "Personal preference",
        subtitle = "Tell the AI what you feel like today",
        placeholder = "Example: quiet café, low budget, close to me..."
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White.copy(alpha = 0.10f),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Place suggestions",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AmoriaPrimaryButton(
                    text = if (aiUi.loading) "Generating..." else "Suggest",
                    onClick = { aiVm.suggestSingle(context) },
                    enabled = !aiUi.loading,
                    modifier = Modifier.weight(1f)
                )

                AmoriaSecondaryButton(
                    text = "Open",
                    onClick = onOpenSuggestions,
                    enabled = aiUi.suggestions.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    if (aiUi.suggestions.isNotEmpty()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White.copy(alpha = 0.10f),
            shape = RoundedCornerShape(22.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "Latest suggestions", color = Color.White, style = MaterialTheme.typography.titleMedium)
                aiUi.suggestions.forEachIndexed { index, suggestion ->
                    HomeSuggestionCard(rank = index + 1, suggestion = suggestion, onOpenReason = { onOpenReason(suggestion) })
                }
            }
        }
    }
}

@Composable
private fun ModeRow(
    text: String,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val borderColor = if (selected) Color(0xFFFF7AA2) else Color.Transparent
    val backgroundColor = if (selected) Color.White.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.10f)

    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = if (enabled) backgroundColor else Color.White.copy(alpha = 0.05f),
        border = BorderStroke(width = if (selected) 2.dp else 0.dp, color = borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = if (text.contains("Friends")) Icons.Default.Groups else Icons.Default.Explore,
                contentDescription = null,
                tint = if (selected) Color(0xFFE14D72) else Color(0xFF7C4D9E),
                modifier = Modifier.size(32.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = text, color = Color.White, style = MaterialTheme.typography.titleMedium)
                Text(
                text = if (text.contains("Friends")) "Group outings with friends" else "Personal suggestions",
                    color = Color.White.copy(alpha = if (enabled) 0.75f else 0.40f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (selected) {
                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFFFF7AA2))
            }
        }
    }
}

@Composable
private fun FeatureUnavailableCard(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White.copy(alpha = 0.10f),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Functionality unavailable",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = message,
                color = Color.White.copy(alpha = 0.75f),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun ExtraPreferenceCard(
    value: String,
    onValueChange: (String) -> Unit,
    title: String,
    subtitle: String,
    placeholder: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        shape = RoundedCornerShape(26.dp),
        color = Color.White.copy(alpha = 0.13f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = title, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(placeholder, color = Color.White.copy(alpha = 0.5f)) },
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFFF7AA2),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.22f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
        }
    }
}

@Composable
private fun HomeSuggestionCard(rank: Int, suggestion: PlaceSuggestion, onOpenReason: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White.copy(alpha = 0.14f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = "$rank. ${suggestion.title}", color = Color.White, modifier = Modifier.weight(1f))
            IconButton(onClick = onOpenReason) {
                Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFFFD9E6))
            }
        }
    }
}
