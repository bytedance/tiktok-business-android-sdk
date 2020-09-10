package com.example.appeventsample;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Application;
import android.os.Bundle;
import android.view.View;

import com.tiktok.TiktokSdk;
import com.tiktok.model.TTAppEvent;
import com.tiktok.appevents.TTAppEventsManager;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TiktokSdk.using((Application) this.getApplicationContext());
    }

    static int count = 0;

    public void genEvent(View view) throws JSONException {
        count += 1;
        JSONObject obj = new JSONObject();
        obj.put("name", "obj" + count);
        JSONObject nested = new JSONObject();
        nested.put("nestedName", "nested" + count);
        obj.put("nestedObj", nested);
        TTAppEvent event = new TTAppEvent("event" + count, obj.toString());
        TTAppEventsManager.logEvent(event);
    }
}
