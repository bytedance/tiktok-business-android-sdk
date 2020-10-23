package com.tiktok.appevents;

import android.app.Application;
import android.content.ContextWrapper;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import com.tiktok.TiktokBusinessSdk;
import com.tiktok.util.TTLogger;
import com.tiktok.util.TTUtil;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        TTUtil.class, TTLogger.class
})
public class TiktokBusinessSdkTest {

    @Before
    public void setup() throws Exception {
//        PowerMockito.mockStatic(TTLogger.class);
//        TTLogger logger = mock(TTLogger.class);
//        PowerMockito.whenNew(TTLogger.class).withAnyArguments().thenReturn(logger);
    }

    @Test
    public void ttConfigNoMetaData() throws Exception {
//        Application context = mock(Application.class);
//        PackageManager packageManager = mock(PackageManager.class);
//        when(context.getPackageManager()).thenReturn(packageManager);
//
//        ApplicationInfo info = mock(ApplicationInfo.class);
//
//        when(packageManager.getApplicationInfo(anyString(), anyInt())).thenReturn(info);
//
//        TTLogger logger = mock(TTLogger.class);
//
//        PowerMockito.whenNew(TTLogger.class).withAnyArguments().thenReturn(logger);
//
//        TTLogger logger1 = new TTLogger("aa", TiktokBusinessSdk.LogLevel.DEBUG);
//
//
//        new TTCrashHandler();
//        // bundle.get will get null
//        Bundle bundle = mock(Bundle.class);
//        info.metaData = bundle;
//
//        TiktokBusinessSdk.TTConfig config = new TiktokBusinessSdk.TTConfig(context);
//        System.out.println(config);
    }
}
