// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.MediationSettings;
import com.mopub.common.MoPubReward;
import com.mopub.common.util.ReflectionTarget;

import java.util.Set;

/**
 * MoPubRewardedVideos is a utility class that holds controller methods for other MoPub rewarded
 * video classes.
 * This has been deprecated in favor of {@link MoPubRewardedAds}.
 */
@Deprecated
public class MoPubRewardedVideos {

    @ReflectionTarget
    public static void setRewardedVideoListener(@Nullable final MoPubRewardedVideoListener listener) {
        if (listener == null) {
            MoPubRewardedAds.setRewardedAdListener(null);
            return;
        }

        final MoPubRewardedAdListener rewardedAdListener = new MoPubRewardedAdListener() {
            @Override
            public void onRewardedAdCompleted(@NonNull Set<String> adUnitIds, @NonNull MoPubReward reward) {
                listener.onRewardedVideoCompleted(adUnitIds, reward);
            }

            @Override
            public void onRewardedAdClosed(@NonNull String adUnitId) {
                listener.onRewardedVideoClosed(adUnitId);
            }

            @Override
            public void onRewardedAdClicked(@NonNull String adUnitId) {
                listener.onRewardedVideoClicked(adUnitId);
            }

            @Override
            public void onRewardedAdShowError(@NonNull String adUnitId, @NonNull MoPubErrorCode errorCode) {
                listener.onRewardedVideoPlaybackError(adUnitId, errorCode);
            }

            @Override
            public void onRewardedAdStarted(@NonNull String adUnitId) {
                listener.onRewardedVideoStarted(adUnitId);
            }

            @Override
            public void onRewardedAdLoadFailure(@NonNull String adUnitId, @NonNull MoPubErrorCode errorCode) {
                listener.onRewardedVideoLoadFailure(adUnitId, errorCode);
            }

            @Override
            public void onRewardedAdLoadSuccess(@NonNull String adUnitId) {
                listener.onRewardedVideoLoadSuccess(adUnitId);
            }
        };
        MoPubRewardedAds.setRewardedAdListener(rewardedAdListener);
    }

    @ReflectionTarget
    public static void loadRewardedVideo(@NonNull String adUnitId,
            @Nullable MediationSettings... mediationSettings) {
        MoPubRewardedAds.loadRewardedAd(adUnitId, mediationSettings);
    }

    @ReflectionTarget
    public static void loadRewardedVideo(@NonNull String adUnitId,
            @Nullable MoPubRewardedVideoManager.RequestParameters requestParameters,
            @Nullable MediationSettings... mediationSettings) {
        MoPubRewardedAdManager.RequestParameters params = null;
        if (requestParameters != null) {
            params = new MoPubRewardedAdManager.RequestParameters(
                    requestParameters.mKeywords,
                    requestParameters.mUserDataKeywords,
                    requestParameters.mLocation,
                    requestParameters.mCustomerId);
        }
        MoPubRewardedAds.loadRewardedAd(adUnitId, params, mediationSettings);
    }

    @ReflectionTarget
    public static boolean hasRewardedVideo(@NonNull String adUnitId) {
        return MoPubRewardedAds.hasRewardedAd(adUnitId);
    }

    @ReflectionTarget
    public static void showRewardedVideo(@NonNull String adUnitId) {
        MoPubRewardedAds.showRewardedAd(adUnitId);
    }

    @ReflectionTarget
    public static void showRewardedVideo(@NonNull String adUnitId, @Nullable String customData) {
        MoPubRewardedAds.showRewardedAd(adUnitId, customData);
    }

    @ReflectionTarget
    public static Set<MoPubReward> getAvailableRewards(@NonNull String adUnitId) {
        return MoPubRewardedAds.getAvailableRewards(adUnitId);
    }

    @ReflectionTarget
    public static void selectReward(@NonNull String adUnitId, @NonNull MoPubReward selectedReward) {
        MoPubRewardedAds.selectReward(adUnitId, selectedReward);
    }
}
