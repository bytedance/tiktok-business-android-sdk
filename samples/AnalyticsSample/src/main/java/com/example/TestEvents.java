package com.example;

import java.util.ArrayList;
import java.util.HashMap;

public class TestEvents {
    private static final String TAG = TestEvents.class.getName();

    public static String[] getEvents() {
        ArrayList<String> events = new ArrayList<String>();
        for (TestEventType TestEventType: TestEventType.values()) {
            events.add(TestEventType.toString());
        }
        return events.toArray(new String[events.size()]);
    }

    public enum TestEventType {
        LAUNCH_APP("LaunchApp"),
        INSTALL_APP("InstallApp"),
        RETENTION_2D("2Dretention"),
        ADD_PAYMENT_INFO("AddPaymentInfo"),
        ADD_TO_CART("AddToCart"),
        ADD_TO_WISHLIST("AddToWishlist"),
        CHECKOUT("CheckOut"),
        COMPLETE_TUTORIAL("CompleteTutorial"),
        VIEW_CONTENT("ViewContent"),
        CREATE_GROUP("CreateGroup"),
        CREATE_ROLE("CreateRole"),
        GENERATE_LEAD("GenerateLead"),
        IN_APP_AD_CLICK("InAppADClick"),
        IN_APP_AD_IMPR("InAppADImpr"),
        JOIN_GROUP("JoinGroup"),
        ACHIEVE_LEVEL("AchieveLevel"),
        LOAN_APPLICATION("LoanApplication"),
        LOAN_APPROVAL("LoanApproval"),
        LOAN_DISBURSAL("LoanDisbursal"),
        LOGIN("Login"),
        PURCHASE("Purchase"),
        RATE("Rate"),
        REGISTRATION("Registration"),
        SEARCH("Search"),
        SPEND_CREDITS("SpendCredits"),
        START_TRIAL("StartTrial"),
        SUBSCRIBE("Subscribe"),
        SHARE("Share"),
        CONTACT("Contact"),
        UNLOCK_ACHIEVEMENT("UnlockAchievement");

        private String eventType;

        TestEventType(String eventType) {
            this.eventType = eventType;
        }

        public String getEventType() {
            return this.eventType;
        }
    }

    public enum TestProperty {
        SUCCESS("success"),
        CONTENT_TYPE("content_type"),
        SKU_ID("sku_id"),
        DESCRIPTION("description"),
        CURRENCY("currency"),
        VALUE("value"),
        PAGE_TYPE("page_type"),
        CONTENT_ID("content_id"),
        NUMBER_OF_ITEMS("num_items"),
        PAYMENT_AVAILABLE("payment_available"),
        GAME_ITEM_TYPE("game_item_type"),
        GAME_ITEM_ID("game_item_id"),
        ROOM_TYPE("room_type"),
        LOCATION("location"),
        CHECKIN_DATE("checkin_date"),
        CHECKOUT_DATE("checkout_date"),
        NUMBER_OF_ROOMS("number_of_rooms"),
        NUMBER_OF_NIGHTS("number_of_nights"),
        SEARCH_STRING("search_string"),
        OUTBOUND_ORIGINATION_CITY("outbound_origination_city"),
        OUTBOUND_DESTINATION_CITY("outbound_destination_city"),
        RETURN_ORIGINATION_CITY("return_origination_city"),
        RETURN_DESTINATION_CITY("return_destination_city"),
        CLASS("class"),
        NUMBER_OF_PASSENGER("number_of_passenger"),
        GROUP_NAME("group_name"),
        GROUP_LOGO("group_logo"),
        GROUP_DESCRIPTION("group_description"),
        GROUP_TYPE("group_type"),
        GROUP_ID("group_id"),
        ROLE_TYPE("role_type"),
        AD_TYPE("ad_type"),
        LEVEL_NUMBER("level_number"),
        SCORE("score"),
        LOAN_TYPE("loan_type"),
        APPLICATION_ID("application_id"),
        COUPON_USED("coupon_used"),
        SERVICE_TYPE("service_type"),
        SERVICE_ID("service_id"),
        CONTENT("content"),
        RATING_VALUE("rating_value"),
        MAX_RATING_VALUE("max_rating_value"),
        RATE("rate"),
        REGISTRATION_METHOD("registration_method"),
        ORIGINATION_CITY("origination_city"),
        DESTINATION_CITY("destination_city"),
        DEPARTURE_DATE("departure_date"),
        RETURN_DATE("return_date"),
        ORDER_ID("order_id"),
        SHARED_DESTINATION("shared_destination"),
        ACHIEVEMENT_TYPE("achievement_type");

        private String property;

        TestProperty(String property) {
            this.property = property;
        }

        public String getProperty() {
            return this.property;
        }
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

