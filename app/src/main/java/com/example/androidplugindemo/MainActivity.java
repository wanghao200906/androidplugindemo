package com.example.androidplugindemo;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import static com.example.androidplugindemo.Parameter.PLUGIN;

/**
 * Time: 2019-08-10
 * Author: Liudeli
 * Description: 宿主主Activity
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 123);
        }
    }

    /**
     * 宿主启动宿主中的LoginActivity
     *
     * @param view
     */
    public void startMainLoginActivity(View view) {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra(PLUGIN, true);
        startActivity(intent);
    }

    /**
     * 宿主启动[插件]中的PluginLoginActivity
     *
     * @param view
     */
    public void startPluginLoginActivity(View view) {
        Intent intent = new Intent();
        intent.putExtra(PLUGIN, true);
        intent.setComponent(new ComponentName("com.example.plugin_package", "com.example.plugin_package.PluginLoginActivity"));
        startActivity(intent);
    }

}
