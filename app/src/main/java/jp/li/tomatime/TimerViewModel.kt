package jp.li.tomatime

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive

class TimerViewModel : ViewModel() {
    companion object {
        const val DEFAULT_POMODORO_TIME = 25 * 60 * 1000L // 25分钟
        const val SHORT_BREAK_TIME = 5 * 60 * 1000L // 5分钟
        const val LONG_BREAK_TIME = 15 * 60 * 1000L // 15分钟
    }

    private val _pomodoroTime = MutableStateFlow(DEFAULT_POMODORO_TIME)
    private val _timeLeft = MutableStateFlow(DEFAULT_POMODORO_TIME)
    val timeLeft: StateFlow<Long> = _timeLeft.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _timerState = MutableStateFlow(TimerState.POMODORO)
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private var timerJob: Job? = null
    var notificationService: NotificationService? = null
    private var lastNotificationUpdate: Long = 0
    private val MIN_NOTIFICATION_UPDATE_INTERVAL = 1000L // 最小通知更新间隔（毫秒）

    fun setPomodoroTime(timeInMillis: Long) {
        _pomodoroTime.value = timeInMillis
        if (_timerState.value == TimerState.POMODORO) {
            _timeLeft.value = timeInMillis
        }
        // 更新通知
        updateNotificationIfNeeded()
    }

    fun startTimer() {
        if (!_isRunning.value) {
            _isRunning.value = true
            timerJob = viewModelScope.launch {
                while (_timeLeft.value > 0 && _isRunning.value) {
                    delay(1000)
                    _timeLeft.value -= 1000
                    // 更新通知
                    updateNotificationIfNeeded()
                }
                if (_timeLeft.value <= 0) {
                    _isRunning.value = false
                    // 显示完成通知
                    notificationService?.showCompletedNotification()
                }
            }
        }
    }

    fun pauseTimer() {
        _isRunning.value = false
        timerJob?.cancel()
        // 更新通知
        updateNotificationIfNeeded()
    }

    fun resetTimer() {
        pauseTimer()
        _timeLeft.value = getCurrentTimerDuration()
        // 更新通知为准备就绪状态
        notificationService?.showReadyNotification()
    }

    fun switchToPomodoro() {
        pauseTimer()
        _timerState.value = TimerState.POMODORO
        _timeLeft.value = _pomodoroTime.value
        // 更新通知
        updateNotificationIfNeeded()
    }

    fun switchToShortBreak() {
        pauseTimer()
        _timerState.value = TimerState.SHORT_BREAK
        _timeLeft.value = SHORT_BREAK_TIME
        // 更新通知
        updateNotificationIfNeeded()
    }

    fun switchToLongBreak() {
        pauseTimer()
        _timerState.value = TimerState.LONG_BREAK
        _timeLeft.value = LONG_BREAK_TIME
        // 更新通知
        updateNotificationIfNeeded()
    }

    fun getCurrentPomodoroTime(): Long {
        return _pomodoroTime.value
    }

    private fun getCurrentTimerDuration(): Long {
        return when (_timerState.value) {
            TimerState.POMODORO -> _pomodoroTime.value
            TimerState.SHORT_BREAK -> SHORT_BREAK_TIME
            TimerState.LONG_BREAK -> LONG_BREAK_TIME
        }
    }
    
    /**
     * 更新通知，但限制更新频率以提高性能
     */
    private fun updateNotificationIfNeeded() {
        val now = System.currentTimeMillis()
        // 限制通知更新频率，避免过于频繁的更新
        if (now - lastNotificationUpdate >= MIN_NOTIFICATION_UPDATE_INTERVAL || 
            _timeLeft.value <= 0) {
            lastNotificationUpdate = now
            notificationService?.showTimerNotification(_timeLeft.value, _isRunning.value)
        }
    }
}

enum class TimerState {
    POMODORO,
    SHORT_BREAK,
    LONG_BREAK
}