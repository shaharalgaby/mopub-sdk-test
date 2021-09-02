// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common;

/**
 * Valid values for the "X-Adtype" header from the MoPub ad server. The value of this header
 * controls the base ad loading behavior.
 */
public class AdType {
    public static final String HTML = "html";
    public static final String MRAID = "mraid";
    public static final String INTERSTITIAL = "interstitial";
    public static final String STATIC_NATIVE = "json";
    public static final String REWARDED_VIDEO = "rewarded_video";
    public static final String REWARDED_PLAYABLE = "rewarded_playable";
    public static final String FULLSCREEN = "fullscreen";
    public static final String CUSTOM = "custom";
    public static final String CLEAR = "clear";
}
