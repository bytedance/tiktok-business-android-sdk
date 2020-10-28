package com.tiktok.appevents;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.*;

public class TTPropertyTest {

    @Test
    public void validProperty() throws JSONException {
        String jsonStr = "{'app_id':'com.shopee.sg','event_source':'APP_EVENTS_SDK','batch':[{'type':'track','event':'Purchase','timestamp':'2020-10-22T14:06:17Z','properties':{'currency':'SGD','value':1.34,'contents':[{'content_id':'android.test.purchased','content_type':'inapp','quantity':1,'description':'Sample description for product: android.test.purchased.','price':1.34}]},'context':{'app':{'name':'IABTest','namespace':'com.example.iabtest','version':'1.0','build':1},'device':{'platform':'Android','gaid':'6a26e92e-841d-44fd-be96-c825d882b4bd'},'locale':'en-US','ip':'10.0.2.16','user_agent':'Mozilla/5.0 (Linux; Android 11; sdk_gphone_x86 Build/RPB3.200720.005; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/83.0.4103.106 Mobile Safari/537.36'}}]}";
        TTProperty property = new TTProperty(new JSONObject(jsonStr));
        assertEquals("com.shopee.sg", property.get().optString("app_id"));
        assertEquals("track", property.get().optJSONArray("batch").getJSONObject(0).optString("type"));
    }

}
