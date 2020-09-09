package com.example;

import java.util.ArrayList;
import java.util.HashMap;

public class TestEvents {
    private static final String TAG = TestEvents.class.getName();

    public static String[] getEvents() {
        ArrayList<String> events = new ArrayList<String>();
        for (TTEventType ttEventType: TTEventType.values()) {
            events.add(ttEventType.toString());
        }
        return events.toArray(new String[events.size()]);
    }

    public enum TTEventType {
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

        TTEventType(String eventType) {
            this.eventType = eventType;
        }

        public String getEventType() {
            return this.eventType;
        }
    }

    public enum TTProperty {
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

        TTProperty(String property) {
            this.property = property;
        }

        public String getProperty() {
            return this.property;
        }
    }

    public static HashMap<TTEventType, TTProperty[]> TTEventProperties = new HashMap<TTEventType, TTProperty[]>();

    static {
        TTEventProperties.put(TTEventType.LAUNCH_APP, new TTProperty[]{});
        TTEventProperties.put(TTEventType.INSTALL_APP, new TTProperty[]{});
        TTEventProperties.put(TTEventType.RETENTION_2D, new TTProperty[]{});
        TTEventProperties.put(TTEventType.ADD_PAYMENT_INFO, new TTProperty[]{
                TTProperty.SUCCESS,
        });
        TTEventProperties.put(TTEventType.ADD_TO_CART, new TTProperty[]{
                TTProperty.CONTENT_TYPE,
                TTProperty.SKU_ID,
                TTProperty.DESCRIPTION,
                TTProperty.CURRENCY,
                TTProperty.VALUE,
        });
        TTEventProperties.put(TTEventType.ADD_TO_WISHLIST, new TTProperty[]{
                TTProperty.PAGE_TYPE,
                TTProperty.CONTENT_ID,
                TTProperty.DESCRIPTION,
                TTProperty.CURRENCY,
                TTProperty.VALUE,
        });
        TTEventProperties.put(TTEventType.CHECKOUT, new TTProperty[]{
                TTProperty.DESCRIPTION,
                TTProperty.SKU_ID,
                TTProperty.NUMBER_OF_ITEMS,
                TTProperty.PAYMENT_AVAILABLE,
                TTProperty.CURRENCY,
                TTProperty.VALUE,
                TTProperty.GAME_ITEM_TYPE,
                TTProperty.GAME_ITEM_ID,
                TTProperty.ROOM_TYPE,
                TTProperty.CURRENCY,
                TTProperty.VALUE,
                TTProperty.LOCATION,
                TTProperty.CHECKIN_DATE,
                TTProperty.CHECKOUT_DATE,
                TTProperty.NUMBER_OF_ROOMS,
                TTProperty.NUMBER_OF_NIGHTS,
        });
        TTEventProperties.put(TTEventType.COMPLETE_TUTORIAL, new TTProperty[]{});
        TTEventProperties.put(TTEventType.VIEW_CONTENT, new TTProperty[]{
                TTProperty.PAGE_TYPE,
                TTProperty.SKU_ID,
                TTProperty.DESCRIPTION,
                TTProperty.CURRENCY,
                TTProperty.VALUE,
                TTProperty.SEARCH_STRING,
                TTProperty.ROOM_TYPE,
                TTProperty.LOCATION,
                TTProperty.CHECKIN_DATE,
                TTProperty.CHECKOUT_DATE,
                TTProperty.NUMBER_OF_ROOMS,
                TTProperty.NUMBER_OF_NIGHTS,
                TTProperty.OUTBOUND_ORIGINATION_CITY,
                TTProperty.OUTBOUND_DESTINATION_CITY,
                TTProperty.RETURN_ORIGINATION_CITY,
                TTProperty.RETURN_DESTINATION_CITY,
                TTProperty.CLASS,
                TTProperty.NUMBER_OF_PASSENGER,
        });
        TTEventProperties.put(TTEventType.CREATE_GROUP, new TTProperty[]{
                TTProperty.GROUP_NAME,
                TTProperty.GROUP_LOGO,
                TTProperty.GROUP_DESCRIPTION,
                TTProperty.GROUP_TYPE,
                TTProperty.GROUP_ID,
        });
        TTEventProperties.put(TTEventType.CREATE_ROLE, new TTProperty[]{
                TTProperty.ROLE_TYPE,
        });
        TTEventProperties.put(TTEventType.GENERATE_LEAD, new TTProperty[]{});
        TTEventProperties.put(TTEventType.IN_APP_AD_CLICK, new TTProperty[]{
                TTProperty.AD_TYPE,
        });
        TTEventProperties.put(TTEventType.IN_APP_AD_IMPR, new TTProperty[]{
                TTProperty.AD_TYPE,
        });
        TTEventProperties.put(TTEventType.JOIN_GROUP, new TTProperty[]{
                TTProperty.LEVEL_NUMBER,
        });
        TTEventProperties.put(TTEventType.ACHIEVE_LEVEL, new TTProperty[]{
                TTProperty.LEVEL_NUMBER,
                TTProperty.SCORE,
        });
        TTEventProperties.put(TTEventType.LOAN_APPLICATION, new TTProperty[]{
                TTProperty.LOAN_TYPE,
                TTProperty.APPLICATION_ID,
        });
        TTEventProperties.put(TTEventType.LOAN_APPROVAL, new TTProperty[]{
                TTProperty.VALUE,
        });
        TTEventProperties.put(TTEventType.LOAN_DISBURSAL, new TTProperty[]{
                TTProperty.VALUE,
        });
        TTEventProperties.put(TTEventType.LOGIN, new TTProperty[]{});
        TTEventProperties.put(TTEventType.PURCHASE, new TTProperty[]{
                TTProperty.PAGE_TYPE,
                TTProperty.SKU_ID,
                TTProperty.DESCRIPTION,
                TTProperty.NUMBER_OF_ITEMS,
                TTProperty.COUPON_USED,
                TTProperty.CURRENCY,
                TTProperty.VALUE,
                TTProperty.GROUP_TYPE,
                TTProperty.GAME_ITEM_ID,
                TTProperty.ROOM_TYPE,
                TTProperty.LOCATION,
                TTProperty.CHECKIN_DATE,
                TTProperty.CHECKOUT_DATE,
                TTProperty.NUMBER_OF_ROOMS,
                TTProperty.NUMBER_OF_NIGHTS,
                TTProperty.OUTBOUND_ORIGINATION_CITY,
                TTProperty.OUTBOUND_DESTINATION_CITY,
                TTProperty.RETURN_ORIGINATION_CITY,
                TTProperty.RETURN_DESTINATION_CITY,
                TTProperty.CLASS,
                TTProperty.NUMBER_OF_PASSENGER,
                TTProperty.SERVICE_TYPE,
                TTProperty.SERVICE_ID,
        });
        TTEventProperties.put(TTEventType.RATE, new TTProperty[]{
                TTProperty.PAGE_TYPE,
                TTProperty.SKU_ID,
                TTProperty.CONTENT,
                TTProperty.RATING_VALUE,
                TTProperty.MAX_RATING_VALUE,
                TTProperty.RATE,
        });
        TTEventProperties.put(TTEventType.REGISTRATION, new TTProperty[]{
                TTProperty.REGISTRATION_METHOD,
        });
        TTEventProperties.put(TTEventType.SEARCH, new TTProperty[]{
                TTProperty.SEARCH_STRING,
                TTProperty.CHECKIN_DATE,
                TTProperty.CHECKOUT_DATE,
                TTProperty.NUMBER_OF_ROOMS,
                TTProperty.NUMBER_OF_NIGHTS,
                TTProperty.ORIGINATION_CITY,
                TTProperty.DESTINATION_CITY,
                TTProperty.DEPARTURE_DATE,
                TTProperty.RETURN_DATE,
                TTProperty.CLASS,
                TTProperty.NUMBER_OF_PASSENGER,
        });
        TTEventProperties.put(TTEventType.SPEND_CREDITS, new TTProperty[]{
                TTProperty.GAME_ITEM_TYPE,
                TTProperty.GAME_ITEM_ID,
                TTProperty.LEVEL_NUMBER,
        });
        TTEventProperties.put(TTEventType.START_TRIAL, new TTProperty[]{
                TTProperty.ORDER_ID,
                TTProperty.CURRENCY,
        });
        TTEventProperties.put(TTEventType.SUBSCRIBE, new TTProperty[]{
                TTProperty.ORDER_ID,
                TTProperty.CURRENCY,
        });
        TTEventProperties.put(TTEventType.SHARE, new TTProperty[]{
                TTProperty.CONTENT_TYPE,
                TTProperty.CONTENT_ID,
                TTProperty.SHARED_DESTINATION,
        });
        TTEventProperties.put(TTEventType.CONTACT, new TTProperty[]{});
        TTEventProperties.put(TTEventType.UNLOCK_ACHIEVEMENT, new TTProperty[]{
                TTProperty.DESCRIPTION,
                TTProperty.ACHIEVEMENT_TYPE,
        });
    }
}

