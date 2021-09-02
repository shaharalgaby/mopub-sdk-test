// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

public enum MoPubErrorCode implements MoPubError {
    AD_SUCCESS("ad successfully loaded."),
    DO_NOT_TRACK("Do not track is enabled."),
    UNSPECIFIED("Unspecified error."),

    // Ad load server errors
    NO_FILL("No ads found."),
    WARMUP("Ad unit is warming up. Try again in a few minutes."),
    SERVER_ERROR("Unable to connect to MoPub adserver."),
    TOO_MANY_REQUESTS("Too many failed requests have been made. Please try again later."),

    // Client ad load errors
    INTERNAL_ERROR("Unable to serve ad due to invalid internal state."),
    RENDER_PROCESS_GONE_WITH_CRASH("Render process for this WebView has crashed."),
    RENDER_PROCESS_GONE_UNSPECIFIED("Render process is gone for this WebView. Unspecified cause."),
    CANCELLED("Ad request was cancelled."),
    MISSING_AD_UNIT_ID("Unable to serve ad due to missing or empty ad unit ID."),
    NO_CONNECTION("No internet connection detected."),

    ADAPTER_NOT_FOUND("Unable to find Native Network or ad adapter."),
    ADAPTER_CONFIGURATION_ERROR("Native Network or ad adapter was configured incorrectly."),
    ADAPTER_INITIALIZATION_SUCCESS("AdapterConfiguration initialization success."),

    /** see {@link com.mopub.common.Constants#AD_EXPIRATION_DELAY } */
    EXPIRED("Ad expired since it was not shown within 4 hours."),

    NETWORK_TIMEOUT("Third-party network failed to respond in a timely manner."),
    NETWORK_NO_FILL("Third-party network failed to provide an ad."),
    NETWORK_INVALID_STATE("Third-party network failed due to invalid internal state."),
    MRAID_LOAD_ERROR("Error loading MRAID ad."),
    HTML_LOAD_ERROR("Error loading MRAID ad."),
    INLINE_LOAD_ERROR("Error loading INLINE ad."),
    FULLSCREEN_LOAD_ERROR("Error loading FULLSCREEN ad."),
    INLINE_SHOW_ERROR("Error showing INLINE ad."),
    FULLSCREEN_SHOW_ERROR("Error showing FULLSCREEN ad."),
    VIDEO_CACHE_ERROR("Error creating a cache to store downloaded videos."),
    VIDEO_DOWNLOAD_ERROR("Error downloading video."),

    GDPR_DOES_NOT_APPLY("GDPR does not apply. Ignoring consent-related actions."),

    REWARDED_CURRENCIES_PARSING_ERROR("Error parsing rewarded currencies JSON header."),
    REWARD_NOT_SELECTED("Reward not selected for rewarded ad."),

    AD_NOT_AVAILABLE("No ad loaded for ad unit."),
    AD_SHOW_ERROR("Error showing an ad."),
    @Deprecated
    VIDEO_NOT_AVAILABLE("No video loaded for ad unit."),
    @Deprecated
    VIDEO_PLAYBACK_ERROR("Error playing a video.");

    private final String message;
    MoPubErrorCode(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return this.message;
    }

    @Override
    public int getIntCode() {
        switch (this) {
            case NETWORK_TIMEOUT:
                return ER_TIMEOUT;
            case ADAPTER_NOT_FOUND:
                return ER_ADAPTER_NOT_FOUND;
            case AD_SUCCESS:
                return ER_SUCCESS;
        }
        return ER_UNSPECIFIED;
    }
}
