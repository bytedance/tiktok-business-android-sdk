package com.example.testdata;

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
