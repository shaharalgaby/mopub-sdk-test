// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.nativeads;

import android.content.Context;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.Constants;
import com.mopub.common.DataKeys;
import com.mopub.common.Preconditions;
import com.mopub.common.ViewabilityVendor;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.nativeads.factories.CustomEventNativeFactory;
import com.mopub.network.AdResponse;

import java.util.Map;
import java.util.Set;

import static com.mopub.common.logging.MoPubLog.SdkLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.SdkLogEvent.CUSTOM_WITH_THROWABLE;
import static com.mopub.mobileads.MoPubErrorCode.NETWORK_TIMEOUT;

final class CustomEventNativeAdapter {

    @NonNull
    private final Handler mHandler;
    @NonNull
    private final Runnable mTimeout;
    @Nullable
    private CustomEventNative customEventNative;
    @NonNull
    private CustomEventNative.CustomEventNativeListener mExternalListener;

    private volatile boolean mCompleted;

    CustomEventNativeAdapter(@NonNull final CustomEventNative.CustomEventNativeListener customEventNativeListener) {
        Preconditions.checkNotNull(customEventNativeListener);

        mExternalListener = customEventNativeListener;

        mCompleted = false;
        mHandler = new Handler();
        mTimeout = new Runnable() {
            @Override
            public void run() {
                if (mCompleted) {
                    return;
                }
                MoPubLog.log(CUSTOM, "CustomEventNativeAdapter() failed with code " +
                        NETWORK_TIMEOUT.getIntCode() + " and message " + NETWORK_TIMEOUT);
                stopLoading();
                mExternalListener.onNativeAdFailed(NativeErrorCode.NETWORK_TIMEOUT);
            }
        };
    }

    public void loadNativeAd(@NonNull final Context context,
                             @NonNull final Map<String, Object> localExtras,
                             @NonNull final AdResponse adResponse) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(localExtras);
        Preconditions.checkNotNull(adResponse);

        String customEventNativeClassName = adResponse.getCustomEventClassName();

        MoPubLog.log(CUSTOM, adResponse.getDspCreativeId());
        try {
            customEventNative = CustomEventNativeFactory.create(customEventNativeClassName);
        } catch (Exception e) {
            MoPubLog.log(CUSTOM, "loadNativeAd() failed with code " +
                    MoPubErrorCode.ADAPTER_NOT_FOUND.getIntCode() + " and message " +
                    MoPubErrorCode.ADAPTER_NOT_FOUND);
            mExternalListener.onNativeAdFailed(NativeErrorCode.NATIVE_ADAPTER_NOT_FOUND);
            return;
        }
        if (adResponse.hasJson()) {
            localExtras.put(DataKeys.JSON_BODY_KEY, adResponse.getJsonBody());
        }

        localExtras.put(DataKeys.CLICK_TRACKING_URL_KEY, adResponse.getClickTrackingUrls());

        final Set<ViewabilityVendor> viewabilityVendors = adResponse.getViewabilityVendors();
        if (viewabilityVendors != null) {
            localExtras.put(DataKeys.VIEWABILITY_VENDORS_KEY, viewabilityVendors);
        }

        // Custom event classes can be developed by any third party and may not be tested.
        // We catch all exceptions here to prevent crashes from untested code.
        try {
            customEventNative.loadNativeAd(
                    context,
                    createListener(),
                    localExtras,
                    adResponse.getServerExtras());

            long timeoutMS = adResponse.getAdTimeoutMillis(Constants.THIRTY_SECONDS_MILLIS);
            mHandler.postDelayed(mTimeout, timeoutMS);
        } catch (Exception e) {
            MoPubLog.log(CUSTOM, "loadNativeAd() failed with code " +
                    MoPubErrorCode.ADAPTER_NOT_FOUND.getIntCode() + " and message " +
                    MoPubErrorCode.ADAPTER_NOT_FOUND);
            mExternalListener.onNativeAdFailed(NativeErrorCode.NATIVE_ADAPTER_NOT_FOUND);
        }
    }

    @NonNull
    private CustomEventNative.CustomEventNativeListener createListener() {
        return new CustomEventNative.CustomEventNativeListener() {
            @Override
            public void onNativeAdLoaded(BaseNativeAd nativeAd) {
                if (mCompleted) {
                    return;
                }
                MoPubLog.log(CUSTOM, "onNativeAdLoaded");
                invalidate();
                mExternalListener.onNativeAdLoaded(nativeAd);
            }

            @Override
            public void onNativeAdFailed(NativeErrorCode errorCode) {
                if (mCompleted) {
                    return;
                }
                MoPubLog.log(CUSTOM, "onNativeAdFailed with code " +
                        errorCode.getIntCode() + " and message " + errorCode);
                invalidate();
                mExternalListener.onNativeAdFailed(errorCode);
            }
        };
    }

    void stopLoading() {
        try {
            if (customEventNative != null && mCompleted)
                customEventNative.onInvalidate();
        } catch (Exception e) {
            MoPubLog.log(CUSTOM_WITH_THROWABLE,  "", e);
        }
        invalidate();
    }

    private synchronized void invalidate() {
        if (!mCompleted) {
            mCompleted = true;
            mHandler.removeCallbacks(mTimeout);
            if (customEventNative != null) {
                try {
                    customEventNative.onInvalidate();
                } catch (Exception e) {
                    MoPubLog.log(CUSTOM,  e.toString());
                }
                customEventNative = null;
            }
        }
    }
}
