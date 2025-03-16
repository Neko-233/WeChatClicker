## 简介

微信消息助手是一款Android应用，旨在帮助用户自动处理微信消息通知。它可以自动点击未读消息，查看后自动返回，大大提高处理消息的效率，特别适合需要管理多个聊天或群组的用户。

## 主要功能

- **主功能开关**：轻松控制整个应用的功能，无需频繁开关无障碍服务
- **自动点击**：自动点击未读消息，无需手动操作
- **自动返回**：查看消息后自动返回微信主界面，便于处理下一条消息
- **保持服务活跃**：确保服务在后台持续运行，不会被系统清理
- **统计功能**：记录已处理的消息数量和最后检查时间

## 使用方法

1. 安装应用
2. 点击主界面上的"启用辅助功能服务"按钮，进入系统无障碍设置
3. 在列表中找到"微信消息助手"并启用
4. 返回应用，确认"无障碍服务状态"已显示为"已启动"
5. 使用主功能开关来控制应用的运行
6. 根据需要配置自动点击、自动返回和保持服务活跃等选项

## 所需权限

- **无障碍服务权限**：用于监听和操作微信界面
- **前台服务权限**：用于保持服务在后台运行
- **通知权限**：用于显示服务状态通知
- **开机自启动权限**：可选，用于设备重启后自动启动服务

## 技术说明

- 基于Android无障碍服务(AccessibilityService)开发
- 使用协程(Coroutines)进行异步任务处理
- 实现了Watchdog机制确保服务持续运行
- 使用SharedPreferences存储用户配置和统计数据
- 适配Android 13及以上版本的广播接收器权限管理

## 注意事项

- 本应用需要无障碍服务权限才能正常工作
- 应用性能可能受到系统优化和厂商定制ROM的影响
- 由于微信界面可能随版本更新而变化，应用识别界面的能力可能需要更新
- 为了保证更好的使用体验，建议将应用加入系统的电池优化白名单
- 本应用仅用于提高消息处理效率，请勿用于其他用途

## 隐私声明

本应用不会收集或上传任何个人数据。所有数据均存储在本地设备上，仅用于应用功能实现。

---

# WeChat Message Assistant

## Introduction

WeChat Message Assistant is an Android application designed to help users automatically process WeChat message notifications. It can automatically click unread messages, return to the main interface after viewing, greatly improving message processing efficiency, especially suitable for users who need to manage multiple chats or groups.

## Key Features

- **Main Switch**: Easily control the entire application without frequently toggling accessibility services
- **Auto-Click**: Automatically click on unread messages without manual operation
- **Auto-Return**: Automatically return to WeChat main interface after viewing messages
- **Keep Service Active**: Ensure the service keeps running in the background
- **Statistics**: Record the number of processed messages and last check time

## How to Use

1. Install the application
2. Click the "Enable Accessibility Service" button to enter system accessibility settings
3. Find "WeChat Message Assistant" in the list and enable it
4. Return to the app and confirm that "Accessibility Service Status" shows "Enabled"
5. Use the main switch to control the application
6. Configure auto-click, auto-return, and keep service active options as needed

## Required Permissions

- **Accessibility Service**: For monitoring and operating the WeChat interface
- **Foreground Service**: For keeping the service running in the background
- **Notification**: For displaying service status notifications
- **Boot Completed**: Optional, for starting the service automatically after device restart

## Technical Information

- Developed based on Android Accessibility Service
- Uses Coroutines for asynchronous task processing
- Implements a Watchdog mechanism to ensure continuous service operation
- Uses SharedPreferences to store user configurations and statistics
- Adapts to Android 13+ broadcast receiver permission management

## Notes

- This application requires accessibility service permission to work properly
- Application performance may be affected by system optimizations and manufacturer customized ROMs
- As WeChat interface may change with version updates, the application's ability to recognize interfaces may need updates
- For better experience, it is recommended to add the application to the battery optimization whitelist
- This application is only for improving message processing efficiency, please do not use it for other purposes

## Privacy Statement

This application does not collect or upload any personal data. All data is stored on the local device and is only used for application functionality.
