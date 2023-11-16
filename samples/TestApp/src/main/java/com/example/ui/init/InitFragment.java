/*******************************************************************************
 * Copyright (c) 2020. Bytedance Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.example.ui.init;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.R;
import com.example.ui.home.HomeViewModel;
import com.tiktok.TikTokBusinessSdk;

public class InitFragment extends Fragment {

    private InitViewModel initViewModel;
    private EditText appId;
    private EditText ttAppId;
    private Button enableAutoIapTrack;
    private Button init;
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        initViewModel = new ViewModelProvider(this).get(InitViewModel.class);
        View root = inflater.inflate(R.layout.fragment_init, container, false);
        appId = root.findViewById(R.id.app_id);
        ttAppId = root.findViewById(R.id.tt_app_id);
        enableAutoIapTrack = root.findViewById(R.id.enableAutoIapTrack);
        init = root.findViewById(R.id.init);
        enableAutoIapTrack.setOnClickListener(view -> switchEnableAutoIapTrack());
        init.setOnClickListener(v -> {
            HomeViewModel homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);

            if (savedInstanceState == null) {
                // !!!!!!!!!!!!!!!!!!!!!!!!!
                // in order for this app to be runnable, plz create a resource file containing the relevant string resources
                // Tiktok sdk init start

                TikTokBusinessSdk.TTConfig ttConfig = new TikTokBusinessSdk.TTConfig(getActivity().getApplicationContext())
                        .disableAutoStart()
                        .disableMonitor()
                        .setLogLevel(TikTokBusinessSdk.LogLevel.DEBUG);
                if(initViewModel.isEnableAutoIapTrack()){
                    ttConfig.enableAutoIapTrack();
                }
                if(appId.getText().toString() == null || appId.getText().toString().isEmpty()){
                    ttConfig.setAppId("com.bytedance.iabtest");
                } else {
                    ttConfig.setAppId(appId.getText().toString());
                }
                if(ttAppId.getText().toString() == null || ttAppId.getText().toString().isEmpty()){
                    ttConfig.setTTAppId("123456");
                } else {
                    ttConfig.setTTAppId(ttAppId.getText().toString());
                }
                TikTokBusinessSdk.initializeSdk(ttConfig);

                // check if user info is cached & init
                homeViewModel.checkInitTTAM();

                TikTokBusinessSdk.setOnCrashListener((thread, ex) -> android.util.Log.i("TikTokBusinessSdk", "setOnCrashListener" + thread.getName(), ex));

                // testing delay tracking, implementing a 6 sec delay manually
                // ideally has to be after accepting tracking permission
                new Handler(Looper.getMainLooper()).postDelayed(TikTokBusinessSdk::startTrack, 10000);
            }
        });
        return root;
    }

    private void switchEnableAutoIapTrack() {
        initViewModel.switchEnableAutoIapTrack();
        if (initViewModel.isEnableAutoIapTrack()) {
            Toast.makeText(requireContext(), "open auto iap track", Toast.LENGTH_SHORT).show();
            enableAutoIapTrack.setText(R.string.disabled_auto_iap_track);
        } else {
            Toast.makeText(requireContext(), "close auto iap track", Toast.LENGTH_SHORT).show();
            enableAutoIapTrack.setText(R.string.enable_auto_iap_track);
        }
    }

}