package com.example.wechatclicker

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.text.method.LinkMovementMethod
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.google.android.material.appbar.MaterialToolbar
import com.example.wechatclicker.model.AppConfig
import java.util.Locale
import android.text.Html
import android.os.Build

class PrivacyPolicyActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_privacy_policy)
        
        // 启用边缘到边缘显示
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // 设置工具栏
        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.privacy_policy)

        // 设置隐私政策文本
        val textView = findViewById<TextView>(R.id.privacyPolicyText)
        textView.movementMethod = LinkMovementMethod.getInstance()
        
        val appConfig = AppConfig(this)
        val currentLanguage = when (appConfig.language) {
            "en" -> "en"
            "zh" -> "zh"
            else -> Locale.getDefault().language // 如果是 "system"，则使用系统语言
        }
        
        val rawText = if (currentLanguage == "en") {
            getString(R.string.privacy_policy_text_en)
        } else {
            getString(R.string.privacy_policy_text)
        }

        // 处理文本格式
        val formattedText = rawText.replace("\n", "<br>")
            .replace("•", "&#8226;") // 使用 HTML 实体来表示项目符号
        
        // 使用 Html.fromHtml 来设置格式化的文本
        textView.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(formattedText, Html.FROM_HTML_MODE_LEGACY)
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(formattedText)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
} 