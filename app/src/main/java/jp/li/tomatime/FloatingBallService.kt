package jp.li.tomatime

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.math.floor

class FloatingBallService : Service() {
    private var windowManager: WindowManager? = null
    private var floatingBallView: View? = null
    private var textView: TextView? = null
    private var params: WindowManager.LayoutParams? = null

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        createFloatingBall()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val timeLeft = intent?.getLongExtra("timeLeft", 0L) ?: 0L
        val isRunning = intent?.getBooleanExtra("isRunning", false) ?: false
        updateFloatingBall(timeLeft, isRunning)
        return START_NOT_STICKY
    }

    private fun createFloatingBall() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // 创建悬浮球视图
        floatingBallView = LayoutInflater.from(this).inflate(R.layout.floating_ball, null)
        textView = floatingBallView?.findViewById(R.id.floating_ball_text)

        // 设置悬浮球参数
        params = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams(
                150,
                150,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )
        } else {
            WindowManager.LayoutParams(
                150,
                150,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )
        }

        params?.gravity = Gravity.TOP or Gravity.START
        params?.x = 0
        params?.y = 100

        // 添加悬浮球到窗口
        try {
            windowManager?.addView(floatingBallView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 设置触摸监听器以支持拖动和点击
        var isMoving = false
        var lastX = 0
        var lastY = 0
        var firstX = 0
        var firstY = 0

        floatingBallView?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isMoving = false
                    lastX = event.rawX.toInt()
                    lastY = event.rawY.toInt()
                    firstX = lastX
                    firstY = lastY
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX.toInt() - lastX
                    val dy = event.rawY.toInt() - lastY
                    params?.x = params?.x?.plus(dx) ?: 0
                    params?.y = params?.y?.plus(dy) ?: 0
                    windowManager?.updateViewLayout(floatingBallView, params)

                    lastX = event.rawX.toInt()
                    lastY = event.rawY.toInt()

                    // 判断是否在移动
                    if (kotlin.math.abs(lastX - firstX) > 5 || kotlin.math.abs(lastY - firstY) > 5) {
                        isMoving = true
                    }
                }
                MotionEvent.ACTION_UP -> {
                    // 如果没有移动，则认为是点击事件
                    if (!isMoving) {
                        // 发送广播通知主Activity处理点击事件
                        val clickIntent = Intent("jp.li.tomatime.FLOATING_BALL_CLICKED")
                        sendBroadcast(clickIntent)
                    }
                }
            }
            true
        }
    }

    private fun updateFloatingBall(timeLeft: Long, isRunning: Boolean) {
        // 更新时间显示
        textView?.text = formatTime(timeLeft)
        
        // 根据运行状态改变颜色
        val backgroundColor = if (isRunning) {
            Color(0xFF4CAF50) // 绿色表示运行中
        } else {
            Color(0xFFFF9800) // 橙色表示暂停
        }
        
        floatingBallView?.setBackgroundColor(backgroundColor.toArgb())
    }

    private fun formatTime(timeInMillis: Long): String {
        val totalSeconds = timeInMillis / 1000
        val minutes = floor(totalSeconds / 60f).toInt()
        val seconds = (totalSeconds % 60).toInt()
        return String.format("%02d:%02d", minutes, seconds)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (floatingBallView != null) {
            windowManager?.removeView(floatingBallView)
        }
    }
}