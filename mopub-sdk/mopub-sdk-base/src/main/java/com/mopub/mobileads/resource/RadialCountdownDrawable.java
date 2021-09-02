// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads.resource;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import androidx.annotation.NonNull;

import com.mopub.common.VisibleForTesting;
import com.mopub.common.util.Dips;
import com.mopub.common.util.Numbers;
import com.mopub.mobileads.base.R;

public class RadialCountdownDrawable extends BaseWidgetDrawable {
    @NonNull private final Paint mBackgroundPaint;
    @NonNull private final Paint mCirclePaint;
    @NonNull private final Paint mArcPaint;
    @NonNull private final Paint mTextPaint;
    @NonNull private Rect mTextRect;

    private final int mCircleStrokeWidth;
    private int mInitialCountdownMilliseconds;
    private int mSecondsRemaining;
    private float mSweepAngle;

    public RadialCountdownDrawable(@NonNull final Context context) {
        mCircleStrokeWidth = Dips.dipsToIntPixels(DrawableConstants.RadialCountdown.CIRCLE_STROKE_WIDTH_DIPS, context);
        final float textSizePixels = context.getResources()
                .getDimensionPixelSize(R.dimen.radial_countdown_text_size);

        // Background
        mBackgroundPaint = new Paint();
        mBackgroundPaint.setColor(DrawableConstants.RadialCountdown.BACKGROUND_COLOR);
        mBackgroundPaint.setStyle(DrawableConstants.RadialCountdown.BACKGROUND_STYLE);
        mBackgroundPaint.setAntiAlias(true);

        // Unfilled progress
        mCirclePaint = new Paint();
        mCirclePaint.setColor(DrawableConstants.RadialCountdown.PROGRESS_CIRCLE_COLOR);
        mCirclePaint.setAlpha(DrawableConstants.RadialCountdown.PROGRESS_CIRCLE_ALPHA);
        mCirclePaint.setStyle(DrawableConstants.RadialCountdown.PROGRESS_CIRCLE_STYLE);
        mCirclePaint.setStrokeWidth(mCircleStrokeWidth);
        mCirclePaint.setAntiAlias(true);

        // Filled progress
        mArcPaint = new Paint();
        mArcPaint.setColor(DrawableConstants.RadialCountdown.PROGRESS_ARC_COLOR);
        mArcPaint.setAlpha(DrawableConstants.RadialCountdown.PROGRESS_ARC_ALPHA);
        mArcPaint.setStyle(DrawableConstants.RadialCountdown.PROGRESS_ARC_STYLE);
        mArcPaint.setStrokeWidth(mCircleStrokeWidth);
        mArcPaint.setAntiAlias(true);

        // Countdown number text
        mTextPaint = new Paint();
        mTextPaint.setColor(DrawableConstants.RadialCountdown.TEXT_COLOR);
        mTextPaint.setTextAlign(DrawableConstants.RadialCountdown.TEXT_ALIGN);
        mTextPaint.setTextSize(textSizePixels);
        mTextPaint.setAntiAlias(true);

        mTextRect = new Rect();
    }

    @Override
    public void draw(final Canvas canvas) {
        final int centerX = getBounds().centerX();
        final int centerY = getBounds().centerY();
        final int radius = Math.min(centerX, centerY);
        // stroke position is center by default so we need to offset the background radius by stroke width / 2
        final int backgroundRadius = radius + (mCircleStrokeWidth / 2);

        canvas.drawCircle(centerX, centerY, backgroundRadius, mBackgroundPaint);
        canvas.drawCircle(centerX, centerY, radius, mCirclePaint);

        final String secondsRemainingText = String.valueOf(mSecondsRemaining);
        drawTextWithinBounds(canvas, mTextPaint, mTextRect, secondsRemainingText);

        final RectF circle = new RectF(getBounds());
        canvas.drawArc(circle, DrawableConstants.RadialCountdown.START_ANGLE, mSweepAngle, false, mArcPaint);
    }

    public void setInitialCountdown(final int initialCountdownMilliseconds) {
        mInitialCountdownMilliseconds = initialCountdownMilliseconds;
    }

    public void updateCountdownProgress(final int currentProgressMilliseconds) {
        int remainingCountdownMilliseconds = mInitialCountdownMilliseconds - currentProgressMilliseconds;
        mSecondsRemaining = (int) Numbers.convertMillisecondsToSecondsRoundedUp(remainingCountdownMilliseconds);
        mSweepAngle = 360f * currentProgressMilliseconds / mInitialCountdownMilliseconds;
        invalidateSelf();
    }

    // for testing
    @Deprecated
    @VisibleForTesting
    public int getInitialCountdownMilliseconds() {
        return mInitialCountdownMilliseconds;
    }
}
