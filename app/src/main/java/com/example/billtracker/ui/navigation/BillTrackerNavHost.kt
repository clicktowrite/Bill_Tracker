package com.example.billtracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.billtracker.data.repository.BillTrackerRepository
import com.example.billtracker.domain.model.UtilityType
import com.example.billtracker.ui.history.BillDetailScreen
import com.example.billtracker.ui.history.BillDetailViewModel
import com.example.billtracker.ui.meters.AddEditMeterScreen
import com.example.billtracker.ui.meters.AddEditMeterViewModel
import com.example.billtracker.ui.meters.CalculateBillScreen
import com.example.billtracker.ui.meters.CalculateBillViewModel
import com.example.billtracker.ui.meters.DailyReadingsScreen
import com.example.billtracker.ui.meters.DailyReadingsViewModel
import com.example.billtracker.ui.meters.MetersScreen
import com.example.billtracker.ui.meters.MetersViewModel
import com.example.billtracker.ui.settings.SettingsScreen
import com.example.billtracker.ui.settings.SettingsViewModel
import com.example.billtracker.ui.tenants.AddEditTenantScreen
import com.example.billtracker.ui.tenants.AddEditTenantViewModel
import com.example.billtracker.ui.tenants.TenantDetailScreen
import com.example.billtracker.ui.tenants.TenantDetailViewModel
import com.example.billtracker.ui.tenants.TenantsScreen
import com.example.billtracker.ui.tenants.TenantsViewModel

class RepositoryViewModelFactory<T : ViewModel>(
    private val create: () -> T
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return create() as T
    }
}

