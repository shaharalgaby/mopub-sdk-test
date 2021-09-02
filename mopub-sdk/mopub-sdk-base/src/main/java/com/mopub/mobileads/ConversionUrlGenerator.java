// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.BaseUrlGenerator;
import com.mopub.common.ClientMetadata;
import com.mopub.common.Constants;
import com.mopub.common.MoPub;

class ConversionUrlGenerator extends BaseUrlGenerator {
    /**
     * Whether or not the conversion url is for session tracking (1) or not (0).
     */
    private static final String SESSION_TRACKER_KEY = "st";

    /**
     * This is the bundle name on Android.
     */
    private static final String PACKAGE_NAME_KEY = "id";

    /**
     * The ad unit used to initialize the SDK. This is not to be confused with AD_UNIT_ID_KEY
     * in BaseUrlGenerator.
     */
    private static final String INITIALIZATION_AD_UNIT_ID_KEY = "adunit";

    @NonNull
    private Context mContext;
    @Nullable
    private String mAdUnit;
    @Nullable
    private String mCurrentConsentStatus;
    @Nullable
    private String mConsentedVendorListVersion;
    @Nullable
    private String mConsentedPrivacyPolicyVersion;
    @Nullable
    private Boolean mGdprApplies;
    private boolean mForceGdprApplies;

    private boolean mSt;

    ConversionUrlGenerator(@NonNull final Context context, @Nullable final String adUnit) {
        mContext = context;
        mAdUnit = adUnit;
    }

    public ConversionUrlGenerator withCurrentConsentStatus(
            @Nullable final String currentConsentStatus) {
        mCurrentConsentStatus = currentConsentStatus;
        return this;
    }

    public ConversionUrlGenerator withGdprApplies(@Nullable final Boolean gdprApplies) {
        mGdprApplies = gdprApplies;
        return this;
    }

    public ConversionUrlGenerator withForceGdprApplies(final boolean forceGdprApplies) {
        mForceGdprApplies = forceGdprApplies;
        return this;
    }

    public ConversionUrlGenerator withConsentedVendorListVersion(@Nullable final String consentedVendorListVersion) {
        mConsentedVendorListVersion = consentedVendorListVersion;
        return this;
    }

    public ConversionUrlGenerator withConsentedPrivacyPolicyVersion(@Nullable final String consentedPrivacyPolicyVersion) {
        mConsentedPrivacyPolicyVersion = consentedPrivacyPolicyVersion;
        return this;
    }

    public ConversionUrlGenerator withSessionTracker(final boolean st) {
        mSt = st;
        return this;
    }

    @Override
    public String generateUrlString(String serverHostname) {
        ClientMetadata clientMetadata = ClientMetadata.getInstance(mContext);

        initUrlString(serverHostname, Constants.CONVERSION_TRACKING_HANDLER);
        setApiVersion("6");
        setAppVersion(clientMetadata.getAppVersion());
        appendAdvertisingInfoTemplates();

        addParam(PLATFORM_KEY, Constants.ANDROID_PLATFORM);
        addParam(INITIALIZATION_AD_UNIT_ID_KEY, mAdUnit);
        addParam(PACKAGE_NAME_KEY, mContext.getPackageName());
        addParam(BUNDLE_ID_KEY, mContext.getPackageName());
        setDeviceInfo(clientMetadata.getDeviceOsVersion(),
                clientMetadata.getDeviceManufacturer(),
                clientMetadata.getDeviceModel(),
                clientMetadata.getDeviceProduct(),
                clientMetadata.getDeviceHardware());

        if (mSt) {
            addParam(SESSION_TRACKER_KEY, true);
        }
        addParam(SDK_VERSION_KEY, MoPub.SDK_VERSION);
        appendAppEngineInfo();
        appendWrapperVersion();
        addParam(CURRENT_CONSENT_STATUS_KEY, mCurrentConsentStatus);
        addParam(CONSENTED_VENDOR_LIST_VERSION_KEY, mConsentedVendorListVersion);
        addParam(CONSENTED_PRIVACY_POLICY_VERSION_KEY, mConsentedPrivacyPolicyVersion);
        addParam(GDPR_APPLIES, mGdprApplies);
        addParam(FORCE_GDPR_APPLIES, mForceGdprApplies);
        return getFinalUrlString();
    }
}
