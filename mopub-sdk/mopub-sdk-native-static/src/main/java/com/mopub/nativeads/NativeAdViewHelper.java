// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.nativeads;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;

import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;

import java.util.WeakHashMap;

import static com.mopub.common.logging.MoPubLog.SdkLogEvent.CUSTOM;

/**
 * Use {@link MoPubStaticNativeAdRenderer} instead
 */
class NativeAdViewHelper {
    private NativeAdViewHelper() {
    }

    @VisibleForTesting
    enum ViewType {
        EMPTY,
        AD
    }

    /**
     * Used to keep track of the last {@link NativeAd} a view was associated with in order to clean
     * up its state before associating with a new {@link NativeAd}
     */
    static private final WeakHashMap<View, NativeAd> sNativeAdMap =
            new WeakHashMap<>();

    @NonNull
    static View getAdView(@Nullable View convertView,
            @Nullable final ViewGroup parent,
            @NonNull final Context context,
            @Nullable final NativeAd nativeAd) {

        if (convertView != null) {
            clearNativeAd(convertView);
        }

        if (nativeAd == null || nativeAd.isDestroyed()) {
            MoPubLog.log(CUSTOM, "NativeAd null or invalid. Returning empty view");
            // Only create a view if one hasn't been created already
            if (convertView == null || !ViewType.EMPTY.equals(convertView.getTag())) {
                convertView = new View(context);
                convertView.setTag(ViewType.EMPTY);
                convertView.setVisibility(View.GONE);
            }
        } else {
            // Only create a view if one hasn't been created already
            if (convertView == null || !ViewType.AD.equals(convertView.getTag())) {
                convertView = nativeAd.createAdView(context, parent);
                convertView.setTag(ViewType.AD);
            }
            prepareNativeAd(convertView, nativeAd);
            nativeAd.renderAdView(convertView);
        }

        return convertView;
    }

    private static void clearNativeAd(@NonNull final View view) {
        final NativeAd nativeAd = sNativeAdMap.get(view);
        if (nativeAd != null) {
            nativeAd.clear(view);
        }
    }

    private static void prepareNativeAd(@NonNull final View view,
            @NonNull final NativeAd nativeAd) {
        sNativeAdMap.put(view, nativeAd);
        nativeAd.prepare(view);
    }
}
