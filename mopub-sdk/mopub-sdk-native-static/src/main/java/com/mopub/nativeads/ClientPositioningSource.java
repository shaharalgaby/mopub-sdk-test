// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.nativeads;

import android.os.Handler;
import androidx.annotation.NonNull;

import com.mopub.nativeads.MoPubNativeAdPositioning.MoPubClientPositioning;

/**
 * Returns a preset client positioning object.
 */
class ClientPositioningSource implements PositioningSource {
    @NonNull private final Handler mHandler = new Handler();
    @NonNull private final MoPubClientPositioning mPositioning;

    ClientPositioningSource(@NonNull MoPubClientPositioning positioning) {
        mPositioning = MoPubNativeAdPositioning.clone(positioning);
    }

    @Override
    public void loadPositions(@NonNull final String adUnitId,
            @NonNull final PositioningListener listener) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                listener.onLoad(mPositioning);
            }
        });
    }
}
