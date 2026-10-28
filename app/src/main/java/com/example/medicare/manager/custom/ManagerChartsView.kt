package com.example.medicare.manager.custom

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import com.example.medicare.R
import kotlin.math.min

/** Compact canvas charts keep the dashboard independent of an external charting library. */
class ManagerMonthlyChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val values = floatArrayOf(.36f, .51f, .70f, 1f, .82f, 1f)
    private val labels = arrayOf("T1", "T2", "T3", "T4", "T5", "T6")

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val density = resources.displayMetrics.density
        val left = 30f * density
        val top = 8f * density
        val bottom = height - 24f * density
        val chartHeight = bottom - top
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f * density
        paint.color = Color.rgb(231, 235, 244)
        repeat(4) { index ->
            val y = top + chartHeight * index / 3f
            canvas.drawLine(left, y, width.toFloat() - 8f * density, y, paint)
        }
        paint.style = Paint.Style.FILL
        paint.textSize = 10f * density
        paint.color = Color.rgb(114, 128, 155)
        arrayOf("1500", "1000", "500", "0").forEachIndexed { index, label ->
            val y = top + chartHeight * index / 3f + 4f * density
            canvas.drawText(label, 0f, y, paint)
        }
        val usableWidth = width - left - 10f * density
        val step = usableWidth / values.size
        val barWidth = min(31f * density, step * .52f)
        values.forEachIndexed { index, value ->
            val x = left + (step * index) + (step - barWidth) / 2f
            val barTop = bottom - chartHeight * value
            paint.color = Color.rgb(94, 104, 239)
            canvas.drawRoundRect(RectF(x, barTop, x + barWidth, bottom), 2f * density, 2f * density, paint)
            paint.color = Color.rgb(114, 128, 155)
            canvas.drawText(labels[index], x + (barWidth - paint.measureText(labels[index])) / 2f, height - 5f * density, paint)
        }
    }
}

class ManagerStatusDonutView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val colors = intArrayOf(
        Color.rgb(32, 182, 213), Color.rgb(70, 108, 239),
        Color.rgb(255, 174, 45), Color.rgb(244, 92, 105),
    )
    private val values = floatArrayOf(14f, 32f, 54f, 15f)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val density = resources.displayMetrics.density
        val size = min(width, height).toFloat() - 10f * density
        val left = (width - size) / 2f
        val top = (height - size) / 2f
        val stroke = size * .24f
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = stroke
        paint.strokeCap = Paint.Cap.BUTT
        var start = -90f
        val total = values.sum()
        values.forEachIndexed { index, value ->
            paint.color = colors[index]
            val sweep = value / total * 360f
            canvas.drawArc(RectF(left + stroke / 2, top + stroke / 2, left + size - stroke / 2, top + size - stroke / 2), start, sweep, false, paint)
            start += sweep
        }
        paint.style = Paint.Style.FILL
        paint.color = ContextCompatColor.get(context, R.color.manager_ink)
        paint.textSize = 22f * density
        paint.typeface = Typeface.DEFAULT_BOLD
        val label = "115"
        canvas.drawText(label, (width - paint.measureText(label)) / 2f, height / 2f + 8f * density, paint)
    }
}

private object ContextCompatColor {
    fun get(context: Context, colorRes: Int) = context.getColor(colorRes)
}
