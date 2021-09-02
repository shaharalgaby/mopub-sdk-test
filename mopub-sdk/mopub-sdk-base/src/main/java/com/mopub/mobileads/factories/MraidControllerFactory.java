// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads.factories;

import android.content.Context;

import androidx.annotation.NonNull;

import com.mopub.common.VisibleForTesting;
import com.mopub.mraid.MraidController;
import com.mopub.mraid.PlacementType;

public class MraidControllerFactory {
    protected static MraidControllerFactory instance = new MraidControllerFactory();

    @VisibleForTesting
    public static void setInstance(MraidControllerFactory factory) {
        instance = factory;
    }

    public static MraidController create(@NonNull final Context context,
                                         @NonNull final String dspCreativeId,
                                         @NonNull final PlacementType placementType) {
        return instance.internalCreate(context, dspCreativeId, placementType);
    }

    protected MraidController internalCreate(@NonNull final Context context,
                                             @NonNull final String dspCreativeId,
                                             @NonNull final PlacementType placementType) {
        return new MraidController(context, dspCreativeId, placementType);
    }
}
