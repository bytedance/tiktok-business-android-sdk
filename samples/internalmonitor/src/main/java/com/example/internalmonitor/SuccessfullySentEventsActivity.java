package com.example.internalmonitor;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

// this activity shows all the activities that are sent successfully to api
public class SuccessfullySentEventsActivity extends AppCompatActivity {
    RecyclerView appEventList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_successfully_sent_events);

        appEventList = findViewById(R.id.appEventList);
        appEventList.setAdapter(new AppEventCycleAdapter(this));
        appEventList.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    protected void onResume() {
        super.onResume();
        appEventList.getAdapter().notifyDataSetChanged();
    }
}