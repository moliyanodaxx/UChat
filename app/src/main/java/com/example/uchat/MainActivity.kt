package com.example.uchat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.uchat.ui.screens.*
import com.example.uchat.ui.theme.UChatTheme
import com.example.uchat.viewmodel.ChatViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            UChatTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    UChatApp()
                }
            }
        }
    }
}

@Composable
fun UChatApp() {
    val navController = rememberNavController()
    val vm: ChatViewModel = viewModel()

    val isLoggedIn by vm.isLoggedIn.collectAsState()
    val needProfile by vm.needProfile.collectAsState()

    LaunchedEffect(isLoggedIn, needProfile) {
        when {
            !isLoggedIn -> navController.navigate("login") {
                popUpTo(0) { inclusive = true }
            }
            needProfile -> navController.navigate("profile") {
                popUpTo(0) { inclusive = true }
            }
            else -> navController.navigate("rooms") {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        composable("login") {
            LoginScreen(
                vm = vm,
                onLoginSuccess = {
                    navController.navigate("rooms") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNeedProfile = {
                    navController.navigate("profile") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("profile") {
            ProfileScreen(
                vm = vm,
                isEditMode = false,
                onSave = {
                    navController.navigate("rooms") {
                        popUpTo("profile") { inclusive = true }
                    }
                }
            )
        }

        composable("profile_edit") {
            ProfileScreen(
                vm = vm,
                isEditMode = true,
                onSave = { navController.popBackStack() },
                onDismiss = { navController.popBackStack() }
            )
        }

        composable("rooms") {
            RoomListScreen(
                vm = vm,
                onRoomClick = { roomId ->
                    navController.navigate("chat/$roomId")
                },
                onFriendsClick = {
                    navController.navigate("friends")
                },
                onProfileClick = {
                    navController.navigate("profile_edit")
                }
            )
        }

        composable(
            route = "chat/{roomId}",
            arguments = listOf(navArgument("roomId") { type = NavType.StringType })
        ) { backStackEntry ->
            val roomId = backStackEntry.arguments?.getString("roomId") ?: ""
            ChatScreen(
                roomId = roomId,
                vm = vm,
                onBack = { navController.popBackStack() }
            )
        }

        composable("friends") {
            FriendsScreen(
                vm = vm,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
