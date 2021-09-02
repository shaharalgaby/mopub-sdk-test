// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import androidx.annotation.NonNull;

import com.mopub.common.MoPubReward;

import java.util.Set;

/**
 * Listener for rewarded video events. Implementers of this interface will receive events for all
 * rewarded video ad units in the app.
 * Deprecated in favor of {@link MoPubRewardedAdListener}.
 */
@Deprecated
public interface MoPubRewardedVideoListener {

    /**
     * Called when the adUnitId has loaded. At this point you should be able to call
     * {@link com.mopub.mobileads.MoPubRewardedAds#showRewardedAd(String)} to show the video.
     */
    @Deprecated
    public void onRewardedVideoLoadSuccess(@NonNull String adUnitId);

    /**
     * Called when a video fails to load for the given ad unit id. The provided error code will
     * give more insight into the reason for the failure to load.
     */
    @Deprecated
    public void onRewardedVideoLoadFailure(@NonNull String adUnitId, @NonNull MoPubErrorCode errorCode);

    /**
     * Called when a rewarded video starts playing.
     */
    @Deprecated
    public void onRewardedVideoStarted(@NonNull String adUnitId);

    /**
     * Called when there is an error during video playback.
     *
     * @deprecated MoPubRewardedAdListener#onRewardedAdShowError is the equivalent to this, though
     * the show error callback handles more than just video playback errors.
     */
    @Deprecated
    public void onRewardedVideoPlaybackError(@NonNull String adUnitId, @NonNull MoPubErrorCode errorCode);

    /**
     * Called when a rewarded video is clicked.
     */
    @Deprecated
    public void onRewardedVideoClicked(@NonNull String adUnitId);

    /**
     * Called when a rewarded video is closed. At this point your application should resume.
     */
    @Deprecated
    public void onRewardedVideoClosed(@NonNull String adUnitId);

    /**
     * Called when a rewarded video is completed and the user should be rewarded.
     */
    @Deprecated
    public void onRewardedVideoCompleted(@NonNull Set<String> adUnitIds, @NonNull MoPubReward reward);
}
