package com.example.medicare.manager.work_schedule

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import kotlin.math.max

/** Canvas board for the weekly doctor schedule: dates, doctors, time grid, and shift blocks. */
class WeeklyScheduleBoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bodyColor = Color.rgb(21, 33, 61)
    private val mutedColor = Color.rgb(114, 128, 155)
    private val lineColor = Color.rgb(228, 234, 244)
    private val blue = Color.rgb(35, 100, 232)
    private val dayFill = Color.rgb(243, 247, 253)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val width = width.toFloat()
        val padding = dp(10f)
        val dateY = dp(26f)
        val chipTop = dp(48f)
        val chipHeight = dp(48f)
        val boardTop = dp(116f)
        val timeColumn = dp(54f)
        val right = width - padding
        val gridLeft = padding + timeColumn
        val columnWidth = (right - gridLeft) / 4f
        val boardBottom = height - dp(10f)
        val hourHeight = max(dp(46f), (boardBottom - boardTop) / 10f)

        drawText(canvas, "‹", padding, dateY, 34f, blue, true)
        drawCenteredText(canvas, "26/05/2025  -  01/06/2025", width / 2f, dateY, 15f, bodyColor, true)
        drawText(canvas, "›", width - padding - dp(22f), dateY, 34f, blue, true)

        val days = listOf("T2\n26/05", "T3\n27/05", "T4\n28/05", "T5\n29/05", "T6\n30/05", "T7\n31/05", "CN\n01/06")
        val gap = dp(6f)
        val chipWidth = (right - padding - gap * 6) / 7f
        days.forEachIndexed { index, day ->
            val left = padding + index * (chipWidth + gap)
            roundRect(canvas, left, chipTop, left + chipWidth, chipTop + chipHeight, if (index == 0) blue else dayFill)
            val textColor = if (index == 0) Color.WHITE else mutedColor
            val parts = day.split("\n")
            drawCenteredText(canvas, parts[0], left + chipWidth / 2f, chipTop + dp(20f), 12f, textColor, true)
            drawCenteredText(canvas, parts[1], left + chipWidth / 2f, chipTop + dp(37f), 10f, textColor, false)
        }

        val doctors = listOf("BS. Nguyễn\nVăn A\nTim mạch", "BS. Trần\nThị B\nNội khoa", "BS. Lê\nVăn C\nThần kinh", "BS. Hoàng\nThị D\nDa liễu")
        doctors.forEachIndexed { index, doctor ->
            val center = gridLeft + columnWidth * index + columnWidth / 2f
            val parts = doctor.split("\n")
            drawCenteredText(canvas, parts[0], center, boardTop + dp(17f), 11f, bodyColor, true)
            drawCenteredText(canvas, parts[1], center, boardTop + dp(33f), 11f, bodyColor, true)
            drawCenteredText(canvas, parts[2], center, boardTop + dp(51f), 10f, mutedColor, false)
        }

        val gridY = boardTop + dp(64f)
        for (column in 0..4) {
            val x = gridLeft + column * columnWidth
            line(canvas, x, gridY, x, boardBottom, lineColor)
        }
        (0..10).forEach { hour ->
            val y = gridY + hour * hourHeight
            line(canvas, gridLeft, y, right, y, lineColor)
            if (hour < 10) drawCenteredText(canvas, "%02d:00".format(hour + 7), padding + timeColumn / 2f, y + dp(5f), 11f, mutedColor, false)
        }

        drawShift(canvas, gridLeft + dp(5f), gridY + hourHeight, columnWidth - dp(10f), hourHeight * 3f, Color.rgb(210, 232, 255), "08:00\n12:00\nKhám bệnh\nPhòng 201")
        drawShift(canvas, gridLeft + columnWidth + dp(5f), gridY + hourHeight, columnWidth - dp(10f), hourHeight * 3f, Color.rgb(211, 248, 220), "08:00\n12:00\nKhám bệnh\nPhòng 202")
        drawShift(canvas, gridLeft + columnWidth * 2 + dp(5f), gridY + hourHeight * 6f, columnWidth - dp(10f), hourHeight * 3f, Color.rgb(255, 232, 188), "13:00\n17:00\nKhám bệnh\nPhòng 203")
        drawShift(canvas, gridLeft + columnWidth * 3 + dp(5f), gridY, columnWidth - dp(10f), boardBottom - gridY, Color.rgb(255, 225, 228), "Nghỉ\ncả ngày", Color.rgb(210, 54, 61))
    }

    private fun drawShift(canvas: Canvas, left: Float, top: Float, width: Float, height: Float, color: Int, content: String, textColor: Int = bodyColor) {
        roundRect(canvas, left, top + dp(3f), left + width, top + height - dp(3f), color)
        val lines = content.split("\n")
        val start = top + height / 2f - (lines.size - 1) * dp(10f)
        lines.forEachIndexed { index, line ->
            drawText(canvas, line, left + dp(10f), start + index * dp(18f), 11f, textColor, index < 2)
        }
    }

    private fun roundRect(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float, color: Int) {
        paint.color = color
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(RectF(left, top, right, bottom), dp(8f), dp(8f), paint)
    }

    private fun line(canvas: Canvas, startX: Float, startY: Float, endX: Float, endY: Float, color: Int) {
        paint.color = color
        paint.strokeWidth = dp(1f)
        canvas.drawLine(startX, startY, endX, endY, paint)
    }

    private fun drawText(canvas: Canvas, text: String, x: Float, y: Float, size: Float, color: Int, bold: Boolean) {
        paint.color = color
        paint.textSize = sp(size)
        paint.typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText(text, x, y, paint)
    }

    private fun drawCenteredText(canvas: Canvas, text: String, x: Float, y: Float, size: Float, color: Int, bold: Boolean) {
        paint.color = color
        paint.textSize = sp(size)
        paint.typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(text, x, y, paint)
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density
    private fun sp(value: Float): Float = value * resources.displayMetrics.scaledDensity
}
