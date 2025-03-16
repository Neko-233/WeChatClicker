package com.example.wechatclicker.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.wechatclicker.model.AppConfig
import com.example.wechatclicker.service.WeChatAccessibilityService

/**
 * 用于监听微信启动事件的广播接收器
 */
class PackageMonitorReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "PackageMonitorReceiver"
        private const val WECHAT_PACKAGE = "com.tencent.mm"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        val packageName = intent.data?.schemeSpecificPart
        val action = intent.action
        
        Log.d(TAG, "Received action: $action for package: $packageName")
        
        if (action == Intent.ACTION_PACKAGE_ADDED || 
            action == Intent.ACTION_PACKAGE_REPLACED ||
            action == Intent.ACTION_PACKAGE_RESTARTED) {
            // 微信的安装、更新或重启
            if (packageName == WECHAT_PACKAGE) {
                Log.d(TAG, "WeChat app was added/replaced/restarted")
                checkAndStartService(context)
            }
        }
    }
    
    private fun checkAndStartService(context: Context) {
        val appConfig = AppConfig(context)
        if (appConfig.keepAlive) {
            // 确保微信无障碍服务处于活跃状态
            if (!WeChatAccessibilityService.isRunning) {
                Log.d(TAG, "Attempting to ensure accessibility service is running")
                // 显示通知提醒用户启用无障碍服务
                WeChatAccessibilityService.showEnableServiceNotification(context)
            }
        }
    }
} 