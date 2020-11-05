/*******************************************************************************
 * Copyright (c) 2020. Bytedance Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.example.internalmonitor;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.tiktok.TikTokBusinessSdk;
import com.tiktok.appevents.TTPurchaseItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class MonitorActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!TikTokBusinessSdk.isInitialized()) {
            TikTokBusinessSdk.TTConfig ttConfig =
                    new TikTokBusinessSdk.TTConfig(getApplication())
                            .setLogLevel(TikTokBusinessSdk.LogLevel.INFO);
            TikTokBusinessSdk.initializeSdk(ttConfig);
        }

        SDKEventHandler handler = new SDKEventHandler(this);
        // TODO, use ViewModel to keep activity state
        setContentView(R.layout.activity_monitor);
        handler = new SDKEventHandler(this);
        TTSDKMonitor monitor = new TTSDKMonitor(handler);
        TikTokBusinessSdk.setUpSdkListeners(monitor, monitor, monitor, monitor);
        setUpHandlers();
    }

    private void sendEvent() {
        JSONObject property = new JSONObject();
        JSONObject inner = new JSONObject();
        JSONArray array = new JSONArray();
        try {
            array.put(0, new JSONObject().put("a", 1));
            array.put(1, new JSONObject().put("b", 1));
            JSONObject obj = new JSONObject();

            obj.put("a", 1)
                    .put("b", new JSONObject().put("c", 1));
            inner.put("attr1", "someValue");
            inner.put("time", new Date());
            property.put("code", "123")
                    .put("data", inner)
                    .put("array", array)
                    .put("object", obj);
            TikTokBusinessSdk.trackEvent("InternalTest", property);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void sendPurchaseEvent(View view) {

        TTPurchaseItem item1 = new TTPurchaseItem(23.5f, 2, "a", "a");
        TTPurchaseItem item2 = new TTPurchaseItem(10.5f, 1, "b", "b");

        try {
            TikTokBusinessSdk.trackEvent("Purchase", TTPurchaseItem.buildPurchaseProperties("dollar", item1, item2));
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
            TikTokBusinessSdk.flush();
        });

        Button resetBtn = findViewById(R.id.reset);
        resetBtn.setOnClickListener(v -> {
            Toast.makeText(MonitorActivity.this, "Please restart app", Toast.LENGTH_SHORT).show();
            TikTokBusinessSdk.clearAll();
            MonitorActivity.this.finish();
            System.exit(0);
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