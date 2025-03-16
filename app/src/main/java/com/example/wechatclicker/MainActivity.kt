package com.example.wechatclicker

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.example.wechatclicker.model.AppConfig
import com.example.wechatclicker.service.ForegroundKeepAliveService
import com.example.wechatclicker.service.WeChatAccessibilityService
import com.google.android.material.card.MaterialCardView
import com.google.android.material.switchmaterial.SwitchMaterial
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    
    private lateinit var appConfig: AppConfig
    private lateinit var serviceStatusText: TextView
    private lateinit var lastCheckedText: TextView
    private lateinit var messagesProcessedText: TextView
    private lateinit var enableServiceButton: Button
    private lateinit var mainServiceSwitch: SwitchMaterial
    private lateinit var autoClickSwitch: SwitchMaterial
    private lateinit var autoReturnSwitch: SwitchMaterial
    private lateinit var keepAliveSwitch: SwitchMaterial
    private lateinit var toolbar: Toolbar
    private lateinit var keepAliveCard: MaterialCardView
    
    private var timer: Timer? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // 设置Toolbar
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        
        appConfig = AppConfig(this)
        
        // 初始化视图组件
        serviceStatusText = findViewById(R.id.serviceStatusText)
        lastCheckedText = findViewById(R.id.lastCheckedText)
        messagesProcessedText = findViewById(R.id.messagesProcessedText)
        enableServiceButton = findViewById(R.id.enableServiceButton)
        mainServiceSwitch = findViewById(R.id.mainServiceSwitch)
        autoClickSwitch = findViewById(R.id.autoClickSwitch)
        autoReturnSwitch = findViewById(R.id.autoReturnSwitch)
        keepAliveSwitch = findViewById(R.id.keepAliveSwitch)
        keepAliveCard = findViewById(R.id.keepAliveCard)
        
        // 设置开关初始状态
        mainServiceSwitch.isChecked = appConfig.mainServiceEnabled
        autoClickSwitch.isChecked = appConfig.autoClickEnabled
        autoReturnSwitch.isChecked = appConfig.autoReturnEnabled
        keepAliveSwitch.isChecked = appConfig.keepAlive
        
        // 设置点击监听器
        enableServiceButton.setOnClickListener {
            openAccessibilitySettings()
        }
        
        mainServiceSwitch.setOnCheckedChangeListener { _, isChecked ->
            appConfig.mainServiceEnabled = isChecked
            
            // 更新UI显示状态
            updateFunctionSwitchesEnabled()
            
            // 处理前台服务
            if (isChecked) {
                if (appConfig.keepAlive && isAccessibilityServiceEnabled()) {
                    // 启动前台服务
                    ForegroundKeepAliveService.startService(this)
                }
                Toast.makeText(
                    this, 
                    "微信消息助手已启用", 
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                // 停止前台服务
                ForegroundKeepAliveService.stopService(this)
                Toast.makeText(
                    this, 
                    "微信消息助手已禁用", 
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        
        autoClickSwitch.setOnCheckedChangeListener { _, isChecked ->
            appConfig.autoClickEnabled = isChecked
            Toast.makeText(
                this, 
                "自动点击功能已${if (isChecked) "启用" else "禁用"}", 
                Toast.LENGTH_SHORT
            ).show()
        }
        
        autoReturnSwitch.setOnCheckedChangeListener { _, isChecked ->
            appConfig.autoReturnEnabled = isChecked
            Toast.makeText(
                this, 
                "自动返回功能已${if (isChecked) "启用" else "禁用"}", 
                Toast.LENGTH_SHORT
            ).show()
        }
        
        keepAliveSwitch.setOnCheckedChangeListener { _, isChecked ->
            appConfig.keepAlive = isChecked
            
            if (isChecked && appConfig.mainServiceEnabled && isAccessibilityServiceEnabled()) {
                // 启动前台服务
                ForegroundKeepAliveService.startService(this)
                Toast.makeText(
                    this, 
                    "保持服务活跃功能已启用，将在微信启动时自动激活服务", 
                    Toast.LENGTH_LONG
                ).show()
            } else {
                // 停止前台服务
                ForegroundKeepAliveService.stopService(this)
                Toast.makeText(
                    this, 
                    "保持服务活跃功能已禁用", 
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                // 打开设置页面
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    override fun onResume() {
        super.onResume()
        startStatusUpdates()
    }
    
    override fun onPause() {
        super.onPause()
        stopStatusUpdates()
    }
    
    private fun openAccessibilitySettings() {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
            Toast.makeText(
                this,
                "请在无障碍服务中找到并启用\"微信消息助手\"",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "无法打开无障碍设置，请手动前往系统设置",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    private fun startStatusUpdates() {
        timer = Timer()
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    updateServiceStatus()
                }
            }
        }, 0, 1000)
    }
    
    private fun stopStatusUpdates() {
        timer?.cancel()
        timer = null
    }
    
    private fun isAccessibilityServiceEnabled(): Boolean {
        return WeChatAccessibilityService.getInstance() != null
    }
    
    private fun updateFunctionSwitchesEnabled() {
        val enabled = appConfig.mainServiceEnabled
        autoClickSwitch.isEnabled = enabled
        autoReturnSwitch.isEnabled = enabled
        keepAliveSwitch.isEnabled = enabled
        keepAliveCard.isEnabled = enabled
    }
    
    private fun updateServiceStatus() {
        val service = WeChatAccessibilityService.getInstance()
        if (service != null) {
            // 服务已启动
            serviceStatusText.text = "无障碍服务状态：已启动"
            serviceStatusText.setTextColor(ContextCompat.getColor(this, R.color.green))
            
            // 更新消息计数
            val count = WeChatAccessibilityService.messageProcessed
            messagesProcessedText.text = "已处理消息数：$count"
            
            // 更新上次检查时间
            val lastCheckedTime = WeChatAccessibilityService.lastCheckedTime
            if (lastCheckedTime > 0) {
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val timeString = sdf.format(Date(lastCheckedTime))
                lastCheckedText.text = "上次检查时间：$timeString"
            } else {
                lastCheckedText.text = "上次检查时间：尚未检查"
            }
            
            // 根据主功能开关状态设置功能开关的启用状态
            updateFunctionSwitchesEnabled()
            
        } else {
            // 服务未启动
            serviceStatusText.text = "无障碍服务状态：未启动"
            serviceStatusText.setTextColor(ContextCompat.getColor(this, R.color.red))
            lastCheckedText.text = "上次检查时间：服务未启动"
            messagesProcessedText.text = "已处理消息数：0"
            
            // 在无障碍服务未启动时，功能开关仍然可点击，但会自动禁用效果
            autoClickSwitch.isEnabled = false
            autoReturnSwitch.isEnabled = false
            keepAliveSwitch.isEnabled = false
            keepAliveCard.isEnabled = false
        }
        
        // 主功能开关始终可用
        mainServiceSwitch.isEnabled = true
        
        // 如果主功能被关闭，确保前台服务不会运行
        if (!appConfig.mainServiceEnabled) {
            ForegroundKeepAliveService.stopService(this)
        }
    }
}