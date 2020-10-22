package com.example.internalmonitor;

import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.tiktok.TiktokBusinessSdk;
import com.tiktok.appevents.TTAppEventLogger;
import com.tiktok.appevents.TTCrashHandler;
import com.tiktok.appevents.TTProperty;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class MonitorActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!TiktokBusinessSdk.isInitialized()) {
            TiktokBusinessSdk.TTConfig ttConfig = new TiktokBusinessSdk.TTConfig(getApplication())
                    .enableDebug();
            TiktokBusinessSdk.initializeSdk(ttConfig);
        }

        SDKEventHandler handler = new SDKEventHandler(this);
        // TODO, use ViewModel to keep activity state
        setContentView(R.layout.activity_monitor);
        handler = new SDKEventHandler(this);
        TTSDKMonitor monitor = new TTSDKMonitor(handler);
        TiktokBusinessSdk.setUpSdkListeners(monitor, monitor, monitor, monitor);
        setUpHandlers();
    }

    private void sendEvent() {
        TTProperty property = new TTProperty();
        JSONObject inner = new JSONObject();
        try {
            inner.put("attr1", "someValue");
            inner.put("time", new Date());
            property.put("code", "123")
                    .put("data", inner);
            TiktokBusinessSdk.trackEvent("testEvent", property);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void setUpHandlers() {
        TextView view = findViewById(R.id.count);
        Button track = findViewById(R.id.track);
        track.setOnClickListener(v -> {
            String countText = view.getText().toString().trim();
            if (countText.length() == 0) {
                Toast.makeText(this, "Track count is empty", Toast.LENGTH_SHORT).show();
                return;
            }
            int count = Integer.parseInt(countText);

            for (int i = 0; i < count; i++) {
                sendEvent();
            }
        });

        Button flushBtn = findViewById(R.id.flush);
        flushBtn.setOnClickListener(v -> {
            TiktokBusinessSdk.flush();
        });

        Button resetBtn = findViewById(R.id.reset);
        resetBtn.setOnClickListener(v -> {
            Toast.makeText(MonitorActivity.this, "Please restart app", Toast.LENGTH_SHORT).show();
            TiktokBusinessSdk.clearAll();
            MonitorActivity.this.finish();
            System.exit(0);
        });

        // make sure our code does not block main thread
        Button counter = findViewById(R.id.counter);
        counter.setOnClickListener(v -> {
            int curr = Integer.parseInt(counter.getText().toString());
            counter.setText((curr + 1) + "");
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.sent_events) {
            Intent intent = new Intent(this, SuccessfullySentEventsActivity.class);
            startActivity(intent);
        }
        return true;
    }

}