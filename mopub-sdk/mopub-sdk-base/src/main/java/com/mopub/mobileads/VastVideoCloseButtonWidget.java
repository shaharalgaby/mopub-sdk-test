// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.ImageUtils;
import com.mopub.mobileads.base.R;
import com.mopub.network.MoPubImageLoader;
import com.mopub.network.MoPubNetworkError;
import com.mopub.network.Networking;

import static com.mopub.common.logging.MoPubLog.SdkLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.SdkLogEvent.ERROR_WITH_THROWABLE;

public class VastVideoCloseButtonWidget extends RelativeLayout {
    @Nullable private TextView mTextView;
    @Nullable private ImageView mImageView;
    @NonNull private final MoPubImageLoader mImageLoader;
    private boolean mHasCustomImage;

    public VastVideoCloseButtonWidget(Context context, AttributeSet attrs) {
        super(context, attrs, 0);

        LayoutInflater.from(context)
                .inflate(R.layout.vast_video_close_button_widget, this, true);

        mImageLoader = Networking.getImageLoader(context);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mImageView = findViewById(R.id.mopub_vast_close_button_image_view);
        mTextView = findViewById(R.id.mopub_vast_close_button_text_view);
    }

    void updateCloseButtonText(@Nullable final String text) {
        if (mTextView != null) {
            mTextView.setText(text);
        }
    }

    void updateCloseButtonIcon(@NonNull final String imageUrl, @NonNull Context context) {
        mImageLoader.fetch(imageUrl, new MoPubImageLoader.ImageListener() {
            @Override
            public void onResponse(@NonNull final MoPubImageLoader.ImageContainer imageContainer,
                                   final boolean isImmediate) {
                Bitmap bitmap = imageContainer.getBitmap();
                if (bitmap != null && mImageView != null) {
                    mImageView.setImageBitmap(bitmap);
                    mHasCustomImage = true;
                } else {
                    MoPubLog.log(CUSTOM, String.format("%s returned null bitmap", imageUrl));
                }
            }

            @Override
            public void onErrorResponse(@NonNull final MoPubNetworkError networkError) {
                MoPubLog.log(ERROR_WITH_THROWABLE, "Failed to load image.", networkError);
            }
        }, ImageUtils.getMaxImageWidth(context));
    }

    void setOnTouchListenerToContent(@Nullable View.OnTouchListener onTouchListener) {
        if (mImageView != null) {
            mImageView.setOnTouchListener(onTouchListener);
        }

        if (mTextView != null) {
            mTextView.setOnTouchListener(onTouchListener);
        }
    }

    void notifyVideoComplete() {
        if (!mHasCustomImage && mImageView != null) {
            mImageView.setImageDrawable(
                    ContextCompat.getDrawable(getContext(), R.drawable.ic_mopub_close_button));
        }
    }

    // for testing
    @Deprecated
    @VisibleForTesting
    ImageView getImageView() {
        return mImageView;
    }

    // for testing
    @Deprecated
    @VisibleForTesting
    void setImageView(ImageView imageView) {
        mImageView = imageView;
    }

    // for testing
    @Deprecated
    @VisibleForTesting
    TextView getTextView() {
        return mTextView;
    }
}
