package com.tiktok.appevents;

import com.tiktok.TiktokBusinessSdk;
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
@PrepareForTest({TTUtil.class, TiktokBusinessSdk.class})
public class TTAppEventsQueueTest extends BaseTest {


    @Test
    public void simpleCase() {
        TTAppEventsQueue.addEvent(new TTAppEvent("aaa", "{}"));
        TTAppEventsQueue.addEvent(new TTAppEvent("bbb", "{}"));

        assertEquals(2, TTAppEventsQueue.size());
        TTAppEventsQueue.clearAll();
        assertEquals(0, TTAppEventsQueue.size());
    }

    @Test
    public void notifyListener() throws Exception {
        PowerMockito.mockStatic(TiktokBusinessSdk.class);
        PowerMockito.doCallRealMethod().when(TiktokBusinessSdk.class, "destroy");
        PowerMockito.doCallRealMethod().when(TiktokBusinessSdk.class, "setUpSdkListeners", any(), any(), any(), any());
        TiktokBusinessSdk.setUpSdkListeners(new TiktokBusinessSdk.MemoryListener() {
            @Override
            public void onMemoryChange(int size) {
                assertEquals(1, size);
            }
        }, null, null, new TiktokBusinessSdk.NextTimeFlushListener() {
            @Override
            public void timeLeft(int timeLeft) {
            }

            @Override
            public void thresholdLeft(int threshold, int left) {
                assertEquals(TTAppEventLogger.THRESHOLD, threshold);
                assertEquals(TTAppEventLogger.THRESHOLD - 1, left);

            }
        });
        TTAppEventsQueue.addEvent(new TTAppEvent("ccc", "{}"));
        TiktokBusinessSdk.destroy();
        TTAppEventsQueue.clearAll();
    }

    @Test
    public void testExport() {
        TTAppEvent e1 = new TTAppEvent("ccc", "{}");
        TTAppEvent e2 = new TTAppEvent("ddd", "{}");
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
