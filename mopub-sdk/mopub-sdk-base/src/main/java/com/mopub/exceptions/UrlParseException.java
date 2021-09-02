// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.exceptions;

public class UrlParseException extends Exception {
    public UrlParseException(final String detailMessage) {
        super(detailMessage);
    }

    public UrlParseException(final Throwable throwable) {
        super(throwable);
    }
}
