// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads.test.support;

import android.content.Context;

import androidx.annotation.NonNull;

import com.mopub.common.Preconditions;
import com.mopub.mobileads.VastManager;
import com.mopub.mobileads.factories.VastManagerFactory;

import static org.mockito.Mockito.mock;

public class TestVastManagerFactory extends VastManagerFactory {
    private VastManager mockVastManager = mock(VastManager.class);

    public static VastManager getSingletonMock() {
        return getTestFactory().mockVastManager;
    }

    private static TestVastManagerFactory getTestFactory() {
        return (TestVastManagerFactory) instance;
    }

    @Override
    public VastManager internalCreate(@NonNull final Context context, final boolean preCacheVideo) {
        Preconditions.checkNotNull(context, "context cannot be null");
        return getTestFactory().mockVastManager;
    }
}
