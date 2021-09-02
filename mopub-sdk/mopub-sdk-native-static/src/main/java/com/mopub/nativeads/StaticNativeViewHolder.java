// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.nativeads;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;

import static com.mopub.common.logging.MoPubLog.SdkLogEvent.ERROR_WITH_THROWABLE;

class StaticNativeViewHolder {
    @Nullable View mainView;
    @Nullable TextView titleView;
    @Nullable TextView textView;
    @Nullable TextView callToActionView;
    @Nullable ImageView mainImageView;
    @Nullable ImageView iconImageView;
    @Nullable ImageView privacyInformationIconImageView;
    @Nullable TextView sponsoredTextView;

    @VisibleForTesting
    static final StaticNativeViewHolder EMPTY_VIEW_HOLDER = new StaticNativeViewHolder();

    // Use fromViewBinder instead of a constructor
    private StaticNativeViewHolder() {}

    @NonNull
    static StaticNativeViewHolder fromViewBinder(@NonNull final View view,
            @NonNull final ViewBinder viewBinder) {
        final StaticNativeViewHolder staticNativeViewHolder = new StaticNativeViewHolder();
        staticNativeViewHolder.mainView = view;
        try {
            staticNativeViewHolder.titleView = view.findViewById(viewBinder.titleId);
            staticNativeViewHolder.textView = view.findViewById(viewBinder.textId);
            staticNativeViewHolder.callToActionView = view.findViewById(viewBinder.callToActionId);
            staticNativeViewHolder.mainImageView = view.findViewById(viewBinder.mainImageId);
            staticNativeViewHolder.iconImageView = view.findViewById(viewBinder.iconImageId);
            staticNativeViewHolder.privacyInformationIconImageView =
                    view.findViewById(viewBinder.privacyInformationIconImageId);
            staticNativeViewHolder.sponsoredTextView = view.findViewById(viewBinder.sponsoredTextId);
            return staticNativeViewHolder;
        } catch (ClassCastException exception) {
            MoPubLog.log(ERROR_WITH_THROWABLE, "Could not cast from id in ViewBinder to expected " +
                    "View type", exception);
            return EMPTY_VIEW_HOLDER;
        }
    }
}
