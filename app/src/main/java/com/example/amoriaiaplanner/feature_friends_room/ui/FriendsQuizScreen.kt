package com.example.amoriaiaplanner.feature_friends_room.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.amoriaiaplanner.core.ui.AmoriaOutlineButton
import com.example.amoriaiaplanner.core.ui.AmoriaPrimaryButton
import com.example.amoriaiaplanner.feature_friends_room.model.FriendRoom
import com.example.amoriaiaplanner.feature_friends_room.model.FriendsQuizQuestions
import com.example.amoriaiaplanner.feature_friends_room.model.RoomMember
import com.example.amoriaiaplanner.feature_friends_room.vm.FriendsQuizViewModel
import com.example.amoriaiaplanner.feature_friends_room.vm.FriendsRoomViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsQuizRoute(
    onBackHome: () -> Unit,
    roomVm: FriendsRoomViewModel = viewModel(),
    quizVm: FriendsQuizViewModel = viewModel()
) {
    val roomUi by roomVm.ui.collectAsState()

    LaunchedEffect(Unit) {
        roomVm.load()
    }

    val room = roomUi.activeRoom

    if (room == null) {
        LoadingQuizScreen(onBackHome = onBackHome)
        return
    }

    val isHost = room.hostUid == roomUi.myUid

    FriendsQuizScreen(
        room = room,
        members = roomUi.members,
        isHost = isHost,
        onFinished = onBackHome,
        vm = quizVm
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsQuizScreen(
    room: FriendRoom,
    members: List<RoomMember>,
    isHost: Boolean,
    onFinished: () -> Unit,
    vm: FriendsQuizViewModel = viewModel()
) {
    val ui by vm.ui.collectAsState()
    val questions = FriendsQuizQuestions.questions
    val question = questions[ui.currentQuestionIndex]

    val bg = Brush.linearGradient(
        listOf(
            Color(0xFFB3202E),
            Color(0xFF4B1E6D),
            Color(0xFF0B2B6B)
        )
    )

    LaunchedEffect(room.roomId, room.quizEndsAt) {
        vm.startTimer(
            roomId = room.roomId,
            quizEndsAt = room.quizEndsAt,
            isHost = isHost
        )
    }

    LaunchedEffect(ui.submitted) {
        if (ui.submitted) {
            onFinished()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Friends quiz",
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onFinished) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close quiz",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF3B0F35)
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
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(26.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Room ${room.code}",
                        color = Color(0xFFFFD9E6),
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = "Time left: ${formatSeconds(ui.secondsLeft)}",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall
                    )

                    LinearProgressIndicator(
                        progress = {
                            (ui.secondsLeft / 180f).coerceIn(0f, 1f)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFFFF7AA2),
                        trackColor = Color.White.copy(alpha = 0.25f)
                    )

                    Text(
                        text = "Question ${ui.currentQuestionIndex + 1}/${questions.size}",
                        color = Color(0xFFFFD9E6),
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Text(
                        text = "You can select more than one answer.",
                        color = Color.White.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = question.title,
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        question.options.forEach { option ->
                            val selected = ui.selectedAnswers[question.id]
                                .orEmpty()
                                .contains(option)

                            QuizOptionCard(
                                text = option,
                                selected = selected,
                                enabled = !ui.submitted,
                                onClick = {
                                    vm.selectAnswer(
                                        questionId = question.id,
                                        option = option,
                                        allowMultiple = question.allowMultiple
                                    )
                                }
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AmoriaOutlineButton(
                            text = "Back",
                            onClick = { vm.previousQuestion() },
                            enabled = ui.currentQuestionIndex > 0 && !ui.submitted,
                            modifier = Modifier.weight(1f)
                        )

                        if (ui.currentQuestionIndex < questions.lastIndex) {
                            AmoriaPrimaryButton(
                                text = "Next",
                                onClick = { vm.nextQuestion() },
                                enabled = !ui.submitted,
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            AmoriaPrimaryButton(
                                text = when {
                                    ui.loading -> "Submitting..."
                                    ui.submitted -> "Submitted"
                                    else -> "Submit"
                                },
                                onClick = {
                                    vm.submit(
                                        roomId = room.roomId,
                                        members = members,
                                        isHost = isHost
                                    )
                                },
                                enabled = !ui.loading && !ui.submitted,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    ui.message?.let {
                        Text(
                            text = it,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Text(
                text = "After submitting, you will return to the room and wait for the group suggestions.",
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun QuizOptionCard(
    text: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val background = if (selected) {
        Color.White.copy(alpha = 0.28f)
    } else {
        Color.White.copy(alpha = 0.10f)
    }

    val borderColor = if (selected) {
        Color(0xFFFFD9E6)
    } else {
        Color.White.copy(alpha = 0.18f)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        color = background,
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (selected) "✓  $text" else text,
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoadingQuizScreen(
    onBackHome: () -> Unit
) {
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
                    Text("Friends quiz", color = Color.White)
                },
                navigationIcon = {
                    IconButton(onClick = onBackHome) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF3B0F35)
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
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Loading room quiz...",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

private fun formatSeconds(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "%d:%02d".format(minutes, remainingSeconds)
}