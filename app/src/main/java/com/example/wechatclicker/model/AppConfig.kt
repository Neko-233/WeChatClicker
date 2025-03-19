package com.example.wechatclicker.model

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * 应用配置类 - 管理所有应用设置
 */
class AppConfig(private val context: Context) {
    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val listeners = mutableListOf<ConfigChangeListener>()
    
    // 主服务开关
    var mainServiceEnabled: Boolean
        get() = prefs.getBoolean("pref_main_service_enabled", false)
        set(value) {
            prefs.edit().putBoolean("pref_main_service_enabled", value).apply()
            notifyListeners("main_service_enabled", value)
        }
    
    // 自动点击功能开关
    var autoClickEnabled: Boolean
        get() = prefs.getBoolean("pref_auto_click_enabled", false)
        set(value) {
            prefs.edit().putBoolean("pref_auto_click_enabled", value).apply()
            notifyListeners("auto_click_enabled", value)
        }
    
    // 自动返回功能开关
    var autoReturnEnabled: Boolean
        get() = prefs.getBoolean("pref_auto_return_enabled", false)
        set(value) {
            prefs.edit().putBoolean("pref_auto_return_enabled", value).apply()
            notifyListeners("auto_return_enabled", value)
        }
    
    // 保活功能开关
    var keepAlive: Boolean
        get() = prefs.getBoolean("pref_keep_alive", false)
        set(value) {
            prefs.edit().putBoolean("pref_keep_alive", value).apply()
            notifyListeners("keep_alive", value)
        }
    
    // 消息检查间隔
    var checkInterval: Long
        get() = prefs.getString("pref_check_interval", "1000")?.toLongOrNull() ?: 1000L
        set(value) {
            prefs.edit().putString("pref_check_interval", value.toString()).apply()
            notifyListeners("check_interval", value)
        }
    
    // 自动返回延迟
    var returnDelay: Long
        get() = prefs.getString("pref_return_delay", "1000")?.toLongOrNull() ?: 1000L
        set(value) {
            prefs.edit().putString("pref_return_delay", value.toString()).apply()
            notifyListeners("return_delay", value)
        }
    
    // 应用语言设置
    var language: String
        get() = prefs.getString("pref_language", "system") ?: "system"
        set(value) {
            prefs.edit().putString("pref_language", value).apply()
            notifyListeners("language", value)
        }
    
    // 最后检查时间
    var lastCheckTime: Long
        get() = prefs.getLong("last_check_time", 0L)
        private set(value) {
            prefs.edit().putLong("last_check_time", value).apply()
        }
    
    // 处理消息计数
    var messagesProcessed: Int
        get() = prefs.getInt("messages_processed", 0)
        private set(value) {
            prefs.edit().putInt("messages_processed", value).apply()
        }
    
    // 更新最后检查时间
    fun updateLastCheckTime() {
        lastCheckTime = System.currentTimeMillis()
    }
    
    // 增加已处理消息计数
    fun incrementMessagesProcessed() {
        messagesProcessed = messagesProcessed + 1
    }
    
    // 重置计数
    fun resetCounters() {
        prefs.edit().apply {
            putLong("last_check_time", 0L)
            putInt("messages_processed", 0)
            apply()
        }
    }
    
    // 添加配置变更监听器
    fun addChangeListener(listener: ConfigChangeListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }
    
    // 移除配置变更监听器
    fun removeChangeListener(listener: ConfigChangeListener) {
        listeners.remove(listener)
    }
    
    // 通知所有监听器配置已变更
    private fun notifyListeners(key: String, value: Any) {
        for (listener in listeners) {
            listener.onConfigChanged(key, value)
        }
    }
    
    // 配置变更监听器接口
    interface ConfigChangeListener {
        fun onConfigChanged(key: String, value: Any)
    }
}

/**
 * 用于SharedPreferences的Boolean属性委托
 */
class BooleanPreference(
    private val preferences: SharedPreferences,
    private val name: String,
    private val defaultValue: Boolean,
    private val onChange: ((String, Boolean) -> Unit)? = null
) : ReadWriteProperty<Any, Boolean> {
    
    override fun getValue(thisRef: Any, property: KProperty<*>): Boolean {
        return preferences.getBoolean(name, defaultValue)
    }
    
    override fun setValue(thisRef: Any, property: KProperty<*>, value: Boolean) {
        preferences.edit().putBoolean(name, value).apply()
        onChange?.invoke(name, value)
    }
}

/**
 * 用于SharedPreferences的Int属性委托
 */
class IntPreference(
    private val preferences: SharedPreferences,
    private val name: String,
    private val defaultValue: Int,
    private val onChange: ((String, Int) -> Unit)? = null
) : ReadWriteProperty<Any, Int> {
    
    override fun getValue(thisRef: Any, property: KProperty<*>): Int {
        return preferences.getInt(name, defaultValue)
    }
    
    override fun setValue(thisRef: Any, property: KProperty<*>, value: Int) {
        preferences.edit().putInt(name, value).apply()
        onChange?.invoke(name, value)
    }
}

/**
 * 用于SharedPreferences的Long属性委托
 */
class LongPreference(
    private val preferences: SharedPreferences,
    private val name: String,
    private val defaultValue: Long,
    private val onChange: ((String, Long) -> Unit)? = null
) : ReadWriteProperty<Any, Long> {
    
    override fun getValue(thisRef: Any, property: KProperty<*>): Long {
        return preferences.getLong(name, defaultValue)
    }
    
    override fun setValue(thisRef: Any, property: KProperty<*>, value: Long) {
        preferences.edit().putLong(name, value).apply()
        onChange?.invoke(name, value)
    }
}

/**
 * 用于SharedPreferences的String属性委托
 */
class StringPreference(
    private val preferences: SharedPreferences,
    private val name: String,
    private val defaultValue: String,
    private val onChange: ((String, String) -> Unit)? = null
) : ReadWriteProperty<Any, String> {
    
    override fun getValue(thisRef: Any, property: KProperty<*>): String {
        return preferences.getString(name, defaultValue) ?: defaultValue
    }
    
    override fun setValue(thisRef: Any, property: KProperty<*>, value: String) {
        preferences.edit().putString(name, value).apply()
        onChange?.invoke(name, value)
    }
} 