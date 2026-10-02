package com.example.amoriaiaplanner.feature_friends_room.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.amoriaiaplanner.feature_ai.model.PlaceSuggestion
import com.example.amoriaiaplanner.feature_friends_room.data.FriendsRoomRepository
import com.example.amoriaiaplanner.feature_friends_room.model.FriendsQuizAnswer
import com.example.amoriaiaplanner.feature_friends_room.model.FriendsQuizQuestions
import com.example.amoriaiaplanner.feature_friends_room.model.RoomMember
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class FriendsQuizUiState(
    val currentQuestionIndex: Int = 0,
    val selectedAnswers: Map<String, List<String>> = emptyMap(),
    val submitted: Boolean = false,
    val loading: Boolean = false,
    val generating: Boolean = false,
    val secondsLeft: Int = 180,
    val message: String? = null,
    val suggestions: List<PlaceSuggestion> = emptyList()
)

class FriendsQuizViewModel(
    private val repo: FriendsRoomRepository = FriendsRoomRepository()
) : ViewModel() {

    private val _ui = MutableStateFlow(FriendsQuizUiState())
    val ui: StateFlow<FriendsQuizUiState> = _ui

    private var timerStarted = false

    fun startTimer(
        roomId: String,
        quizEndsAt: Long,
        isHost: Boolean
    ) {
        if (timerStarted) return
        timerStarted = true

        viewModelScope.launch {
            while (true) {
                val now = System.currentTimeMillis()
                val remainingMillis = (quizEndsAt - now).coerceAtLeast(0L)
                val remainingSeconds = (remainingMillis / 1000L).toInt()

                _ui.value = _ui.value.copy(secondsLeft = remainingSeconds)

                if (remainingSeconds <= 0) {
                    autoSubmitOnTimeout(
                        roomId = roomId,
                        isHost = isHost
                    )
                    break
                }

                delay(1000L)
            }
        }
    }

    fun selectAnswer(
        questionId: String,
        option: String,
        allowMultiple: Boolean
    ) {
        if (_ui.value.submitted) return

        val current = _ui.value.selectedAnswers[questionId].orEmpty()

        val updated = if (allowMultiple) {
            if (current.contains(option)) {
                current - option
            } else {
                current + option
            }
        } else {
            listOf(option)
        }

        _ui.value = _ui.value.copy(
            selectedAnswers = _ui.value.selectedAnswers + (questionId to updated),
            message = null
        )
    }

    fun nextQuestion() {
        val lastIndex = FriendsQuizQuestions.questions.lastIndex
        val current = _ui.value.currentQuestionIndex

        if (current < lastIndex) {
            _ui.value = _ui.value.copy(
                currentQuestionIndex = current + 1
            )
        }
    }

    fun previousQuestion() {
        val current = _ui.value.currentQuestionIndex

        if (current > 0) {
            _ui.value = _ui.value.copy(
                currentQuestionIndex = current - 1
            )
        }
    }

    fun submit(
        roomId: String,
        members: List<RoomMember>,
        isHost: Boolean
    ) {
        viewModelScope.launch {
            try {
                if (_ui.value.submitted) return@launch

                _ui.value = _ui.value.copy(
                    loading = true,
                    message = null
                )

                val answer = buildAnswer()

                repo.saveQuizAnswer(
                    roomId = roomId,
                    answer = answer
                )

                _ui.value = _ui.value.copy(
                    loading = false,
                    submitted = true,
                    message = "Your answers were submitted"
                )

                val shouldGenerate = repo.shouldGenerateSuggestions(roomId)

                if (shouldGenerate && isHost) {
                    tryGenerateSuggestions(roomId)
                }
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(
                    loading = false,
                    message = e.message ?: "Failed to submit answers"
                )
            }
        }
    }

    fun tryGenerateSuggestions(roomId: String) {
        viewModelScope.launch {
            try {
                val shouldGenerate = repo.shouldGenerateSuggestions(roomId)

                if (!shouldGenerate) {
                    _ui.value = _ui.value.copy(
                        message = "Waiting for all members or for the timer to finish."
                    )
                    return@launch
                }

                _ui.value = _ui.value.copy(
                    generating = true,
                    message = "Generating group suggestions..."
                )

                val suggestions = repo.generateAndSaveGroupSuggestions(roomId)

                _ui.value = _ui.value.copy(
                    generating = false,
                    suggestions = suggestions,
                    message = "Group suggestions generated"
                )
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(
                    generating = false,
                    message = e.message ?: "Failed to generate suggestions"
                )
            }
        }
    }

    private fun autoSubmitOnTimeout(
        roomId: String,
        isHost: Boolean
    ) {
        viewModelScope.launch {
            try {
                if (_ui.value.submitted) {
                    if (isHost) {
                        tryGenerateSuggestions(roomId)
                    }
                    return@launch
                }

                _ui.value = _ui.value.copy(
                    loading = true,
                    message = "Time is over. Submitting your current answers..."
                )

                val answer = buildAnswer()

                repo.saveQuizAnswer(
                    roomId = roomId,
                    answer = answer
                )

                _ui.value = _ui.value.copy(
                    loading = false,
                    submitted = true,
                    message = "Time is over. Your current answers were submitted."
                )

                if (isHost) {
                    tryGenerateSuggestions(roomId)
                }
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(
                    loading = false,
                    message = e.message ?: "Failed to auto-submit quiz"
                )
            }
        }
    }

    private fun buildAnswer(): FriendsQuizAnswer {
        fun manyAsText(id: String): String {
            return _ui.value.selectedAnswers[id]
                .orEmpty()
                .joinToString(separator = ", ")
        }

        fun many(id: String): List<String> {
            return _ui.value.selectedAnswers[id].orEmpty()
        }

        return FriendsQuizAnswer(
            groupMood = manyAsText("groupMood"),
            budget = manyAsText("budget"),
            outingType = manyAsText("outingType"),
            foodPreference = manyAsText("foodPreference"),
            energyLevel = manyAsText("energyLevel"),
            indoorOutdoor = manyAsText("indoorOutdoor"),
            distancePreference = manyAsText("distancePreference"),
            timePreference = manyAsText("timePreference"),
            activityPreferences = many("activityPreferences"),
            dealBreaker = manyAsText("dealBreaker")
        )
    }
}