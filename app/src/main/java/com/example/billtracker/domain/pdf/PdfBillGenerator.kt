package com.example.billtracker.domain.pdf

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.billtracker.domain.calculator.PaymentSummary
import com.example.billtracker.domain.formatter.CurrencyFormatter
import com.example.billtracker.domain.formatter.ReadingFormatter
import com.example.billtracker.domain.model.MeterReading
import com.example.billtracker.domain.model.Tenant
import com.example.billtracker.domain.model.UtilityType
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfBillGenerator {

    private const val PAGE_WIDTH = 595 // A4 width at 72 dpi
    private const val PAGE_HEIGHT = 842 // A4 height at 72 dpi

    fun generateAndGetShareUri(
        context: Context,
        reading: MeterReading,
        utilityType: UtilityType,
        meterName: String,
        tenant: Tenant?,
        paymentSummary: PaymentSummary,
        currencySymbol: String = "₱"
    ): Uri? {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
        }

        var y = 50f
        val startX = 40f
        val endX = PAGE_WIDTH - 40f
        val dateFormatter = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())

        // Header
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 22f
        paint.color = Color.parseColor("#006874") // Primary brand color
        canvas.drawText("UTILITY BILL STATEMENT", startX, y, paint)

        y += 28f
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.DKGRAY
        canvas.drawText("Bill Date: ${dateFormatter.format(Date(reading.billingDate))}", startX, y, paint)

        y += 18f
        canvas.drawLine(startX, y, endX, y, paint)

        // Tenant & Unit Info
        y += 30f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 14f
        paint.color = Color.BLACK
        canvas.drawText("RECIPIENT DETAILS", startX, y, paint)

        y += 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 12f
        val nameStr = tenant?.name ?: "Personal Meter"
        val roomStr = tenant?.roomOrUnit ?: "N/A"
        canvas.drawText("Name: $nameStr", startX, y, paint)
        y += 16f
        canvas.drawText("Room / Unit: $roomStr", startX, y, paint)
        y += 16f
        canvas.drawText("Meter Name: $meterName", startX, y, paint)

        y += 24f
        canvas.drawLine(startX, y, endX, y, paint)

        // Consumption Breakdown
        y += 30f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 14f
        canvas.drawText("${utilityType.displayName.uppercase()} CONSUMPTION DETAILS", startX, y, paint)

        y += 22f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 12f

        canvas.drawText("Previous Reading:", startX, y, paint)
        canvas.drawText(ReadingFormatter.formatReading(reading.previousReading, utilityType), endX - 120f, y, paint)

        y += 18f
        canvas.drawText("Current Reading:", startX, y, paint)
        canvas.drawText(ReadingFormatter.formatReading(reading.currentReading, utilityType), endX - 120f, y, paint)

        y += 18f
        canvas.drawText("Total Consumption:", startX, y, paint)
        canvas.drawText(ReadingFormatter.formatReading(reading.usage, utilityType), endX - 120f, y, paint)

        y += 18f
        canvas.drawText("Applied Rate (${reading.rateType.displayName}):", startX, y, paint)
        canvas.drawText(ReadingFormatter.formatRate(reading.rate, utilityType, currencySymbol), endX - 120f, y, paint)

        y += 24f
        canvas.drawLine(startX, y, endX, y, paint)

        // Financial Summary
        y += 30f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 14f
        canvas.drawText("STATEMENT SUMMARY", startX, y, paint)

        y += 22f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 12f
        canvas.drawText("Total Utility Charge:", startX, y, paint)
        canvas.drawText(CurrencyFormatter.format(reading.totalAmount, currencySymbol), endX - 120f, y, paint)

        y += 18f
        canvas.drawText("Payments Received:", startX, y, paint)
        canvas.drawText(CurrencyFormatter.format(paymentSummary.totalPaidAmount, currencySymbol), endX - 120f, y, paint)

        y += 22f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 14f
        paint.color = Color.parseColor("#006874")
        canvas.drawText("REMAINING BALANCE DUE:", startX, y, paint)
        canvas.drawText(CurrencyFormatter.format(paymentSummary.remainingBalance, currencySymbol), endX - 120f, y, paint)

        y += 18f
        paint.textSize = 12f
        canvas.drawText("Status: ${paymentSummary.status.displayName.uppercase()}", startX, y, paint)

        if (!reading.notes.isNullOrBlank()) {
            y += 30f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.color = Color.DKGRAY
            canvas.drawText("Notes: ${reading.notes}", startX, y, paint)
        }

        // Footer
        paint.color = Color.GRAY
        paint.textSize = 10f
        canvas.drawText("Generated by Bill Tracker App • Offline & Secure", startX, PAGE_HEIGHT - 40f, paint)

        document.finishPage(page)

        // Save PDF file in cache
        val pdfDir = File(context.cacheDir, "pdf_bills")
        if (!pdfDir.exists()) pdfDir.mkdirs()

        val pdfFile = File(pdfDir, "UtilityBill_${reading.id}.pdf")
        try {
            val outputStream = FileOutputStream(pdfFile)
            document.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            document.close()

            return FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )
        } catch (_: Exception) {
            document.close()
            return null
        }
    }
}
