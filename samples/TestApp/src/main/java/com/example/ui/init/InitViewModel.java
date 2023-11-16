/*******************************************************************************
 * Copyright (c) 2020. Bytedance Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.example.ui.init;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;

public class InitViewModel extends AndroidViewModel {


    boolean enableAutoIapTrack;

    public InitViewModel(Application application) {
        super(application);
    }

    public void switchEnableAutoIapTrack() {
        enableAutoIapTrack = !enableAutoIapTrack;
    }

    public boolean isEnableAutoIapTrack() {
        return enableAutoIapTrack;
    }

}