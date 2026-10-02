package com.example.amoriaiaplanner.feature_friends_room.model

data class FriendsQuizQuestion(
    val id: String,
    val title: String,
    val options: List<String>,
    val allowMultiple: Boolean = true
)

object FriendsQuizQuestions {

    val questions = listOf(
        FriendsQuizQuestion(
            id = "groupMood",
            title = "What vibe should this outing have? Choose one or more.",
            options = listOf(
                "Chill",
                "Fun",
                "Adventurous",
                "Cozy",
                "Foodie",
                "Cultural"
            )
        ),
        FriendsQuizQuestion(
            id = "budget",
            title = "What budget feels good for you? Choose one or more.",
            options = listOf(
                "Low budget",
                "Medium budget",
                "High budget",
                "No strict budget"
            )
        ),
        FriendsQuizQuestion(
            id = "outingType",
            title = "What kind of place are you in the mood for? Choose one or more.",
            options = listOf(
                "Café",
                "Restaurant",
                "Dessert place",
                "Outdoor walk",
                "Game/activity place",
                "Cultural place"
            )
        ),
        FriendsQuizQuestion(
            id = "foodPreference",
            title = "What food or drink mood do you prefer? Choose one or more.",
            options = listOf(
                "Coffee / drinks",
                "Fast food",
                "Traditional food",
                "Sweet / dessert",
                "Healthy food",
                "No food needed"
            )
        ),
        FriendsQuizQuestion(
            id = "energyLevel",
            title = "How much energy do you want the outing to have? Choose one or more.",
            options = listOf(
                "Very calm",
                "Balanced",
                "Active",
                "Very energetic"
            )
        ),
        FriendsQuizQuestion(
            id = "indoorOutdoor",
            title = "Indoor or outdoor? Choose one or more.",
            options = listOf(
                "Indoor",
                "Outdoor",
                "Both are okay"
            )
        ),
        FriendsQuizQuestion(
            id = "distancePreference",
            title = "How far are you willing to go? Choose one or more.",
            options = listOf(
                "Very close",
                "Around the city",
                "A bit far is okay",
                "Distance does not matter"
            )
        ),
        FriendsQuizQuestion(
            id = "timePreference",
            title = "Best time for this outing? Choose one or more.",
            options = listOf(
                "Morning",
                "Afternoon",
                "Evening",
                "Night"
            )
        ),
        FriendsQuizQuestion(
            id = "activityPreferences",
            title = "Choose activities you would enjoy.",
            options = listOf(
                "Talking and chilling",
                "Eating together",
                "Walking",
                "Taking photos",
                "Playing games",
                "Discovering a new place"
            )
        ),
        FriendsQuizQuestion(
            id = "dealBreaker",
            title = "What should we avoid? Choose one or more.",
            options = listOf(
                "Too expensive",
                "Too noisy",
                "Too crowded",
                "Too far",
                "Only food places",
                "No deal breaker"
            )
        )
    )
}