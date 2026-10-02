package com.example.amoriaiaplanner.core.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.getValue
import com.example.amoriaiaplanner.core.ui.AmoriaBottomBar
import com.example.amoriaiaplanner.core.ui.BottomBarItem
import com.example.amoriaiaplanner.feature_account.ui.AccountScreen
import com.example.amoriaiaplanner.feature_admin.ui.AdminDashboardScreen
import com.example.amoriaiaplanner.feature_auth.ui.BlockedAccountScreen
import com.example.amoriaiaplanner.feature_auth.ui.ForcePasswordChangeScreen
import com.example.amoriaiaplanner.feature_auth.ui.LoginScreen
import com.example.amoriaiaplanner.feature_friends_room.ui.FriendsQuizRoute
import com.example.amoriaiaplanner.feature_friends_room.ui.FriendsRoomSuggestionsRoute
import com.example.amoriaiaplanner.feature_home.ui.HomeScreen
import com.example.amoriaiaplanner.feature_onboarding.ui.OnboardingScreen
import com.example.amoriaiaplanner.feature_profile.data.ProfileRepository
import com.example.amoriaiaplanner.feature_profile.model.UserRole
import com.example.amoriaiaplanner.feature_sign_up.ui.SignUpScreen
import com.example.amoriaiaplanner.feature_suggestions.ui.SuggestionsScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun AppNav() {
    val nav = rememberNavController()
    val startRoute = remember { mutableStateOf<String?>(null) }
    val profileRepo = remember { ProfileRepository() }

    LaunchedEffect(Unit) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            startRoute.value = Routes.LOGIN
        } else {
            user.reload()
            val appUser = profileRepo.getCurrentAppUser()
            when {
                appUser == null -> {
                    profileRepo.ensureUserDocumentsExist()
                    val refreshed = profileRepo.getCurrentAppUser()
                    startRoute.value = when {
                        refreshed?.isBlocked == true -> Routes.BLOCKED
                        refreshed?.mustChangePassword == true -> Routes.FORCE_PASSWORD_CHANGE
                        refreshed?.role == UserRole.ADMIN -> Routes.ADMIN_DASHBOARD
                        profileRepo.isProfileCompleted() -> Routes.HOME
                        else -> Routes.ONBOARDING
                    }
                }
                appUser.isBlocked -> startRoute.value = Routes.BLOCKED
                appUser.mustChangePassword -> startRoute.value = Routes.FORCE_PASSWORD_CHANGE
                appUser.role == UserRole.ADMIN -> startRoute.value = Routes.ADMIN_DASHBOARD
                else -> {
                    val profileCompleted = profileRepo.isProfileCompleted()
                    startRoute.value = if (profileCompleted) Routes.HOME else Routes.ONBOARDING
                }
            }
        }
    }

    val start = startRoute.value ?: return
    val backStackEntry by nav.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val latestRoute = rememberUpdatedState(currentRoute)

    DisposableEffect(FirebaseAuth.getInstance().currentUser?.uid) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            onDispose { }
        } else {
            val registration = FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null || !snapshot.exists()) {
                        return@addSnapshotListener
                    }

                    val route = latestRoute.value
                    val isBlocked = snapshot.getBoolean("isBlocked") == true
                    val mustChangePassword = snapshot.getBoolean("mustChangePassword") == true
                    val role = snapshot.getString("role") ?: UserRole.USER

                    when {
                        isBlocked && route != Routes.BLOCKED -> {
                            nav.navigate(Routes.BLOCKED) {
                                popUpTo(0)
                            }
                        }
                        !isBlocked && route == Routes.BLOCKED -> {
                            nav.navigate(Routes.POST_LOGIN_CHECK) {
                                popUpTo(0)
                            }
                        }
                        mustChangePassword && route != Routes.FORCE_PASSWORD_CHANGE -> {
                            nav.navigate(Routes.FORCE_PASSWORD_CHANGE) {
                                popUpTo(0)
                            }
                        }
                        !mustChangePassword && route == Routes.FORCE_PASSWORD_CHANGE -> {
                            nav.navigate(Routes.POST_LOGIN_CHECK) {
                                popUpTo(0)
                            }
                        }
                        role == UserRole.ADMIN &&
                            route != Routes.ADMIN_DASHBOARD -> {
                            nav.navigate(Routes.ADMIN_DASHBOARD) {
                                popUpTo(0)
                            }
                        }
                        role != UserRole.ADMIN && route == Routes.ADMIN_DASHBOARD -> {
                            nav.navigate(Routes.POST_LOGIN_CHECK) {
                                popUpTo(0)
                            }
                        }
                    }
                }

            onDispose {
                registration.remove()
            }
        }
    }

    val showBottomBar = currentRoute in listOf(
        Routes.HOME,
        Routes.SUGGESTIONS,
        Routes.FRIENDS_ROOM_SUGGESTIONS
    )

    val selectedBottomItem = when (currentRoute) {
        Routes.ONBOARDING -> BottomBarItem.PROFILE
        else -> BottomBarItem.HOME
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = nav,
            startDestination = start,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(Routes.LOGIN) {
                LoginScreen(
                    onLoginSuccess = {
                        nav.navigate(Routes.POST_LOGIN_CHECK) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    },
                    onGoToSignup = {
                        nav.navigate(Routes.SIGN_UP)
                    }
                )
            }

            composable(Routes.SIGN_UP) {
                SignUpScreen(
                    onBackToLogin = { nav.popBackStack() },
                    onSignUpSuccess = { _ -> nav.popBackStack() }
                )
            }

            composable(Routes.POST_LOGIN_CHECK) {
                LaunchedEffect(Unit) {
                    val appUser = profileRepo.getCurrentAppUser()
                    val destination = when {
                        appUser?.isBlocked == true -> Routes.BLOCKED
                        appUser?.mustChangePassword == true -> Routes.FORCE_PASSWORD_CHANGE
                        appUser?.role == UserRole.ADMIN -> Routes.ADMIN_DASHBOARD
                        profileRepo.isProfileCompleted() -> Routes.HOME
                        else -> Routes.ONBOARDING
                    }

                    nav.navigate(destination) {
                        popUpTo(Routes.POST_LOGIN_CHECK) { inclusive = true }
                    }
                }
            }

            composable(Routes.ADMIN_DASHBOARD) {
                AdminDashboardScreen(
                    onLogout = {
                        FirebaseAuth.getInstance().signOut()
                        nav.navigate(Routes.LOGIN) {
                            popUpTo(0)
                        }
                    }
                )
            }

            composable(Routes.FORCE_PASSWORD_CHANGE) {
                ForcePasswordChangeScreen(
                    onPasswordChanged = {
                        nav.navigate(Routes.POST_LOGIN_CHECK) {
                            popUpTo(0)
                        }
                    },
                    onLogout = {
                        FirebaseAuth.getInstance().signOut()
                        nav.navigate(Routes.LOGIN) {
                            popUpTo(0)
                        }
                    }
                )
            }

            composable(Routes.BLOCKED) {
                BlockedAccountScreen(
                    onLogout = {
                        FirebaseAuth.getInstance().signOut()
                        nav.navigate(Routes.LOGIN) {
                            popUpTo(0)
                        }
                    }
                )
            }

            composable(Routes.ONBOARDING) {
                OnboardingScreen(
                    onFinished = {
                        nav.navigate(Routes.HOME) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                        }
                    },
                    onCancel = {
                        nav.popBackStack()
                    }
                )
            }

            composable(Routes.HOME) {
                HomeScreen(
                    onLogout = {
                        FirebaseAuth.getInstance().signOut()
                        nav.navigate(Routes.LOGIN) {
                            popUpTo(Routes.HOME) { inclusive = true }
                        }
                    },
                    onOpenAccount = {
                        nav.navigate(Routes.ACCOUNT)
                    },
                    onOpenQuiz = {
                        nav.navigate(Routes.ONBOARDING)
                    },
                    onOpenFriendsQuiz = {
                        nav.navigate(Routes.FRIENDS_QUIZ)
                    },
                    onOpenSuggestions = {
                        nav.navigate(Routes.SUGGESTIONS)
                    },
                    onOpenFriendsSuggestions = {
                        nav.navigate(Routes.FRIENDS_ROOM_SUGGESTIONS)
                    }
                )
            }

            composable(Routes.ACCOUNT) {
                AccountScreen(
                    onBack = { nav.popBackStack() },
                    onAccountDeleted = {
                        nav.navigate(Routes.LOGIN) {
                            popUpTo(0)
                        }
                    }
                )
            }

            composable(Routes.SUGGESTIONS) {
                SuggestionsScreen(
                    onBack = { nav.popBackStack() }
                )
            }

            composable(Routes.FRIENDS_QUIZ) {
                FriendsQuizRoute(
                    onBackHome = { nav.popBackStack() }
                )
            }

            composable(Routes.FRIENDS_ROOM_SUGGESTIONS) {
                FriendsRoomSuggestionsRoute(
                    onBack = { nav.popBackStack() }
                )
            }
        }

        if (showBottomBar) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 4.dp)
            ) {
                AmoriaBottomBar(
                    selected = selectedBottomItem,
                    onHomeClick = {
                        nav.navigate(Routes.HOME) {
                            popUpTo(Routes.HOME) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    onManageProfileClick = {
                        nav.navigate(Routes.ONBOARDING)
                    },
                    onLogoutClick = {
                        FirebaseAuth.getInstance().signOut()
                        nav.navigate(Routes.LOGIN) {
                            popUpTo(0)
                        }
                    }
                )
            }
        }
    }
}
