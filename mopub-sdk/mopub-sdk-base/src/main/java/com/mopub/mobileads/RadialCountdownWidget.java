// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;

import com.mopub.common.VisibleForTesting;
import com.mopub.common.util.Dips;
import com.mopub.mobileads.resource.DrawableConstants;
import com.mopub.mobileads.resource.RadialCountdownDrawable;

public class RadialCountdownWidget extends ImageView {
    @NonNull private RadialCountdownDrawable mRadialCountdownDrawable;
    private int mLastProgressMilliseconds;

    public RadialCountdownWidget(Context context, AttributeSet attrs) {
        super(context, attrs, 0);

        mRadialCountdownDrawable = new RadialCountdownDrawable(context);
        setImageDrawable(mRadialCountdownDrawable);
    }

    public void calibrate(final int initialCountdownMilliseconds) {
        mRadialCountdownDrawable.setInitialCountdown(initialCountdownMilliseconds);
        setVisibility(INVISIBLE);
    }

    public void updateCountdownProgress(final int initialCountdownMilliseconds, final int currentProgressMilliseconds) {
        // There exists an Android video player bug where VideoView.getCurrentPosition()
        // temporarily returns 0 right after backgrounding and switching back to the app.
        // Therefore, we check against the last known current position to ensure that it's
        // monotonically increasing.
        if (currentProgressMilliseconds >= mLastProgressMilliseconds) {
            int millisecondsUntilSkippable = initialCountdownMilliseconds - currentProgressMilliseconds;

            // XXX
            // After backgrounding and switching back to the app,
            // this widget becomes erroneously visible.
            if (millisecondsUntilSkippable < 0) {
                setVisibility(GONE);
            } else {
                mRadialCountdownDrawable.updateCountdownProgress(currentProgressMilliseconds);
                mLastProgressMilliseconds = currentProgressMilliseconds;
            }
        }
    }

    // for testing
    @Deprecated
    @VisibleForTesting
    public RadialCountdownDrawable getImageViewDrawable() {
        return mRadialCountdownDrawable;
    }

    // for testing
    @Deprecated
    @VisibleForTesting
    public void setImageViewDrawable(RadialCountdownDrawable drawable) {
        mRadialCountdownDrawable = drawable;
    }
}
