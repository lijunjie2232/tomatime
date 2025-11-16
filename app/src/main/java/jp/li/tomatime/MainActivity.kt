package jp.li.tomatime

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import jp.li.tomatime.ui.theme.TomatimeTheme
import jp.li.tomatime.utils.TimeUtils
import jp.li.tomatime.utils.DebounceUtils
import kotlinx.coroutines.launch
import androidx.core.net.toUri

class MainActivity : ComponentActivity() {
    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
        private const val OVERLAY_PERMISSION_REQUEST_CODE = 1002
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            NOTIFICATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Notification permission granted
                    // 通知权限被授予
                }
            }
        }
    }

    private lateinit var viewModel: TimerViewModel
    private var floatingBallReceiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewModel = TimerViewModel()

        // 请求通知权限
        requestNotificationPermission()

        // 请求悬浮球权限
        requestOverlayPermission()

        // 注册悬浮球点击广播接收器
        registerFloatingBallReceiver()

        setContent {
            TomatimeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PomodoroTimer(
                        modifier = Modifier.padding(innerPadding),
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    private fun requestOverlayPermission() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                "package:$packageName".toUri()
            )
            startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE)
        }
//        }
    }

    /**
     * 检查是否已获得悬浮窗权限
     */
    private fun checkOverlayPermission(): Boolean {
//        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            Settings.canDrawOverlays(this)
//        } else {
//            // Android 6.0 以下版本默认有悬浮窗权限
//            true
//        }
        return Settings.canDrawOverlays(this)
    }

    private fun registerFloatingBallReceiver() {
        floatingBallReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    "jp.li.tomatime.FLOATING_BALL_CLICKED" -> {
                        // Handle floating ball click event
                        // 处理悬浮球点击事件
                        handleFloatingBallClick()
                    }
                }
            }
        }

        val filter = IntentFilter("jp.li.tomatime.FLOATING_BALL_CLICKED")
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        registerReceiver(floatingBallReceiver, filter, RECEIVER_NOT_EXPORTED)
//        } else {
//            registerReceiver(floatingBallReceiver, filter)
//        }
    }

    private fun handleFloatingBallClick() {
        // 根据当前状态执行相应操作
        // Perform corresponding action based on current state
        if (viewModel.isRunning.value) {
            // 如果正在运行，则暂停
            // If running, then pause
            viewModel.pauseTimer()
        } else {
            // 如果已暂停，则检查剩余时间
            // If paused, check remaining time
            if (viewModel.timeLeft.value > 0) {
                // 如果还有剩余时间，则开始
                // If there is time left, then start
                viewModel.startTimer()
            } else {
                // 如果时间已到，则重置
                // If time is up, then reset
                viewModel.resetTimer()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        floatingBallReceiver?.let {
            unregisterReceiver(it)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            OVERLAY_PERMISSION_REQUEST_CODE -> {
                // Handle overlay permission result
                // 用户从悬浮窗权限设置返回
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    // Permission granted
                    // 已获得权限
                } else {
                    // Permission not granted
                    // 未获得权限
                    Toast.makeText(this, R.string.overlay_permission_required, Toast.LENGTH_SHORT).show()
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        "package:$packageName".toUri()
                    )
                    startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE)

                }
//                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroTimer(
    modifier: Modifier = Modifier,
    viewModel: TimerViewModel = viewModel(),
) {
    val timeLeft by viewModel.timeLeft.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val timerState by viewModel.timerState.collectAsState()
    var showSettings by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val primaryColor = MaterialTheme.colorScheme.primary

    // 计算总时间（用于进度计算）
    val totalTime = when (timerState) {
        TimerState.POMODORO -> viewModel.getCurrentPomodoroTime()
        TimerState.SHORT_BREAK -> TimerViewModel.SHORT_BREAK_TIME
        TimerState.LONG_BREAK -> TimerViewModel.LONG_BREAK_TIME
    }

    // 计算进度 (0f-1f)
    val progress = 1f - (timeLeft.toFloat() / totalTime.toFloat())

    LaunchedEffect(Unit) {
        // 初始化通知服务
        viewModel.notificationService = NotificationService(context)
        // 显示初始通知
        viewModel.notificationService?.showReadyNotification()

        // 启动悬浮球服务
        val serviceIntent = Intent(context, FloatingBallService::class.java)
        serviceIntent.putExtra("timeLeft", timeLeft)
        serviceIntent.putExtra("isRunning", isRunning)
        ContextCompat.startForegroundService(context, serviceIntent)
    }

    // 当时间或运行状态改变时更新悬浮球
    LaunchedEffect(timeLeft, isRunning) {
        // 使用防抖动机制，避免过于频繁地更新悬浮球
        if (DebounceUtils.shouldExecute("floating_ball_update", 1000)) {
            val serviceIntent = Intent(context, FloatingBallService::class.java)
            serviceIntent.putExtra("timeLeft", timeLeft)
            serviceIntent.putExtra("isRunning", isRunning)
            context.startService(serviceIntent)
        }
    }

    if (showSettings) {
        PomodoroSettings(
            onDismiss = { showSettings = false },
            onSave = { minutes, seconds ->
                val timeInMillis = (minutes * 60 + seconds) * 1000L
                viewModel.setPomodoroTime(timeInMillis)
                showSettings = false
            },
            currentPomodoroTime = timeLeft
        )
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 显示当前状态
        Text(
            text = when (timerState) {
                TimerState.POMODORO -> context.getString(R.string.focus_time)
                TimerState.SHORT_BREAK -> context.getString(R.string.short_break)
                TimerState.LONG_BREAK -> context.getString(R.string.long_break)
            },
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        // 显示带环形进度条的时间
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(vertical = 32.dp)
        ) {
            // 环形进度条
            Canvas(
                modifier = Modifier.size(200.dp)
            ) {
                // 背景圆环
                drawCircle(
                    color = Color.LightGray,
                    style = Stroke(width = 12f),
                    radius = size.width / 2 - 12f
                )
                
                // 进度圆环
                drawArc(
                    color = if (timerState == TimerState.POMODORO) primaryColor 
                           else if (timerState == TimerState.SHORT_BREAK) Color(0xFF4CAF50) 
                           else Color(0xFF2196F3),
                    startAngle = -90f,
                    sweepAngle = 360 * progress,
                    useCenter = false,
                    style = Stroke(width = 12f, cap = StrokeCap.Round),
                    size = Size(size.width - 24f, size.height - 24f),
                    topLeft = androidx.compose.ui.geometry.Offset(12f, 12f)
                )
            }
            
            // 时间文本（点击可设置专注时间）
            Text(
                text = TimeUtils.formatTime(timeLeft),
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier
                    .clickableNoRipple {
                        if (timerState == TimerState.POMODORO) {
                            showSettings = true
                        }
                    }
            )
        }

        // 控制按钮
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(vertical = 16.dp)
        ) {
            Button(
                onClick = { if (isRunning) viewModel.pauseTimer() else viewModel.startTimer() },
                shape = CircleShape,
                modifier = Modifier.size(128.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) Color(0xFFE57373) else MaterialTheme.colorScheme.primary
                )
            ) {
                Text(if (isRunning) context.getString(R.string.pause) else context.getString(R.string.start))
            }

            Button(
                onClick = { viewModel.resetTimer() },
                shape = CircleShape,
                modifier = Modifier.size(128.dp)
            ) {
                Text(context.getString(R.string.reset))
            }
        }

        // 状态切换按钮
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.switchToPomodoro() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (timerState == TimerState.POMODORO)
                        MaterialTheme.colorScheme.primary else
                        MaterialTheme.colorScheme.secondary
                )
            ) {
                Text(context.getString(R.string.focus))
            }

            Button(
                onClick = { viewModel.switchToShortBreak() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (timerState == TimerState.SHORT_BREAK)
                        MaterialTheme.colorScheme.primary else
                        MaterialTheme.colorScheme.secondary
                )
            ) {
                Text(context.getString(R.string.short_break_button))
            }

            Button(
                onClick = { viewModel.switchToLongBreak() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (timerState == TimerState.LONG_BREAK)
                        MaterialTheme.colorScheme.primary else
                        MaterialTheme.colorScheme.secondary
                )
            ) {
                Text(context.getString(R.string.long_break_button))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroSettings(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onSave: (Int, Int) -> Unit,
    currentPomodoroTime: Long
) {
    val sheetState = rememberModalBottomSheetState()
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        val totalSeconds = (currentPomodoroTime / 1000).toInt()
        val currentMinutes = totalSeconds / 60
        val currentSeconds = totalSeconds % 60

        var minutes by remember { androidx.compose.runtime.mutableIntStateOf(currentMinutes) }
        var seconds by remember { androidx.compose.runtime.mutableIntStateOf(currentSeconds) }

        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = context.getString(R.string.focus_time_settings),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Text(
                text = TimeUtils.formatTime((minutes * 60 + seconds) * 1000L),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(context.getString(R.string.minutes), modifier = Modifier.padding(bottom = 8.dp))
                    NumberPicker(
                        value = minutes,
                        onValueChange = { minutes = it },
                        range = 1..60
                    )
                }

                Text(":", style = MaterialTheme.typography.headlineMedium)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(context.getString(R.string.seconds), modifier = Modifier.padding(bottom = 8.dp))
                    NumberPicker(
                        value = seconds,
                        onValueChange = { seconds = it },
                        range = 0..59
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = onDismiss) {
                    Text(context.getString(R.string.cancel))
                }

                Button(onClick = { onSave(minutes, seconds) }) {
                    Text(context.getString(R.string.save))
                }
            }
        }
    }
}

