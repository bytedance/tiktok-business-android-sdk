/*******************************************************************************
 * Copyright (c) 2023. Bytedance Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.tiktok.appevents.contents;

import static com.tiktok.appevents.contents.TTContentsEventConstants.ContentsEventName.EVENT_NAME_PURCHASE;

import org.json.JSONObject;

public class TTPurchaseEvent extends TTContentsEvent {

    TTPurchaseEvent(String eventName, JSONObject properties, String eventId) {
        super(eventName, properties, eventId);
    }

    public static Builder newBuilder() {
        return new Builder(EVENT_NAME_PURCHASE, "");
    }

    public static Builder newBuilder(String eventId) {
        return new Builder(EVENT_NAME_PURCHASE, eventId);
    }
}
