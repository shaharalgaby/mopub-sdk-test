// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common;

import androidx.annotation.NonNull;

/**
 * Application engine information is optional data that is sent to the MoPub server
 * and used for analytics purposes
 */
public final class AppEngineInfo {
    @NonNull
    final String mName;
    @NonNull
    final String mVersion;

    /**
     *
     * @param name application engine name, for example "unity"
     * @param version application engine version, for example "2017.1.2f2"
     */
    public AppEngineInfo(@NonNull String name, @NonNull String version) {
        Preconditions.checkNotNull(name);
        Preconditions.checkNotNull(version);

        mName = name;
        mVersion = version;
    }
}
