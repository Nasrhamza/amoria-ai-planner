package com.example.amoriaiaplanner.feature_onboarding.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.amoriaiaplanner.core.ui.AmoriaPrimaryButton
import com.example.amoriaiaplanner.feature_onboarding.vm.OnboardingViewModel
import kotlinx.coroutines.launch
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    onCancel: () -> Unit,
    vm: OnboardingViewModel = viewModel()
) {
    val ui by vm.ui.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        vm.loadExistingProfile()
    }

    val bg = Brush.linearGradient(
        listOf(
            Color(0xFFB3202E),
            Color(0xFF4B1E6D),
            Color(0xFF0B2B6B)
        )
    )

    val relationshipStatusOptions = listOf(
        "Dating",
        "Engaged",
        "Married",
        "Long-distance"
    )

    val relationshipDurationOptions = listOf(
        "Less than 3 months",
        "3-6 months",
        "6-12 months",
        "1-3 years",
        "3+ years"
    )

    val dateFrequencyOptions = listOf(
        "Rarely",
        "Once a week",
        "A few times a week",
        "Almost every day"
    )

    val budgetOptions = listOf(
        "Low (budget-friendly)",
        "Medium",
        "High",
        "Flexible"
    )

    val lifestyleOptions = listOf(
        "Both students",
        "Working couple",
        "One working, one studying",
        "Busy schedules"
    )

    val transportationOptions = listOf(
        "Car",
        "Taxi",
        "Public transport",
        "Walking"
    )

    val availabilityOptions = listOf(
        "Weekday evenings",
        "Weekends",
        "Flexible",
        "Late nights"
    )

    val outingEnergyOptions = listOf(
        "Chill & relaxed",
        "Balanced",
        "Active & adventurous"
    )

    val datePreferenceOptions = listOf(
        "Coffee",
        "Restaurants",
        "Walks / nature",
        "Shopping",
        "Activities / games",
        "Beach",
        "Cultural places"
    )

    val indoorOutdoorOptions = listOf(
        "Indoor",
        "Outdoor",
        "Both"
    )

    val questionCount = 10
    val pageCount = 11
    val pagerState = rememberPagerState(pageCount = { pageCount })
    val maxUnlockedPage = remember { mutableIntStateOf(0) }

    fun isPageAnswered(page: Int): Boolean {
        return when (page) {
            0 -> ui.relationshipStatus.isNotBlank()
            1 -> ui.relationshipDuration.isNotBlank()
            2 -> ui.dateFrequency.isNotBlank()
            3 -> ui.budget.isNotBlank()
            4 -> ui.lifestyle.isNotBlank()
            5 -> ui.transportation.isNotBlank()
            6 -> ui.availability.isNotBlank()
            7 -> ui.outingEnergy.isNotBlank()
            8 -> ui.datePreferences.size >= 2
            9 -> ui.indoorOutdoorPreference.isNotBlank()
            10 -> true
            else -> false
        }
    }

    suspend fun animateToPage(page: Int) {
        pagerState.animateScrollToPage(
            page = page,
            animationSpec = tween(
                durationMillis = 450,
                easing = FastOutSlowInEasing
            )
        )
    }

    LaunchedEffect(
        ui.relationshipStatus,
        ui.relationshipDuration,
        ui.dateFrequency,
        ui.budget,
        ui.lifestyle,
        ui.transportation,
        ui.availability,
        ui.outingEnergy,
        ui.datePreferences,
        ui.indoorOutdoorPreference,
        pagerState.currentPage
    ) {
        val current = pagerState.currentPage
        if (current < questionCount && isPageAnswered(current)) {
            maxUnlockedPage.intValue = max(maxUnlockedPage.intValue, current + 1)
        }
    }

    fun nextPage() {
        val current = pagerState.currentPage
        if (!isPageAnswered(current)) return
        if (current < pageCount - 1) {
            scope.launch {
                maxUnlockedPage.intValue = max(maxUnlockedPage.intValue, current + 1)
                animateToPage(current + 1)
            }
        }
    }

    fun previousPage() {
        val current = pagerState.currentPage
        if (current > 0) {
            scope.launch { animateToPage(current - 1) }
        }
    }

    fun goToPage(page: Int) {
        scope.launch { animateToPage(page) }
    }

    val progress = if (pagerState.currentPage >= questionCount) 1f
    else (pagerState.currentPage + 1) / questionCount.toFloat()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Complete your profile",
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge
                        )
                        if (pagerState.currentPage < questionCount) {
                            Text(
                                text = "Question ${pagerState.currentPage + 1} of $questionCount",
                                color = Color(0xFFFFD9E6),
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else {
                            Text(
                                text = "Review your answers",
                                color = Color(0xFFFFD9E6),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel onboarding",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF3A1639),
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color.Transparent,
        bottomBar = {
            Surface(color = Color(0x22000000), tonalElevation = 0.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 12.dp)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = { previousPage() },
                        enabled = pagerState.currentPage > 0,
                        modifier = Modifier.weight(1f).height(56.dp)
                    ) {
                        Text("Previous", color = Color.White)
                    }

                    if (pagerState.currentPage < questionCount) {
                        AmoriaPrimaryButton(
                            text = "Next",
                            onClick = { nextPage() },
                            enabled = isPageAnswered(pagerState.currentPage),
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        AmoriaPrimaryButton(
                            text = if (ui.loading) "Saving..." else "Finish",
                            onClick = { vm.save(onFinished) },
                            enabled = !ui.loading,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bg)
                .padding(padding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = Color(0xFFFFD9E6),
                    trackColor = Color.White.copy(alpha = 0.18f)
                )

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = false
                ) { page ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        when (page) {
                            0 -> SectionCard("1. What best describes your situation?") {
                                relationshipStatusOptions.forEach { option ->
                                    RadioRow(
                                        text = option,
                                        selected = ui.relationshipStatus == option,
                                        onClick = { vm.setRelationshipStatus(option) }
                                    )
                                }
                            }

                            1 -> SectionCard("2. How long have you been together?") {
                                relationshipDurationOptions.forEach { option ->
                                    RadioRow(
                                        text = option,
                                        selected = ui.relationshipDuration == option,
                                        onClick = { vm.setRelationshipDuration(option) }
                                    )
                                }
                            }

                            2 -> SectionCard("3. How often do you go out together?") {
                                dateFrequencyOptions.forEach { option ->
                                    RadioRow(
                                        text = option,
                                        selected = ui.dateFrequency == option,
                                        onClick = { vm.setDateFrequency(option) }
                                    )
                                }
                            }

                            3 -> SectionCard("4. What's your usual budget for a date?") {
                                budgetOptions.forEach { option ->
                                    RadioRow(
                                        text = option,
                                        selected = ui.budget == option,
                                        onClick = { vm.setBudget(option) }
                                    )
                                }
                            }

                            4 -> SectionCard("5. What's your current lifestyle?") {
                                lifestyleOptions.forEach { option ->
                                    RadioRow(
                                        text = option,
                                        selected = ui.lifestyle == option,
                                        onClick = { vm.setLifestyle(option) }
                                    )
                                }
                            }

                            5 -> SectionCard("6. How do you usually get around?") {
                                transportationOptions.forEach { option ->
                                    RadioRow(
                                        text = option,
                                        selected = ui.transportation == option,
                                        onClick = { vm.setTransportation(option) }
                                    )
                                }
                            }

                            6 -> SectionCard("7. When do you usually go out?") {
                                availabilityOptions.forEach { option ->
                                    RadioRow(
                                        text = option,
                                        selected = ui.availability == option,
                                        onClick = { vm.setAvailability(option) }
                                    )
                                }
                            }

                            7 -> SectionCard("8. What kind of outings do you prefer?") {
                                outingEnergyOptions.forEach { option ->
                                    RadioRow(
                                        text = option,
                                        selected = ui.outingEnergy == option,
                                        onClick = { vm.setOutingEnergy(option) }
                                    )
                                }
                            }

                            8 -> SectionCard("9. What kind of dates do you enjoy? (Pick 2-5)") {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    datePreferenceOptions.forEach { option ->
                                        val selected = ui.datePreferences.contains(option)
                                        AssistChip(
                                            onClick = { vm.toggleDatePreference(option) },
                                            label = {
                                                Text(
                                                    text = option,
                                                    color = if (selected) Color.White else Color(0xFFF6EFFF)
                                                )
                                            },
                                            colors = AssistChipDefaults.assistChipColors(
                                                containerColor = if (selected) Color(0xFFE14D72) else Color(0x33FFFFFF),
                                                labelColor = if (selected) Color.White else Color(0xFFF6EFFF)
                                            ),
                                            border = AssistChipDefaults.assistChipBorder(
                                                enabled = true,
                                                borderColor = if (selected) Color(0xFFFFB3C7) else Color(0x66FFFFFF),
                                                borderWidth = 1.dp
                                            )
                                        )
                                    }
                                }

                                Spacer(Modifier.height(8.dp))
                                Text("${ui.datePreferences.size}/5 selected", color = Color(0xFFFFD9E6))
                                Text(
                                    text = if (ui.datePreferences.isEmpty()) "Selected: none"
                                    else "Selected: ${ui.datePreferences.joinToString()}",
                                    color = Color(0xFFFFE6F0)
                                )
                            }

                            9 -> SectionCard("10. Do you prefer indoor or outdoor dates?") {
                                indoorOutdoorOptions.forEach { option ->
                                    RadioRow(
                                        text = option,
                                        selected = ui.indoorOutdoorPreference == option,
                                        onClick = { vm.setIndoorOutdoorPreference(option) }
                                    )
                                }
                            }

                            10 -> SectionCard("Review your answers") {
                                ReviewRow("Situation", ui.relationshipStatus) { goToPage(0) }
                                ReviewRow("Relationship duration", ui.relationshipDuration) { goToPage(1) }
                                ReviewRow("Date frequency", ui.dateFrequency) { goToPage(2) }
                                ReviewRow("Budget", ui.budget) { goToPage(3) }
                                ReviewRow("Lifestyle", ui.lifestyle) { goToPage(4) }
                                ReviewRow("Transportation", ui.transportation) { goToPage(5) }
                                ReviewRow("Availability", ui.availability) { goToPage(6) }
                                ReviewRow("Outing style", ui.outingEnergy) { goToPage(7) }
                                ReviewRow("Date preferences", ui.datePreferences.joinToString()) { goToPage(8) }
                                ReviewRow("Indoor / Outdoor", ui.indoorOutdoorPreference) { goToPage(9) }
                            }
                        }

                        if (ui.message != null) {
                            Text(
                                text = ui.message!!,
                                color = if (ui.loading) Color.White else Color(0xFFFFC2C2)
                            )
                        }

                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White.copy(alpha = 0.10f),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
            content()
        }
    }
}

@Composable
private fun RadioRow(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = Color(0xFFFF7AA2),
                unselectedColor = Color(0xFFE7D9F7)
            )
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun ReviewRow(label: String, value: String, onEdit: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = Color(0xFFFFBFD1),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onEdit, modifier = Modifier.height(48.dp)) {
                Text("Edit", color = Color(0xFFFF9FBC))
            }
        }

        Text(
            text = if (value.isBlank()) "-" else value,
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
