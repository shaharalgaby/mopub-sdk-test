// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads

import com.mopub.common.MoPubReward

/**
 * Listener for rewarded ad events. Implementers of this interface will receive events for all
 * rewarded ad units in the app.:
 */
interface MoPubRewardedAdListener {
    /**
     * Called when the adUnitId has loaded. At this point you should be able to call
     * [com.mopub.mobileads.MoPubRewardedAds.showRewardedAd] to show the rewarded ad.
     */
    fun onRewardedAdLoadSuccess(adUnitId: String)

    /**
     * Called when a rewarded ad fails to load for the given ad unit id. The provided error code will
     * give more insight into the reason for the failure to load.
     */
    fun onRewardedAdLoadFailure(
        adUnitId: String,
        errorCode: MoPubErrorCode
    )

    /**
     * Called when a rewarded ad starts.
     */
    fun onRewardedAdStarted(adUnitId: String)

    /**
     * Called when there is an error while a rewarded ad is showing.
     */
    fun onRewardedAdShowError(
        adUnitId: String,
        errorCode: MoPubErrorCode
    )

    /**
     * Called when a rewarded ad is clicked.
     */
    fun onRewardedAdClicked(adUnitId: String)

    /**
     * Called when a rewarded ad is closed. At this point your application should resume.
     */
    fun onRewardedAdClosed(adUnitId: String)

    /**
     * Called when a rewarded ad is completed and the user should be rewarded.
     */
    fun onRewardedAdCompleted(
        adUnitIds: Set<String?>,
        reward: MoPubReward
    )
}
