package com.tiktok.util;

public class TTConst {
    public static final String TTSDK_KEY_VALUE_STORE = "com.tiktok.sdk.keystore";

    public static final String TTSDK_APP_VERSION = "com.tiktok.sdk.appVersion";
    public static final String TTSDK_APP_BUILD = "com.tiktok.sdk.appBuild";
    public static final String TTSDK_APP_LAST_LAUNCH = "com.tiktok.sdk.lastLaunch";
    public static final String TTSDK_APP_2DRENTION_LOGGED = "com.tiktok.sdk.2DretentionLogged";

    public static final String TTSDK_SKU_DETAILS = "com.tiktok.sdk.skuDetails";

    // https://bytedance.feishu.cn/docs/doccn8N5iGw2DGesf1O96KvCS9f#
    public static enum AppEventName {
        LaunchApp,
        InstallApp,
        TwoDayRetention("2Dretention"),
        AddPaymentInfo,
        AddToCart,
        AddToWishlist,
        Checkout,
        CompleteTutorial,
        ViewContent,
        CreateGroup,
        CreateRole,
        GenerateLead,
        InAppADClick,
        InAppADImpr,
        JoinGroup,
        AchieveLevel,
        LoanApplication,
        LoanApproval,
        LoanDisbursal,
        Login,
        Purchase,
        Registration,
        @Deprecated
        Rate,
        Search,
        SpendCredits,
        StartTrial,
        Subscribe,
        Share,
        Contact,
        UnlockAchievement,
        InternalTest;

        private String alias;

        private AppEventName() {
        }

        private AppEventName(String name) {
            this.alias = name;
        }

        @Override
        public String toString() {
            if (this.alias != null) {
                return this.alias;
            }
            return super.toString();
        }
    }
}