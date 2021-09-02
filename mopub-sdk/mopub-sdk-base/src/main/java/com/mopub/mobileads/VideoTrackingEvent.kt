// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads

/**
 * Internal Video Tracking events, defined in ad server
 */
enum class VideoTrackingEvent(val value: String) {
    START("start"),
    FIRST_QUARTILE("firstQuartile"),
    MIDPOINT("midpoint"),
    THIRD_QUARTILE("thirdQuartile"),
    COMPLETE("complete"),
    COMPANION_AD_VIEW("companionAdView"),
    COMPANION_AD_CLICK("companionAdClick"),
    UNKNOWN("");

    companion object {
        fun fromString(name: String?): VideoTrackingEvent {
            return values().find { it.value.equals(name, true) } ?: UNKNOWN
        }
    }

    fun toFloat(): Float {
        return when (this) {
            FIRST_QUARTILE -> 0.25f
            MIDPOINT -> 0.5f
            THIRD_QUARTILE -> 0.75f
            COMPLETE -> 1.0f
            else -> 0.0f
        }
    }
}
