// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.AdFormat;
import com.mopub.common.Preconditions;
import com.mopub.network.AdLoader;
import com.mopub.network.AdResponse;
import com.mopub.network.SingleImpression;
import com.mopub.network.TrackingRequest;

import java.util.Collections;
import java.util.List;

class AdLoaderRewardedAd extends AdLoader {
    private boolean mImpressionTrackerFired;
    private boolean mClickTrackerFired;

    AdLoaderRewardedAd(@NonNull String url,
                       @NonNull AdFormat adFormat,
                       @NonNull String adUnitId,
                       @NonNull Context context,
                       @NonNull Listener listener) {
        super(url, adFormat, adUnitId, context, listener);

        mImpressionTrackerFired = false;
        mClickTrackerFired = false;
    }

    @Nullable
    String getFailurl() {
        if (mMultiAdResponse != null) {
            return mMultiAdResponse.getFailURL();
        }
        return null;
    }

    @NonNull
    List<String> getImpressionUrls() {
        if (mLastDeliveredResponse != null) {
            return mLastDeliveredResponse.getImpressionTrackingUrls();
        }
        return Collections.emptyList();
    }

    @NonNull
    List<String> getClickUrls() {
        if (mLastDeliveredResponse != null) {
            return mLastDeliveredResponse.getClickTrackingUrls();
        }
        return Collections.emptyList();
    }

    @Nullable
    AdResponse getLastDeliveredResponse() {
        return mLastDeliveredResponse;
    }

    void trackImpression(@NonNull Context context) {
        Preconditions.checkNotNull(context);

        if (mLastDeliveredResponse == null || mImpressionTrackerFired) {
            return;
        }

        mImpressionTrackerFired = true;
        TrackingRequest.makeTrackingHttpRequest(
                getImpressionUrls(),
                context);

        final String adUnitId = mLastDeliveredResponse.getAdUnitId();
        new SingleImpression(adUnitId, mLastDeliveredResponse.getImpressionData()).sendImpression();
    }

    void trackClick(@NonNull Context context) {
        Preconditions.checkNotNull(context);

        if (mLastDeliveredResponse == null || mClickTrackerFired) {
            return;
        }

        mClickTrackerFired = true;
        TrackingRequest.makeTrackingHttpRequest(
                getClickUrls(),
                context);
    }
}
