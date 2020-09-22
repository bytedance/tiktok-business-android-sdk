package com.tiktok.util;

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

    public static String doPost(String url, Map<String, String> headerParamMap, String jsonStr) {

        String result = null;

        OutputStream outputStream = null;

        try {
            byte[] writeBytes =jsonStr.getBytes();

            URL httpURL = new URL(url);

            HttpURLConnection connection = (HttpURLConnection) httpURL.openConnection();

            connection.setRequestMethod("POST");
            connection.setConnectTimeout(2000);
            connection.setReadTimeout(5000);
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);

            for(Map.Entry<String, String> entry: headerParamMap.entrySet()) {
                connection.setRequestProperty(entry.getKey(), entry.getValue());
            }
            connection.setRequestProperty("Content-Length",String.valueOf(writeBytes.length));

            connection.connect();
            outputStream = connection.getOutputStream();
            outputStream.write(jsonStr.getBytes("UTF-8"));
            outputStream.flush();

            int responseCode = connection.getResponseCode();
            if(responseCode == HttpURLConnection.HTTP_OK) {
                result = streamToString(connection.getInputStream());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (outputStream!=null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return result;
    }

    private static String streamToString(InputStream is) {
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line = "";
            while((line = bufferedReader.readLine()) != null){
                sb.append(line);
            }
            return sb.toString().trim();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
