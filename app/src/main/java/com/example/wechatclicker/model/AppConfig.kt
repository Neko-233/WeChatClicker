package com.example.wechatclicker.model

import android.content.Context
import android.content.SharedPreferences
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * 应用配置类，用于管理应用的各项设置
 */
class AppConfig(private val preferences: SharedPreferences) {
    
    /**
     * 从Context创建配置实例
     */
    constructor(context: Context) : this(
        context.getSharedPreferences("wechat_clicker_prefs", Context.MODE_PRIVATE)
    )
    
    // 主功能开关
    var mainServiceEnabled: Boolean by BooleanPreference(preferences, "main_service_enabled", true)
    
    // 自动点击未读消息
    var autoClickEnabled: Boolean by BooleanPreference(preferences, "auto_click_enabled", true)
    
    // 自动返回主界面
    var autoReturnEnabled: Boolean by BooleanPreference(preferences, "auto_return_enabled", true)
    
    // 返回延迟(毫秒)
    var returnDelay: Long by LongPreference(preferences, "return_delay", 2000L)
    
    // 检查间隔(毫秒)
    var checkInterval: Long by LongPreference(preferences, "check_interval", 1000L)
    
    // 仅在WiFi下检查
    var wifiOnly: Boolean by BooleanPreference(preferences, "wifi_only", false)
    
    // 消息过滤模式 (all, group, private, important)
    var filterMode: String by StringPreference(preferences, "filter_mode", "all")
    
    // 保持服务活跃
    var keepAlive: Boolean by BooleanPreference(preferences, "keep_alive", true)
    
    // 统计数据: 处理的消息数量
    var messagesProcessed: Int by IntPreference(preferences, "messages_processed", 0)
    
    // 统计数据: 上次检查时间
    var lastCheckTime: Long by LongPreference(preferences, "last_check_time", 0L)
    
    /**
     * 重置统计数据
     */
    fun resetStatistics() {
        messagesProcessed = 0
        lastCheckTime = 0L
    }
    
    /**
     * 增加处理消息数量
     */
    fun incrementMessagesProcessed() {
        messagesProcessed++
    }
    
    /**
     * 更新检查时间
     */
    fun updateLastCheckTime() {
        lastCheckTime = System.currentTimeMillis()
    }
}

/**
 * 用于SharedPreferences的Boolean属性委托
 */
class BooleanPreference(
    private val preferences: SharedPreferences,
    private val name: String,
    private val defaultValue: Boolean
) : ReadWriteProperty<Any, Boolean> {
    
    override fun getValue(thisRef: Any, property: KProperty<*>): Boolean {
        return preferences.getBoolean(name, defaultValue)
    }
    
    override fun setValue(thisRef: Any, property: KProperty<*>, value: Boolean) {
        preferences.edit().putBoolean(name, value).apply()
    }
}

/**
 * 用于SharedPreferences的Int属性委托
 */
class IntPreference(
    private val preferences: SharedPreferences,
    private val name: String,
    private val defaultValue: Int
) : ReadWriteProperty<Any, Int> {
    
    override fun getValue(thisRef: Any, property: KProperty<*>): Int {
        return preferences.getInt(name, defaultValue)
    }
    
    override fun setValue(thisRef: Any, property: KProperty<*>, value: Int) {
        preferences.edit().putInt(name, value).apply()
    }
}

/**
 * 用于SharedPreferences的Long属性委托
 */
class LongPreference(
    private val preferences: SharedPreferences,
    private val name: String,
    private val defaultValue: Long
) : ReadWriteProperty<Any, Long> {
    
    override fun getValue(thisRef: Any, property: KProperty<*>): Long {
        return preferences.getLong(name, defaultValue)
    }
    
    override fun setValue(thisRef: Any, property: KProperty<*>, value: Long) {
        preferences.edit().putLong(name, value).apply()
    }
}

/**
 * 用于SharedPreferences的String属性委托
 */
class StringPreference(
    private val preferences: SharedPreferences,
    private val name: String,
    private val defaultValue: String
) : ReadWriteProperty<Any, String> {
    
    override fun getValue(thisRef: Any, property: KProperty<*>): String {
        return preferences.getString(name, defaultValue) ?: defaultValue
    }
    
    override fun setValue(thisRef: Any, property: KProperty<*>, value: String) {
        preferences.edit().putString(name, value).apply()
    }
} 