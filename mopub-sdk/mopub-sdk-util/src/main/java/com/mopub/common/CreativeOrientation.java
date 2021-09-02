// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Represents the orientation returned for MoPub ads from the MoPub ad server.
 */
public enum CreativeOrientation {
    PORTRAIT, LANDSCAPE, DEVICE;

    @NonNull
    public static CreativeOrientation fromString(@Nullable String orientation) {
        if ("l".equalsIgnoreCase(orientation)) {
            return LANDSCAPE;
        }

        if ("p".equalsIgnoreCase(orientation)) {
            return PORTRAIT;
        }

        return DEVICE;
    }
}
