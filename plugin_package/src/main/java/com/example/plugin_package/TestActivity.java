package com.example.plugin_package;

import android.os.Bundle;

import androidx.annotation.Nullable;

public class TestActivity extends BaseActivity {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_test);
    }
}
