// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mraid;

import java.util.Locale;

public enum PlacementType {
    INLINE,
    INTERSTITIAL;

    String toJavascriptString() {
        return toString().toLowerCase(Locale.US);
    }
}
