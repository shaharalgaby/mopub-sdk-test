// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.network;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * ImpressionListener is an interface to notify the application about ad impressions for all
 * ad formats.
 */
public interface ImpressionListener {
    /**
     * SDK will call method onImpression once the ad becomes visible for the first time.
     *
     * @param adUnitId  - ad unit ID of the ad.
     * @param impressionData - extended information about the ad including revenue per impression.
     *                       This value can be null if impression level revenue data is not enabled
     *                       for this MoPub account.
     */
    @AnyThread
    void onImpression(@NonNull String adUnitId, @Nullable ImpressionData impressionData);
}
