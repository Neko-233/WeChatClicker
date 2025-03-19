package com.example.wechatclicker

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.wechatclicker.model.AppConfig
import com.example.wechatclicker.service.WeChatAccessibilityService
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.switchmaterial.SwitchMaterial
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), AppConfig.ConfigChangeListener {
    
    companion object {
        private const val TAG = "MainActivity"
    }
    
    private lateinit var appConfig: AppConfig
    
    private lateinit var serviceStatusText: TextView
    private lateinit var lastCheckText: TextView
    private lateinit var messageCountText: TextView
    private lateinit var mainSwitch: SwitchMaterial
    private lateinit var autoClickSwitch: SwitchMaterial
    private lateinit var autoReturnSwitch: SwitchMaterial
    private lateinit var accessibilityBtn: MaterialButton
    private lateinit var settingsBtn: MaterialButton
    private lateinit var aboutBtn: MaterialButton
    private lateinit var toolbar: MaterialToolbar

    override fun attachBaseContext(newBase: Context) {
        // 在Activity创建之前应用语言设置
        val context = applyLanguage(newBase)
        super.attachBaseContext(context)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // 设置工具栏
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        
        // 初始化配置
        appConfig = AppConfig(this)
        appConfig.addChangeListener(this)

        // 初始化视图
        initializeViews()
        
        // 初始化卡片点击事件
        setupCardClickListeners()
        
        // 更新界面状态
        updateServiceStatus()
        updateStatistics()
    }
    
    private fun setupCardClickListeners() {
        // 为主功能卡片添加点击效果
        val mainFunctionCard = findViewById<MaterialCardView>(R.id.mainFunctionCard)
        mainFunctionCard.setOnClickListener {
            // 切换开关状态
            mainSwitch.isChecked = !mainSwitch.isChecked
        }
        
        // 为服务状态卡片添加点击效果
        val serviceStatusCard = findViewById<MaterialCardView>(R.id.serviceStatusCard)
        serviceStatusCard.setOnClickListener {
            if (!isAccessibilityServiceEnabled()) {
                openAccessibilitySettings()
            }
        }
        
        // 为功能设置卡片添加点击效果
        val functionSettingsCard = findViewById<MaterialCardView>(R.id.functionSettingsCard)
        functionSettingsCard.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        
        // 为关于卡片添加点击效果
        val aboutCard = findViewById<MaterialCardView>(R.id.aboutCard)
        aboutCard.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }
    }
    
    private fun initializeViews() {
        // 初始化视图
        serviceStatusText = findViewById(R.id.serviceStatusText)
        lastCheckText = findViewById(R.id.lastCheckText)
        messageCountText = findViewById(R.id.messageCountText)
        mainSwitch = findViewById(R.id.mainSwitch)
        autoClickSwitch = findViewById(R.id.autoClickSwitch)
        autoReturnSwitch = findViewById(R.id.autoReturnSwitch)
        accessibilityBtn = findViewById(R.id.accessibilityBtn)
        settingsBtn = findViewById(R.id.settingsBtn)
        aboutBtn = findViewById(R.id.aboutBtn)

        // 设置点击监听器
        mainSwitch.setOnCheckedChangeListener { _, isChecked ->
            appConfig.mainServiceEnabled = isChecked
            updateFunctionSwitchesEnabled()
            updateServiceStatus()
        }

        autoClickSwitch.setOnCheckedChangeListener { _, isChecked ->
            appConfig.autoClickEnabled = isChecked
        }

        autoReturnSwitch.setOnCheckedChangeListener { _, isChecked ->
            appConfig.autoReturnEnabled = isChecked
        }

        accessibilityBtn.setOnClickListener {
            openAccessibilitySettings()
        }

        settingsBtn.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        
        aboutBtn.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            R.id.action_about -> {
                startActivity(Intent(this, AboutActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    override fun onResume() {
        super.onResume()
        
        // 更新无障碍服务状态
        updateServiceStatus()
        
        // 更新统计信息
        updateStatistics()
        
        // 从配置同步UI状态
        syncUIWithConfig()
    }
    
    private fun syncUIWithConfig() {
        mainSwitch.isChecked = appConfig.mainServiceEnabled
        autoClickSwitch.isChecked = appConfig.autoClickEnabled
        autoReturnSwitch.isChecked = appConfig.autoReturnEnabled
        
        updateFunctionSwitchesEnabled()
    }
    
    private fun updateServiceStatus() {
        val serviceEnabled = isAccessibilityServiceEnabled()
        val statusText = if (serviceEnabled) {
            getString(R.string.service_status_format, getString(R.string.service_status_running))
        } else {
            getString(R.string.service_status_format, getString(R.string.service_status_stopped))
        }
        serviceStatusText.text = statusText
        
        // 更新按钮状态
        accessibilityBtn.isEnabled = !serviceEnabled
    }
    
    private fun updateStatistics() {
        if (isAccessibilityServiceEnabled()) {
            // 显示上次检查时间
            val lastCheckTime = appConfig.lastCheckTime
            if (lastCheckTime > 0) {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val formattedTime = dateFormat.format(Date(lastCheckTime))
                lastCheckText.text = getString(R.string.last_check_time_format, formattedTime)
            } else {
                lastCheckText.text = getString(R.string.last_check_time_none)
            }
            
            // 显示已处理消息数
            messageCountText.text = getString(R.string.messages_processed_format, appConfig.messagesProcessed)
        } else {
            lastCheckText.text = getString(R.string.service_not_running_status)
            messageCountText.text = getString(R.string.messages_processed_format, 0)
        }
    }
    
    private fun isAccessibilityServiceEnabled(): Boolean {
        return WeChatAccessibilityService.isRunning
    }
    
    // 更新功能开关的状态
    private fun updateFunctionSwitchesEnabled() {
        val enabled = mainSwitch.isChecked && isAccessibilityServiceEnabled()
        autoClickSwitch.isEnabled = enabled
        autoReturnSwitch.isEnabled = enabled
    }
    
    // 打开无障碍设置
    private fun openAccessibilitySettings() {
        try {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        } catch (e: Exception) {
            Toast.makeText(this, R.string.toast_cannot_open_accessibility_settings, Toast.LENGTH_LONG).show()
        }
    }
    
    // 应用语言设置
    private fun applyLanguage(context: Context): Context {
        val appConfig = AppConfig(context)
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
        
        Log.d(TAG, "应用语言: ${locale.language}")
        
        return updateResources(context, locale)
    }
    
    private fun updateResources(context: Context, locale: Locale): Context {
        Locale.setDefault(locale)
        
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        
        return context.createConfigurationContext(configuration)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        appConfig.removeChangeListener(this)
    }
    
    // 实现 AppConfig.ConfigChangeListener 接口
    override fun onConfigChanged(key: String, value: Any) {
        runOnUiThread {
            // 配置发生变化时更新UI
            when (key) {
                "main_service_enabled" -> {
                    mainSwitch.isChecked = value as Boolean
                    updateFunctionSwitchesEnabled()
                }
                "auto_click_enabled" -> autoClickSwitch.isChecked = value as Boolean
                "auto_return_enabled" -> autoReturnSwitch.isChecked = value as Boolean
            }
            
            // 更新服务状态和统计信息
            updateServiceStatus()
            updateStatistics()
        }
    }
}