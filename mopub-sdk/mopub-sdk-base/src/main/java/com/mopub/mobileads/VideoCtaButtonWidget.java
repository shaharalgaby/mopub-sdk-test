// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;

import com.mopub.common.VisibleForTesting;
import com.mopub.mobileads.resource.CtaButtonDrawable;

public class VideoCtaButtonWidget extends ImageView {
    @NonNull private CtaButtonDrawable mCtaButtonDrawable;

    private boolean mIsVideoComplete;
    private boolean mHasCompanionAd;
    private boolean mHasClickthroughUrl;

    public VideoCtaButtonWidget(Context context, AttributeSet attrs) {
        super(context, attrs, 0);

        mCtaButtonDrawable = new CtaButtonDrawable(context);
        setImageDrawable(mCtaButtonDrawable);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        updateLayoutAndVisibility();
    }

    void updateCtaText(@NonNull final String customCtaText) {
        mCtaButtonDrawable.setCtaText(customCtaText);
    }

    void notifyVideoComplete() {
        mIsVideoComplete = true;
        updateLayoutAndVisibility();
    }

    private void updateLayoutAndVisibility() {
        // If the video does not have a clickthrough url, never show the CTA button
        if (!mHasClickthroughUrl) {
            setVisibility(View.GONE);
            return;
        }

        // If video has finished playing and there's a companion ad, do not show CTA button
        if (mIsVideoComplete && mHasCompanionAd) {
            setVisibility(View.GONE);
            return;
        }

        setVisibility(View.VISIBLE);
    }

    void setHasCompanionAd(final boolean hasCompanionAd) {
        mHasCompanionAd = hasCompanionAd;
        updateLayoutAndVisibility();
    }

    void setHasClickthroughUrl(final boolean hasClickthroughUrl) {
        mHasClickthroughUrl = hasClickthroughUrl;
        updateLayoutAndVisibility();
    }

    // for testing
    @Deprecated
    @VisibleForTesting
    String getCtaText() {
        return mCtaButtonDrawable.getCtaText();
    }
}
