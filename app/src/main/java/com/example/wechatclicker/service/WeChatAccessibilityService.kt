package com.example.wechatclicker.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.wechatclicker.MainActivity
import com.example.wechatclicker.R
import com.example.wechatclicker.model.AppConfig
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class WeChatAccessibilityService : AccessibilityService() {
    companion object {
        private const val TAG = "WeChatAccessibility"
        private const val PROCESS_DELAY = 300L
        private const val WECHAT_PACKAGE = "com.tencent.mm"
        private const val WATCHDOG_ACTION = "com.example.wechatclicker.WATCHDOG_CHECK"
        private const val WATCHDOG_INTERVAL = 60000L // 60秒检查一次
        private const val NOTIFICATION_ID_SERVICE = 1001
        private const val NOTIFICATION_ID_ENABLE = 1002
        private const val NOTIFICATION_CHANNEL_ID = "wechat_clicker_service"
        
        private var instance: WeChatAccessibilityService? = null
        
        // 服务运行状态
        var isRunning: Boolean = false
            private set
        
        fun getInstance(): WeChatAccessibilityService? {
            return instance
        }
        
        // 用于UI显示的状态变量
        var isServiceEnabled: Boolean = false
        var lastCheckedTime: Long = 0L
        var messageProcessed: Int = 0
        var lastReturnTime: Long = 0
        
        // 显示启用服务的通知
        fun showEnableServiceNotification(context: Context) {
            // 创建通知渠道
            createNotificationChannel(context)
            
            // 创建打开无障碍设置的Intent
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent, 
                PendingIntent.FLAG_IMMUTABLE
            )
            
            // 构建通知
            val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText("请启用微信消息助手无障碍服务")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
            
            // 显示通知
            with(NotificationManagerCompat.from(context)) {
                try {
                    notify(NOTIFICATION_ID_ENABLE, notification)
                } catch (e: SecurityException) {
                    Log.e(TAG, "没有通知权限", e)
                }
            }
        }
        
        // 创建通知渠道
        private fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val name = "微信消息助手服务"
                val descriptionText = "显示微信消息助手服务的状态"
                val importance = NotificationManager.IMPORTANCE_DEFAULT
                val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
                    description = descriptionText
                }
                // 注册通知渠道
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    private lateinit var appConfig: AppConfig
    private val mainHandler = Handler(Looper.getMainLooper())
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var isProcessing = false
    private var lastEventTime = 0L
    private var checkJob: Job? = null
    private var forceReturnJob: Job? = null
    private var isInWeChatApp = false
    private var watchdogReceiver: BroadcastReceiver? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // 检查主功能开关是否启用
        if (!appConfig.mainServiceEnabled) {
            return
        }
        
        // 首先检查事件是否来自微信
        val packageName = event.packageName?.toString() ?: ""
        
        // 更新当前应用状态
        val wasInWeChatApp = isInWeChatApp
        isInWeChatApp = packageName == WECHAT_PACKAGE
        
        // 如果刚刚切换到微信，尝试重置和启动服务
        if (isInWeChatApp && !wasInWeChatApp) {
            Log.d(TAG, "检测到进入微信应用，准备激活服务")
            resetAndStartService()
        }
        
        if (!isInWeChatApp) {
            // 不是微信应用，忽略
            return
        }
        
        // 记录微信事件
        Log.d(TAG, "收到事件: ${getEventTypeString(event.eventType)}, 类名: ${event.className}")
        
        // 检测到打开微信主界面的事件
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && 
            event.className?.toString()?.contains("LauncherUI") == true) {
            Log.d(TAG, "检测到微信主界面，激活服务")
            resetAndStartService()
        }

        // 防止事件处理过于频繁
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastEventTime < PROCESS_DELAY) {
            return
        }
        lastEventTime = currentTime

        // 如果正在处理其他事件，跳过
        if (isProcessing) {
            Log.d(TAG, "正在处理其他事件，跳过")
            return
        }

        serviceScope.launch {
            try {
                isProcessing = true
                processEvent(event)
            } catch (e: Exception) {
                Log.e(TAG, "处理事件时出错", e)
            } finally {
                isProcessing = false
            }
        }
    }

    private suspend fun processEvent(event: AccessibilityEvent) {
        // 再次确认是微信应用
        if (event.packageName != WECHAT_PACKAGE) {
            return
        }

        val rootNode = rootInActiveWindow
        if (rootNode == null) {
            Log.e(TAG, "无法获取活动窗口")
            return
        }
        
        try {
            val currentActivity = event.className?.toString() ?: ""
            Log.d(TAG, "当前Activity: $currentActivity")
            
            // 更新上次检查时间
            appConfig.updateLastCheckTime()
            // 同时更新静态变量以便UI显示
            lastCheckedTime = appConfig.lastCheckTime
            
            // 检查是否在聊天界面
            if (isChatUI(currentActivity, rootNode)) {
                Log.d(TAG, "检测到聊天界面")
                
                // 如果启用了自动返回功能，则返回主界面
                if (appConfig.autoReturnEnabled) {
                    Log.d(TAG, "启用了自动返回，准备返回主界面")
                    delay(appConfig.returnDelay)
                    performGlobalAction(GLOBAL_ACTION_BACK)
                    lastReturnTime = System.currentTimeMillis()
                }
                delay(PROCESS_DELAY)
                return
            }

            // 检查是否在主界面
            if (isMainUI(rootNode)) {
                Log.d(TAG, "检测到主界面，查找未读消息")
                
                // 如果启用了自动点击功能，查找未读消息
                if (appConfig.autoClickEnabled) {
                    val unreadItem = findUnreadMessageItem(rootNode)
                    if (unreadItem != null) {
                        Log.d(TAG, "找到未读消息，准备点击")
                        unreadItem.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        
                        // 增加处理消息计数
                        appConfig.incrementMessagesProcessed()
                        // 同时更新静态变量以便UI显示
                        messageProcessed = appConfig.messagesProcessed
                    } else {
                        Log.d(TAG, "未发现未读消息")
                    }
                }
            } else {
                Log.d(TAG, "不在主界面，当前在微信内部")
                
                // 如果启用了自动返回功能，且距离上次返回操作已经超过设定的时间，则尝试返回
                if (appConfig.autoReturnEnabled) {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastReturnTime > appConfig.returnDelay) {
                        Log.d(TAG, "尝试返回主界面")
                        performGlobalAction(GLOBAL_ACTION_BACK)
                        lastReturnTime = currentTime
                    }
                }
            }
        } finally {
            rootNode.recycle()
        }
    }

    // 强制返回到主界面的计时器
    private fun scheduleForceReturn() {
        if (!appConfig.autoReturnEnabled || !isInWeChatApp) return
        
        cancelForceReturn()
        forceReturnJob = serviceScope.launch {
            delay(appConfig.returnDelay)
            Log.d(TAG, "强制返回计时器触发，尝试返回主界面")
            performGlobalAction(GLOBAL_ACTION_BACK)
            lastReturnTime = System.currentTimeMillis()
        }
    }
    
    private fun cancelForceReturn() {
        forceReturnJob?.cancel()
        forceReturnJob = null
    }

    // 判断是否在聊天界面
    private fun isChatUI(className: String, rootNode: AccessibilityNodeInfo): Boolean {
        // 通过类名判断
        if (className.contains("chatting") || 
            className.contains("ChattingUI") || 
            className.contains("ConversationUI")) {
            return true
        }
        
        // 通过UI元素判断
        // 聊天界面通常有"返回"按钮
        val backButtons = rootNode.findAccessibilityNodeInfosByText("返回")
        if (backButtons.isNotEmpty()) {
            // 查找输入框等聊天界面特有元素
            val possibleInputIds = listOf(
                "com.tencent.mm:id/b4a", 
                "com.tencent.mm:id/ayd"
            )
            
            for (id in possibleInputIds) {
                try {
                    val inputBoxes = rootNode.findAccessibilityNodeInfosByViewId(id)
                    if (inputBoxes.isNotEmpty()) {
                        return true
                    }
                } catch (e: Exception) {
                    // 忽略错误，继续检查下一个ID
                }
            }
            
            // 检查是否有发送按钮
            val sendButtons = rootNode.findAccessibilityNodeInfosByText("发送")
            if (sendButtons.isNotEmpty()) {
                return true
            }
        }
        
        return false
    }

    private fun isMainUI(rootNode: AccessibilityNodeInfo): Boolean {
        // 通过底部导航栏判断是否在主界面
        val tabTexts = listOf("微信", "通讯录", "发现", "我")
        var foundCount = 0
        
        tabTexts.forEach { text ->
            if (rootNode.findAccessibilityNodeInfosByText(text).isNotEmpty()) {
                foundCount++
            }
        }
        
        Log.d(TAG, "底部导航栏找到 $foundCount 个标签")
        return foundCount >= 3
    }

    private fun findUnreadMessageItem(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        // 检查各种可能的未读消息ID
        val possibleIds = listOf(
            "com.tencent.mm:id/o_u",  // 未读消息数字标记
            "com.tencent.mm:id/a_h",   // 未读红点
            "com.tencent.mm:id/rw1",   // 可能的其他ID
            "com.tencent.mm:id/qm1",   // 可能的其他ID
            "com.tencent.mm:id/g98"    // 可能的其他ID
        )
        
        for (id in possibleIds) {
            try {
                val unreadNodes = rootNode.findAccessibilityNodeInfosByViewId(id)
                Log.d(TAG, "查找未读消息ID: $id, 找到: ${unreadNodes.size}个")
                
                for (node in unreadNodes) {
                    var parent = node.parent
                    var depth = 0
                    while (parent != null && depth < 10) {  // 限制搜索深度
                        if (parent.isClickable) {
                            return parent
                        }
                        parent = parent.parent
                        depth++
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "查找未读消息出错: $id", e)
            }
        }

        return null
    }

    private fun getEventTypeString(eventType: Int): String {
        return when (eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> "窗口状态改变"
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> "窗口内容改变"
            AccessibilityEvent.TYPE_VIEW_CLICKED -> "点击事件"
            else -> "其他事件($eventType)"
        }
    }
    
    // 重置并启动服务
    private fun resetAndStartService() {
        Log.d(TAG, "重置并启动服务")
        isProcessing = false
        
        // 确保服务标志为启用状态
        isInWeChatApp = true
        
        // 更新静态变量以便UI显示
        isServiceEnabled = true
        
        // 启动定期检查任务
        startCheckJob()
        
        // 注册定期自检
        registerWatchdog()
        
        // 显示一个提示
        Toast.makeText(this, "微信自动点击服务已激活", Toast.LENGTH_SHORT).show()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "WeChatAccessibilityService已连接")
        
        // 保存实例引用
        instance = this
        isRunning = true
        
        // 初始化配置
        appConfig = AppConfig(this)
        
        // 设置服务配置
        serviceInfo?.apply {
            // 监听的事件类型
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_CLICKED
            
            // 反馈类型
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            
            // 通知超时
            notificationTimeout = 100
            
            // 设置包名过滤
            packageNames = arrayOf(WECHAT_PACKAGE)
        }

        // 更新UI状态
        isServiceEnabled = true
        
        // 注册watchdog广播接收器
        registerWatchdogReceiver()
        scheduleWatchdog()
        
        // 重置并启动服务
        resetAndStartService()
        
        Toast.makeText(this, "微信消息助手已启动", Toast.LENGTH_SHORT).show()
    }
    
    // 启动定期检查任务
    private fun startCheckJob() {
        checkJob?.cancel()
        checkJob = serviceScope.launch {
            while (isActive) {
                try {
                    // 检查主功能开关是否启用
                    if (!appConfig.mainServiceEnabled) {
                        delay(appConfig.checkInterval)
                        continue
                    }
                    
                    // 只在微信应用内执行检查
                    if (!isInWeChatApp) {
                        Log.d(TAG, "当前不在微信应用内，跳过检查")
                        delay(appConfig.checkInterval)
                        continue
                    }
                    
                    // 如果正在处理事件，跳过
                    if (isProcessing) {
                        delay(appConfig.checkInterval)
                        continue
                    }
                    
                    Log.d(TAG, "定期检查运行中...")
                    
                    // 再次检查当前界面，确认是否在微信
                    val currentPackage = try {
                        val info = rootInActiveWindow?.packageName?.toString()
                        rootInActiveWindow?.recycle()
                        info
                    } catch (e: Exception) {
                        null
                    }
                    
                    // 确保仍在微信应用内
                    if (currentPackage != WECHAT_PACKAGE) {
                        isInWeChatApp = false
                        Log.d(TAG, "检测到已离开微信应用，更新状态")
                        delay(appConfig.checkInterval)
                        continue
                    }
                    
                    // 确保仍在微信应用内且需要执行操作
                    if (currentPackage == WECHAT_PACKAGE) {
                        // 检查当前界面
                        val rootNode = rootInActiveWindow
                        if (rootNode != null) {
                            try {
                                // 更新上次检查时间
                                appConfig.updateLastCheckTime()
                                // 同时更新静态变量以便UI显示
                                lastCheckedTime = appConfig.lastCheckTime
                                
                                if (!isMainUI(rootNode) && appConfig.autoReturnEnabled) {
                                    Log.d(TAG, "定期检查: 不在主界面，尝试返回")
                                    performGlobalAction(GLOBAL_ACTION_BACK)
                                    lastReturnTime = System.currentTimeMillis()
                                } else if (appConfig.autoClickEnabled) {
                                    val unreadItem = findUnreadMessageItem(rootNode)
                                    if (unreadItem != null) {
                                        Log.d(TAG, "定期检查: 找到未读消息，准备点击")
                                        unreadItem.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                                        
                                        // 增加处理消息计数
                                        appConfig.incrementMessagesProcessed()
                                        // 同时更新静态变量以便UI显示
                                        messageProcessed = appConfig.messagesProcessed
                                    }
                                }
                            } finally {
                                rootNode.recycle()
                            }
                        }
                    } else {
                        // 如果检测到当前应用不是微信，更新状态
                        isInWeChatApp = false
                        Log.d(TAG, "当前不在微信应用内，跳过检查")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "定期检查出错", e)
                }
                delay(appConfig.checkInterval)
            }
        }
    }
    
    // 注册看门狗定时器，确保服务一直运行
    private fun registerWatchdog() {
        try {
            // 先取消之前的Watchdog
            unregisterWatchdog()
            
            // 创建看门狗广播接收器
            watchdogReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    Log.d(TAG, "看门狗定时触发，重置服务")
                    
                    // 确认当前应用是否为微信
                    val currentPackage = try {
                        val info = rootInActiveWindow?.packageName?.toString()
                        rootInActiveWindow?.recycle()
                        info
                    } catch (e: Exception) {
                        null
                    }
                    
                    // 更新状态
                    isInWeChatApp = currentPackage == WECHAT_PACKAGE
                    
                    // 只有在微信应用中才重置服务
                    if (isInWeChatApp) {
                        resetAndStartService()
                    } else {
                        // 不在微信内，只更新状态
                        Log.d(TAG, "当前不在微信应用内，仅更新状态")
                        isProcessing = false
                    }
                }
            }
            
            // 注册广播接收器 - 使用applicationContext以避免内存泄漏
            val filter = IntentFilter(WATCHDOG_ACTION)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    applicationContext.registerReceiver(watchdogReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
                } else {
                    applicationContext.registerReceiver(watchdogReceiver, filter)
                }
                Log.d(TAG, "看门狗广播接收器注册成功")
            } catch (e: Exception) {
                Log.e(TAG, "注册看门狗广播接收器失败", e)
            }
            
            // 设置定时器
            try {
                val alarmManager = getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                if (alarmManager == null) {
                    Log.e(TAG, "获取AlarmManager失败")
                    return
                }
                
                val pendingIntent = PendingIntent.getBroadcast(
                    applicationContext, 
                    0, 
                    Intent(WATCHDOG_ACTION),
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    } else {
                        PendingIntent.FLAG_UPDATE_CURRENT
                    }
                )
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    try {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            System.currentTimeMillis() + WATCHDOG_INTERVAL,
                            pendingIntent
                        )
                        Log.d(TAG, "看门狗定时器已设置(精确模式)，间隔: ${WATCHDOG_INTERVAL/1000}秒")
                    } catch (e: Exception) {
                        Log.e(TAG, "设置精确看门狗定时器失败", e)
                        alarmManager.set(
                            AlarmManager.RTC_WAKEUP,
                            System.currentTimeMillis() + WATCHDOG_INTERVAL,
                            pendingIntent
                        )
                        Log.d(TAG, "看门狗定时器已设置(普通模式)，间隔: ${WATCHDOG_INTERVAL/1000}秒")
                    }
                } else {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        System.currentTimeMillis() + WATCHDOG_INTERVAL,
                        pendingIntent
                    )
                    Log.d(TAG, "看门狗定时器已设置(普通模式)，间隔: ${WATCHDOG_INTERVAL/1000}秒")
                }
            } catch (e: Exception) {
                Log.e(TAG, "设置看门狗定时器失败", e)
            }
            
            // 使用Handler作为备选方案
            try {
                mainHandler.postDelayed({
                    Log.d(TAG, "备选看门狗触发")
                    // 确认当前应用是否为微信
                    val currentPackage = try {
                        val info = rootInActiveWindow?.packageName?.toString()
                        rootInActiveWindow?.recycle()
                        info
                    } catch (e: Exception) {
                        null
                    }
                    
                    // 更新状态
                    isInWeChatApp = currentPackage == WECHAT_PACKAGE
                    
                    // 只有在微信应用中才重置服务
                    if (isInWeChatApp) {
                        resetAndStartService()
                    }
                }, WATCHDOG_INTERVAL)
                Log.d(TAG, "备选看门狗定时器已设置")
            } catch (e2: Exception) {
                Log.e(TAG, "备选看门狗设置也失败", e2)
            }
        } catch (e: Exception) {
            Log.e(TAG, "设置看门狗定时器失败", e)
        }
    }
    
    private fun unregisterWatchdog() {
        try {
            watchdogReceiver?.let {
                try {
                    applicationContext.unregisterReceiver(it)
                    Log.d(TAG, "看门狗广播接收器注销成功")
                } catch (e: Exception) {
                    Log.e(TAG, "注销看门狗广播接收器失败", e)
                }
                watchdogReceiver = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "注销看门狗失败", e)
        }
        
        // 清除备选Handler
        try {
            mainHandler.removeCallbacksAndMessages(null)
            Log.d(TAG, "备选看门狗已清除")
        } catch (e: Exception) {
            Log.e(TAG, "清除备选看门狗失败", e)
        }
    }

    private fun requestIgnoreBatteryOptimization() {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = android.net.Uri.parse("package:$packageName")
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "请求忽略电池优化失败", e)
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "WeChatAccessibilityService被中断")
        
        // 更新状态
        isRunning = false
        
        // 更新静态变量以便UI显示
        isServiceEnabled = false
        
        cancelAllJobs()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "WeChatAccessibilityService已销毁")
        
        // 取消所有任务
        serviceScope.cancel()
        
        // 解注册watchdog接收器
        unregisterWatchdogReceiver()
        
        // 更新状态
        instance = null
        isServiceEnabled = false
        isRunning = false
        
        Toast.makeText(this, "微信消息助手已停止", Toast.LENGTH_SHORT).show()
    }
    
    private fun cancelAllJobs() {
        checkJob?.cancel()
        forceReturnJob?.cancel()
        serviceScope.cancel()
        unregisterWatchdog()
    }

    // 注册Watchdog广播接收器
    private fun registerWatchdogReceiver() {
        if (watchdogReceiver != null) {
            return
        }
        
        watchdogReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == WATCHDOG_ACTION) {
                    Log.d(TAG, "收到Watchdog广播，检查服务状态")
                    
                    // 确保服务仍在运行，如有需要重新启动相关任务
                    if (isServiceEnabled && appConfig.keepAlive) {
                        resetAndStartService()
                    }
                    
                    // 重新调度下一次检查
                    scheduleWatchdog()
                }
            }
        }
        
        val filter = IntentFilter(WATCHDOG_ACTION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(watchdogReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(watchdogReceiver, filter)
        }
        Log.d(TAG, "已注册Watchdog广播接收器")
    }
    
    // 解注册Watchdog广播接收器
    private fun unregisterWatchdogReceiver() {
        if (watchdogReceiver != null) {
            try {
                unregisterReceiver(watchdogReceiver)
                Log.d(TAG, "已解注册Watchdog广播接收器")
            } catch (e: Exception) {
                Log.e(TAG, "解注册Watchdog广播接收器失败: ${e.message}")
            }
            watchdogReceiver = null
        }
    }
    
    // 调度Watchdog检查
    private fun scheduleWatchdog() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(WATCHDOG_ACTION)
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // 设置闹钟，定期触发服务检查
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + WATCHDOG_INTERVAL,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + WATCHDOG_INTERVAL,
                pendingIntent
            )
        }
        
        Log.d(TAG, "Watchdog检查已调度，将在 ${WATCHDOG_INTERVAL / 1000} 秒后触发")
    }
} 