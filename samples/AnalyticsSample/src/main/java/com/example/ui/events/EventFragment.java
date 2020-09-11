package com.example.ui.events;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.R;
import com.example.model.EventLog;
import com.example.testdata.TestEventType;
import com.example.testdata.TestEvents;
import com.example.testdata.TestProperty;
import com.example.ui.eventlog.EventLogViewModel;

import org.json.JSONException;

import java.util.Date;
import java.util.Iterator;
import java.util.Objects;

public class EventFragment extends Fragment {

    private EventViewModel eventViewModel;
    private EventLogViewModel eventLogViewModel;

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        eventViewModel = new ViewModelProvider(this).get(EventViewModel.class);
        eventLogViewModel = new ViewModelProvider(this).get(EventLogViewModel.class);

        View root = inflater.inflate(R.layout.fragment_events, container, false);

        final TextView propsTV = root.findViewById(R.id.propsPrettyViewer);
        eventViewModel.getLiveProperties().observe(getViewLifecycleOwner(), s -> {
            try {
                assert s != null;
                propsTV.setText(s.toString(4));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });

        final TextView eventTV = root.findViewById(R.id.eventName);
        eventViewModel.getLiveEventName().observe(getViewLifecycleOwner(), s -> eventTV.setText(s));

        Button resetBtn = root.findViewById(R.id.resetBtn);
        resetBtn.setOnClickListener(view -> {
            eventViewModel.resetEvent();
            eventViewModel.resetProps();
        });

        propsTV.setOnClickListener(view -> {
            Intent intent = new Intent(requireContext(), PropEditActivity.class);
            Bundle bundlePros = new Bundle();
            Iterator<String> keys = eventViewModel.getPropIterator();
            while(keys.hasNext()) {
                String key = keys.next();
                try {
                    bundlePros.putString(key, eventViewModel.getProp(key));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            intent.putExtras(bundlePros);
            startActivityForResult(intent, 2);
        });

        ImageButton savedEventsBtn = root.findViewById(R.id.savedEventsBtn);
        savedEventsBtn.setOnClickListener(v -> {
            final String[] events = TestEvents.getAllEvents();
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setTitle("Select an event");
            builder.setItems(events, (dialog, selected) -> {
                eventViewModel.resetProps();
                TestEventType testEventType = TestEventType.valueOf(events[selected]);
                eventViewModel.setEvent(testEventType.getEventType());
                for (TestProperty ttProperty : Objects.requireNonNull(TestEvents.TTEventProperties.get(testEventType))) {
                    try {
                        eventViewModel.addProp(ttProperty.getProperty(), "");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
            builder.show();
        });

        Button sendBtn = root.findViewById(R.id.sendBtn);
        sendBtn.setOnClickListener(view -> {
            assert eventViewModel.getLiveEventName().getValue() != null;
            eventLogViewModel.save(new EventLog(
                    eventViewModel.getLiveEventName().getValue(),
                    eventViewModel.getLiveProperties().getValue().toString()
            ));
        });

        return root;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == 2) {
            assert data != null;
            Bundle bundle = data.getExtras();
            eventViewModel.resetProps();
            if (bundle != null) {
                for (String key : bundle.keySet()) {
                    if (bundle.get(key) != null) {
                        try {
                            eventViewModel.addProp(key, bundle.get(key));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

}