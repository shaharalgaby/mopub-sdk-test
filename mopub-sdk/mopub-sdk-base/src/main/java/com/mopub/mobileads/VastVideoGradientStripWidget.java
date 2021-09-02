// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.GradientDrawable;

import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.base.R;

import static com.mopub.common.logging.MoPubLog.SdkLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.SdkLogEvent.CUSTOM_WITH_THROWABLE;

public class VastVideoGradientStripWidget extends ImageView {
    private int mVisibilityForCompanionAd;
    private boolean mHasCompanionAd;
    private boolean mIsVideoComplete;
    private boolean mAlwaysVisibleDuringVideo;

    private int gradientStartColor;
    private int gradientEndColor;

    public VastVideoGradientStripWidget(Context context) {
        super(context);
    }

    public VastVideoGradientStripWidget(Context context, AttributeSet attrs) {
        super(context, attrs, 0);

        final GradientDrawable.Orientation gradientOrientation =
                getGradientOrientationFromAttributeSet(context,
                        attrs,
                        GradientDrawable.Orientation.TOP_BOTTOM);

        gradientStartColor = context.getResources().getColor(R.color.gradient_strip_start_color);
        gradientEndColor = context.getResources().getColor(R.color.gradient_strip_end_color);
        final GradientDrawable gradientDrawable = new GradientDrawable(gradientOrientation,
                new int[] { gradientStartColor, gradientEndColor });
        setImageDrawable(gradientDrawable);
    }

    void setVisibilityForCompanionAd(final int visibilityForCompanionAd) {
        mVisibilityForCompanionAd = visibilityForCompanionAd;
    }

    void setHasCompanionAd(final boolean hasCompanionAd) {
        mHasCompanionAd = hasCompanionAd;
    }

    void setAlwaysVisibleDuringVideo(final boolean alwaysVisibleDuringVideo) {
        mAlwaysVisibleDuringVideo = alwaysVisibleDuringVideo;
    }

    void notifyVideoComplete() {
        mIsVideoComplete = true;
        updateVisibility();
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        updateVisibility();
    }

    void setGradientOrientation(final GradientDrawable.Orientation gradientOrientation) {
        final GradientDrawable gradientDrawable = new GradientDrawable(gradientOrientation,
                new int[] { gradientStartColor, gradientEndColor });

        setImageDrawable(gradientDrawable);
    }

    void updateVisibility() {
        if (mIsVideoComplete) {
            if (mHasCompanionAd) {
                setVisibility(mVisibilityForCompanionAd);
            } else {
                setVisibility(View.GONE);
            }

            return;
        }

        if (mAlwaysVisibleDuringVideo) {
            setVisibility(View.VISIBLE);
            return;
        }

        final int currentOrientation = getResources().getConfiguration().orientation;

        switch (currentOrientation) {
            case Configuration.ORIENTATION_LANDSCAPE:
                setVisibility(View.VISIBLE);
                break;
            case Configuration.ORIENTATION_PORTRAIT:
                setVisibility(View.INVISIBLE);
                break;
            case Configuration.ORIENTATION_UNDEFINED:
                MoPubLog.log(CUSTOM, "Screen orientation undefined: do not show gradient strip widget");
                setVisibility(View.INVISIBLE);
                break;
            case Configuration.ORIENTATION_SQUARE:
                MoPubLog.log(CUSTOM, "Screen orientation is deprecated ORIENTATION_SQUARE: do not show gradient strip widget");
                setVisibility(View.INVISIBLE);
                break;
            default:
                MoPubLog.log(CUSTOM, "Unrecognized screen orientation: do not show gradient strip widget");
                setVisibility(View.INVISIBLE);
                break;
        }
    }

    private GradientDrawable.Orientation getGradientOrientationFromAttributeSet(
            final Context context,
            final AttributeSet attrs,
            final GradientDrawable.Orientation defaultGradientOrientation) {
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.VastVideoGradientStripWidget,
                0, 0);

        GradientDrawable.Orientation returnValue = defaultGradientOrientation;

        try {
            final int gradientOrientationInt = a.getInteger(
                    R.styleable.VastVideoGradientStripWidget_gradientOrientation,
                    defaultGradientOrientation.ordinal());
            returnValue = GradientDrawable.Orientation.values()[gradientOrientationInt];
        } catch (Resources.NotFoundException rnfe) {
            MoPubLog.log(CUSTOM_WITH_THROWABLE,
                    "Encountered a problem while setting the GradientDrawable.Orientation",
                    rnfe);
        } finally {
            a.recycle();
        }

        return returnValue;
    }

    @VisibleForTesting
    boolean getHasCompanionAd() {
        return mHasCompanionAd;
    }

    @VisibleForTesting
    boolean getAlwaysVisibleDuringVideo() {
        return mAlwaysVisibleDuringVideo;
    }
}
