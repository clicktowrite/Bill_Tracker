package com.example.billtracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.billtracker.domain.model.PaymentStatus

@Composable
fun PaymentStatusBadge(
    status: PaymentStatus,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor) = when (status) {
        PaymentStatus.PAID -> Pair(Color(0xFFE8F5E9), Color(0xFF2E7D32))
        PaymentStatus.PARTIALLY_PAID -> Pair(Color(0xFFFFF3E0), Color(0xFFE65100))
        PaymentStatus.UNPAID -> Pair(Color(0xFFECEFF1), Color(0xFF455A64))
        PaymentStatus.OVERDUE -> Pair(Color(0xFFFFEBEE), Color(0xFFC62828))
    }

    Text(
        text = status.displayName,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
        fontWeight = FontWeight.Bold,
        color = textColor,
        modifier = modifier
            .background(color = backgroundColor, shape = RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}