@Composable
fun BillTrackerNavHost(
    navController: NavHostController,
    repository: BillTrackerRepository,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.METERS,
        modifier = modifier
    ) {
        // Profiles List
        composable(NavRoutes.TENANTS) {
            val viewModel: TenantsViewModel = viewModel(
                factory = RepositoryViewModelFactory { TenantsViewModel(repository) }
            )
            val uiState by viewModel.uiState.collectAsState()

            TenantsScreen(
                uiState = uiState,
                onSearchQueryChange = viewModel::onSearchQueryChange,
                onOpenAddProfileSheet = viewModel::onOpenAddProfileSheet,
                onCloseAddProfileSheet = viewModel::onCloseAddProfileSheet,
                onNameChange = viewModel::onNameChange,
                onRoomOrUnitChange = viewModel::onRoomOrUnitChange,
                onNotesChange = viewModel::onNotesChange,
                onSaveProfileFromSheet = viewModel::saveProfileFromSheet,
                onTenantClick = { tenantId ->
                    navController.navigate(NavRoutes.createTenantDetailRoute(tenantId))
                },
                onEditTenantClick = { tenantId ->
                    navController.navigate(NavRoutes.createAddEditTenantRoute(tenantId))
                },
                onDeleteTenantConfirm = viewModel::deleteTenant
            )
        }

        // Meters List
        composable(NavRoutes.METERS) {
            val viewModel: MetersViewModel = viewModel(
                factory = RepositoryViewModelFactory { MetersViewModel(repository) }
            )
            val uiState by viewModel.uiState.collectAsState()

            MetersScreen(
                uiState = uiState,
                onFilterSelect = viewModel::setFilter,
                onOpenAddMeterSheet = viewModel::onOpenAddMeterSheet,
                onCloseAddMeterSheet = viewModel::onCloseAddMeterSheet,
                onNewMeterNameChange = viewModel::onNewMeterNameChange,
                onUtilityTypeChange = viewModel::onUtilityTypeChange,
                onStartingReadingChange = viewModel::onStartingReadingChange,
                onTenantSelect = viewModel::onTenantSelect,
                onRateTypeChange = viewModel::onRateTypeChange,
                onFixedRateChange = viewModel::onFixedRateChange,
                onSaveMeterFromSheet = viewModel::saveMeterFromSheet,
                onCalculateClick = { meterId ->
                    navController.navigate(NavRoutes.createCalculateBillRoute(meterId))
                },
                onDailyLogClick = { meterId ->
                    navController.navigate(NavRoutes.createDailyReadingsRoute(meterId))
                },
                onEditClick = { meterId ->
                    navController.navigate(NavRoutes.createAddEditMeterRoute(meterId = meterId))
                },
                onDeleteConfirm = viewModel::deleteMeter
            )
        }

        // Settings
        composable(NavRoutes.SETTINGS) {
            val viewModel: SettingsViewModel = viewModel(
                factory = RepositoryViewModelFactory { SettingsViewModel(repository) }
            )
            val uiState by viewModel.uiState.collectAsState()

            SettingsScreen(
                uiState = uiState,
                onThemeSelect = viewModel::setThemeMode,
                onCurrencySelect = viewModel::setCurrencySymbol,
                onRecordReadingTimeToggle = viewModel::setRecordReadingTime
            )
        }

        // Add / Edit Meter
        composable(
            route = NavRoutes.ADD_EDIT_METER,
            arguments = listOf(
                navArgument("meterId") { type = NavType.LongType; defaultValue = 0L },
                navArgument("utilityType") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val meterId = backStackEntry.arguments?.getLong("meterId") ?: 0L
            val utilityTypeStr = backStackEntry.arguments?.getString("utilityType") ?: ""
            val initialUtilityType = try {
                if (utilityTypeStr.isNotBlank()) UtilityType.valueOf(utilityTypeStr) else null
            } catch (_: Exception) { null }

            val viewModel: AddEditMeterViewModel = viewModel(
                factory = RepositoryViewModelFactory {
                    AddEditMeterViewModel(repository, meterId.takeIf { it > 0 }, initialUtilityType)
                }
            )
            val uiState by viewModel.uiState.collectAsState()

            AddEditMeterScreen(
                uiState = uiState,
                onNameChange = viewModel::onNameChange,
                onUtilityTypeChange = viewModel::onUtilityTypeChange,
                onStartingReadingChange = viewModel::onStartingReadingChange,
                onTenantSelect = viewModel::onTenantSelect,
                onRateTypeChange = viewModel::onRateTypeChange,
                onFixedRateChange = viewModel::onFixedRateChange,
                onSaveClick = viewModel::saveMeter,
                onBackClick = { navController.popBackStack() }
            )
        }

        // Add / Edit Tenant / Profile
        composable(
            route = NavRoutes.ADD_EDIT_TENANT,
            arguments = listOf(
                navArgument("tenantId") { type = NavType.LongType; defaultValue = 0L }
            )
        ) { backStackEntry ->
            val tenantId = backStackEntry.arguments?.getLong("tenantId") ?: 0L

            val viewModel: AddEditTenantViewModel = viewModel(
                factory = RepositoryViewModelFactory {
                    AddEditTenantViewModel(repository, tenantId.takeIf { it > 0 })
                }
            )
            val uiState by viewModel.uiState.collectAsState()

            AddEditTenantScreen(
                uiState = uiState,
                onNameChange = viewModel::onNameChange,
                onRoomOrUnitChange = viewModel::onRoomOrUnitChange,
                onNotesChange = viewModel::onNotesChange,
                onSaveClick = viewModel::saveTenant,
                onBackClick = { navController.popBackStack() }
            )
        }

        // Calculate Bill
        composable(
            route = NavRoutes.CALCULATE_BILL,
            arguments = listOf(
                navArgument("meterId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val meterId = backStackEntry.arguments?.getLong("meterId") ?: 0L

            val viewModel: CalculateBillViewModel = viewModel(
                factory = RepositoryViewModelFactory { CalculateBillViewModel(repository, meterId) }
            )
            val uiState by viewModel.uiState.collectAsState()

            CalculateBillScreen(
                uiState = uiState,
                onTenantSelect = viewModel::onTenantSelect,
                onOpenAddProfileSheet = viewModel::onOpenAddProfileSheet,
                onCloseAddProfileSheet = viewModel::onCloseAddProfileSheet,
                onNewProfileNameChange = viewModel::onNewProfileNameChange,
                onNewProfileRoomChange = viewModel::onNewProfileRoomChange,
                onNewProfileNotesChange = viewModel::onNewProfileNotesChange,
                onSaveProfileAndResumeBillSave = viewModel::saveProfileAndResumeBillSave,
                onCurrentReadingChange = viewModel::onCurrentReadingChange,
                onBillingDateChange = viewModel::onBillingDateChange,
                onHasTimeToggle = viewModel::onHasTimeToggle,
                onMeterResetToggle = viewModel::onMeterResetToggle,
                onRateTypeChange = viewModel::onRateTypeChange,
                onFixedRateChange = viewModel::onFixedRateChange,
                onProviderTotalBillChange = viewModel::onProviderTotalBillChange,
                onProviderTotalUsageChange = viewModel::onProviderTotalUsageChange,
                onNotesChange = viewModel::onNotesChange,
                onPhotoPathChange = viewModel::onPhotoPathChange,
                onSaveBillClick = viewModel::saveBill,
                onBillSavedSuccess = { tenantId ->
                    val targetRoute = if (tenantId > 0) NavRoutes.createTenantDetailRoute(tenantId) else NavRoutes.TENANTS
                    navController.navigate(targetRoute) {
                        popUpTo(NavRoutes.METERS) { inclusive = false }
                    }
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        // Daily Readings Log
        composable(
            route = NavRoutes.DAILY_READINGS,
            arguments = listOf(
                navArgument("meterId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val meterId = backStackEntry.arguments?.getLong("meterId") ?: 0L

            val viewModel: DailyReadingsViewModel = viewModel(
                factory = RepositoryViewModelFactory { DailyReadingsViewModel(repository, meterId) }
            )
            val uiState by viewModel.uiState.collectAsState()

            DailyReadingsScreen(
                uiState = uiState,
                onOpenAddReadingForm = viewModel::onOpenAddReadingForm,
                onOpenEditReadingForm = viewModel::onOpenEditReadingForm,
                onCloseAddReadingForm = viewModel::onCloseAddReadingForm,
                onToggleForecastExpanded = viewModel::toggleForecastExpanded,
                onValueChange = viewModel::onNewReadingValueChange,
                onDateSelected = viewModel::onDateSelected,
                onTimeSelected = viewModel::onTimeSelected,
                onHasTimeToggle = viewModel::onHasTimeToggle,
                onMeterResetToggle = viewModel::onMeterResetToggle,
                onIsLastReadingToggle = viewModel::onIsLastReadingToggle,
                onDestinationTenantSelect = viewModel::onDestinationTenantSelect,
                onConfirmClosePeriod = viewModel::confirmCloseTrackingPeriodAndTransfer,
                onDismissClosePeriodDialog = viewModel::onDismissClosePeriodDialog,
                onPhotoPathChange = viewModel::onPhotoPathChange,
                onNotesChange = viewModel::onNotesChange,
                onLogClick = viewModel::logDailyReading,
                onToggleForecast = viewModel::toggleForecastEnabled,
                onDeleteReadingClick = viewModel::deleteDailyReading,
                onGenerateBillClick = {
                    navController.navigate(NavRoutes.createCalculateBillRoute(meterId))
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        // Profile Detail
        composable(
            route = NavRoutes.TENANT_DETAIL,
            arguments = listOf(
                navArgument("tenantId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val tenantId = backStackEntry.arguments?.getLong("tenantId") ?: 0L

            val viewModel: TenantDetailViewModel = viewModel(
                factory = RepositoryViewModelFactory { TenantDetailViewModel(repository, tenantId) }
            )
            val uiState by viewModel.uiState.collectAsState()

            TenantDetailScreen(
                uiState = uiState,
                onEditTenantClick = { tid ->
                    navController.navigate(NavRoutes.createAddEditTenantRoute(tid))
                },
                onCalculateMeterBillClick = { meterId ->
                    navController.navigate(NavRoutes.createCalculateBillRoute(meterId))
                },
                onDailyLogClick = { meterId ->
                    navController.navigate(NavRoutes.createDailyReadingsRoute(meterId))
                },
                onBillClick = { readingId ->
                    navController.navigate(NavRoutes.createBillDetailRoute(readingId))
                },
                onTogglePaymentStatus = viewModel::togglePaymentStatus,
                onToggleCycleEstimation = viewModel::toggleCycleEstimation,
                onOpenEditReading = viewModel::onOpenEditReading,
                onCloseEditReading = viewModel::onCloseEditReading,
                onEditValueChange = viewModel::onEditValueChange,
                onEditDateChange = viewModel::onEditDateChange,
                onEditNotesChange = viewModel::onEditNotesChange,
                onEditPhotoPathChange = viewModel::onEditPhotoPathChange,
                onSaveEditedReading = viewModel::saveEditedReading,
                onRequestDeleteReading = viewModel::requestDeleteReading,
                onDismissDeleteReading = viewModel::dismissDeleteReading,
                onConfirmDeleteReading = viewModel::confirmDeleteReading,
                onOpenAddMissingReading = viewModel::onOpenAddMissingReading,
                onCloseAddMissingReading = viewModel::onCloseAddMissingReading,
                onMissingReadingValueChange = viewModel::onMissingReadingValueChange,
                onMissingReadingDateChange = viewModel::onMissingReadingDateChange,
                onMissingReadingNotesChange = viewModel::onMissingReadingNotesChange,
                onMissingReadingPhotoPathChange = viewModel::onMissingReadingPhotoPathChange,
                onSaveMissingReading = viewModel::saveMissingReading,
                onBackClick = { navController.popBackStack() }
            )
        }

        // Bill Detail Summary
        composable(
            route = NavRoutes.BILL_DETAIL,
            arguments = listOf(
                navArgument("readingId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val readingId = backStackEntry.arguments?.getLong("readingId") ?: 0L

            val viewModel: BillDetailViewModel = viewModel(
                factory = RepositoryViewModelFactory { BillDetailViewModel(repository, readingId) }
            )
            val uiState by viewModel.uiState.collectAsState()

            BillDetailScreen(
                uiState = uiState,
                onRecordPayment = viewModel::recordPayment,
                onDeletePayment = viewModel::deletePayment,
                onDeleteConfirm = viewModel::deleteReading,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
