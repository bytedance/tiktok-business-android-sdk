package com.tiktok.appevents;

import com.tiktok.TikTokBusinessSdk;
import com.tiktok.util.TTUtil;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.List;

import static org.junit.Assert.*;

import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({TTUtil.class, TikTokBusinessSdk.class})
public class TTAppEventsQueueTest extends BaseTest {


    @Test
    public void simpleCase() {
        TTAppEventsQueue.addEvent(new TTAppEvent("InternalTest", "{}"));
        TTAppEventsQueue.addEvent(new TTAppEvent("InternalTest", "{}"));

        assertEquals(2, TTAppEventsQueue.size());
        TTAppEventsQueue.clearAll();
        assertEquals(0, TTAppEventsQueue.size());
    }

    @Test
    public void notifyListener() throws Exception {
        PowerMockito.mockStatic(TikTokBusinessSdk.class);
        PowerMockito.doCallRealMethod().when(TikTokBusinessSdk.class, "destroy");
        PowerMockito.doCallRealMethod().when(TikTokBusinessSdk.class, "setUpSdkListeners", any(), any(), any(), any());
        TikTokBusinessSdk.setUpSdkListeners(new TikTokBusinessSdk.MemoryListener() {
            @Override
            public void onMemoryChange(int size) {
                assertEquals(1, size);
            }
        }, null, null, new TikTokBusinessSdk.NextTimeFlushListener() {
            @Override
            public void timeLeft(int timeLeft) {
            }

            @Override
            public void thresholdLeft(int threshold, int left) {
                assertEquals(TTAppEventLogger.THRESHOLD, threshold);
                assertEquals(TTAppEventLogger.THRESHOLD - 1, left);

            }
        });
        TTAppEventsQueue.addEvent(new TTAppEvent("InternalTest", "{}"));
        TikTokBusinessSdk.destroy();
        TTAppEventsQueue.clearAll();
    }

    @Test
    public void testExport() {
        TTAppEvent e1 = new TTAppEvent("InternalTest", "{}");
        TTAppEvent e2 = new TTAppEvent("InternalTest", "{}");
        TTAppEventsQueue.addEvent(e1);
        TTAppEventsQueue.addEvent(e2);


        assertEquals(2, TTAppEventsQueue.size());
        List<TTAppEvent> exported = TTAppEventsQueue.exportAllEvents();

        assertEquals(2, exported.size());

        assertEquals(e1, exported.get(0));
        assertEquals(e2, exported.get(1));
        assertEquals(0, TTAppEventsQueue.size());

    }
}
