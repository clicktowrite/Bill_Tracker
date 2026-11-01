package com.example.billtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.billtracker.domain.model.ThemeMode
import com.example.billtracker.ui.navigation.BillTrackerBottomBar
import com.example.billtracker.ui.navigation.BillTrackerNavHost
import com.example.billtracker.ui.navigation.NavRoutes
import com.example.billtracker.ui.theme.BillTrackerTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as BillTrackerApp
        val repository = app.repository

        setContent {
            val themeMode by repository.themeModeFlow.collectAsState(initial = ThemeMode.SYSTEM)

            BillTrackerTheme(themeMode = themeMode) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val showBottomBar = currentRoute in listOf(
                    NavRoutes.TENANTS,
                    NavRoutes.METERS,
                    NavRoutes.SETTINGS
                )

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (showBottomBar) {
                            BillTrackerBottomBar(
                                currentRoute = currentRoute,
                                onNavigateToDestination = { destination ->
                                    navController.navigate(destination.route) {
                                        popUpTo(NavRoutes.METERS) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    BillTrackerNavHost(
                        navController = navController,
                        repository = repository,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
