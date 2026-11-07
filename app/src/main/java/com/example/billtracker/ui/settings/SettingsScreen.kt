package com.example.billtracker.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Description
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.billtracker.BillTrackerApp
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.SettingsBrightness
import com.example.billtracker.data.backup.BackupRestoreManager
import com.example.billtracker.data.backup.BackupValidationResult
import com.example.billtracker.domain.model.DarkModePreference
import com.example.billtracker.domain.model.ThemeMode
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onThemeSelect: (ThemeMode) -> Unit,
    onDarkModeSelect: (DarkModePreference) -> Unit = {},
    onNavigateToAiModels: () -> Unit = {},
    onNavigateToAiDocuments: () -> Unit = {},
    onCurrencySelect: (String) -> Unit = {},
    onRecordReadingTimeToggle: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val app = context.applicationContext as BillTrackerApp
    val currencies = listOf("₱", "$", "€", "£", "¥", "₹", "RM", "S$")

    var pendingRestoreValidation by remember { mutableStateOf<BackupValidationResult?>(null) }

    // SAF Launchers
    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            scope.launch {
                val success = BackupRestoreManager.createBackup(context, app.database, uri)
                if (success) {
                    Toast.makeText(context, "Backup created successfully!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Failed to create backup.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            scope.launch {
                val result = BackupRestoreManager.validateBackup(context, uri)
                if (result.isValid) {
                    pendingRestoreValidation = result
                } else {
                    Toast.makeText(context, result.errorMessage ?: "Invalid backup file.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Theme Setting: 4 Distinct Modern UI Themes with Visual Preview Cards
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SettingsBrightness,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.padding(4.dp))
                        Text(
                            text = "Appearance Mode",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Choose whether you prefer Light Mode, Night Mode, or following your Device System settings:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DarkModePreference.values().forEach { pref ->
                            val isPrefSelected = uiState.darkModePreference == pref
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onDarkModeSelect(pref) },
                                shape = MaterialTheme.shapes.medium,
                                border = if (isPrefSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isPrefSelected)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(vertical = 12.dp, horizontal = 6.dp)
                                        .fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = when (pref) {
                                            DarkModePreference.SYSTEM -> Icons.Default.SettingsBrightness
                                            DarkModePreference.LIGHT -> Icons.Default.LightMode
                                            DarkModePreference.DARK -> Icons.Default.DarkMode
                                        },
                                        contentDescription = null,
                                        tint = if (isPrefSelected)
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = when (pref) {
                                            DarkModePreference.SYSTEM -> "System"
                                            DarkModePreference.LIGHT -> "Light"
                                            DarkModePreference.DARK -> "Night"
                                        },
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isPrefSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isPrefSelected)
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.padding(4.dp))
                        Text(
                            text = "Appearance Theme Style",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Choose from 4 distinct modern visual designs featuring unique shapes, color palettes, and container styles:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    val systemIsDark = isSystemInDarkTheme()
                    val previewDark = when (uiState.darkModePreference) {
                        DarkModePreference.SYSTEM -> systemIsDark
                        DarkModePreference.LIGHT -> false
                        DarkModePreference.DARK -> true
                    }

                    ThemeMode.values().forEach { mode ->
                        val isSelected = uiState.themeMode == mode
                        ThemePreviewCard(
                            themeMode = mode,
                            isSelected = isSelected,
                            isDarkTheme = previewDark,
                            onSelect = { onThemeSelect(mode) }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }

            // Local AI Management Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.padding(4.dp))
                        Text(
                            text = "Local AI & Document Intelligence",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Manage offline GGUF AI models and customer billing documents for local RAG intelligence.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onNavigateToAiModels,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Memory, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("AI Models", maxLines = 1, softWrap = false)
                        }

                        OutlinedButton(
                            onClick = onNavigateToAiDocuments,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Description, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("AI Library", maxLines = 1, softWrap = false)
                        }
                    }
                }
            }

            // Record Reading Time Toggle Setting
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Record Reading Time",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Include exact time along with dates for meter readings",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = uiState.recordReadingTime,
                        onCheckedChange = onRecordReadingTimeToggle
                    )
                }
            }

            // Currency Setting
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AttachMoney,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.padding(4.dp))
                        Text(
                            text = "Currency Symbol",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        currencies.take(4).forEach { symbol ->
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onCurrencySelect(symbol) },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (uiState.currencySymbol == symbol)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Text(
                                    text = symbol,
                                    modifier = Modifier
                                        .padding(vertical = 12.dp)
                                        .fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        currencies.drop(4).forEach { symbol ->
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onCurrencySelect(symbol) },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (uiState.currencySymbol == symbol)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Text(
                                    text = symbol,
                                    modifier = Modifier
                                        .padding(vertical = 12.dp)
                                        .fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = uiState.currencySymbol,
                        onValueChange = onCurrencySelect,
                        label = { Text("Custom Currency Symbol") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            // Backup & Restore Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Backup,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.padding(4.dp))
                        Text(
                            text = "Backup and Restore",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Safely export or restore your utility billing database offline.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                createBackupLauncher.launch("BillTracker_Backup_${System.currentTimeMillis()}.json")
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Backup, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Create Backup")
                        }

                        OutlinedButton(
                            onClick = {
                                restoreBackupLauncher.launch(arrayOf("application/json", "*/*"))
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Restore, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Restore")
                        }
                    }
                }
            }

            // About App Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.padding(4.dp))
                        Text(
                            text = "About Bill Tracker",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Smart offline utility billing management app designed for renters, landlords, boarding houses, and apartments.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    ListItem(
                        headlineContent = { Text("Version") },
                        supportingContent = { Text("2.1.0 (Live Forecasts & Timestamps)") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Restore Confirmation Modal Dialog
    pendingRestoreValidation?.let { valResult ->
        AlertDialog(
            onDismissRequest = { pendingRestoreValidation = null },
            title = { Text("Confirm Database Restore", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("The selected backup file was verified successfully. It contains:")
                    Text("• ${valResult.tenantCount} Renters / Tenants")
                    Text("• ${valResult.meterCount} Electricity/Water Meters")
                    Text("• ${valResult.readingCount} Billing History Records")
                    Text("• ${valResult.paymentCount} Payment Records")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Warning: Restoring will overwrite current database records with the backup. Do you want to proceed?",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val json = valResult.jsonContent
                        pendingRestoreValidation = null
                        if (json != null) {
                            scope.launch {
                                val success = BackupRestoreManager.restoreFromValidatedBackup(app.database, json)
                                if (success) {
                                    Toast.makeText(context, "Database restored successfully!", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Restoration failed.", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                ) {
                    Text("Proceed Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestoreValidation = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ThemePreviewCard(
    themeMode: ThemeMode,
    isSelected: Boolean,
    isDarkTheme: Boolean = themeMode.isDark,
    onSelect: () -> Unit
) {
    val (cardBg, surfaceColor, primaryAccent, secondaryAccent, textColor, cardShape, borderStroke) = when (themeMode) {
        ThemeMode.FUTURISTIC -> if (isDarkTheme) {
            PreviewConfig(
                background = Color(0xFF030712),
                surface = Color(0xFF0B1329),
                primary = Color(0xFF00F2FE),
                secondary = Color(0xFF9D4EDD),
                textColor = Color(0xFFF0F6FC),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.5.dp, Color(0xFF00F2FE).copy(alpha = if (isSelected) 0.9f else 0.35f))
            )
        } else {
            PreviewConfig(
                background = Color(0xFFF0F4F8),
                surface = Color(0xFFFFFFFF),
                primary = Color(0xFF007A87),
                secondary = Color(0xFF7B1FA2),
                textColor = Color(0xFF0F172A),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.5.dp, Color(0xFF007A87).copy(alpha = if (isSelected) 0.9f else 0.35f))
            )
        }
        ThemeMode.LIQUID_GLASS -> if (isDarkTheme) {
            PreviewConfig(
                background = Color(0xFF050E17),
                surface = Color(0xFF0F2338),
                primary = Color(0xFF38BDF8),
                secondary = Color(0xFF2DD4BF),
                textColor = Color(0xFFE2E8F0),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.5.dp, Color(0xFF38BDF8).copy(alpha = if (isSelected) 0.9f else 0.35f))
            )
        } else {
            PreviewConfig(
                background = Color(0xFFF0F9FF),
                surface = Color(0xFFFFFFFF),
                primary = Color(0xFF0284C7),
                secondary = Color(0xFF0D9488),
                textColor = Color(0xFF082F49),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.5.dp, Color(0xFF0284C7).copy(alpha = if (isSelected) 0.9f else 0.35f))
            )
        }
        ThemeMode.CLAYMORPHISM -> if (isDarkTheme) {
            PreviewConfig(
                background = Color(0xFF1E1F29),
                surface = Color(0xFF282937),
                primary = Color(0xFFA29BFE),
                secondary = Color(0xFF55EFC4),
                textColor = Color(0xFFF5F6FA),
                shape = RoundedCornerShape(26.dp),
                border = BorderStroke(2.dp, if (isSelected) Color(0xFFA29BFE) else Color(0xFF343648))
            )
        } else {
            PreviewConfig(
                background = Color(0xFFF0EDF6),
                surface = Color(0xFFFFFFFF),
                primary = Color(0xFF6C5CE7),
                secondary = Color(0xFF00B894),
                textColor = Color(0xFF2D3436),
                shape = RoundedCornerShape(26.dp),
                border = BorderStroke(2.dp, if (isSelected) Color(0xFF6C5CE7) else Color(0xFFFFFFFF))
            )
        }
        ThemeMode.GLASSMORPHISM -> if (isDarkTheme) {
            PreviewConfig(
                background = Color(0xFF0B0F19),
                surface = Color(0xFF131B2E),
                primary = Color(0xFF60A5FA),
                secondary = Color(0xFF2DD4BF),
                textColor = Color(0xFFF8FAFC),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, if (isSelected) Color(0xFF60A5FA) else Color(0xFF334155))
            )
        } else {
            PreviewConfig(
                background = Color(0xFFF1F5F9),
                surface = Color(0xFFFFFFFF),
                primary = Color(0xFF2563EB),
                secondary = Color(0xFF0D9488),
                textColor = Color(0xFF0F172A),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, if (isSelected) Color(0xFF2563EB) else Color(0xFFCBD5E1))
            )
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        shape = cardShape,
        border = borderStroke,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .background(cardBg)
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = themeMode.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Text(
                        text = themeMode.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor.copy(alpha = 0.75f)
                    )
                }

                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Active Theme",
                        tint = primaryAccent,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Mini visual mockup preview of theme UI elements
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(cardShape)
                    .background(surfaceColor)
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Swatches
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(primaryAccent)
                        )
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(secondaryAccent)
                        )
                        Text(
                            text = if (isDarkTheme) "Night Mode" else "Light Mode",
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Mini preview button
                    Box(
                        modifier = Modifier
                            .clip(cardShape)
                            .background(primaryAccent)
                            .clickable { onSelect() }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isSelected) "Active" else "Apply Theme",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkTheme && (themeMode == ThemeMode.FUTURISTIC || themeMode == ThemeMode.LIQUID_GLASS)) Color(0xFF030712) else Color.White
                        )
                    }
                }
            }
        }
    }
}

private data class PreviewConfig(
    val background: Color,
    val surface: Color,
    val primary: Color,
    val secondary: Color,
    val textColor: Color,
    val shape: androidx.compose.ui.graphics.Shape,
    val border: BorderStroke
)
