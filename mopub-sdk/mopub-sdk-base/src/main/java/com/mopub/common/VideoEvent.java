// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common;

public enum VideoEvent {
    AD_PAUSED,
    AD_RESUMED,
    AD_SKIPPED,
    AD_IMPRESSED,

    AD_BUFFER_START, // playback paused due to buffering
    AD_BUFFER_END, // playback resumes after buffering

    AD_VIDEO_FIRST_QUARTILE,
    AD_VIDEO_MIDPOINT,
    AD_VIDEO_THIRD_QUARTILE,
    AD_COMPLETE,

    AD_FULLSCREEN,
    AD_NORMAL,

    AD_VOLUME_CHANGE,
    AD_CLICK_THRU,
    // mapped to other event
    RECORD_AD_ERROR
}
