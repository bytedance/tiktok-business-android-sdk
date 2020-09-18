package com.example;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.tiktok.TiktokBusinessSdk;
import com.tiktok.TiktokBusinessSdk.TTConfig;
import com.tiktok.appevents.TTProperty;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Tiktok sdk init start
        TTConfig ttConfig = new TTConfig(getApplication())
                .setAppKey("thisisanappkey")
                .enableDebug();
        TiktokBusinessSdk.startTracking(ttConfig);

        // track with no custom property
        TiktokBusinessSdk.with(this).track("ViewContent");
        // track with custom call
        TiktokBusinessSdk.with(this).track(
                "ViewContent",
                new TTProperty().put("activity", this.getLocalClassName()));
    }
}
