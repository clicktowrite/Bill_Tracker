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
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.remember
import com.example.billtracker.domain.ai.GgufModelManager
import com.example.billtracker.domain.ai.LocalDocumentLibrary
import com.example.billtracker.domain.ai.LocalRagEngine
import com.example.billtracker.ui.components.FloatingAiAssistantOverlay
import com.example.billtracker.ui.navigation.NavRoutes
import com.example.billtracker.ui.theme.BillTrackerTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as BillTrackerApp
        val repository = app.repository

        setContent {
            val themeMode by repository.themeModeFlow.collectAsState(initial = ThemeMode.GLASSMORPHISM)

            // Local AI Architecture (isolated from billing core)
            val modelManager = remember { GgufModelManager(applicationContext) }
            val documentLibrary = remember { LocalDocumentLibrary(applicationContext) }
            val ragEngine = remember { LocalRagEngine(repository, documentLibrary, modelManager.inferenceEngine) }
            val activeModelId by modelManager.activeModelId.collectAsState()
            val models by modelManager.models.collectAsState()
            val activeModel = models.find { it.id == activeModelId }

            BillTrackerTheme(themeMode = themeMode) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val showBottomBar = currentRoute in listOf(
                    NavRoutes.TENANTS,
                    NavRoutes.METERS,
                    NavRoutes.SETTINGS
                )

                val showFloatingAssistant = currentRoute in listOf(
                    NavRoutes.TENANTS,
                    NavRoutes.METERS,
                    NavRoutes.SETTINGS,
                    NavRoutes.TENANT_DETAIL
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
                    Box(modifier = Modifier.fillMaxSize()) {
                        BillTrackerNavHost(
                            navController = navController,
                            repository = repository,
                            modelManager = modelManager,
                            documentLibrary = documentLibrary,
                            modifier = Modifier.padding(innerPadding)
                        )

                        if (showFloatingAssistant) {
                            FloatingAiAssistantOverlay(
                                activeModel = activeModel,
                                ragEngine = ragEngine,
                                onOpenModelManager = {
                                    navController.navigate(NavRoutes.AI_MODELS)
                                },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                }
            }
        }
    }
}
