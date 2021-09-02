// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.mopub.common.Constants;
import com.mopub.common.MoPub;
import com.mopub.common.Preconditions;
import com.mopub.common.SharedPreferencesHelper;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.privacy.ConsentData;
import com.mopub.common.privacy.PersonalInfoManager;
import com.mopub.network.TrackingRequest;

import static com.mopub.common.logging.MoPubLog.SdkLogEvent.CUSTOM;

public class MoPubConversionTracker {
    private static final String WANT_TO_TRACK = " wantToTrack";

    @NonNull
    private final Context mContext;
    @NonNull
    private final String mWantToTrack;
    @NonNull
    private final String mIsTrackedKey;
    @NonNull
    private SharedPreferences mSharedPreferences;

    public MoPubConversionTracker(@NonNull final Context context) {
        Preconditions.checkNotNull(context);

        mContext = context.getApplicationContext();
        String packageName = mContext.getPackageName();
        mWantToTrack = packageName + WANT_TO_TRACK;
        mIsTrackedKey = packageName + " tracked";
        mSharedPreferences = SharedPreferencesHelper.getSharedPreferences(mContext);
    }

    /**
     * Call this to report conversion tracking.
     */
    public void reportAppOpen() {
        reportAppOpen(false);
    }

    /**
     * This method is only used internally. Do not call this method.
     *
     * @param sessionTracker - true for session tracking
     */
    public void reportAppOpen(final boolean sessionTracker) {
        final PersonalInfoManager infoManager = MoPub.getPersonalInformationManager();
        if (infoManager == null) {
            MoPubLog.log(CUSTOM, "Cannot report app open until initialization is done");
            return;
        }


        if (!sessionTracker && isAlreadyTracked()) {
            MoPubLog.log(CUSTOM, "Conversion already tracked");
            return;
        }

        if (!sessionTracker && !MoPub.canCollectPersonalInformation()) {
            mSharedPreferences
                    .edit()
                    .putBoolean(mWantToTrack, true)
                    .apply();
            return;
        }

        final ConsentData consentData = infoManager.getConsentData();
        final String url = new ConversionUrlGenerator(mContext, consentData.chooseAdUnit())
                .withGdprApplies(infoManager.gdprApplies())
                .withForceGdprApplies(consentData.isForceGdprApplies())
                .withCurrentConsentStatus(infoManager.getPersonalInfoConsentStatus().getValue())
                .withConsentedPrivacyPolicyVersion(consentData.getConsentedPrivacyPolicyVersion())
                .withConsentedVendorListVersion(consentData.getConsentedVendorListVersion())
                .withSessionTracker(sessionTracker)
                .generateUrlString(Constants.HOST);

        TrackingRequest.makeTrackingHttpRequest(url,
                mContext, new TrackingRequest.Listener() {
                    @Override
                    public void onResponse(@NonNull final String url) {
                        if (sessionTracker) {
                            return;
                        }
                        mSharedPreferences
                                .edit()
                                .putBoolean(mIsTrackedKey, true)
                                .putBoolean(mWantToTrack, false)
                                .apply();
                    }
                });
    }

    public boolean shouldTrack() {
        PersonalInfoManager infoManager = MoPub.getPersonalInformationManager();
        if (infoManager == null) {
            return false;
        }

        return infoManager.canCollectPersonalInformation() &&
                mSharedPreferences.getBoolean(mWantToTrack, false);
    }

    private boolean isAlreadyTracked() {
        return mSharedPreferences.getBoolean(mIsTrackedKey, false);
    }
}
