package com.example.billtracker.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomBarDestination(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Tenants : BottomBarDestination("tenants", "Profile", Icons.Default.People)
    object Meters : BottomBarDestination("meters", "Meters", Icons.Default.Speed)
    object Settings : BottomBarDestination("settings", "Settings", Icons.Default.Settings)
}

object NavRoutes {
    const val TENANTS = "tenants"
    const val METERS = "meters"
    const val SETTINGS = "settings"

    const val ADD_EDIT_METER = "add_edit_meter?meterId={meterId}&utilityType={utilityType}"
    fun createAddEditMeterRoute(meterId: Long = 0, utilityTypeStr: String = ""): String =
        "add_edit_meter?meterId=$meterId&utilityType=$utilityTypeStr"

    const val ADD_EDIT_TENANT = "add_edit_tenant?tenantId={tenantId}"
    fun createAddEditTenantRoute(tenantId: Long = 0): String =
        "add_edit_tenant?tenantId=$tenantId"

    const val CALCULATE_BILL = "calculate_bill/{meterId}"
    fun createCalculateBillRoute(meterId: Long): String =
        "calculate_bill/$meterId"

    const val DAILY_READINGS = "daily_readings/{meterId}"
    fun createDailyReadingsRoute(meterId: Long): String =
        "daily_readings/$meterId"

    const val TENANT_DETAIL = "tenant_detail/{tenantId}"
    fun createTenantDetailRoute(tenantId: Long): String =
        "tenant_detail/$tenantId"

    const val BILL_DETAIL = "bill_detail/{readingId}"
    fun createBillDetailRoute(readingId: Long): String =
        "bill_detail/$readingId"
}
