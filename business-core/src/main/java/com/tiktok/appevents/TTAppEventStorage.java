package com.tiktok.appevents;

import android.content.Context;

import com.tiktok.TiktokSdk;
import com.tiktok.model.TTAppEvent;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

public class TTAppEventStorage {
    private static final String TAG = TTAppEventStorage.class.getCanonicalName();

    private static final String EVENT_STORAGE_FILE = "events_cache";

    public static void persist(){
        List<TTAppEvent> appEventList = TTAppEventsQueue.exportAllEvents();

        TTAppEventPersist appEventPersist = readFromDisk();

        if(appEventList.isEmpty() && appEventPersist.isEmpty()){
            return;
        }

        appEventPersist.addEvents(appEventList);

        saveToDisk(appEventPersist);
    }

    public static void persistForFLushFailed(List<TTAppEvent> failedEvents){
        if(failedEvents == null || failedEvents.size()==0) {
            return;
        }

        List<TTAppEvent> appEventList = TTAppEventsQueue.exportAllEvents();

        TTAppEventPersist appEventPersist = readFromDisk();

        appEventPersist.addEvents(appEventList);

        appEventPersist.addEvents(failedEvents);

        saveToDisk(appEventPersist);
    }

    // TODO, api level 16 does not support auto try finally
    private static boolean saveToDisk(TTAppEventPersist appEventPersist) {
        Context context = TiktokSdk.getConfig().getContext();
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(
                    new BufferedOutputStream(context.openFileOutput(EVENT_STORAGE_FILE, Context.MODE_PRIVATE)));
            oos.writeObject(appEventPersist);
            return true;
        }catch (Exception e) {
            e.printStackTrace();
            return false;
        }finally {
            if (oos != null) {
                try {
                    oos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
            }
        }
    }

    public static TTAppEventPersist readFromDisk() {
        Context context = TiktokSdk.getConfig().getContext();
        File f = new File(context.getFilesDir(), EVENT_STORAGE_FILE);
        if (!f.exists()) {
            return new TTAppEventPersist();
        }

        TTAppEventPersist appEventPersist = new TTAppEventPersist();

        ObjectInputStream ois = null;

        try {
            ois = new ObjectInputStream(new BufferedInputStream(context.openFileInput(EVENT_STORAGE_FILE)));

            appEventPersist = (TTAppEventPersist) ois.readObject();

            f.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            try {
                if(ois != null) {
                    ois.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return appEventPersist;
    }

//    public static boolean saveToDisk() {
//        Context context = TiktokSdk.getConfig().getContext();
//        FileOutputStream fos = null;
//        try {
//            fos = context.openFileOutput(EVENT_STORAGE_FILE, Context.MODE_APPEND);
//            PrintStream ps = new PrintStream(fos);
//            JSONArray events = new JSONArray();
//            synchronized (AppEventsQueue.class) {
//                List<AppEvent> appEvents = AppEventsQueue.accumulatedEvents();
//
//                for (AppEvent event : appEvents) {
//                    JSONObject obj = new JSONObject();
//                    obj.put("n", event.getEventName());
//                    obj.put("d", event.getTimeStamp().toString());
//                    obj.put("o", event.getJsonObject());
//                    events.put(obj);
//                }
//            }
//            JSONObject root = new JSONObject();
//            root.put("events", events);
//            ps.println(root.toString(2));
//            Log.v(TAG, "Saved " + events.length() + " to disk");
//            AppEventsQueue.clear();
//            return true;
//        } catch (JSONException | IOException e) {
//            e.printStackTrace();
//            return false;
//        } finally {
//            if (fos != null) {
//                try {
//                    fos.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                    return false;
//                }
//            }
//        }
//    }

//    public static List<AppEvent> readFromDisk() {
//        Context context = TiktokSdk.getConfig().getContext();
//        FileInputStream fis = null;
//        try {
//            File f = new File(context.getFilesDir(), EVENT_STORAGE_FILE);
//            if (!f.exists()) {
//                return new ArrayList<AppEvent>();
//            }
//            fis = new FileInputStream(f);
//            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
//            StringBuffer buffer = new StringBuffer();
//            String str = "";
//            while ((str = reader.readLine()) != null) {
//                buffer.append(str + "\n");
//            }
//            JSONObject root = new JSONObject(buffer.toString());
//
//            JSONArray events = root.getJSONArray("events");
//            int eventLength = events.length();
//            List<AppEvent> results = new ArrayList<>();
//            for (int i = 0; i < eventLength; i++) {
//                JSONObject eventObj = (JSONObject) events.get(i);
//                AppEvent appEvent = new AppEvent(
//                        eventObj.getString("n"),
//                        new Date(eventObj.getString("d")),
//                        eventObj.getJSONObject("o")
//                );
//                results.add(appEvent);
//            }
//            f.delete();
//            return results;
//        } catch (JSONException | IOException e) {
//            e.printStackTrace();
//            return new ArrayList<AppEvent>();
//        } finally {
//            if (fis != null) {
//                try {
//                    fis.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                    return new ArrayList<AppEvent>();
//                }
//            }
//        }
//    }
}
