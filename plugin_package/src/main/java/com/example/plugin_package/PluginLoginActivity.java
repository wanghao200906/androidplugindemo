package com.example.plugin_package;

import android.os.Bundle;
import android.widget.Toast;

public class PluginLoginActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plugin_login);
        Toast.makeText(this, getString(R.string.app_name), Toast.LENGTH_LONG).show();
    }
}
