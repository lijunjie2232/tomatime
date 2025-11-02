package jp.li.tomatime

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import jp.li.tomatime.FloatingBallView
import jp.li.tomatime.utils.TimeUtils

class FloatingBallService : Service() {
    private var windowManager: WindowManager? = null
    private var floatingBallView: FloatingBallView? = null
    private var layout: View? = null
    private var params: WindowManager.LayoutParams? = null
    private var lastUpdateTime: Long = 0
    private val MIN_UPDATE_INTERVAL = 1000L // 最小更新间隔（毫秒）
    
    companion object {
        private const val OVERLAY_PERMISSION_CHECK_INTERVAL = 5000L // 悬浮窗权限检查间隔（毫秒）
        private var lastOverlayPermissionCheck: Long = 0
        private var lastOverlayPermissionResult: Boolean = false
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
    
    /**
     * 检查悬浮窗权限
     * 使用缓存机制减少频繁检查
     */
    private fun checkOverlayPermission(): Boolean {
        val now = System.currentTimeMillis()
        // 检查缓存，避免频繁检查权限
        if (now - lastOverlayPermissionCheck < OVERLAY_PERMISSION_CHECK_INTERVAL) {
            return lastOverlayPermissionResult
        }
        
        lastOverlayPermissionCheck = now
        lastOverlayPermissionResult =
            Settings.canDrawOverlays(this)

        return lastOverlayPermissionResult
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
        layout = LayoutInflater.from(this).inflate(R.layout.floating_ball, null)
        floatingBallView = layout?.findViewById(R.id.floating_ball_view)

        // 设置悬浮球参数
        params =
            WindowManager.LayoutParams(
                150,
                150,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )

        params?.gravity = Gravity.TOP or Gravity.START
        params?.x = 0
        params?.y = 100

        // 添加悬浮球到窗口
        try {
            if (checkOverlayPermission()) {
                windowManager?.addView(layout, params)
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 设置触摸监听器以支持拖动和点击
        var isMoving = false
        var lastX = 0
        var lastY = 0
        var firstX = 0
        var firstY = 0

        layout?.setOnTouchListener { view, event ->
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
                    if (checkOverlayPermission()) {
                        params?.x = params?.x?.plus(dx) ?: 0
                        params?.y = params?.y?.plus(dy) ?: 0
                        windowManager?.updateViewLayout(layout, params)
                    }

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
                        // 调用 performClick 以满足 Android 规范
                        view?.performClick()
                        // 发送广播通知主Activity处理点击事件
                        val clickIntent = Intent("jp.li.tomatime.FLOATING_BALL_CLICKED")
                        // 为确保安全，指定包名以避免发送给其他应用
                        clickIntent.setPackage(packageName)
                        sendBroadcast(clickIntent)
                    }
                }
            }
            true
        }
    }

    private fun updateFloatingBall(timeLeft: Long, isRunning: Boolean) {
        val now = System.currentTimeMillis()
        // 限制更新频率，避免过于频繁的更新
        if (now - lastUpdateTime >= MIN_UPDATE_INTERVAL) {
            lastUpdateTime = now
            // 更新时间显示
            floatingBallView?.setTime(TimeUtils.formatTime(timeLeft))
            
            // 更新运行状态
            floatingBallView?.setState(isRunning)
        } else if (!isRunning) {
            // 如果暂停了计时器，立即更新状态
            floatingBallView?.setState(isRunning)
        }
    }

    

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (checkOverlayPermission()) {
                layout?.let { windowManager?.removeView(it) }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}