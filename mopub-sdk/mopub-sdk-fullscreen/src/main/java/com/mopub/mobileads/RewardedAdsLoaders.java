// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.AdFormat;
import com.mopub.common.Preconditions;
import com.mopub.common.VisibleForTesting;
import com.mopub.network.AdLoader;
import com.mopub.network.AdResponse;
import com.mopub.network.MoPubNetworkError;
import com.mopub.network.MoPubRequest;

import java.util.HashMap;
import java.util.Map;

class RewardedAdsLoaders {
    @NonNull private final HashMap<String, AdLoaderRewardedAd> mAdUnitToAdLoader;
    @NonNull private final MoPubRewardedAdManager moPubRewardedAdManager;

    public class RewardedAdRequestListener implements AdLoader.Listener {
        public final String adUnitId;

        RewardedAdRequestListener(String adUnitId) {
            this.adUnitId = adUnitId;
        }

        @Override
        public void onResponse(@NonNull final AdResponse response) {
            moPubRewardedAdManager.onAdSuccess(response);
        }

        @Override
        public void onErrorResponse(@NonNull final MoPubNetworkError networkError) {
            moPubRewardedAdManager.onAdError(networkError, adUnitId);
        }
    }

    RewardedAdsLoaders(@NonNull final MoPubRewardedAdManager rewardedAdManager){
        moPubRewardedAdManager = rewardedAdManager;
        mAdUnitToAdLoader = new HashMap<>();
    }

    @Nullable
    MoPubRequest<?> loadNextAd(@NonNull Context context,
                               @NonNull String adUnitId,
                               @NonNull String adUrlString,
                               @Nullable MoPubErrorCode errorCode) {
        Preconditions.checkNotNull(adUnitId);
        Preconditions.checkNotNull(context);

        AdLoaderRewardedAd adLoader = mAdUnitToAdLoader.get(adUnitId);

        if (adLoader == null || !adLoader.hasMoreAds()) {
            adLoader = new AdLoaderRewardedAd(adUrlString,
                                                 AdFormat.REWARDED_AD,
                                                 adUnitId,
                                                 context,
                                                 new RewardedAdRequestListener(adUnitId));
            mAdUnitToAdLoader.put(adUnitId, adLoader);
        }

        return adLoader.loadNextAd(errorCode);
    }

    boolean isLoading(@NonNull final String adUnitId) {
        return mAdUnitToAdLoader.containsKey(adUnitId) && mAdUnitToAdLoader.get(adUnitId).isRunning();
    }

    void markFail(@NonNull final String adUnitId) {
        Preconditions.checkNotNull(adUnitId);

        mAdUnitToAdLoader.remove(adUnitId);
    }

    void markPlayed(@NonNull final String adUnitId) {
        Preconditions.checkNotNull(adUnitId);

        mAdUnitToAdLoader.remove(adUnitId);
    }

    void onRewardedAdStarted(@NonNull String adUnitId, @NonNull Context context) {
        Preconditions.checkNotNull(adUnitId);
        Preconditions.checkNotNull(context);

        AdLoaderRewardedAd loaderRewardedAd = mAdUnitToAdLoader.get(adUnitId);
        if (loaderRewardedAd == null) {
            return;
        }

        loaderRewardedAd.trackImpression(context);
    }

    void onRewardedAdClicked(@NonNull String adUnitId, @NonNull Context context){
        Preconditions.checkNotNull(adUnitId);
        Preconditions.checkNotNull(context);

        AdLoaderRewardedAd loaderRewardedAd = mAdUnitToAdLoader.get(adUnitId);
        if (loaderRewardedAd == null) {
            return;
        }

        loaderRewardedAd.trackClick(context);
    }

    boolean canPlay(@NonNull final String adUnitId) {
        AdLoaderRewardedAd loaderRewardedAd = mAdUnitToAdLoader.get(adUnitId);
        if (loaderRewardedAd == null) {
            return false;
        }

        AdResponse adResponse =  loaderRewardedAd.getLastDeliveredResponse();
        return adResponse != null;
    }

    boolean hasMoreAds(@NonNull final String adUnitId) {
        AdLoaderRewardedAd loaderRewardedAd = mAdUnitToAdLoader.get(adUnitId);
        return loaderRewardedAd != null && loaderRewardedAd.hasMoreAds();
    }

    void creativeDownloadSuccess(@NonNull final String adUnitId){
        AdLoaderRewardedAd loaderRewardedAd = mAdUnitToAdLoader.get(adUnitId);
        if (loaderRewardedAd == null) {
            return;
        }

        loaderRewardedAd.creativeDownloadSuccess();
    }

    @Deprecated
    @VisibleForTesting
    void clearMapping() {
        mAdUnitToAdLoader.clear();
    }

    @Deprecated
    @VisibleForTesting
    Map<String, AdLoaderRewardedAd> getLoadersMap(){
        return mAdUnitToAdLoader;
    }
}
