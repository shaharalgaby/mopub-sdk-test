// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common.util;

import androidx.annotation.VisibleForTesting;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class DateAndTime {
    protected static DateAndTime instance = new DateAndTime();

    @Deprecated
    @VisibleForTesting
    public static void setInstance(DateAndTime newInstance) {
        instance = newInstance;
    }

    public static TimeZone localTimeZone() {
        return instance.internalLocalTimeZone();
    }

    public static Date now() {
        return instance.internalNow();
    }

    public static String getTimeZoneOffsetString() {
        // A new instance is created with each call because DateFormat objects have
        // internal state and are not thread safe.
        SimpleDateFormat format = new SimpleDateFormat("Z", Locale.US);
        format.setTimeZone(localTimeZone());
        return format.format(now());
    }

    public TimeZone internalLocalTimeZone() {
        return TimeZone.getDefault();
    }

    public Date internalNow() {
        return new Date();
    }
}
