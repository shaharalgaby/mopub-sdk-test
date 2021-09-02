// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common;

import android.content.Context;

/**
 * Called when Sdk initialization completes from
 * {@link MoPub#initializeSdk(Context, SdkConfiguration, SdkInitializationListener)}
 */
public interface SdkInitializationListener {
    void onInitializationFinished();
}
