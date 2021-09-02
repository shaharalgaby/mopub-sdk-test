// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common.logging;

import androidx.annotation.Nullable;

public interface MoPubLogger {

    void log(@Nullable String className, @Nullable String methodName,
             @Nullable String identifier, @Nullable String message);
}
