package com.example.billtracker.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.billtracker.domain.formatter.CurrencyFormatter
import com.example.billtracker.domain.formatter.ReadingFormatter
import com.example.billtracker.domain.model.MeterReading
import com.example.billtracker.domain.model.UtilityType
import com.example.billtracker.ui.theme.ElectricityYellow
import com.example.billtracker.ui.theme.LocalThemeStyleConfig
import com.example.billtracker.ui.theme.WaterBlue
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BillSummaryCard(
    reading: MeterReading,
    utilityType: UtilityType,
    meterName: String,
    tenantName: String? = null,
    currencySymbol: String = "₱",
    modifier: Modifier = Modifier
) {
    val isElectricity = utilityType == UtilityType.ELECTRICITY
    val icon = if (isElectricity) Icons.Default.ElectricBolt else Icons.Default.WaterDrop
    val badgeColor = if (isElectricity) ElectricityYellow else WaterBlue
    val dateFormatter = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())

    val styleConfig = LocalThemeStyleConfig.current

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = styleConfig.cardShape,
        border = styleConfig.cardBorder,
        elevation = CardDefaults.cardElevation(defaultElevation = styleConfig.elevationDp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = badgeColor
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = meterName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (!tenantName.isNullOrBlank()) {
                        Text(
                            text = "Tenant: $tenantName",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Billed on: ${dateFormatter.format(Date(reading.billingDate))}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // Photo Evidence (if attached)
            if (!reading.photoPath.isNullOrBlank() && File(reading.photoPath).exists()) {
                val bitmap = BitmapFactory.decodeFile(reading.photoPath)
                if (bitmap != null) {
                    Text(
                        text = "Meter Photo Evidence",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Meter Reading Photo",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Readings
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Previous Reading", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        ReadingFormatter.formatReading(reading.previousReading, utilityType),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Current Reading", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        ReadingFormatter.formatReading(reading.currentReading, utilityType),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Usage", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        ReadingFormatter.formatReading(reading.usage, utilityType),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Rate Applied (${reading.rateType.displayName})", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        ReadingFormatter.formatRate(reading.rate, utilityType, currencySymbol),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (!reading.providerTotalBill.isNullOrBlank() && !reading.providerTotalUsage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Provider inputs: ${CurrencyFormatter.format(reading.providerTotalBill, currencySymbol)} for ${ReadingFormatter.formatReading(reading.providerTotalUsage, utilityType)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // Total Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${utilityType.displayName} Bill",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = CurrencyFormatter.format(reading.totalAmount, currencySymbol),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
