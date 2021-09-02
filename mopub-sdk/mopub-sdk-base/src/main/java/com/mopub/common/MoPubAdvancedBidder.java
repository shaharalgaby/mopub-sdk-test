// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common;

import android.content.Context;

/**
 * Interface for all advanced bidders.
 */
public interface MoPubAdvancedBidder {
    public String getToken(Context context);
    public String getCreativeNetworkName();
}
