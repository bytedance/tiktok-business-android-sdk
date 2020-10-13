package com.tiktok.util;

import com.tiktok.appevents.TTCrashHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

public class HttpRequestUtil {

    private static final String TAG = HttpRequestUtil.class.getCanonicalName();

    public static String doPost(String url, Map<String, String> headerParamMap, String jsonStr) {

        String result = null;

        HttpURLConnection connection = null;

        OutputStream outputStream = null;

        try {
            byte[] writeBytes = jsonStr.getBytes("UTF-8");

            URL httpURL = new URL(url);

            connection = (HttpURLConnection) httpURL.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(2000);
            connection.setReadTimeout(5000);
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);

            for (Map.Entry<String, String> entry : headerParamMap.entrySet()) {
                connection.setRequestProperty(entry.getKey(), entry.getValue());
            }
            connection.setRequestProperty("Content-Length", String.valueOf(writeBytes.length));

            connection.connect();
            outputStream = connection.getOutputStream();
            outputStream.write(writeBytes);
            outputStream.flush();

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                result = streamToString(connection.getInputStream());
            }
        } catch (Exception e) {
            TTCrashHandler.handleCrash(TAG, e);
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    TTCrashHandler.handleCrash(TAG, e);
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }

        return result;
    }

    private static String streamToString(InputStream is) {
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
            StringBuilder sb = new StringBuilder();
            String line = "";
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString().trim();
        } catch (Exception e) {
            TTCrashHandler.handleCrash(TAG, e);
        }
        return null;
    }
}
