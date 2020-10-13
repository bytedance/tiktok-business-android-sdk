package com.example;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import android.view.Menu;

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

import com.tiktok.TiktokBusinessSdk;
import com.tiktok.TiktokBusinessSdk.TTConfig;
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
                R.id.nav_home, R.id.nav_events, R.id.nav_eventlog)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        if(savedInstanceState == null) {
            // Tiktok sdk init start
            TTConfig ttConfig = new TTConfig(getApplication())
//                .optOutAdvertiserIDCollection()
//                .optOutAutoEventTracking()
                    .enableDebug();
            TiktokBusinessSdk.initializeSdk(ttConfig);
            // Tiktok sdk init end

            // testing delay tracking, implementing a 60 sec delay manually
            // ideally has to be after accepting tracking permission
            new Handler(Looper.getMainLooper()).postDelayed(TiktokBusinessSdk::startTracking, 1000);
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
                        count ++;
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