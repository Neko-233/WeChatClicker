package com.example.wechatclicker

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.button.MaterialButton
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.concurrent.Executors

class LogViewerActivity : AppCompatActivity() {
    
    private lateinit var toolbar: Toolbar
    private lateinit var logContentText: TextView
    private lateinit var refreshButton: MaterialButton
    private lateinit var clearButton: MaterialButton
    
    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_viewer)
        
        // 初始化视图
        toolbar = findViewById(R.id.toolbar)
        logContentText = findViewById(R.id.logContentText)
        refreshButton = findViewById(R.id.refreshButton)
        clearButton = findViewById(R.id.clearButton)
        
        // 设置工具栏
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        
        // 设置按钮点击事件
        refreshButton.setOnClickListener {
            loadLogcat()
        }
        
        clearButton.setOnClickListener {
            clearLogcat()
        }
        
        // 设置返回按钮点击事件
        toolbar.setNavigationOnClickListener {
            finish()
        }
        
        // 加载日志
        loadLogcat()
    }
    
    private fun loadLogcat() {
        logContentText.text = "正在加载日志..."
        
        executor.execute {
            try {
                val process = Runtime.getRuntime().exec("logcat -d -v threadtime")
                val bufferedReader = BufferedReader(InputStreamReader(process.inputStream))
                val log = StringBuilder()
                var line: String?
                
                while (bufferedReader.readLine().also { line = it } != null) {
                    if (line?.contains(packageName) == true || 
                        line?.contains("WeChatAccessibilityService") == true) {
                        log.append(line).append("\n")
                    }
                }
                
                handler.post {
                    if (log.isEmpty()) {
                        logContentText.text = "未找到相关日志"
                    } else {
                        logContentText.text = log.toString()
                    }
                }
            } catch (e: IOException) {
                handler.post {
                    logContentText.text = "加载日志失败: ${e.message}"
                }
            }
        }
    }
    
    private fun clearLogcat() {
        executor.execute {
            try {
                Runtime.getRuntime().exec("logcat -c")
                handler.post {
                    logContentText.text = "日志已清除"
                    Toast.makeText(this, "日志已清除", Toast.LENGTH_SHORT).show()
                }
            } catch (e: IOException) {
                handler.post {
                    Toast.makeText(this, "清除日志失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }
} 