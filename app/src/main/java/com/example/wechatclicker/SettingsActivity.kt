package com.example.wechatclicker

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import androidx.preference.SeekBarPreference
import androidx.preference.Preference
import androidx.preference.ListPreference
import androidx.preference.EditTextPreference
import com.example.wechatclicker.model.AppConfig
import com.google.android.material.appbar.MaterialToolbar

class SettingsActivity : AppCompatActivity() {

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

    class SettingsFragment : PreferenceFragmentCompat() {
        private lateinit var appConfig: AppConfig

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)
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
            val checkIntervalPref = findPreference<SeekBarPreference>("key_check_interval")
            val intervalMs = appConfig.checkInterval
            val intervalSec = (intervalMs / 1000).toInt()
            checkIntervalPref?.value = intervalSec
            checkIntervalPref?.setOnPreferenceChangeListener { _, newValue ->
                val newIntervalSec = newValue as Int
                appConfig.checkInterval = newIntervalSec * 1000L
                true
            }
            
            // 只在WiFi下检查
            val wifiOnlyPref = findPreference<SwitchPreferenceCompat>("key_wifi_only")
            wifiOnlyPref?.isChecked = appConfig.wifiOnly
            wifiOnlyPref?.setOnPreferenceChangeListener { _, newValue ->
                appConfig.wifiOnly = newValue as Boolean
                true
            }
            
            // 消息过滤模式
            val filterModePref = findPreference<ListPreference>("key_filter_mode")
            filterModePref?.value = appConfig.filterMode
            filterModePref?.setOnPreferenceChangeListener { _, newValue ->
                appConfig.filterMode = newValue as String
                true
            }
        }
        
        private fun setupAutoReturnPreferences() {
            // 返回延迟设置
            val returnDelayPref = findPreference<SeekBarPreference>("key_return_delay")
            val delayMs = appConfig.returnDelay
            val delaySec = (delayMs / 1000).toInt()
            returnDelayPref?.value = delaySec
            returnDelayPref?.setOnPreferenceChangeListener { _, newValue ->
                val newDelaySec = newValue as Int
                appConfig.returnDelay = newDelaySec * 1000L
                true
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
                appConfig.resetStatistics()
                true
            }
            
            // 查看日志
            val viewLogsPref = findPreference<Preference>("key_view_logs")
            viewLogsPref?.setOnPreferenceClickListener {
                startActivity(android.content.Intent(requireContext(), LogViewerActivity::class.java))
                true
            }
        }
        
        private fun setupAboutPreferences() {
            // 应用版本
            val versionPref = findPreference<Preference>("key_app_version")
            val packageInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            versionPref?.summary = packageInfo.versionName
            
            // 隐私政策
            val privacyPolicyPref = findPreference<Preference>("key_privacy_policy")
            privacyPolicyPref?.setOnPreferenceClickListener {
                startActivity(android.content.Intent(requireContext(), PrivacyPolicyActivity::class.java))
                true
            }
        }
    }
} 