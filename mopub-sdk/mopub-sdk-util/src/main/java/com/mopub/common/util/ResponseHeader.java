// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common.util;

public enum ResponseHeader {
    BACKOFF_REASON("backoff_reason"),
    BACKOFF_MS("backoff_ms"),
    AD_TIMEOUT("x-ad-timeout-ms"),
    AD_TYPE("x-adtype"),
    AD_GROUP_ID("x-adgroupid"),
    ADUNIT_FORMAT("adunit-format"),
    IMPRESSION_DATA("impdata"),
    CLICK_TRACKING_URL("clicktrackers"),
    CUSTOM_EVENT_DATA("x-custom-event-class-data"),
    CUSTOM_EVENT_NAME("x-custom-event-class-name"),
    CREATIVE_ID("x-creativeid"),
    DSP_CREATIVE_ID("x-dspcreativeid"),
    FAIL_URL("x-next-url"),
    FULL_AD_TYPE("x-fulladtype"),
    HEIGHT("x-height"),
    IMPRESSION_URL("x-imptracker"),
    IMPRESSION_URLS("imptrackers"),
    NATIVE_PARAMS("x-nativeparams"),
    NETWORK_TYPE("x-networktype"),
    ORIENTATION("x-orientation"),
    REFRESH_TIME("x-refreshtime"),
    WARMUP("x-warmup"),
    WIDTH("x-width"),
    BACKFILL("x-backfill"),
    REQUEST_ID("x-request-id"),
    CREATIVE_EXPERIENCE_SETTINGS("creative_experience_settings"),
    REWARDED("rewarded"),

    // HTTP headers
    CONTENT_TYPE("content-type"),
    LOCATION("location"),
    USER_AGENT("user-agent"),
    ACCEPT_LANGUAGE("accept-language"),

    BROWSER_AGENT("x-browser-agent"),

    // Banner impression tracking fields
    BANNER_IMPRESSION_MIN_VISIBLE_DIPS("x-banner-impression-min-pixels"),
    BANNER_IMPRESSION_MIN_VISIBLE_MS("x-banner-impression-min-ms"),

    // Native fields
    IMPRESSION_MIN_VISIBLE_PERCENT("x-impression-min-visible-percent"),
    IMPRESSION_VISIBLE_MS("x-impression-visible-ms"),
    IMPRESSION_MIN_VISIBLE_PX("x-native-impression-min-px"),

    // Rewarded Ad fields
    REWARDED_VIDEO_CURRENCY_NAME("x-rewarded-video-currency-name"),
    REWARDED_VIDEO_CURRENCY_AMOUNT("x-rewarded-video-currency-amount"),
    REWARDED_CURRENCIES("x-rewarded-currencies"),
    REWARDED_VIDEO_COMPLETION_URL("x-rewarded-video-completion-url"),

    // Internal Video Trackers
    VIDEO_TRACKERS("x-video-trackers"),

    // Viewability fields
    DISABLE_VIEWABILITY("x-disable-viewability"),
    VIEWABILITY_VERIFICATION("viewability-verification-resources"),

    // Client-side Waterfall
    AD_RESPONSES("ad-responses"),
    CONTENT("content"),
    METADATA("metadata"),

    BEFORE_LOAD_URL("x-before-load-url"),
    AFTER_LOAD_URL("x-after-load-url"),
    AFTER_LOAD_SUCCESS_URL("x-after-load-success-url"),
    AFTER_LOAD_FAIL_URL("x-after-load-fail-url"),

    // Consent fields
    INVALIDATE_CONSENT("invalidate_consent"),
    FORCE_EXPLICIT_NO("force_explicit_no"),
    REACQUIRE_CONSENT("reacquire_consent"),
    CONSENT_CHANGE_REASON("consent_change_reason"),
    FORCE_GDPR_APPLIES("force_gdpr_applies"),

    // Enable logging with rewrite
    ENABLE_DEBUG_LOGGING("enable_debug_logging"),

    // Experiment keys
    VAST_CLICK_ENABLED("vast-click-enabled");

    private final String key;
    ResponseHeader(String key) {
        this.key = key;
    }

    public String getKey() {
        return this.key;
    }
}

