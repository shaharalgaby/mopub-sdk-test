// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common.factories;

import androidx.annotation.VisibleForTesting;

import static com.mopub.common.util.Reflection.MethodBuilder;

public class MethodBuilderFactory {
    protected static MethodBuilderFactory instance = new MethodBuilderFactory();

    @Deprecated
    @VisibleForTesting
    public static void setInstance(MethodBuilderFactory factory) {
        instance = factory;
    }

    public static MethodBuilder create(Object object, String methodName) {
        return instance.internalCreate(object, methodName);
    }

    protected MethodBuilder internalCreate(Object object, String methodName) {
        return new MethodBuilder(object, methodName);
    }
}

