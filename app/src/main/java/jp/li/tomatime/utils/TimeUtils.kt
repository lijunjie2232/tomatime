package jp.li.tomatime.utils

import kotlin.math.floor

/**
 * 时间工具类，提供时间格式化等功能
 */
object TimeUtils {
    
    /**
     * 将毫秒数格式化为 MM:SS 格式的字符串
     *
     * @param timeInMillis 毫秒数
     * @return 格式化后的时间字符串，格式为 MM:SS
     */
    fun formatTime(timeInMillis: Long): String {
        val totalSeconds = timeInMillis / 1000
        val minutes = floor(totalSeconds / 60f).toInt()
        val seconds = (totalSeconds % 60).toInt()
        return String.format("%02d:%02d", minutes, seconds)
    }
}