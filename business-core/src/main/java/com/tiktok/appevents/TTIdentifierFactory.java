package com.tiktok.appevents;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Looper;
import android.os.Parcel;
import android.os.RemoteException;
import android.text.TextUtils;

import com.tiktok.util.TTLogger;

import java.util.concurrent.LinkedBlockingQueue;

import static com.tiktok.TiktokBusinessSdk.LogLevel;

public class TTIdentifierFactory {
    private static final String TAG = TTIdentifierFactory.class.getSimpleName();

    public interface Listener {
        void onIdentifierFactoryFinish(AdInfo adInfo);
        void onIdentifierFactoryFail(Exception exception);
    }

    protected Listener mListener;
    protected Handler  mHandler;

    private TTLogger logger;

    private TTIdentifierFactory(LogLevel ll) {
        this.logger = new TTLogger(TAG, ll);
    }

    public static synchronized void getAdvertisingId(Context context, LogLevel logLevel, Listener listener) {
        new TTIdentifierFactory(logLevel).start(context, listener);
    }

    public static class AdInfo {

        private final String  mAdvertisingId;
        private final boolean mLimitAdTrackingEnabled;

        AdInfo(String advertisingId, boolean limitAdTrackingEnabled) {
            mAdvertisingId = advertisingId;
            mLimitAdTrackingEnabled = limitAdTrackingEnabled;
        }

        public String getId() {
            return mAdvertisingId;
        }

        public boolean isLimitAdTrackingEnabled() {
            return mLimitAdTrackingEnabled;
        }
    }

    protected static class AdvertisingConnection implements ServiceConnection {

        boolean retrieved = false;
        private final LinkedBlockingQueue<IBinder> queue = new LinkedBlockingQueue<>(1);

        public void onServiceConnected(ComponentName name, IBinder service) {

            try {
                this.queue.put(service);
            } catch (InterruptedException ignored) {}
        }

        public void onServiceDisconnected(ComponentName name) {}

        public IBinder getBinder() throws InterruptedException {

            if (this.retrieved) { throw new IllegalStateException(); }
            this.retrieved = true;
            return (IBinder) this.queue.take();
        }
    }

    protected static class AdvertisingInterface implements IInterface {

        private IBinder binder;

        public AdvertisingInterface(IBinder pBinder) {
            binder = pBinder;
        }

        public IBinder asBinder() {
            return binder;
        }

        public String getId() throws RemoteException {
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            String id;
            try {
                data.writeInterfaceToken("com.google.android.gms.ads.identifier.internal.IAdvertisingIdService");
                binder.transact(1, data, reply, 0);
                reply.readException();
                id = reply.readString();
            } finally {
                reply.recycle();
                data.recycle();
            }
            return id;
        }

        public boolean isLimitAdTrackingEnabled(boolean paramBoolean) throws RemoteException {

            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            boolean limitAdTracking;
            try {
                data.writeInterfaceToken("com.google.android.gms.ads.identifier.internal.IAdvertisingIdService");
                data.writeInt(paramBoolean ? 1 : 0);
                binder.transact(2, data, reply, 0);
                reply.readException();
                limitAdTracking = 0 != reply.readInt();
            } finally {
                reply.recycle();
                data.recycle();
            }
            return limitAdTracking;
        }
    }

    protected void start(final Context context, final Listener listener) {
        if (listener == null) {
            logger.error(null,"getAdvertisingId - Error: null listener, dropping call");
        } else {
            mHandler = new Handler(Looper.getMainLooper());
            mListener = listener;
            if (context == null) {
                invokeFail(new Exception(TAG + " - Error: context null"));
            } else {
                new Thread(() -> getAdvertisingIdInfo(context)).start();
            }
        }
    }

    private void getAdvertisingIdInfo(Context context) {
        logger.verbose("getAdvertisingIdInfo");
        try {
            PackageManager pm = context.getPackageManager();
            pm.getPackageInfo("com.android.vending", 0);
            Intent intent = new Intent("com.google.android.gms.ads.identifier.service.START");
            intent.setPackage("com.google.android.gms");
            AdvertisingConnection connection = new AdvertisingConnection();
            try {
                if (context.bindService(intent, connection, Context.BIND_AUTO_CREATE)) {
                    AdvertisingInterface adInterface = new AdvertisingInterface(connection.getBinder());
                    String id = adInterface.getId();
                    if(TextUtils.isEmpty(id)) {
                        logger.verbose("getAdvertisingIdInfo - Error: ID Not available");
                        invokeFail(new Exception("Advertising ID extraction Error: ID Not available"));
                    } else {
                        invokeFinish(new AdInfo(id, adInterface.isLimitAdTrackingEnabled(true)));
                    }
                }
            } catch (Exception exception) {
                logger.verbose("getAdvertisingIdInfo - Error: " + exception);
                invokeFail(exception);
            } finally {
                context.unbindService(connection);
            }
        } catch (Exception exception) {
            logger.verbose("getAdvertisingIdInfo - Error: " + exception);
            invokeFail(exception);
        }
    }

    protected void invokeFinish(final AdInfo adInfo) {
        logger.verbose( "invokeFinish");
        mHandler.post(() -> {
            if (mListener != null) {
                mListener.onIdentifierFactoryFinish(adInfo);
            }
        });
    }

    protected void invokeFail(final Exception exception) {
        logger.verbose( "invokeFail: " + exception);
        mHandler.post(() -> {
            if (mListener != null) {
                mListener.onIdentifierFactoryFail(exception);
            }
        });
    }
}
