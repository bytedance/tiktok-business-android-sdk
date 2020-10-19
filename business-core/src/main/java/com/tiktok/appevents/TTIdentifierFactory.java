package com.tiktok.appevents;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.*;

import android.text.TextUtils;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * get advertiser id info using Google Play API
 */
class TTIdentifierFactory {

    public static AdIdInfo getGoogleAdIdInfo(Context context) throws PackageManager.NameNotFoundException, RemoteException {
        PackageManager packageManager = context.getPackageManager();
        try {
            // is google play installed
            packageManager.getPackageInfo("com.android.vending", 0);
        } catch (PackageManager.NameNotFoundException e) {
            // google play is not installed
            throw new PackageManager.NameNotFoundException("package 'com.android.vending' not found");
        }

        // service binding intent
        Intent intent = new Intent("com.google.android.gms.ads.identifier.service.START");
        intent.setPackage("com.google.android.gms");
        AdIdConnection serviceConnection = new AdIdConnection();
        try {
            // if connection is successful
            if (context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)) {
                AdIdInterface adIdInterface = new AdIdInterface(serviceConnection.getBinder());
                String adId = adIdInterface.getAdId();
                boolean isAdTrackingEnabled = adIdInterface.isAdIdTrackingEnabled();
                if (TextUtils.isEmpty(adId)) {
                    // empty ad id, something went wrong
                    throw new IllegalStateException("Ad ID is null or empty");
                } else {
                    // everything is ok, call listener
                    return new AdIdInfo(adId, isAdTrackingEnabled);
                }
            } else {
                // connection to service was not successful
                throw new IllegalStateException("Bad GMS service connection");
            }
        } catch (RemoteException e) {
            // can't process IBinder object
            throw e;
        } finally {
            // finally unbind from service
            context.unbindService(serviceConnection);
        }
    }


    /**
     * Holds 'Ad ID and 'Is Limited Ad Tracking' flag
     */
    public static class AdIdInfo {
        private final String adId;
        private final boolean isAdTrackingEnabled;

        private AdIdInfo(String adId, boolean isAdTrackingEnabled) {
            this.adId = adId;
            this.isAdTrackingEnabled = isAdTrackingEnabled;
        }

        public String getAdId() {
            return adId;
        }

        public boolean isAdTrackingEnabled() {
            return isAdTrackingEnabled;
        }
    }

    /**
     * Service connection that retrieves Binder object from connected service
     */
    private static class AdIdConnection implements ServiceConnection {

        private final BlockingQueue<IBinder> queue = new ArrayBlockingQueue<>(1);

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) throws IllegalStateException {
            try {
                this.queue.put(iBinder);
            } catch (InterruptedException ex) {
                throw new IllegalStateException("Exception trying to parse GMS connection");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
        }

        public IBinder getBinder() throws IllegalStateException {
            try {
                return queue.take();
            } catch (InterruptedException e) {
                throw new IllegalStateException("Exception trying to retrieve GMS connection");
            }
        }
    }

    /**
     * Interface that deals with advertising service's Binder
     */
    private static class AdIdInterface implements IInterface {

        private static final String INTERFACE_TOKEN =
                "com.google.android.gms.ads.identifier.internal.IAdvertisingIdService";
        private static final int AD_ID_TRANSACTION_CODE = 1;
        private static final int AD_TRACKING_TRANSACTION_CODE = 2;

        private final IBinder mIBinder;

        private AdIdInterface(IBinder binder) {
            this.mIBinder = binder;
        }

        @Override
        public IBinder asBinder() {
            return mIBinder;
        }

        private String getAdId() throws RemoteException {
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            String adId;
            try {
                data.writeInterfaceToken(INTERFACE_TOKEN);
                mIBinder.transact(AD_ID_TRANSACTION_CODE, data, reply, 0);
                reply.readException();
                adId = reply.readString();
            } finally {
                data.recycle();
                reply.recycle();
            }
            return adId;
        }

        private boolean isAdIdTrackingEnabled() throws RemoteException {
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            boolean limitedTrackingEnabled;
            try {
                data.writeInterfaceToken(INTERFACE_TOKEN);
                data.writeInt(1);
                mIBinder.transact(AD_TRACKING_TRANSACTION_CODE, data, reply, 0);
                reply.readException();
                limitedTrackingEnabled = 0 != reply.readInt();
            } finally {
                data.recycle();
                reply.recycle();
            }
            return limitedTrackingEnabled;
        }
    }
}