@Composable
fun NumberPicker(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    modifier: Modifier = Modifier
) {
    val itemHeight = 40.dp
    val visibleItems = 3
    val halfVisibleItems = visibleItems / 2

    // 计算初始索引（value - range.first 是相对于 range 起始的偏移量）
    val initialIndex = value - range.first

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialIndex,
        initialFirstVisibleItemScrollOffset = 0
    )

    val coroutineScope = rememberCoroutineScope()
    val itemHeightPx = with(LocalDensity.current) { itemHeight.toPx() }

    // 计算当前选中项的索引
    val selectedIndex by remember {
        derivedStateOf {
            val offset = listState.firstVisibleItemScrollOffset
            val index = listState.firstVisibleItemIndex
            // 根据偏移量判断应该选择哪个项
            if (offset > itemHeightPx / 2) index + 1 else index
        }
    }

    // 当滚动停止时，吸附到最近的项并回调
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            coroutineScope.launch {
                listState.animateScrollToItem(selectedIndex)
                // 将 selectedIndex 转换为实际值
                val actualValue = range.first + selectedIndex
                if (actualValue in range) {
                    onValueChange(actualValue)
                }
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .height(itemHeight * visibleItems)
            .width(80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(vertical = itemHeight * halfVisibleItems),
        flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    ) {
        items(range.count()) { index ->
            val itemValue = range.first + index
            val isSelected = selectedIndex == index

            Text(
                text = itemValue.toString(),
                fontSize = if (isSelected) 24.sp else 16.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                modifier = Modifier
                    .height(itemHeight)
                    .fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}


// 添加无波纹效果的可点击修饰符
@Composable
fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier {
    return this.clickable(
        indication = null,
        interactionSource = remember { MutableInteractionSource() }
    ) { onClick() }
}

@Preview(showBackground = true)
@Composable
fun PomodoroTimerPreview() {
    TomatimeTheme {
        PomodoroTimer(viewModel = remember { TimerViewModel() })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun PomodoroSettingsPreview() {
    TomatimeTheme {
        PomodoroSettings(
            onDismiss = {},
            onSave = { _, _ -> },
            currentPomodoroTime = TimerViewModel.DEFAULT_POMODORO_TIME
        )
    }
}