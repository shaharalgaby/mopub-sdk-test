// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.mobileads.base.R;

/**
 * CloseableLayout provides a layout class that shows a close button, and allows setting a
 * {@link OnCloseListener}. Otherwise CloseableLayout behaves identically to
 * {@link FrameLayout}.
 * <p>
 * Rather than adding a button to the view tree, CloseableLayout is designed to draw the close
 * button directly on the canvas and to track MotionEvents on its close region. While
 * marginally more efficient, the main benefit to this is that CloseableLayout can function
 * exactly as a regular FrameLayout without needing to override addView, removeView,
 * removeAllViews, and a host of other methods.
 *
 * You can hide the close button using {@link #setCloseVisible}.
 */
public class CloseableLayout extends FrameLayout {
    public interface OnCloseListener {
        void onClose();
    }

    @Nullable
    private OnCloseListener mOnCloseListener;
  
    private final int mCloseRegionSize;  // Size of the touchable close region.

    @Nullable
    private ImageButton mCloseButton;

    private boolean mCloseAlwaysInteractable;

    @Nullable
    private UnsetPressedState mUnsetPressedState;

    public CloseableLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CloseableLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        LayoutInflater.from(context)
                .inflate(R.layout.closeable_layout, this, true);

        mCloseButton = findViewById(R.id.mopub_closeable_layout_close_button);
        mCloseButton.setOnClickListener(v -> onClosePressed() );

        mCloseButton.setOnLongClickListener(v -> {
            onClosePressed();
            return true;
        });

        mCloseRegionSize = getResources()
                .getDimensionPixelSize(R.dimen.closeable_layout_region_size);

        setWillNotDraw(false);
        mCloseAlwaysInteractable = true;
        final int blackColor = getResources().getColor(android.R.color.black);
        setBackgroundColor(blackColor);
    }

    public void setOnCloseListener(@Nullable OnCloseListener onCloseListener) {
        mOnCloseListener = onCloseListener;
    }

    public void setCloseVisible(boolean visible) {
        if (mCloseButton == null) {
            return;
        } else if (!visible) {
            mCloseButton.setVisibility(mCloseAlwaysInteractable ? INVISIBLE : GONE);
        } else {
            mCloseButton.setVisibility(VISIBLE);
        }
        mCloseButton.invalidate();
    }

    // These are essential for recognizing when the close button will be drawn offscreen
    public void applyCloseRegionBounds(Rect bounds, Rect closeBounds) {
        applyCloseBoundsWithSize(mCloseRegionSize, bounds, closeBounds);
    }

    private void applyCloseBoundsWithSize(final int size, Rect bounds, Rect outBounds) {
        Gravity.apply(Gravity.RIGHT|Gravity.TOP, size, size, bounds, outBounds);
    }

    @Override
    public void addView(final View child, final int index) {
        super.addView(child, index);
        // Always keep the close button above everything else
        bringChildToFront(mCloseButton);
    }

    @Override
    public void addView(final View child, final int width, final int height) {
        super.addView(child, width, height);
        // Always keep the close button above everything else
        bringChildToFront(mCloseButton);
    }

    @Override
    public void addView(final View child, final int index, final ViewGroup.LayoutParams params) {
        super.addView(child, index, params);
        // Always keep the close button above everything else
        bringChildToFront(mCloseButton);
    }

    @Override
    public void removeAllViews() {
        super.removeAllViews();

        final Context context = getContext();
        LayoutInflater.from(context)
                .inflate(R.layout.closeable_layout, this, true);

        mCloseButton = findViewById(R.id.mopub_closeable_layout_close_button);
        mCloseButton.setOnClickListener(v -> {
            mCloseButton.setEnabled(false);
            performClose();
            mUnsetPressedState = new UnsetPressedState();
            postDelayed(mUnsetPressedState, ViewConfiguration.getPressedStateDuration());
        });
    }

    /**
     * Sets it so that touch events are also valid when the button is not visible.
     *
     * @param closeAlwaysInteractable True if you want to allow touch events to an invisible button
     */
    public void setCloseAlwaysInteractable(boolean closeAlwaysInteractable) {
        mCloseAlwaysInteractable = closeAlwaysInteractable;
    }

    @VisibleForTesting
    boolean shouldAllowPress() {
        return mCloseAlwaysInteractable
                || mCloseButton == null
                || mCloseButton.getVisibility() == VISIBLE;
    }

    @VisibleForTesting
    boolean isClosePressed() {
        return mCloseButton != null && !mCloseButton.isEnabled();
    }

    private void onClosePressed() {
        mCloseButton.setEnabled(false);
        performClose();
        mUnsetPressedState = new UnsetPressedState();
        postDelayed(mUnsetPressedState, ViewConfiguration.getPressedStateDuration());
    }

    private void performClose() {
        playSoundEffect(SoundEffectConstants.CLICK);
        if (mOnCloseListener != null) {
            mOnCloseListener.onClose();
        }
    }

    /**
     * This is a copy of the UnsetPressedState pattern from Android's View.java, which is used
     * to unset the pressed state of a button after a delay.
     */
    private final class UnsetPressedState implements Runnable {
        public void run() {
            if (mCloseButton != null) {
                mCloseButton.setEnabled(true);
            }
        }
    }

    @VisibleForTesting
    public boolean isCloseVisible() {
        int result = GONE;
        if (mCloseButton != null) {
            result = mCloseButton.getVisibility();
        }
        return result == VISIBLE;
    }

    @VisibleForTesting
    public boolean clickCloseButton() {
        return mCloseButton != null && mCloseButton.callOnClick();
    }
}
