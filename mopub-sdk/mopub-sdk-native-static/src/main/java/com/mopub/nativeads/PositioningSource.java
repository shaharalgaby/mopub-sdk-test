// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.nativeads;

import androidx.annotation.NonNull;

import com.mopub.nativeads.MoPubNativeAdPositioning.MoPubClientPositioning;

/**
 * Allows asynchronously requesting positioning information.
 */
interface PositioningSource {

    interface PositioningListener {
        void onLoad(@NonNull MoPubClientPositioning positioning);

        void onFailed();
    }

    void loadPositions(@NonNull String adUnitId, @NonNull PositioningListener listener);

}
