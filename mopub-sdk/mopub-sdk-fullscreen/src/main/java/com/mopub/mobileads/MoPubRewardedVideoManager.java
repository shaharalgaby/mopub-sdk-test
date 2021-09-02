// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.app.Activity;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.MediationSettings;
import com.mopub.common.MoPub;

/**
 * Deprecated class that points to {@link MoPubRewardedAdManager} to minimize integration failures.
 */
@Deprecated
public class MoPubRewardedVideoManager {

    @Deprecated
    public static void init(@NonNull final Activity mainActivity, final MediationSettings... mediationSettings) {
        MoPubRewardedAdManager.init(mainActivity, mediationSettings);
    }

    @Deprecated
    public static void updateActivity(@Nullable final Activity activity) {
        MoPubRewardedAdManager.updateActivity(activity);
    }

    @Deprecated
    public static <T extends MediationSettings> T getGlobalMediationSettings(@NonNull final Class<T> clazz) {
        return MoPubRewardedAdManager.getGlobalMediationSettings(clazz);
    }

    @Deprecated
    public static <T extends MediationSettings> T getInstanceMediationSettings(
            @NonNull final Class<T> clazz, @NonNull final String adUnitId) {
        return MoPubRewardedAdManager.getInstanceMediationSettings(clazz, adUnitId);
    }

    @Deprecated
    public static void setRewardedVideoListener(@Nullable final MoPubRewardedVideoListener listener) {
        MoPubRewardedVideos.setRewardedVideoListener(listener);
    }

    @Deprecated
    public static final class RequestParameters {
        @Deprecated
        @Nullable
        public final String mKeywords;
        @Deprecated
        @Nullable
        public final String mUserDataKeywords;
        @Deprecated
        @Nullable
        public final Location mLocation;
        @Deprecated
        @Nullable
        public final String mCustomerId;

        @Deprecated
        public RequestParameters(@Nullable final String keywords) {
            this(keywords, null);
        }

        @Deprecated
        public RequestParameters(@Nullable final String keywords, @Nullable final String userDataKeywords) {
            this(keywords, userDataKeywords, null);
        }

        @Deprecated
        public RequestParameters(@Nullable final String keywords,
                                 @Nullable final String userDataKeywords,
                                 @Nullable final Location location) {
            this(keywords, userDataKeywords, location, null);
        }

        @Deprecated
        public RequestParameters(@Nullable final String keywords,
                                 @Nullable final String userDataKeywords,
                                 @Nullable final Location location,
                                 @Nullable final String customerId) {
            mKeywords = keywords;
            mCustomerId = customerId;

            // Only add userDataKeywords and location to RequestParameters if we are allowed to collect
            // personal information from a user
            final boolean canCollectPersonalInformation = MoPub.canCollectPersonalInformation();
            mUserDataKeywords = canCollectPersonalInformation ? userDataKeywords : null;
            mLocation = canCollectPersonalInformation ? location : null;
        }
    }
}
