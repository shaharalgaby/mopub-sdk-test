// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads.factories;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.mopub.common.Preconditions;
import com.mopub.mobileads.VastManager;

public class VastManagerFactory {
    protected static VastManagerFactory instance = new VastManagerFactory();

    public static VastManager create(@NonNull final Context context) {
        Preconditions.checkNotNull(context, "context cannot be null");
        return instance.internalCreate(context, true);
    }

    public static VastManager create(@NonNull final Context context, boolean preCacheVideo) {
        Preconditions.checkNotNull(context, "context cannot be null");
        return instance.internalCreate(context, preCacheVideo);
    }

    public VastManager internalCreate(@NonNull final Context context, boolean preCacheVideo) {
        Preconditions.checkNotNull(context, "context cannot be null");
        return new VastManager(context, preCacheVideo);
    }

    @Deprecated
    @VisibleForTesting
    public static void setInstance(VastManagerFactory factory) {
        instance = factory;
    }
}
