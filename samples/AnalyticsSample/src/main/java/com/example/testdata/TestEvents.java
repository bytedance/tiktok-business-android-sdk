package com.example.testdata;

import java.util.ArrayList;
import java.util.HashMap;

public class TestEvents {

    public static String[] getAllEvents() {
        ArrayList<String> events = new ArrayList<String>();
        for (TestEventType TestEventType: TestEventType.values()) {
            events.add(TestEventType.toString());
        }
        return events.toArray(new String[events.size()]);
    }

    public static HashMap<TestEventType, TestProperty[]> TTEventProperties = new HashMap<TestEventType, TestProperty[]>();

    static {
        TTEventProperties.put(TestEventType.LAUNCH_APP, new TestProperty[]{});
        TTEventProperties.put(TestEventType.INSTALL_APP, new TestProperty[]{});
        TTEventProperties.put(TestEventType.RETENTION_2D, new TestProperty[]{});
        TTEventProperties.put(TestEventType.ADD_PAYMENT_INFO, new TestProperty[]{
                TestProperty.SUCCESS,
        });
        TTEventProperties.put(TestEventType.ADD_TO_CART, new TestProperty[]{
                TestProperty.CONTENT_TYPE,
                TestProperty.SKU_ID,
                TestProperty.DESCRIPTION,
                TestProperty.CURRENCY,
                TestProperty.VALUE,
        });
        TTEventProperties.put(TestEventType.ADD_TO_WISHLIST, new TestProperty[]{
                TestProperty.PAGE_TYPE,
                TestProperty.CONTENT_ID,
                TestProperty.DESCRIPTION,
                TestProperty.CURRENCY,
                TestProperty.VALUE,
        });
        TTEventProperties.put(TestEventType.CHECKOUT, new TestProperty[]{
                TestProperty.DESCRIPTION,
                TestProperty.SKU_ID,
                TestProperty.NUMBER_OF_ITEMS,
                TestProperty.PAYMENT_AVAILABLE,
                TestProperty.CURRENCY,
                TestProperty.VALUE,
                TestProperty.GAME_ITEM_TYPE,
                TestProperty.GAME_ITEM_ID,
                TestProperty.ROOM_TYPE,
                TestProperty.CURRENCY,
                TestProperty.VALUE,
                TestProperty.LOCATION,
                TestProperty.CHECKIN_DATE,
                TestProperty.CHECKOUT_DATE,
                TestProperty.NUMBER_OF_ROOMS,
                TestProperty.NUMBER_OF_NIGHTS,
        });
        TTEventProperties.put(TestEventType.COMPLETE_TUTORIAL, new TestProperty[]{});
        TTEventProperties.put(TestEventType.VIEW_CONTENT, new TestProperty[]{
                TestProperty.PAGE_TYPE,
                TestProperty.SKU_ID,
                TestProperty.DESCRIPTION,
                TestProperty.CURRENCY,
                TestProperty.VALUE,
                TestProperty.SEARCH_STRING,
                TestProperty.ROOM_TYPE,
                TestProperty.LOCATION,
                TestProperty.CHECKIN_DATE,
                TestProperty.CHECKOUT_DATE,
                TestProperty.NUMBER_OF_ROOMS,
                TestProperty.NUMBER_OF_NIGHTS,
                TestProperty.OUTBOUND_ORIGINATION_CITY,
                TestProperty.OUTBOUND_DESTINATION_CITY,
                TestProperty.RETURN_ORIGINATION_CITY,
                TestProperty.RETURN_DESTINATION_CITY,
                TestProperty.CLASS,
                TestProperty.NUMBER_OF_PASSENGER,
        });
        TTEventProperties.put(TestEventType.CREATE_GROUP, new TestProperty[]{
                TestProperty.GROUP_NAME,
                TestProperty.GROUP_LOGO,
                TestProperty.GROUP_DESCRIPTION,
                TestProperty.GROUP_TYPE,
                TestProperty.GROUP_ID,
        });
        TTEventProperties.put(TestEventType.CREATE_ROLE, new TestProperty[]{
                TestProperty.ROLE_TYPE,
        });
        TTEventProperties.put(TestEventType.GENERATE_LEAD, new TestProperty[]{});
        TTEventProperties.put(TestEventType.IN_APP_AD_CLICK, new TestProperty[]{
                TestProperty.AD_TYPE,
        });
        TTEventProperties.put(TestEventType.IN_APP_AD_IMPR, new TestProperty[]{
                TestProperty.AD_TYPE,
        });
        TTEventProperties.put(TestEventType.JOIN_GROUP, new TestProperty[]{
                TestProperty.LEVEL_NUMBER,
        });
        TTEventProperties.put(TestEventType.ACHIEVE_LEVEL, new TestProperty[]{
                TestProperty.LEVEL_NUMBER,
                TestProperty.SCORE,
        });
        TTEventProperties.put(TestEventType.LOAN_APPLICATION, new TestProperty[]{
                TestProperty.LOAN_TYPE,
                TestProperty.APPLICATION_ID,
        });
        TTEventProperties.put(TestEventType.LOAN_APPROVAL, new TestProperty[]{
                TestProperty.VALUE,
        });
        TTEventProperties.put(TestEventType.LOAN_DISBURSAL, new TestProperty[]{
                TestProperty.VALUE,
        });
        TTEventProperties.put(TestEventType.LOGIN, new TestProperty[]{});
        TTEventProperties.put(TestEventType.PURCHASE, new TestProperty[]{
                TestProperty.PAGE_TYPE,
                TestProperty.SKU_ID,
                TestProperty.DESCRIPTION,
                TestProperty.NUMBER_OF_ITEMS,
                TestProperty.COUPON_USED,
                TestProperty.CURRENCY,
                TestProperty.VALUE,
                TestProperty.GROUP_TYPE,
                TestProperty.GAME_ITEM_ID,
                TestProperty.ROOM_TYPE,
                TestProperty.LOCATION,
                TestProperty.CHECKIN_DATE,
                TestProperty.CHECKOUT_DATE,
                TestProperty.NUMBER_OF_ROOMS,
                TestProperty.NUMBER_OF_NIGHTS,
                TestProperty.OUTBOUND_ORIGINATION_CITY,
                TestProperty.OUTBOUND_DESTINATION_CITY,
                TestProperty.RETURN_ORIGINATION_CITY,
                TestProperty.RETURN_DESTINATION_CITY,
                TestProperty.CLASS,
                TestProperty.NUMBER_OF_PASSENGER,
                TestProperty.SERVICE_TYPE,
                TestProperty.SERVICE_ID,
        });
        TTEventProperties.put(TestEventType.RATE, new TestProperty[]{
                TestProperty.PAGE_TYPE,
                TestProperty.SKU_ID,
                TestProperty.CONTENT,
                TestProperty.RATING_VALUE,
                TestProperty.MAX_RATING_VALUE,
                TestProperty.RATE,
        });
        TTEventProperties.put(TestEventType.REGISTRATION, new TestProperty[]{
                TestProperty.REGISTRATION_METHOD,
        });
        TTEventProperties.put(TestEventType.SEARCH, new TestProperty[]{
                TestProperty.SEARCH_STRING,
                TestProperty.CHECKIN_DATE,
                TestProperty.CHECKOUT_DATE,
                TestProperty.NUMBER_OF_ROOMS,
                TestProperty.NUMBER_OF_NIGHTS,
                TestProperty.ORIGINATION_CITY,
                TestProperty.DESTINATION_CITY,
                TestProperty.DEPARTURE_DATE,
                TestProperty.RETURN_DATE,
                TestProperty.CLASS,
                TestProperty.NUMBER_OF_PASSENGER,
        });
        TTEventProperties.put(TestEventType.SPEND_CREDITS, new TestProperty[]{
                TestProperty.GAME_ITEM_TYPE,
                TestProperty.GAME_ITEM_ID,
                TestProperty.LEVEL_NUMBER,
        });
        TTEventProperties.put(TestEventType.START_TRIAL, new TestProperty[]{
                TestProperty.ORDER_ID,
                TestProperty.CURRENCY,
        });
        TTEventProperties.put(TestEventType.SUBSCRIBE, new TestProperty[]{
                TestProperty.ORDER_ID,
                TestProperty.CURRENCY,
        });
        TTEventProperties.put(TestEventType.SHARE, new TestProperty[]{
                TestProperty.CONTENT_TYPE,
                TestProperty.CONTENT_ID,
                TestProperty.SHARED_DESTINATION,
        });
        TTEventProperties.put(TestEventType.CONTACT, new TestProperty[]{});
        TTEventProperties.put(TestEventType.UNLOCK_ACHIEVEMENT, new TestProperty[]{
                TestProperty.DESCRIPTION,
                TestProperty.ACHIEVEMENT_TYPE,
        });
    }
}

