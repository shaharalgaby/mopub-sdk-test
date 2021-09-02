// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads

import android.app.Activity
import com.mopub.common.MediationSettings
import com.mopub.common.MoPubReward
import com.mopub.common.SdkConfiguration
import com.mopub.common.util.ReflectionTarget

/**
 * MoPubRewardedAds is a utility class that holds controller methods for other MoPub rewarded
 * ad classes.
 */
object MoPubRewardedAds {
    /**
     * This should only be called from MoPub#initializeSdk.
     */
    @ReflectionTarget
    @JvmStatic
    @Suppress("unused")
    private fun initializeRewardedAds(activity: Activity, sdkConfiguration: SdkConfiguration) {
        MoPubRewardedAdManager.init(activity, *sdkConfiguration.mediationSettings)
    }

    /**
     * Sets the {@link MoPubRewardedAdListener} that will receive events from the
     * rewarded ads system. Set this to null to stop receiving event callbacks.
     */
    @ReflectionTarget
    @JvmStatic
    fun setRewardedAdListener(listener: MoPubRewardedAdListener?) {
        MoPubRewardedAdManager.setRewardedAdListener(listener)
    }

    /**
     * Loads a rewarded ad.
     *
     * @param adUnitId Ad unit to load the rewarded ad
     * @param mediationSettings Optional instance mediation settings valid only for this request
     */
    @ReflectionTarget
    @JvmStatic
    fun loadRewardedAd(
        adUnitId: String,
        vararg mediationSettings: MediationSettings?
    ) {
        MoPubRewardedAdManager.loadAd(adUnitId, null, *mediationSettings)
    }

    /**
     * Loads a rewarded ad.
     *
     * @param adUnitId Ad unit to load the rewarded ad
     * @param requestParameters Optional request parameters
     * @param mediationSettings Optional instance mediation settings valid only for this request
     */
    @ReflectionTarget
    @JvmStatic
    fun loadRewardedAd(
        adUnitId: String,
        requestParameters: MoPubRewardedAdManager.RequestParameters?,
        vararg mediationSettings: MediationSettings?
    ) {
        MoPubRewardedAdManager.loadAd(adUnitId, requestParameters, *mediationSettings)
    }

    /**
     * Returns whether or not there is an ad ready for a given ad unit id.
     *
     * @param adUnitId Ad unit to check rewarded ad status
     * @return True if there is an ad ready to show, false if there is no ad ready to show.
     */
    @ReflectionTarget
    @JvmStatic
    fun hasRewardedAd(adUnitId: String): Boolean {
        return MoPubRewardedAdManager.hasAd(adUnitId)
    }

    /**
     * Shows the rewarded ad associated with the ad unit.
     *
     * @param adUnitId Ad unit to show the rewarded ad
     */
    @ReflectionTarget
    @JvmStatic
    fun showRewardedAd(adUnitId: String) {
        MoPubRewardedAdManager.showAd(adUnitId)
    }

    /**
     * Shows the rewarded ad associated with the ad unit. Can optionally provide some custom data
     * when showing the rewarded ad.
     *
     * @param adUnitId Ad unit to show the rewarded ad
     * @param customData Optional String data to give to the rewarded ad
     */
    @ReflectionTarget
    @JvmStatic
    fun showRewardedAd(adUnitId: String, customData: String?) {
        MoPubRewardedAdManager.showAd(adUnitId, customData)
    }

    /**
     * Gets all the available rewards associated with a particular ad unit.
     *
     * @param adUnitId Ad unit to get all the rewards that are available
     * @return Set of MoPubReward that are available rewards
     */
    @ReflectionTarget
    @JvmStatic
    fun getAvailableRewards(adUnitId: String): Set<MoPubReward> {
        return MoPubRewardedAdManager.getAvailableRewards(adUnitId)
    }

    /**
     * Choose a particular reward for a specific ad unit. Note that if a reward not in the available
     * rewards set is chosen then no reward will be set.
     *
     * @param adUnitId Ad unit to specify which reward should be chosen
     * @param selectedReward The chosen reward
     */
    @ReflectionTarget
    @JvmStatic
    fun selectReward(adUnitId: String, selectedReward: MoPubReward) {
        MoPubRewardedAdManager.selectReward(adUnitId, selectedReward)
    }
}
