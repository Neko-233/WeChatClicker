package com.example.wechatclicker

import android.os.Bundle
import android.text.Html
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

class PrivacyPolicyActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var privacyPolicyText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.privacy_policy)

        // 初始化工具栏
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // 设置隐私政策文本
        privacyPolicyText = findViewById(R.id.privacyPolicyText)
        privacyPolicyText.text = Html.fromHtml(
            getString(R.string.privacy_policy_text), 
            Html.FROM_HTML_MODE_COMPACT
        )

        // 设置返回按钮点击事件
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }
} 