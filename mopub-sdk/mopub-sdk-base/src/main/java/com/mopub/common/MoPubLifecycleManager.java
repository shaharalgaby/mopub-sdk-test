// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common;

import android.app.Activity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.privacy.PersonalInfoManager;

import java.lang.ref.WeakReference;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * This class handles delegating lifecycle callback events to ads SDKs that require them.
 */
public class MoPubLifecycleManager implements LifecycleListener {
    private static MoPubLifecycleManager sInstance;

    @NonNull private final Set<LifecycleListener> mLifecycleListeners;
    @NonNull private final WeakReference<Activity> mMainActivity;

    private MoPubLifecycleManager(Activity mainActivity) {
        mLifecycleListeners = new CopyOnWriteArraySet<>();
        mMainActivity = new WeakReference<Activity>(mainActivity);
    }

    @NonNull public static synchronized MoPubLifecycleManager getInstance(Activity mainActivity) {
        if (sInstance == null) {
            sInstance = new MoPubLifecycleManager(mainActivity);
        }

        return sInstance;
    }

    /**
     * Adds a lifecycle listener to the manager. The manager takes ownership with a strong reference.
     *
     * @param listener the listener to add to the lifecycle manager.
     */
    public void addLifecycleListener(@Nullable LifecycleListener listener) {
        // Get the instance or bail if not initialized.
        if (listener == null) {
            return;
        }
        if (mLifecycleListeners.add(listener)) {
            Activity activity = mMainActivity.get();
            if (activity != null) {
                listener.onCreate(activity);
                listener.onStart(activity);
            }
        }
    }

    @Override
    public void onCreate(@NonNull final Activity activity) {
        for (LifecycleListener lifecycleListener : mLifecycleListeners) {
            lifecycleListener.onCreate(activity);
        }
    }

    @Override
    public void onStart(@NonNull final Activity activity) {
        for (LifecycleListener lifecycleListener : mLifecycleListeners) {
            lifecycleListener.onStart(activity);
        }
    }

    @Override
    public void onPause(@NonNull final Activity activity) {
        for (LifecycleListener lifecycleListener : mLifecycleListeners) {
            lifecycleListener.onPause(activity);
        }
    }

    @Override
    public void onResume(@NonNull final Activity activity) {
        final PersonalInfoManager personalInfoManager = MoPub.getPersonalInformationManager();
        if (personalInfoManager != null) {
            personalInfoManager.requestSync(false);
        }

        for (LifecycleListener lifecycleListener : mLifecycleListeners) {
            lifecycleListener.onResume(activity);
        }
    }

    @Override
    public void onRestart(@NonNull final Activity activity) {
        for (LifecycleListener lifecycleListener : mLifecycleListeners) {
            lifecycleListener.onRestart(activity);
        }
    }

    @Override
    public void onStop(@NonNull final Activity activity) {
        for (LifecycleListener lifecycleListener : mLifecycleListeners) {
            lifecycleListener.onStop(activity);
        }
    }

    @Override
    public void onDestroy(@NonNull final Activity activity) {
        for (LifecycleListener lifecycleListener : mLifecycleListeners) {
            lifecycleListener.onDestroy(activity);
        }
    }

    @Override
    public void onBackPressed(@NonNull final Activity activity) {
        for (LifecycleListener lifecycleListener : mLifecycleListeners) {
            lifecycleListener.onBackPressed(activity);
        }
    }
}
