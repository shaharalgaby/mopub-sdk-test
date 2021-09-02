// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common.util;

import android.app.Activity;
import android.view.View;
import android.view.Window;

import androidx.annotation.NonNull;

import com.mopub.common.Preconditions;

import java.security.MessageDigest;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

public class Utils {
    private static final AtomicLong sNextGeneratedId = new AtomicLong(1);

    public static String sha1(String string) {
        StringBuilder stringBuilder = new StringBuilder();

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] bytes = string.getBytes("UTF-8");
            digest.update(bytes, 0, bytes.length);
            bytes = digest.digest();

            for (final byte b : bytes) {
                stringBuilder.append(String.format("%02X", b));
            }

            return stringBuilder.toString().toLowerCase(Locale.US);
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Adaptation of View.generateViewId() to get a unique id when not interacting with Views.
     * There is only a guarantee of ID uniqueness within a given session. Please do not store these
     * values between sessions.
     */
    public static long generateUniqueId() {
        for (;;) {
            final long result = sNextGeneratedId.get();
            long newValue = result + 1;
            if (newValue > Long.MAX_VALUE - 1) {
                newValue = 1;
            }
            if (sNextGeneratedId.compareAndSet(result, newValue)) {
                return result;
            }
        }
    }

    public static boolean bitMaskContainsFlag(final int bitMask, final int flag) {
        return (bitMask & flag) != 0;
    }

    public static void hideNavigationBar(@NonNull final Activity activity) {
        Preconditions.checkNotNull(activity);

        final Window window = activity.getWindow();
        if (window != null) {
            final View view = window.getDecorView();
            hideNavigation(view);
            view.setOnSystemUiVisibilityChangeListener(createHideNavigationListener(view));
        }
    }

    static View.OnSystemUiVisibilityChangeListener createHideNavigationListener(
            @NonNull final View view) {
        Preconditions.checkNotNull(view);

        return new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                if ((visibility & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0) {
                    hideNavigation(view);
                }
            }
        };
    }

    static void hideNavigation(@NonNull final View view) {
        Preconditions.checkNotNull(view);

        view.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
        );
    }
}
