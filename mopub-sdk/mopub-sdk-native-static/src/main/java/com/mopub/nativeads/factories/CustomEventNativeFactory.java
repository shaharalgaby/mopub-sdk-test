// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.nativeads.factories;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.mopub.common.Preconditions;
import com.mopub.nativeads.CustomEventNative;
import com.mopub.nativeads.MoPubCustomEventNative;

import java.lang.reflect.Constructor;

public class CustomEventNativeFactory {
    protected static CustomEventNativeFactory instance = new CustomEventNativeFactory();

    public static CustomEventNative create(@Nullable final String className) throws Exception {
        if (className != null) {
            final Class<? extends CustomEventNative> nativeClass = Class.forName(className)
                    .asSubclass(CustomEventNative.class);
            return instance.internalCreate(nativeClass);
        } else {
            return new MoPubCustomEventNative();
        }
    }

    @Deprecated
    @VisibleForTesting
    public static void setInstance(
            @NonNull final CustomEventNativeFactory customEventNativeFactory) {
        Preconditions.checkNotNull(customEventNativeFactory);

        instance = customEventNativeFactory;
    }

    @NonNull
    protected CustomEventNative internalCreate(
            @NonNull final Class<? extends CustomEventNative> nativeClass) throws Exception {
        Preconditions.checkNotNull(nativeClass);

        final Constructor<?> nativeConstructor = nativeClass.getDeclaredConstructor((Class[]) null);
        nativeConstructor.setAccessible(true);
        return (CustomEventNative) nativeConstructor.newInstance();
    }
}
