// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.DataKeys;
import com.mopub.common.IntentActions;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.Intents;
import com.mopub.common.util.Utils;
import com.mopub.exceptions.IntentNotResolvableException;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.mopub.common.DataKeys.AD_DATA_KEY;
import static com.mopub.common.IntentActions.ACTION_FULLSCREEN_SHOW;
import static com.mopub.common.logging.MoPubLog.AdLogEvent.CUSTOM_WITH_THROWABLE;
import static com.mopub.common.logging.MoPubLog.AdLogEvent.SHOW_FAILED;
import static com.mopub.common.logging.MoPubLog.AdLogEvent.SHOW_SUCCESS;
import static com.mopub.common.logging.MoPubLog.SdkLogEvent.CUSTOM;
import static com.mopub.mobileads.BaseBroadcastReceiver.broadcastAction;
import static com.mopub.mobileads.MoPubErrorCode.FULLSCREEN_SHOW_ERROR;

public class MoPubFullscreenActivity extends Activity {

    @Nullable private FullscreenAdController mFullscreenAdController;

    public static void start(@NonNull Context context, @NonNull AdData adData) {
        final Intent intent = createIntent(context, adData);
        try {
            Intents.startActivity(context, intent);
        } catch (IntentNotResolvableException exception) {
            Log.d("MoPubFullscreenActivity", "MoPubFullscreenActivity.class not found. " +
                    "Did you declare MoPubFullscreenActivity in your manifest?");
        }
    }

    @VisibleForTesting
    protected static Intent createIntent(@NonNull final Context context, @NonNull final AdData adData) {
        Intent intent = new Intent(context, MoPubFullscreenActivity.class);
        intent.putExtra(AD_DATA_KEY, adData);
        intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final AdData adData = getAdDataFromIntent(getIntent());
        if (adData == null) {
            // This is a bug and should never happen.
            MoPubLog.log(CUSTOM, "Ad data to show ad is null. Failed to show fullscreen ad.");
            finish();
            return;
        }

        final long broadcastIdentifier = adData.getBroadcastIdentifier();

        try {
            mFullscreenAdController = new FullscreenAdController(this,
                    savedInstanceState, getIntent(), adData);
        } catch (IllegalStateException e) {
            // This can happen if the activity was started without valid intent extras. We leave
            // mFullscreenAdController set to null, and finish the activity immediately.

            MoPubLog.log(SHOW_FAILED, FULLSCREEN_SHOW_ERROR, FULLSCREEN_SHOW_ERROR.getIntCode());
            MoPubLog.log(CUSTOM_WITH_THROWABLE, FULLSCREEN_SHOW_ERROR, e);
            broadcastAction(this, broadcastIdentifier, IntentActions.ACTION_FULLSCREEN_FAIL);
            finish();
            return;
        }

        MoPubLog.log(SHOW_SUCCESS);
        broadcastAction(this, adData.getBroadcastIdentifier(), ACTION_FULLSCREEN_SHOW);

        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Utils.hideNavigationBar(this);
    }

    @Override
    protected void onPause() {
        if (mFullscreenAdController != null) {
            mFullscreenAdController.pause();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mFullscreenAdController != null) {
            mFullscreenAdController.resume();
        }
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if (mFullscreenAdController != null) {
            mFullscreenAdController.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onDestroy() {
        if (mFullscreenAdController != null) {
            mFullscreenAdController.destroy();
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (mFullscreenAdController == null || mFullscreenAdController.backButtonEnabled()) {
            super.onBackPressed();
        }
    }

    @Nullable
    protected static AdData getAdDataFromIntent(Intent intent) {
        try {
            return (AdData) intent.getParcelableExtra(DataKeys.AD_DATA_KEY);
        } catch (ClassCastException e) {
            return null;
        }
    }

    @Deprecated
    @VisibleForTesting
    FullscreenAdController getFullscreenAdController() {
        return mFullscreenAdController;
    }

    @Deprecated
    @VisibleForTesting
    void setFullscreenAdController(@NonNull final FullscreenAdController fullscreenAdController) {
        mFullscreenAdController = fullscreenAdController;
    }
}
