// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common.privacy;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.provider.Settings;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;

import com.mopub.common.GpsHelper;
import com.mopub.common.Preconditions;
import com.mopub.common.SdkInitializationListener;
import com.mopub.common.SharedPreferencesHelper;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.AsyncTasks;

import static com.mopub.common.logging.MoPubLog.SdkLogEvent.CUSTOM;

public class MoPubIdentifier {

    public interface AdvertisingIdChangeListener {
        void onIdChanged(@NonNull final AdvertisingId oldId, @NonNull final AdvertisingId newId);
    }

    private static final String PREF_AD_INFO_GROUP = "com.mopub.settings.identifier";
    private static final String PREF_IFA_IDENTIFIER = "privacy.identifier.ifa";
    private static final String PREF_MOPUB_IDENTIFIER = "privacy.identifier.mopub";
    private static final String PREF_IDENTIFIER_TIME = "privacy.identifier.time";
    private static final String PREF_LIMIT_AD_TRACKING = "privacy.limit.ad.tracking";
    private static final int MISSING_VALUE = -1;

    @NonNull
    private AdvertisingId mAdInfo;

    @NonNull
    private final Context mAppContext;

    @Nullable
    private AdvertisingIdChangeListener mIdChangeListener;

    private boolean mRefreshingAdvertisingInfo;

    private boolean initialized;

    @Nullable
    private volatile SdkInitializationListener mInitializationListener;

    public MoPubIdentifier(@NonNull final Context appContext) {
        this(appContext, null);
    }

    @VisibleForTesting
    MoPubIdentifier(@NonNull final Context appContext,
                    @Nullable final AdvertisingIdChangeListener idChangeListener) {
        Preconditions.checkNotNull(appContext);

        mAppContext = appContext;
        mIdChangeListener = idChangeListener;
        mAdInfo = readIdFromStorage(mAppContext);
        if (mAdInfo == null) {
            mAdInfo = AdvertisingId.generateFreshAdvertisingId();
        }
        refreshAdvertisingInfo();
    }

    /**
     * @return the most recent advertising ID and Do Not Track settings. This method  internally
     * initiates AdvertisingId refresh. The value is returned instantly on UI thread,
     * but may take some time to communicate with Google Play Services API when called
     * from background thread.
     */
    @NonNull
    public AdvertisingId getAdvertisingInfo() {
        final AdvertisingId adInfo = mAdInfo;
        refreshAdvertisingInfo();
        return adInfo;
    }

    private void refreshAdvertisingInfo() {
        if (mRefreshingAdvertisingInfo) {
            return;
        }
        mRefreshingAdvertisingInfo = true;
        AsyncTasks.safeExecuteOnExecutor(new RefreshAdvertisingInfoAsyncTask());
    }

    void refreshAdvertisingInfoBackgroundThread() {
        final AdvertisingId oldInfo = mAdInfo;
        AdvertisingId newInfo;

        // try google
        final GpsHelper.AdvertisingInfo googleAdInfo = GpsHelper.fetchAdvertisingInfoSync(mAppContext);
        if (googleAdInfo != null && !TextUtils.isEmpty(googleAdInfo.advertisingId)) {
            newInfo = new AdvertisingId(googleAdInfo.advertisingId, oldInfo.mMopubId, googleAdInfo.limitAdTracking);
        } else {
            newInfo = getAmazonAdvertisingInfo(mAppContext);
        }

        if (newInfo != null) {
            setAdvertisingInfo(newInfo.mAdvertisingId, oldInfo.mMopubId, newInfo.mDoNotTrack);
        } else {
            setAdvertisingInfo(mAdInfo);
        }
    }

    @Nullable
    static synchronized AdvertisingId readIdFromStorage(@NonNull final Context appContext) {
        Preconditions.checkNotNull(appContext);

        try {
            final SharedPreferences preferences = SharedPreferencesHelper.getSharedPreferences(appContext, PREF_AD_INFO_GROUP);
            final String ifa_id = preferences.getString(PREF_IFA_IDENTIFIER, "");
            final String mopub_id = preferences.getString(PREF_MOPUB_IDENTIFIER, "");
            final boolean limitTracking = preferences.getBoolean(PREF_LIMIT_AD_TRACKING, false);
            if (!TextUtils.isEmpty(ifa_id) && !TextUtils.isEmpty(mopub_id)) {
                return new AdvertisingId(ifa_id, mopub_id, limitTracking);
            }
        } catch (ClassCastException ex) {
            MoPubLog.log(CUSTOM, "Cannot read identifier from shared preferences");
        }
        return null;
    }

