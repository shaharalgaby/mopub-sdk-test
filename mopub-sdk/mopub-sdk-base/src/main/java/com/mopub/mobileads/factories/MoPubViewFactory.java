// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads.factories;

import android.content.Context;

import com.mopub.common.VisibleForTesting;
import com.mopub.mobileads.MoPubView;

public class MoPubViewFactory {
    protected static MoPubViewFactory instance = new MoPubViewFactory();

    @VisibleForTesting
    @Deprecated
    public static void setInstance(MoPubViewFactory factory) {
        instance = factory;
    }

    public static MoPubView create(Context context) {
        return instance.internalCreate(context);
    }

    protected MoPubView internalCreate(Context context) {
        return new MoPubView(context);
    }
}
