// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.exceptions;

public class IntentNotResolvableException extends Exception {
    public IntentNotResolvableException(Throwable throwable) {
        super(throwable);
    }

    public IntentNotResolvableException(String message) {
        super(message);
    }
}
