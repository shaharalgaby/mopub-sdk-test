// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mraid;

import android.content.pm.ActivityInfo;

enum MraidOrientation {
    PORTRAIT(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT),
    LANDSCAPE(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE),
    NONE(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

    private final int mActivityInfoOrientation;

    MraidOrientation(final int activityInfoOrientation) {
        mActivityInfoOrientation = activityInfoOrientation;
    }

    int getActivityInfoOrientation() {
        return mActivityInfoOrientation;
    }
}
