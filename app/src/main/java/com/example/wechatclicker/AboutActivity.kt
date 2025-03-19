package com.example.wechatclicker

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton

class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        
        // 设置工具栏
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        // 显示版本信息
        val versionText = findViewById<TextView>(R.id.versionText)
        try {
            // 获取应用版本信息
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val versionName = packageInfo.versionName
            
            // 只显示版本名称
            versionText.text = getString(R.string.version_format, versionName)
        } catch (e: PackageManager.NameNotFoundException) {
            versionText.text = getString(R.string.unknown_version)
        }
        
        // 设置隐私政策按钮点击事件
        val privacyPolicyButton = findViewById<MaterialButton>(R.id.privacyPolicyButton)
        privacyPolicyButton.setOnClickListener {
            startActivity(Intent(this, PrivacyPolicyActivity::class.java))
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