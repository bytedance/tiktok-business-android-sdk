/*******************************************************************************
 * Copyright (c) 2020. Bytedance Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.tiktok.appevents;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class TTTaskUtils {
    private static String TAG = TTTaskUtils.class.getCanonicalName();
    private static ExecutorService thread = Executors.newSingleThreadExecutor(new TTThreadFactory());

    // runs an async method in a synchronous fashion
    public static <T> T runTaskSync(Callable<T> runnable) {
        Future<T> f = thread.submit(runnable);
        try {
            return f.get();
        } catch (Exception e) {
            TTCrashHandler.handleCrash(TAG, e);
        }
        return null;
    }

}
