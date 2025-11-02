package jp.li.tomatime

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

class FloatingBallView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var ballPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var gradientPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    
    private var radius = 0f
    private var centerX = 0f
    private var centerY = 0f
    private var timeText = "25:00"
    private var isRunning = false
    
    init {
        textPaint.textSize = 40f
        textPaint.color = Color.WHITE
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.isFakeBoldText = true
    }
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        radius = min(width, height) / 2f - 10f
        centerX = width / 2f
        centerY = height / 2f
        
        updateGradient()
    }
    
    private fun updateGradient() {
        if (radius <= 0) return
        
        // 创建从中心到边缘的径向渐变，边缘颜色更浅
        val colors = if (isRunning) {
            // 运行状态：加深的绿色渐变
            intArrayOf(
                Color.parseColor("#2E7D32"), // 中心颜色 - 更深的绿色
                Color.parseColor("#4CAF50"), // 中间颜色 - 标准绿色
                Color.parseColor("#81C784")  // 边缘颜色 - 浅绿色
            )
        } else {
            // 暂停状态：加深的橙色渐变
            intArrayOf(
                Color.parseColor("#EF6C00"), // 中心颜色 - 更深的橙色
                Color.parseColor("#FF9800"), // 中间颜色 - 标准橙色
                Color.parseColor("#FFB74D")  // 边缘颜色 - 浅橙色
            )
        }
        
        // 设置渐变位置
        val positions = floatArrayOf(0f, 0.7f, 1f)
        
        // 创建径向渐变
        val gradient = RadialGradient(
            centerX,
            centerY,
            radius,
            colors,
            positions,
            Shader.TileMode.CLAMP
        )
        
        gradientPaint.shader = gradient
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // 绘制渐变背景
        canvas.drawCircle(centerX, centerY, radius, gradientPaint)
        
        // 绘制边框
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        borderPaint.style = Paint.Style.STROKE
        borderPaint.strokeWidth = 4f
        borderPaint.color = Color.WHITE
        canvas.drawCircle(centerX, centerY, radius, borderPaint)
        
        // 绘制时间文本
        val fontMetrics = textPaint.fontMetrics
        val textHeight = fontMetrics.bottom - fontMetrics.top
        canvas.drawText(timeText, centerX, centerY + textHeight / 2 - fontMetrics.bottom, textPaint)
    }
    
    fun setTime(time: String) {
        timeText = time
        invalidate()
    }
    
    fun setState(running: Boolean) {
        if (isRunning != running) {
            isRunning = running
            updateGradient()
            invalidate()
        }
    }
}