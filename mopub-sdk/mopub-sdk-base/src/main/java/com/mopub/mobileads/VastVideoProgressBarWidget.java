// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.content.Context;
import androidx.annotation.NonNull;

import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.mopub.common.VisibleForTesting;
import com.mopub.common.util.Dips;
import com.mopub.common.util.Utils;
import com.mopub.mobileads.resource.DrawableConstants;
import com.mopub.mobileads.resource.ProgressBarDrawable;

public class VastVideoProgressBarWidget extends ImageView {
    @NonNull private ProgressBarDrawable mProgressBarDrawable;

    public VastVideoProgressBarWidget(Context context, AttributeSet attrs) {
        super(context, attrs, 0);

        mProgressBarDrawable = new ProgressBarDrawable(context);
        setImageDrawable(mProgressBarDrawable);
    }

    public void calibrateAndMakeVisible(final int duration, final int skipOffset) {
        mProgressBarDrawable.setDurationAndSkipOffset(duration, skipOffset);
        setVisibility(View.VISIBLE);
    }

    public void updateProgress(final int progress) {
        mProgressBarDrawable.setProgress(progress);
    }

    public void reset() {
        mProgressBarDrawable.reset();
        mProgressBarDrawable.setProgress(0);
    }

    // for testing
    @Deprecated
    @VisibleForTesting
    ProgressBarDrawable getImageViewDrawable() {
        return mProgressBarDrawable;
    }

    // for testing
    @Deprecated
    @VisibleForTesting
    void setImageViewDrawable(@NonNull ProgressBarDrawable drawable) {
        mProgressBarDrawable = drawable;
    }
}
