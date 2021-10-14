/*******************************************************************************
 * Copyright (c) 2020. Bytedance Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.tiktok.util;

import com.tiktok.appevents.TTCrashHandler;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

public class HttpRequestUtil {

    public static class HttpRequestOptions {
        private static int UNSET = -1;
        public int connectTimeout = UNSET;
        public int readTimeout = UNSET;

        public void configConnection(HttpURLConnection connection) {
            if (connectTimeout != UNSET) {
                connection.setConnectTimeout(connectTimeout);
            }
            if (readTimeout != UNSET) {
                connection.setReadTimeout(readTimeout);
            }
        }
    }

    private static final String TAG = HttpRequestUtil.class.getCanonicalName();

    public static String doGet(String url, Map<String, String> headerParamMap) {
        HttpRequestOptions options = new HttpRequestOptions();
        options.connectTimeout = 2000;
        options.readTimeout = 5000;
        return doGet(url, headerParamMap, options);
    }

    public static HttpsURLConnection connect(String url, Map<String, String> headerParamMap, HttpRequestOptions options, String method, String contentLength) {
        HttpsURLConnection connection = null;

        try {
            URL httpURL = new URL(url);
            connection = (HttpsURLConnection) httpURL.openConnection();
            connection.setRequestMethod(method);
            options.configConnection(connection);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            if(method.equals("GET")) {
                connection.setDoOutput(false);
            } else if(method.equals("POST")) {
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Length", contentLength);
            }

            for (Map.Entry<String, String> entry : headerParamMap.entrySet()) {
                connection.setRequestProperty(entry.getKey(), entry.getValue());
            }

            connection.connect();
        } catch (Exception e) {
            TTCrashHandler.handleCrash(TAG, e);
            if (connection != null) {
                try {
                    connection.disconnect();
                }catch (Exception exc){
                    TTCrashHandler.handleCrash(TAG, exc);
                }
            }
        }
        return connection;
    }

    public static boolean shouldRedirect(int status) {
        if (status != HttpURLConnection.HTTP_OK) {
            if (status == HttpURLConnection.HTTP_MOVED_TEMP
                    || status == HttpURLConnection.HTTP_MOVED_PERM
                    || status == HttpURLConnection.HTTP_SEE_OTHER
                    || status == 307)
                return true;
        }
        return false;
    }

    public static String doGet(String url, Map<String, String> headerParamMap, HttpRequestOptions options) {
        String result = null;
        HttpsURLConnection connection = connect(url, headerParamMap, options, "GET", null);
        if (connection == null) return result;

        try{
            boolean redirect = shouldRedirect(connection.getResponseCode());
            if (redirect) {
                String redirectUrl = connection.getHeaderField("Location");
                connection.disconnect();
                connection = connect(redirectUrl, headerParamMap, options, "GET", null);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                result = streamToString(connection.getInputStream());
            }
        } catch (Exception e) {
            TTCrashHandler.handleCrash(TAG, e);
        } finally {
            if (connection != null) {
                try {
                    connection.disconnect();
                }catch (Exception e){
                    TTCrashHandler.handleCrash(TAG, e);
                }
            }
        }
        return result;
    }

    public static String doPost(String url, Map<String, String> headerParamMap, String jsonStr) {
        HttpRequestOptions options = new HttpRequestOptions();
        options.connectTimeout = 2000;
        options.readTimeout = 5000;
        return doPost(url, headerParamMap, jsonStr, options);
    }

    public static String doPost(String url, Map<String, String> headerParamMap, String jsonStr, HttpRequestOptions options) {

        String result = null;

        HttpURLConnection connection = null;

        OutputStream outputStream = null;

        try {
            byte[] writeBytes = jsonStr.getBytes("UTF-8");
            String contentLength = String.valueOf(writeBytes.length);

            connection = connect(url, headerParamMap, options, "POST", contentLength);
            if (connection == null) return result;
            outputStream = connection.getOutputStream();
            outputStream.write(writeBytes);
            outputStream.flush();
            boolean redirect = shouldRedirect(connection.getResponseCode());
            if (redirect) {
                String redirectUrl = connection.getHeaderField("Location");
                connection.disconnect();
                connection = connect(redirectUrl, headerParamMap, options, "POST", contentLength);
                outputStream = connection.getOutputStream();
                outputStream.write(writeBytes);
                outputStream.flush();
            }

            int responseCode = connection.getResponseCode();
            // http code is different from the code returned by api
            if (responseCode == HttpURLConnection.HTTP_OK) {
                result = streamToString(connection.getInputStream());
                JSONObject obj = new JSONObject(result);
                System.out.println(obj);
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
                try {
                    connection.disconnect();
                }catch (Exception e){
                    TTCrashHandler.handleCrash(TAG, e);
                }
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