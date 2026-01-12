package com.example.subscription

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.subscription.ui.addedit.AddEditScreen
import com.example.subscription.ui.dashboard.DashboardScreen
import com.example.subscription.ui.detail.DetailScreen
import com.example.subscription.ui.settings.SettingsScreen
import com.example.subscription.ui.theme.SubscriptionTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SubscriptionTheme {
                val navController = rememberNavController()
                
                NavHost(
                    navController = navController,
                    startDestination = "dashboard",
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable("dashboard") {
                        DashboardScreen(navController = navController)
                    }
                    
                    composable(
                        route = "add_edit?subId={subId}",
                        arguments = listOf(
                            navArgument("subId") {
                                type = NavType.StringType
                                defaultValue = "-1"
                                nullable = false
                            }
                        )
                    ) {
                        AddEditScreen(navController = navController)
                    }
                    
                    composable(
                        route = "detail/{subId}",
                        arguments = listOf(
                            navArgument("subId") {
                                type = NavType.StringType
                            }
                        )
                    ) {
                        DetailScreen(navController = navController)
                    }
                    
                    composable("settings") {
                        SettingsScreen(navController = navController)
                    }
                }
            }
        }
    }
}