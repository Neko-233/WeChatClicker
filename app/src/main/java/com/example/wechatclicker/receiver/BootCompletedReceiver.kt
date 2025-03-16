package com.example.wechatclicker.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.wechatclicker.model.AppConfig
import com.example.wechatclicker.service.ForegroundKeepAliveService

/**
 * 监听设备启动完成事件的广播接收器，用于在设备启动后自动启动服务
 */
class BootCompletedReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "BootCompletedReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "Received action: $action")
        
        if (action == Intent.ACTION_BOOT_COMPLETED || 
            action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            
            // 检查用户是否启用了保持服务活跃的选项
            val appConfig = AppConfig(context)
            if (appConfig.keepAlive) {
                Log.d(TAG, "Starting service after boot/update")
                // 启动前台服务
                ForegroundKeepAliveService.startService(context)
            }
        }
    }
} 