// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.nativeads;

import android.os.SystemClock;
import androidx.annotation.NonNull;

class TimestampWrapper<T> {
    @NonNull final T mInstance;
    long mCreatedTimestamp;

    TimestampWrapper(@NonNull final T instance) {
        mInstance = instance;
        mCreatedTimestamp = SystemClock.uptimeMillis();
    }
}
