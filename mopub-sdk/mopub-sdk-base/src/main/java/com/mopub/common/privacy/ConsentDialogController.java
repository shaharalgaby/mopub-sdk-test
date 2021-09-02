// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common.privacy;

import android.content.Context;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;

import com.mopub.common.Constants;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.network.MoPubNetworkError;
import com.mopub.network.Networking;

import static com.mopub.common.logging.MoPubLog.ConsentLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.ConsentLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.ConsentLogEvent.SHOW_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.ConsentLogEvent.SHOW_FAILED;
import static com.mopub.common.logging.MoPubLog.SdkLogEvent.CUSTOM;

public class ConsentDialogController implements ConsentDialogRequest.Listener {
    @NonNull
    private final Context mAppContext;

    @Nullable private String mHtmlBody;
    @Nullable private ConsentDialogListener mExtListener;
    volatile boolean mReady;
    volatile boolean mRequestInFlight;
    private final Handler mHandler;

    ConsentDialogController(@NonNull final Context appContext) {
        Preconditions.checkNotNull(appContext);

        mAppContext = appContext.getApplicationContext();
        mHandler = new Handler();
    }

    @Override
    public void onResponse(@NonNull final ConsentDialogResponse response) {
        mRequestInFlight = false;
        mHtmlBody = response.getHtml();
        if (TextUtils.isEmpty(mHtmlBody)) {
            mReady = false;
            if (mExtListener != null) {
                MoPubLog.log(LOAD_FAILED, MoPubErrorCode.INTERNAL_ERROR.getIntCode(),
                        MoPubErrorCode.INTERNAL_ERROR);
                mExtListener.onConsentDialogLoadFailed(MoPubErrorCode.INTERNAL_ERROR);
            }
            return;
        }

        MoPubLog.log(LOAD_SUCCESS);

        mReady = true;
        if (mExtListener != null) {
            mExtListener.onConsentDialogLoaded();
        }
    }

    @Override
    public void onErrorResponse(@NonNull final MoPubNetworkError networkError) {
        final ConsentDialogListener loadListener = mExtListener;
        resetState();

        if (loadListener == null) {
            return;
        }

        if (networkError.getReason() != null) {
            if (networkError.getReason() == MoPubNetworkError.Reason.BAD_BODY) {
                MoPubLog.log(LOAD_FAILED, MoPubErrorCode.INTERNAL_ERROR.getIntCode(), MoPubErrorCode.INTERNAL_ERROR);
                loadListener.onConsentDialogLoadFailed(MoPubErrorCode.INTERNAL_ERROR);
                return;
            } else {
                MoPubLog.log(LOAD_FAILED, MoPubErrorCode.UNSPECIFIED.getIntCode(), MoPubErrorCode.UNSPECIFIED);
            }
        }

        loadListener.onConsentDialogLoadFailed(MoPubErrorCode.UNSPECIFIED);
    }

    synchronized void loadConsentDialog(@Nullable final ConsentDialogListener listener,
            @Nullable final Boolean gdprApplies,
            @NonNull final PersonalInfoData personalInfoData) {
        Preconditions.checkNotNull(personalInfoData);

        if (mReady) {
            if (listener != null) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        MoPubLog.log(LOAD_SUCCESS);
                        listener.onConsentDialogLoaded();
                    }
                });
            }
            return;
        } else if (mRequestInFlight) {
            MoPubLog.log(CUSTOM, "Already making a consent dialog load request.");
            return;
        }

        mExtListener = listener;
        mRequestInFlight = true;

        ConsentDialogRequest consentDialogRequest = new ConsentDialogRequest(mAppContext,
                new ConsentDialogUrlGenerator(mAppContext, personalInfoData.getAdUnitId(),
                        personalInfoData.getConsentStatus().getValue())
                        .withGdprApplies(gdprApplies)
                        .withConsentedPrivacyPolicyVersion(personalInfoData.getConsentedPrivacyPolicyVersion())
                        .withConsentedVendorListVersion(personalInfoData.getConsentedVendorListVersion())
                        .withForceGdprApplies(personalInfoData.isForceGdprApplies())
                        .generateUrlString(Constants.HOST), this);
        Networking.getRequestQueue(mAppContext).add(consentDialogRequest);
    }

    boolean showConsentDialog() {
        MoPubLog.log(SHOW_ATTEMPTED);
        if (!mReady || TextUtils.isEmpty(mHtmlBody)) {
            MoPubLog.log(SHOW_FAILED, MoPubErrorCode.INTERNAL_ERROR.getIntCode(),
                    MoPubErrorCode.INTERNAL_ERROR);
            return false;
        }

        mReady = false;
        ConsentDialogActivity.start(mAppContext, mHtmlBody);
        resetState();
        return true;
    }

    boolean isReady() {
        return mReady;
    }

    private void resetState() {
        mRequestInFlight = false;
        mReady = false;
        mExtListener = null;
        mHtmlBody = null;
    }
}
