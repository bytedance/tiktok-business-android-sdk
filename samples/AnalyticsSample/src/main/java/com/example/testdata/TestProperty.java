package com.example.testdata;

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
