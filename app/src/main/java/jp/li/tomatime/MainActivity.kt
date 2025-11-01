package jp.li.tomatime

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import jp.li.tomatime.ui.theme.TomatimeTheme
import kotlin.math.floor
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var notificationManager: NotificationManagerCompat
    private var viewModel: TimerViewModel? = null
    
    companion object {
        private const val CHANNEL_ID = "timer_channel"
        private const val NOTIFICATION_ID = 1
        const val ACTION_PAUSE_CONTINUE = "jp.li.tomatime.ACTION_PAUSE_CONTINUE"
        const val ACTION_STOP = "jp.li.tomatime.ACTION_STOP"
    }
    
    private val timerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_PAUSE_CONTINUE -> {
                    // 处理暂停/继续操作
                    viewModel?.let { vm ->
                        if (vm.isRunning.value) {
                            vm.pauseTimer()
                        } else {
                            vm.startTimer()
                        }
                    }
                }
                ACTION_STOP -> {
                    // 处理停止操作
                    viewModel?.resetTimer()
                }
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 注册广播接收器
        registerReceiver(timerReceiver, IntentFilter().apply {
            addAction(ACTION_PAUSE_CONTINUE)
            addAction(ACTION_STOP)
        })
        
        // 创建通知渠道
        createNotificationChannel()
        notificationManager = NotificationManagerCompat.from(this)
        
        // 请求通知权限（Android 13+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        enableEdgeToEdge()
        setContent {
            TomatimeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val timerViewModel = viewModel<TimerViewModel>()
                    viewModel = timerViewModel
                    
                    PomodoroTimer(
                        viewModel = timerViewModel,
                        modifier = Modifier.padding(innerPadding),
                        onTimerStateChanged = { isRunning, timeLeft, timerState ->
                            updateNotification(isRunning, timeLeft, timerState)
                        }
                    )
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(timerReceiver)
        // 清除通知
        notificationManager.cancel(NOTIFICATION_ID)
    }
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // 权限请求结果处理
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
                .setName("计时器通知")
                .setDescription("显示当前计时状态")
                .build()
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun updateNotification(isRunning: Boolean, timeLeft: Long, timerState: TimerState) {
        val stateText = when (timerState) {
            TimerState.POMODORO -> "专注中"
            TimerState.SHORT_BREAK -> "短休息中"
            TimerState.LONG_BREAK -> "长休息中"
        }
        
        val actionText = if (isRunning) "暂停" else "继续"
        
        // 创建暂停/继续操作的 PendingIntent
        val pauseContinueIntent = Intent(this, timerReceiver.javaClass).apply {
            action = ACTION_PAUSE_CONTINUE
        }
        val pauseContinuePendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            pauseContinueIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // 创建停止操作的 PendingIntent
        val stopIntent = Intent(this, timerReceiver.javaClass).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("番茄时钟")
            .setContentText("$stateText - ${formatTime(timeLeft)}")
            .setSmallIcon(android.R.drawable.ic_media_play) // 使用系统默认图标
            .setOngoing(true)
            .addAction(
                0,
                actionText,
                pauseContinuePendingIntent
            )
            .addAction(
                0,
                "停止",
                stopPendingIntent
            )
            .build()
        
        // 检查权限后再显示通知
        if (checkNotificationPermission()) {
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }
    
    private fun checkNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
    
    private fun formatTime(timeInMillis: Long): String {
        val totalSeconds = timeInMillis / 1000
        val minutes = floor(totalSeconds / 60f).toInt()
        val seconds = (totalSeconds % 60).toInt()
        return String.format("%02d:%02d", minutes, seconds)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroTimer(
    viewModel: TimerViewModel = viewModel(),
    modifier: Modifier = Modifier,
    onTimerStateChanged: (Boolean, Long, TimerState) -> Unit = { _, _, _ -> }
) {
    val timeLeft by viewModel.timeLeft.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val timerState by viewModel.timerState.collectAsState()
    var showSettings by remember { mutableStateOf(false) }
    
    // 当计时器状态改变时通知 MainActivity
    LaunchedEffect(isRunning, timeLeft, timerState) {
        onTimerStateChanged(isRunning, timeLeft, timerState)
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
                TimerState.POMODORO -> "专注时间"
                TimerState.SHORT_BREAK -> "短休息"
                TimerState.LONG_BREAK -> "长休息"
            },
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        // 显示时间（点击可设置专注时间）
        Text(
            text = formatTime(timeLeft),
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier
                .padding(vertical = 32.dp)
                .clickableNoRipple { 
                    if (timerState == TimerState.POMODORO) {
                        showSettings = true
                    }
                }
        )

        // 控制按钮
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(vertical = 16.dp)
        ) {
            Button(
                onClick = { if (isRunning) viewModel.pauseTimer() else viewModel.startTimer() },
                shape = CircleShape,
                modifier = Modifier.size(80.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) Color(0xFFE57373) else MaterialTheme.colorScheme.primary
                )
            ) {
                Text(if (isRunning) "暂停" else "开始")
            }
            
            Button(
                onClick = { viewModel.resetTimer() },
                shape = CircleShape,
                modifier = Modifier.size(80.dp)
            ) {
                Text("重置")
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
                Text("专注")
            }
            
            Button(
                onClick = { viewModel.switchToShortBreak() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (timerState == TimerState.SHORT_BREAK) 
                        MaterialTheme.colorScheme.primary else 
                        MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("短休息")
            }
            
            Button(
                onClick = { viewModel.switchToLongBreak() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (timerState == TimerState.LONG_BREAK) 
                        MaterialTheme.colorScheme.primary else 
                        MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("长休息")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroSettings(
    onDismiss: () -> Unit,
    onSave: (Int, Int) -> Unit,
    currentPomodoroTime: Long,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState()
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        val totalSeconds = (currentPomodoroTime / 1000).toInt()
        val currentMinutes = totalSeconds / 60
        val currentSeconds = totalSeconds % 60
        
        var minutes by remember { mutableStateOf(currentMinutes) }
        var seconds by remember { mutableStateOf(currentSeconds) }
        
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "专注时间设置",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            Text(
                text = formatTime((minutes * 60 + seconds) * 1000L),
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
                    Text("分钟", modifier = Modifier.padding(bottom = 8.dp))
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
                    Text("秒钟", modifier = Modifier.padding(bottom = 8.dp))
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
                    Text("取消")
                }
                
                Button(onClick = { onSave(minutes, seconds) }) {
                    Text("保存")
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

@Composable
fun formatTime(timeInMillis: Long): String {
    val totalSeconds = timeInMillis / 1000
    val minutes = floor(totalSeconds / 60f).toInt()
    val seconds = (totalSeconds % 60).toInt()
    return String.format("%02d:%02d", minutes, seconds)
}

// 添加无波纹效果的可点击修饰符
@Composable
fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier {
    return this.then(
        clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() }
        ) { onClick() }
    )
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