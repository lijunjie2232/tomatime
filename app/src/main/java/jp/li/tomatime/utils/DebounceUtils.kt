package jp.li.tomatime.utils

/**
 * 防抖动工具类，用于限制操作的执行频率
 */
object DebounceUtils {
    private val lastCallTimeMap = mutableMapOf<String, Long>()
    
    /**
     * 检查是否应该执行操作（防抖动）
     *
     * @param key 操作的唯一标识符
     * @param delayMillis 防抖动延迟时间（毫秒）
     * @return 如果应该执行操作返回true，否则返回false
     */
    fun shouldExecute(key: String, delayMillis: Long): Boolean {
        val now = System.currentTimeMillis()
        val lastCallTime = lastCallTimeMap[key] ?: 0
        if (now - lastCallTime >= delayMillis) {
            lastCallTimeMap[key] = now
            return true
        }
        return false
    }
    
    /**
     * 清除指定key的记录
     *
     * @param key 操作的唯一标识符
     */
    fun clear(key: String) {
        lastCallTimeMap.remove(key)
    }
    
    /**
     * 清除所有记录
     */
    fun clearAll() {
        lastCallTimeMap.clear()
    }
}