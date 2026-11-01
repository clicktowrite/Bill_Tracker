package com.example.billtracker.ui.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun BillTrackerBottomBar(
    currentRoute: String?,
    onNavigateToDestination: (BottomBarDestination) -> Unit
) {
    val items = listOf(
        BottomBarDestination.Tenants,
        BottomBarDestination.Meters,
        BottomBarDestination.Settings
    )

    NavigationBar {
        items.forEach { destination ->
            val selected = currentRoute == destination.route
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigateToDestination(destination) },
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = destination.title
                    )
                },
                label = { Text(destination.title) }
            )
        }
    }
}
