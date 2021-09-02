// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mraid;

class MraidCommandException extends Exception {
    MraidCommandException() {
        super();
    }

    MraidCommandException(String detailMessage) {
        super(detailMessage);
    }

    MraidCommandException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    MraidCommandException(Throwable throwable) {
        super(throwable);
    }
}
