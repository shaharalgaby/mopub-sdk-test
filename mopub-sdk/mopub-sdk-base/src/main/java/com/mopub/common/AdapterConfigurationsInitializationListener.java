// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common;

import androidx.annotation.NonNull;

import java.util.Map;

interface AdapterConfigurationsInitializationListener extends OnNetworkInitializationFinishedListener{
    void onAdapterConfigurationsInitialized(
            @NonNull final Map<String, AdapterConfiguration> adapterConfigurations);
}
