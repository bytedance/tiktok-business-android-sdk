package com.tiktok.appevents;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

import com.tiktok.util.TTUtil;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * get advertiser id info using Google Play API, also handles google play is not installed
 */
class TTIdentifierFactory {
    private static final String TAG = TTIdentifierFactory.class.getCanonicalName();

    private TTIdentifierFactory() {
    }

    public static class AdInfo {

        private final String mAdvertisingId;
        private final boolean mLimitAdTrackingEnabled;

        AdInfo(String advertisingId, boolean limitAdTrackingEnabled) {
            mAdvertisingId = advertisingId;
            mLimitAdTrackingEnabled = limitAdTrackingEnabled;
            // fetch global switch here
        }

        public String getGaid() {
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
            } catch (InterruptedException ignored) {
            }
        }

        public void onServiceDisconnected(ComponentName name) {
        }

        public IBinder getBinder() throws InterruptedException {

            if (this.retrieved) {
                throw new IllegalStateException();
            }
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

        /**
         * Returns advertiser id
         */
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

        /**
         * get limit ad tracking flag
         */
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

    private static AdInfo adInfoCache;

    /**
     * returns advertiser id info
     */
    public static synchronized AdInfo getAdvertisingIdInfo(Context context) {
        TTUtil.checkThread(TAG);
        if (adInfoCache != null) {
            return adInfoCache;
        }
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
                    adInfoCache = new AdInfo(id, adInterface.isLimitAdTrackingEnabled(true));
                    return adInfoCache;
                }
            } catch (Exception exception) {
                TTCrashHandler.handleCrash(TAG, exception);
            } finally {
                context.unbindService(connection);
            }
        } catch (Exception exception) {
            TTCrashHandler.handleCrash(TAG, exception);
        }
        return null;
    }

}