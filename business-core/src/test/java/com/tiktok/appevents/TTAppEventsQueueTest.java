package com.tiktok.appevents;

import com.tiktok.TiktokBusinessSdk;
import com.tiktok.util.TTUtil;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.*;

import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({TTUtil.class})
public class TTAppEventsQueueTest extends BaseTest implements TiktokBusinessSdk.MemoryListener, TiktokBusinessSdk.NextTimeFlushListener {

    @Test
    public void simpleCase() {
        TTAppEventsQueue.addEvent(new TTAppEvent("aaa", "{}"));
        TTAppEventsQueue.addEvent(new TTAppEvent("bbb", "{}"));

        assertEquals(2, TTAppEventsQueue.size());
//        TTAppEventsQueue.clearAll();
//        assertEquals(0, TTAppEventsQueue.size());
    }

    @Test
    public void notifyListener() {
        TiktokBusinessSdk.setUpSdkListeners(this, null, null, this);
//        System.out.println(TiktokBusinessSdk);
    }

    @Override
    public void onMemoryChange(int size) {

    }

    @Override
    public void timeLeft(int timeLeft) {
    }

    @Override
    public void thresholdLeft(int threshold, int left) {

    }
}
