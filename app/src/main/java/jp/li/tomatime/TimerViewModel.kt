package jp.li.tomatime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

    private var timerJob: kotlinx.coroutines.Job? = null

    fun setPomodoroTime(timeInMillis: Long) {
        _pomodoroTime.value = timeInMillis
        if (_timerState.value == TimerState.POMODORO) {
            _timeLeft.value = timeInMillis
        }
    }

    fun startTimer() {
        if (!_isRunning.value) {
            _isRunning.value = true
            timerJob = viewModelScope.launch {
                while (_timeLeft.value > 0 && _isRunning.value) {
                    delay(1000)
                    _timeLeft.value -= 1000
                }
                if (_timeLeft.value <= 0) {
                    _isRunning.value = false
                    // 可以在这里添加完成提示音或通知
                }
            }
        }
    }

    fun pauseTimer() {
        _isRunning.value = false
        timerJob?.cancel()
    }

    fun resetTimer() {
        pauseTimer()
        _timeLeft.value = getCurrentTimerDuration()
    }

    fun switchToPomodoro() {
        pauseTimer()
        _timerState.value = TimerState.POMODORO
        _timeLeft.value = _pomodoroTime.value
    }

    fun switchToShortBreak() {
        pauseTimer()
        _timerState.value = TimerState.SHORT_BREAK
        _timeLeft.value = SHORT_BREAK_TIME
    }

    fun switchToLongBreak() {
        pauseTimer()
        _timerState.value = TimerState.LONG_BREAK
        _timeLeft.value = LONG_BREAK_TIME
    }

    private fun getCurrentTimerDuration(): Long {
        return when (_timerState.value) {
            TimerState.POMODORO -> _pomodoroTime.value
            TimerState.SHORT_BREAK -> SHORT_BREAK_TIME
            TimerState.LONG_BREAK -> LONG_BREAK_TIME
        }
    }
}

enum class TimerState {
    POMODORO,
    SHORT_BREAK,
    LONG_BREAK
}