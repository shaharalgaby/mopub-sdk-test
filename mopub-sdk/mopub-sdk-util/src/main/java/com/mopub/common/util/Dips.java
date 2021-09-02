// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common.util;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import androidx.annotation.NonNull;

import com.mopub.common.Preconditions;

public class Dips {
    public static float pixelsToFloatDips(final float pixels, @NonNull final Context context) {
        Preconditions.checkNotNull(context);

        return pixels / getDensity(context);
    }

    public static int pixelsToIntDips(final float pixels, @NonNull final Context context) {
        Preconditions.checkNotNull(context);

        return (int) (pixelsToFloatDips(pixels, context) + 0.5f);
    }

    public static float dipsToFloatPixels(final float dips, @NonNull final Context context) {
        Preconditions.checkNotNull(context);

        return dips * getDensity(context);
    }

    public static int dipsToIntPixels(final float dips, @NonNull final Context context) {
        Preconditions.checkNotNull(context);

        return (int) (dipsToFloatPixels(dips, context) + 0.5f);
    }

    private static float getDensity(@NonNull final Context context) {
        Preconditions.checkNotNull(context);

        return context.getResources().getDisplayMetrics().density;
    }

    public static float asFloatPixels(float dips, @NonNull final Context context) {
        Preconditions.checkNotNull(context);

        final DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dips, displayMetrics);
    }

    public static int asIntPixels(float dips, @NonNull final Context context) {
        Preconditions.checkNotNull(context);

        return (int) (asFloatPixels(dips, context) + 0.5f);
    }

    public static int screenWidthAsIntDips(@NonNull final Context context) {
        Preconditions.checkNotNull(context);

        return pixelsToIntDips(context.getResources().getDisplayMetrics().widthPixels, context);
    }

    public static int screenHeightAsIntDips(@NonNull final Context context) {
        Preconditions.checkNotNull(context);

        return pixelsToIntDips(context.getResources().getDisplayMetrics().heightPixels, context);
    }
}
