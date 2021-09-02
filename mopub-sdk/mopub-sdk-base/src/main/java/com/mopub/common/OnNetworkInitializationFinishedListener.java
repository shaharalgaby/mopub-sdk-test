// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common;

import androidx.annotation.NonNull;

import com.mopub.mobileads.MoPubErrorCode;

public interface OnNetworkInitializationFinishedListener {
    void onNetworkInitializationFinished(@NonNull final Class<? extends AdapterConfiguration> clazz,
            @NonNull final MoPubErrorCode moPubErrorCode);
}
