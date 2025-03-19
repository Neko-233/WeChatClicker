package com.example.wechatclicker

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.LocaleList
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import androidx.preference.SeekBarPreference
import androidx.preference.Preference
import androidx.preference.EditTextPreference
import com.example.wechatclicker.model.AppConfig
import com.google.android.material.appbar.MaterialToolbar
import java.util.Locale
import android.content.res.Resources
import android.net.Uri
import android.os.Build
import android.util.Log
import android.content.SharedPreferences

class SettingsActivity : AppCompatActivity() {

    companion object {
        // 添加TAG常量，供日志使用
        private const val TAG = "SettingsActivity"
        
        // 从SettingsActivity.Companion中移动过来的方法
        fun getLanguagePreference(context: Context): String {
            return context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                .getString("language", "system") ?: "system"
        }

        fun applyLanguage(context: Context) {
            val languageCode = getLanguagePreference(context)
            val locale = when (languageCode) {
                "system" -> {
                    // 获取系统语言
                    val systemLocale = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        context.resources.configuration.locales[0]
                    } else {
                        @Suppress("DEPRECATION")
                        context.resources.configuration.locale
                    }
                    // 如果系统语言是中文或英文，使用系统语言，否则默认使用英文
                    when (systemLocale.language) {
                        "zh" -> Locale("zh", "CN")
                        "en" -> Locale("en")
                        else -> Locale("en")
                    }
                }
                "zh" -> Locale("zh", "CN")
                "en" -> Locale("en")
                else -> Locale("en")
            }

            Locale.setDefault(locale)
            val config = Configuration(context.resources.configuration)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                config.setLocales(LocaleList(locale))
            } else {
                @Suppress("DEPRECATION")
                config.locale = locale
            }
            context.createConfigurationContext(config)
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
        }
    }

    override fun attachBaseContext(base: Context) {
        // 在Activity创建前应用语言设置
        val appConfig = AppConfig(base)
        val localeCode = appConfig.language
        
        val locale = when (localeCode) {
            "system" -> {
                // 获取系统语言
                val systemLocale = Resources.getSystem().configuration.locales.get(0)
                // 如果系统语言是中文或英文，使用系统语言，否则默认英文
                when {
                    systemLocale.language.startsWith("zh") -> Locale.CHINESE
                    systemLocale.language.startsWith("en") -> Locale.ENGLISH
                    else -> Locale.ENGLISH
                }
            }
            "zh" -> Locale.CHINESE
            "en" -> Locale.ENGLISH
            else -> Locale.ENGLISH
        }
        
        Locale.setDefault(locale)
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocale(locale)
        val context = base.createConfigurationContext(configuration)
        
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        // 启用边缘到边缘显示
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // 设置工具栏
        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.title_settings)

        // 添加设置内容Fragment
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings_container, SettingsFragment())
                .commit()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {
        
        companion object {
            private const val TAG = "SettingsFragment"
        }
        
        private lateinit var sharedPreferences: SharedPreferences
        private lateinit var appConfig: AppConfig

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)
            sharedPreferences = preferenceManager.sharedPreferences!!
            appConfig = AppConfig(requireContext())

            // 功能设置组
            setupFunctionalPreferences()
            
            // 消息检查组
            setupMessageCheckPreferences()
            
            // 自动返回组
            setupAutoReturnPreferences()
            
            // 高级设置组
            setupAdvancedPreferences()
            
            // 关于应用组
            setupAboutPreferences()

            setupLanguagePreference()
        }
        
        override fun onResume() {
            super.onResume()
            sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        }
        
        override fun onPause() {
            super.onPause()
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        }
        
        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            when (key) {
                "check_interval" -> {
                    val intervalPref = findPreference<Preference>("check_interval")
                    val interval = sharedPreferences?.getInt("check_interval", 1000) ?: 1000
                    intervalPref?.summary = getString(R.string.pref_check_interval_summary, interval)
                }
                "return_delay" -> {
                    val delayPref = findPreference<Preference>("return_delay")
                    val delay = sharedPreferences?.getInt("return_delay", 1000) ?: 1000
                    delayPref?.summary = getString(R.string.pref_return_delay_summary, delay)
                }
                "language" -> {
                    // 更新界面语言
                    val newLocale = when (sharedPreferences?.getString("language", "system")) {
                        "zh" -> Locale("zh", "CN")
                        "en" -> Locale("en", "US")
                        else -> // 系统语言
                            Resources.getSystem().configuration.locales.get(0)
                    }
                    
                    // 更新语言设置后需要重启Activity
                    updateLocale(newLocale)
                    
                    // 重启Activity以应用新的语言设置
                    requireActivity().recreate()
                }
            }
        }
        
        private fun updateLocale(locale: Locale) {
            val config = Configuration(resources.configuration)
            config.setLocale(locale)
            resources.updateConfiguration(config, resources.displayMetrics)
        }
        
        private fun setupFunctionalPreferences() {
            // 自动点击开关
            val autoClickPref = findPreference<SwitchPreferenceCompat>("key_auto_click")
            autoClickPref?.isChecked = appConfig.autoClickEnabled
            autoClickPref?.setOnPreferenceChangeListener { _, newValue ->
                appConfig.autoClickEnabled = newValue as Boolean
                true
            }
            
            // 自动返回开关
            val autoReturnPref = findPreference<SwitchPreferenceCompat>("key_auto_return")
            autoReturnPref?.isChecked = appConfig.autoReturnEnabled
            autoReturnPref?.setOnPreferenceChangeListener { _, newValue ->
                appConfig.autoReturnEnabled = newValue as Boolean
                true
            }
        }
        
        private fun setupMessageCheckPreferences() {
            // 检查间隔设置
            val checkIntervalPref = findPreference<EditTextPreference>("key_check_interval")
            val intervalMs = appConfig.checkInterval
            checkIntervalPref?.apply {
                text = intervalMs.toString()
                setSummaryProvider { 
                    "${text}${getString(R.string.milliseconds)}"
                }
                setOnPreferenceChangeListener { _, newValue ->
                    try {
                        val newIntervalMs = (newValue as String).toInt().coerceIn(100, 10000)
                        appConfig.checkInterval = newIntervalMs.toLong()
                        text = newIntervalMs.toString()
                        true
                    } catch (e: NumberFormatException) {
                        Toast.makeText(requireContext(), getString(R.string.toast_input_number_range), Toast.LENGTH_SHORT).show()
                        false
                    }
                }
            }
            
            // 只在WiFi下检查 - 临时禁用该功能
            val wifiOnlyPref = findPreference<SwitchPreferenceCompat>("key_wifi_only")
            wifiOnlyPref?.isEnabled = false
            wifiOnlyPref?.setOnPreferenceChangeListener { _, _ ->
                // 当前版本不支持此功能
                Toast.makeText(requireContext(), getString(R.string.feature_not_implemented), Toast.LENGTH_SHORT).show()
                false
            }
        }
        
        private fun setupAutoReturnPreferences() {
            // 返回延迟设置
            val returnDelayPref = findPreference<EditTextPreference>("key_return_delay")
            val delayMs = appConfig.returnDelay
            returnDelayPref?.apply {
                text = delayMs.toString()
                setSummaryProvider { 
                    "${text}${getString(R.string.milliseconds)}"
                }
                setOnPreferenceChangeListener { _, newValue ->
                    try {
                        val newDelayMs = (newValue as String).toInt().coerceIn(100, 10000)
                        appConfig.returnDelay = newDelayMs.toLong()
                        text = newDelayMs.toString()
                        true
                    } catch (e: NumberFormatException) {
                        Toast.makeText(requireContext(), getString(R.string.toast_input_number_range), Toast.LENGTH_SHORT).show()
                        false
                    }
                }
            }
        }
        
        private fun setupAdvancedPreferences() {
            // 保持服务活跃
            val keepAlivePref = findPreference<SwitchPreferenceCompat>("key_keep_alive")
            keepAlivePref?.isChecked = appConfig.keepAlive
            keepAlivePref?.setOnPreferenceChangeListener { _, newValue ->
                appConfig.keepAlive = newValue as Boolean
                true
            }
            
            // 统计重置
            val resetStatsPref = findPreference<Preference>("key_reset_stats")
            resetStatsPref?.setOnPreferenceClickListener {
                appConfig.resetCounters()
                Toast.makeText(requireContext(), getString(R.string.stats_reset), Toast.LENGTH_SHORT).show()
                true
            }
            
            // 查看日志 - 临时禁用
            val viewLogsPref = findPreference<Preference>("key_view_logs")
            viewLogsPref?.isEnabled = false
            viewLogsPref?.setOnPreferenceClickListener {
                Toast.makeText(requireContext(), getString(R.string.feature_not_implemented), Toast.LENGTH_SHORT).show()
                true
            }
        }
        
        private fun setupAboutPreferences() {
            // 设置关于页面
            val aboutPref = findPreference<Preference>("key_about")
            aboutPref?.setOnPreferenceClickListener {
                startActivity(Intent(requireContext(), AboutActivity::class.java))
                true
            }
        }

        private fun setupLanguagePreference() {
            val languagePref = findPreference<ListPreference>("language")
            languagePref?.apply {
                setOnPreferenceChangeListener { _, newValue ->
                    val appConfig = AppConfig(requireContext())
                    appConfig.language = newValue.toString()
                    
                    // 立即应用语言变更
                    // 注意：这里会重新创建整个Activity栈，确保所有页面都使用新语言
                    val intent = Intent(requireContext(), MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                    
                    true
                }
            }
        }
    }
} 