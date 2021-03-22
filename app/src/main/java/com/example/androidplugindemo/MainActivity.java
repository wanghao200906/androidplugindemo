package com.example.androidplugindemo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.lang.reflect.Proxy;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},123);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    // 加载插件
    public void loadPlugin(View view) {
        PluginManager.getInstance(this).loadPlugin();
    }

    // 启动插件里面的Activity
    public void startPluginActivity(View view) {
        File file = new File(Environment.getExternalStorageDirectory() + File.separator + "p.apk");
        String path = file.getAbsolutePath();

        // 获取插件包 里面的 Activity
        PackageManager packageManager = getPackageManager();
        PackageInfo packageInfo = packageManager.getPackageArchiveInfo(path, PackageManager.GET_ACTIVITIES);
        ActivityInfo activityInfo = packageInfo.activities[0];// 这里是0就是com.example.plugin_package.PluginActivity，如果多了的话那就得遍历了。

        // 占位  代理Activity
        Intent intent = new Intent(this, ProxyActivity.class);
        intent.putExtra("className", activityInfo.name);
        startActivity(intent);
    }

    // 注册 插件里面 配置的 静态广播接收者
    public void loadStaticReceiver(View view) {
        PluginManager.getInstance(this).parserApkAction();
    }

    // 发送静态广播
    public void sendStaticReceiver(View view) {
        Intent intent = new Intent();
        intent.setAction("plugin.static_receiver");
        sendBroadcast(intent);
    }
}
