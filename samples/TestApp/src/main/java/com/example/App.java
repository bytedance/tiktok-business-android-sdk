/*******************************************************************************
 * Copyright (c) 2020. Bytedance Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.example;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;

import com.example.model.EventLog;
import com.example.persistence.EventLogRepo;
import com.example.testdata.TestEvents;
import com.google.android.material.navigation.NavigationView;

import androidx.annotation.NonNull;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.tiktok.TikTokBusinessSdk;
import com.tiktok.TikTokBusinessSdk.TTConfig;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Objects;

public class App extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_billing, R.id.nav_events, R.id.nav_eventlog)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        if (savedInstanceState == null) {
            // !!!!!!!!!!!!!!!!!!!!!!!!!
            // in order for this app to be runnable, plz create a resource file containing the relevant string resources
            String appId = this.getResources().getString(R.string.tiktok_business_app_id);
            String ttAppId = this.getResources().getString(R.string.tiktok_tt_app_id);
            String accessToken = this.getResources().getString(R.string.tiktok_business_app_access_token);
            // Tiktok sdk init start

            long beforeStartTT = System.currentTimeMillis();
            TTConfig ttConfig = new TTConfig(getApplication())
                    .setAppId(appId)
                    .setTTAppId(ttAppId)
                    .setAccessToken(accessToken)
                    .disableAutoStart()
                    .setLogLevel(TikTokBusinessSdk.LogLevel.DEBUG);
            TikTokBusinessSdk.initializeSdk(ttConfig);
            long afterStartTT = System.currentTimeMillis();
            android.util.Log.i("TikTokBusinessSdk", " Init time in ms " + (afterStartTT-beforeStartTT));

            // testing delay tracking, implementing a 60 sec delay manually
            // ideally has to be after accepting tracking permission
            new Handler(Looper.getMainLooper()).postDelayed(TikTokBusinessSdk::startTrack, 6000);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.app, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        EventLogRepo eventLogRepo = new EventLogRepo(getApplication());
        switch (item.getItemId()) {
            case R.id.action_clearlog:
                eventLogRepo.clear();
                break;
            case R.id.action_preload:
                for (String event : TestEvents.getAllEvents()) {
                    JSONObject props = new JSONObject();
                    try {
                        for (String prop : Objects.requireNonNull(TestEvents.TTEventProperties.get(event))) {
                            props.put(prop, "");
                        }
                        eventLogRepo.save(new EventLog(event, props.toString()));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case R.id.action_loadtest:
                int MAX = 1000;
                int count = 0;
                List<EventLog> logs = null;
                try {
                    logs = eventLogRepo.getLogs();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                while (count < MAX && logs.size() > 0) {
                    for (EventLog log : Objects.requireNonNull(logs)) {
                        eventLogRepo.save(new EventLog(log.eventType, log.properties));
                        count++;
                        if (count >= MAX) break;
                    }
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}
