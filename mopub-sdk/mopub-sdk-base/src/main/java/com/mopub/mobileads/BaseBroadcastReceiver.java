// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.mopub.common.DataKeys;
import com.mopub.common.Preconditions;

public abstract class BaseBroadcastReceiver extends BroadcastReceiver {
    private final long mBroadcastIdentifier;
    @Nullable private Context mContext;

    public BaseBroadcastReceiver(final long broadcastIdentifier) {
        mBroadcastIdentifier = broadcastIdentifier;
    }

    public static void broadcastAction(@NonNull final Context context, final long broadcastIdentifier,
            @NonNull final String action) {
        Preconditions.checkNotNull(context, "context cannot be null");
        Preconditions.checkNotNull(action, "action cannot be null");
        Intent intent = new Intent(action);
        intent.putExtra(DataKeys.BROADCAST_IDENTIFIER_KEY, broadcastIdentifier);
        LocalBroadcastManager.getInstance(context.getApplicationContext()).sendBroadcast(intent);
    }

    @NonNull
    public abstract IntentFilter getIntentFilter();

    public void register(@NonNull final BroadcastReceiver broadcastReceiver,
                         @NonNull final Context context) {
        mContext = context.getApplicationContext();
        LocalBroadcastManager.getInstance(mContext).registerReceiver(broadcastReceiver,
                getIntentFilter());
    }

    public void unregister(final @Nullable BroadcastReceiver broadcastReceiver) {
        if (mContext != null && broadcastReceiver != null) {
            LocalBroadcastManager.getInstance(mContext).unregisterReceiver(broadcastReceiver);
            mContext = null;
        }
    }

    /**
     * Only consume this broadcast if the identifier on the received Intent and this broadcast
     * match up. This allows us to target broadcasts to the ad that spawned them. We include
     * this here because there is no appropriate IntentFilter condition that can recreate this
     * behavior.
     */
    public boolean shouldConsumeBroadcast(@NonNull final Intent intent) {
        Preconditions.checkNotNull(intent, "intent cannot be null");
        final long receivedIdentifier = intent.getLongExtra(DataKeys.BROADCAST_IDENTIFIER_KEY, -1);
        return mBroadcastIdentifier == receivedIdentifier;
    }
}
