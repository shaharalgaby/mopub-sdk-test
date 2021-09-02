// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads.factories;

import android.content.Context;

import androidx.annotation.VisibleForTesting;

import com.mopub.mobileads.AdViewController;
import com.mopub.mobileads.MoPubAd;
import com.mopub.mobileads.MoPubView;

public class AdViewControllerFactory {
    protected static AdViewControllerFactory instance = new AdViewControllerFactory();

    @Deprecated
    @VisibleForTesting
    public static void setInstance(AdViewControllerFactory factory) {
        instance = factory;
    }

    public static AdViewController create(Context context, MoPubAd moPubAd) {
        return instance.internalCreate(context, moPubAd);
    }

    protected AdViewController internalCreate(Context context, MoPubAd moPubAd) {
        return new AdViewController(context, moPubAd);
    }
}
