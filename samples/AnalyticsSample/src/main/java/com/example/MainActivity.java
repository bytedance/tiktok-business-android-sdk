package com.example;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.tiktok.TiktokSdk;
import com.tiktok.TiktokSdk.TTConfig;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getCanonicalName();

    private JSONObject properties;
    private TextView propsTV;
    private TextView eventTV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setCustomView(R.layout.actionbar_layout);

        propsTV = (TextView) findViewById(R.id.propsPrettyViewer);
        eventTV = (TextView) findViewById(R.id.eventName);
        properties = new JSONObject();
        ImageButton savedEventsBtn = (ImageButton) findViewById(R.id.savedEventsBtn);

        propsTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getBaseContext(), PropEditor.class);
                Bundle bundlePros = new Bundle();
                Iterator<String> keys = properties.keys();
                while(keys.hasNext()) {
                    String key = keys.next();
                    try {
                        if (properties.get(key) instanceof String) {
                            bundlePros.putString(key, properties.get(key).toString());
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                intent.putExtras(bundlePros);
                startActivityForResult(intent, 2);
            }
        });
        updatePropsTV();

        savedEventsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String[] events = TestEvents.getEvents();
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Select an event");
                builder.setItems(events, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int selected) {
                        properties = new JSONObject();
                        TestEvents.TTEventType ttEventType = TestEvents.TTEventType.valueOf(events[selected]);
                        eventTV.setText(ttEventType.getEventType());
                        for (TestEvents.TTProperty ttProperty : TestEvents.TTEventProperties.get(ttEventType)) {
                            try {
                                properties.put(ttProperty.getProperty(), "");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        updatePropsTV();
                    }
                });
                builder.show();
            }
        });

        // Tiktok sdk init start
        TTConfig ttConfig = new TTConfig(getApplicationContext())
                .optOutAutoEventTracking()
                .optOutAdvertiserIDCollection()
                .enableDebug();
        TiktokSdk.initialize(ttConfig);
        // Tiktok sdk init end
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == 2) {
            assert data != null;
            Bundle bundle = data.getExtras();
            properties = new JSONObject();
            if (bundle != null) {
                for (String key : bundle.keySet()) {
                    if (bundle.get(key) != null) {
                        try {
                            properties.put(key, bundle.get(key));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
                updatePropsTV();
            }
        }
    }

    private void updatePropsTV() {
        try {
            propsTV.setText(properties.toString(4));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

}
