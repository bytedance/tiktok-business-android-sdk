/*******************************************************************************
 * Copyright (c) 2020. Bytedance Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.tiktok.appevents;

import java.util.concurrent.ThreadFactory;

public class TTThreadFactory implements ThreadFactory {
    @Override
    public Thread newThread(Runnable r) {
        final Thread t = new Thread(r);
        t.setUncaughtExceptionHandler(new TTUncaughtExceptionHandler());
        return t;
    }
}
