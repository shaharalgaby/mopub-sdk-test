// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

/**
 * Temporary solution, this interface will be removed in the next major release
 */
@Deprecated
public interface MoPubError {
    int ER_SUCCESS = 0;
    int ER_ADAPTER_NOT_FOUND = 1;
    int ER_TIMEOUT = 2;
    int ER_INVALID_DATA = 3;
    int ER_UNSPECIFIED = 10000;

    /**
     * {@link MoPubErrorCode} and NativeErrorCode must implement this function to map
     * enum value to server error code value
     * @return ER_* constant value
     */
    int getIntCode();
}
