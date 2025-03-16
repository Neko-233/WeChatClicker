package com.example.wechatclicker.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.wechatclicker.MainActivity
import com.example.wechatclicker.R
import com.example.wechatclicker.model.AppConfig
import java.util.Timer
import java.util.TimerTask

/**
 * 前台服务，确保无障碍服务持续运行，即使应用被后台清理
 */
class ForegroundKeepAliveService : Service() {
    
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "wechat_clicker_service_channel"
        private const val TAG = "FgKeepAliveService"
        
        // 启动前台服务的方法
        fun startService(context: Context) {
            val intent = Intent(context, ForegroundKeepAliveService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            Log.d(TAG, "Service start requested")
        }
        
        // 停止前台服务的方法
        fun stopService(context: Context) {
            val intent = Intent(context, ForegroundKeepAliveService::class.java)
            context.stopService(intent)
            Log.d(TAG, "Service stop requested")
        }
    }
    
    private var timer: Timer? = null
    private lateinit var appConfig: AppConfig
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate")
        appConfig = AppConfig(this)
        
        // 创建通知渠道（仅Android 8.0及以上需要）
        createNotificationChannel()
        
        // 启动前台服务
        startForeground(NOTIFICATION_ID, createNotification())
        
        // 启动定时检查
        startServiceStatusCheck()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service onStartCommand")
        
        // 如果服务被系统杀死后重新创建，将再次启动前台模式
        if (intent == null) {
            startForeground(NOTIFICATION_ID, createNotification())
        }
        
        // 确保无障碍服务在运行
        ensureAccessibilityServiceRunning()
        
        // 返回START_STICKY表示服务如果被系统杀死，会尝试重新创建
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    override fun onDestroy() {
        Log.d(TAG, "Service onDestroy")
        stopTimer()
        super.onDestroy()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "微信消息助手服务",
                NotificationManager.IMPORTANCE_LOW // 低重要性以减少视觉干扰
            ).apply {
                description = "保持微信消息助手在后台运行"
                setShowBadge(false) // 不显示角标
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        // 创建点击通知时打开应用的意图
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        
        // 构建通知
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("微信消息助手正在运行")
            .setContentText("服务保持活跃中，确保消息能被自动处理")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true) // 设置通知为持续通知
            .build()
    }
    
    private fun startServiceStatusCheck() {
        stopTimer()
        timer = Timer()
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                ensureAccessibilityServiceRunning()
            }
        }, 0, 10000) // 每10秒检查一次
    }
    
    private fun stopTimer() {
        timer?.cancel()
        timer = null
    }
    
    private fun ensureAccessibilityServiceRunning() {
        // 首先检查主功能开关是否开启
        if (!appConfig.mainServiceEnabled) {
            // 主功能关闭，停止前台服务
            stopSelf()
            return
        }
        
        // 检查无障碍服务是否在运行
        val service = WeChatAccessibilityService.getInstance()
        if (service == null && appConfig.keepAlive) {
            Log.d(TAG, "Attempting to ensure accessibility service is running")
            
            // 可以在这里添加提醒用户启用服务的逻辑，如发送一个广播
            // 但不能直接启动无障碍服务，这需要用户手动在设置中开启
        }
    }
} 