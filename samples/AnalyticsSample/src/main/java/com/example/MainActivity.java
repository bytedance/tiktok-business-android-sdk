package com.example;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.tiktok.TiktokSdk;
import com.tiktok.TiktokSdk.TTConfig;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Tiktok sdk init start
        TTConfig ttConfig = new TTConfig(getApplicationContext())
                .optOutAutoEventTracking()
                .optOutAdvertiserIDCollection()
                .enableDebug();
        TiktokSdk.initialize(ttConfig);
        // Tiktok sdk init end
    }
}
