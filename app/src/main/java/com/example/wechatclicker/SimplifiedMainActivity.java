package com.example.wechatclicker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SimplifiedMainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 创建一个线性布局
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(30, 30, 30, 30);
        
        // 使用TextView显示简单消息
        TextView tv = new TextView(this);
        tv.setText("微信消息助手已启动！");
        tv.setPadding(0, 0, 0, 30);
        tv.setTextSize(18);
        
        // 创建一个按钮跳转到主界面
        Button btn = new Button(this);
        btn.setText("打开主界面");
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SimplifiedMainActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
        
        // 将TextView和Button添加到布局中
        layout.addView(tv);
        layout.addView(btn);
        
        // 设置内容视图为布局
        setContentView(layout);
    }
}