    private static synchronized void writeIdToStorage(@NonNull final Context context, @NonNull final AdvertisingId info) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(info);

        final SharedPreferences preferences = SharedPreferencesHelper.getSharedPreferences(context, PREF_AD_INFO_GROUP);
        final SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(PREF_LIMIT_AD_TRACKING, info.mDoNotTrack);
        editor.putString(PREF_IFA_IDENTIFIER, info.mAdvertisingId);
        editor.putString(PREF_MOPUB_IDENTIFIER, info.mMopubId);
        editor.apply();
    }

    @VisibleForTesting
    static synchronized void clearStorage(@NonNull final Context context) {
        Preconditions.checkNotNull(context);

        final SharedPreferences preferences = SharedPreferencesHelper.getSharedPreferences(context, PREF_AD_INFO_GROUP);
        final SharedPreferences.Editor editor = preferences.edit();
        editor.remove(PREF_LIMIT_AD_TRACKING);
        editor.remove(PREF_IFA_IDENTIFIER);
        editor.remove(PREF_MOPUB_IDENTIFIER);
        editor.remove(PREF_IDENTIFIER_TIME);
        editor.apply();
    }

    private void setAdvertisingInfo(@NonNull String advertisingId, @NonNull String mopubId, boolean limitAdTracking) {
        Preconditions.checkNotNull(advertisingId);
        Preconditions.checkNotNull(mopubId);

        setAdvertisingInfo(new AdvertisingId(advertisingId, mopubId, limitAdTracking));
    }

    void setAdvertisingInfo(@NonNull final AdvertisingId newId) {
        AdvertisingId oldId = mAdInfo;
        mAdInfo = newId;
        writeIdToStorage(mAppContext, mAdInfo);

        if (mAdInfo.mAdvertisingId.endsWith("10ca1ad1abe1")) {
            MoPubLog.setLogLevel(MoPubLog.LogLevel.DEBUG);
        }

        if (!mAdInfo.equals(oldId) || !initialized) {
            notifyIdChangeListener(oldId, mAdInfo);
        }
        initialized = true;

        reportInitializationComplete();
    }

    /**
     * @param idChangeListener - will be called every time the OS Advertising ID or
     *                         flag do-not-track changes its value. Pass null to stop listening.
     */
    public void setIdChangeListener(@Nullable final AdvertisingIdChangeListener idChangeListener) {
        mIdChangeListener = idChangeListener;
    }

    void setInitializationListener(@Nullable final SdkInitializationListener initializationListener) {
        mInitializationListener = initializationListener;
        if (initialized) {
            reportInitializationComplete();
        }
    }

    private synchronized void reportInitializationComplete() {
        final SdkInitializationListener listener = mInitializationListener;
        if (listener != null) {
            mInitializationListener = null;
            listener.onInitializationFinished();
        }
    }

    private void notifyIdChangeListener(@NonNull final AdvertisingId oldId, @NonNull final AdvertisingId newId) {
        Preconditions.checkNotNull(newId);

        if (mIdChangeListener != null) {
            mIdChangeListener.onIdChanged(oldId, newId);
        }
    }

    // For Amazon tablets running Fire OS 5.1+ and TV devices running Fire OS 5.2.1.1+, the
    // advertising info is available as System Settings.
    // See https://developer.amazon.com/public/solutions/devices/fire-tv/docs/fire-tv-advertising-id
    @Nullable
    private AdvertisingId getAmazonAdvertisingInfo(@NonNull final Context context) {
        Preconditions.NoThrow.checkNotNull(context);

        ContentResolver resolver = context.getContentResolver();
        int limitAdTracking = Settings.Secure.getInt(resolver, "limit_ad_tracking", MISSING_VALUE);
        String advertisingId = Settings.Secure.getString(resolver, "advertising_id");

        if (limitAdTracking != MISSING_VALUE && !TextUtils.isEmpty(advertisingId)) {
            boolean doNotTrack = limitAdTracking != 0;
            final AdvertisingId oldId = mAdInfo;
            // merge Amazon and MoPub data in one object
            return new AdvertisingId(advertisingId, oldId.mMopubId, doNotTrack);
        }
        return null;
    }

    private class RefreshAdvertisingInfoAsyncTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(final Void... voids) {
            refreshAdvertisingInfoBackgroundThread();
            mRefreshingAdvertisingInfo = false;
            return null;
        }
    }
}
