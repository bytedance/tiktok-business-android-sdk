package com.example;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.tiktok.TTProperty;
import com.tiktok.TiktokBusinessSdk;
import com.tiktok.TiktokBusinessSdk.TTConfig;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Tiktok sdk init start
        TTConfig ttConfig = new TTConfig(getApplication())
                .setAppKey("thisisanappkey")
                .enableDebug();
        TiktokBusinessSdk.initialize(ttConfig);

        TiktokBusinessSdk.with(this).track("ViewContent");
    }
}
