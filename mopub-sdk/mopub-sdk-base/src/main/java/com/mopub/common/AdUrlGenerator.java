// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common;

import android.content.Context;
import android.graphics.Point;
import android.location.Location;
import android.text.TextUtils;
import android.view.WindowInsets;

import androidx.annotation.Nullable;

import com.mopub.common.privacy.ConsentData;
import com.mopub.common.privacy.PersonalInfoManager;
import com.mopub.common.util.DateAndTime;
import com.mopub.common.util.ResponseHeader;
import com.mopub.network.RequestRateTracker;

import static com.mopub.common.ClientMetadata.MoPubNetworkType;

public abstract class AdUrlGenerator extends BaseUrlGenerator {

    /**
     * q = query. This is for sending application keywords that better match ads.
     */
    private static final String KEYWORDS_KEY = "q";

    /**
     * user_data_q = userDataQuery. This is for MoPub partners to send up certain
     * user data keywords that better match ads. Will only be sent if the user has granted
     * MoPub consent to gather and send user data information.
     */
    private static final String USER_DATA_KEYWORDS_KEY = "user_data_q";

    /**
     * Location represented in latitude and longitude.
     * e.g. "47.638,-122.321"
     */
    private static final String LAT_LONG_KEY = "ll";

    /**
     * Estimated accuracy of this location, in meters.
     * See {@link android.location.Location#getAccuracy()}
     * for more information.
     */
    private static final String LAT_LONG_ACCURACY_KEY = "lla";

    /**
     * Milliseconds since location was updated.
     */
    private static final String LAT_LONG_FRESHNESS_KEY = "llf";

    /**
     * Whether or not the location came from the MoPub SDK
     * and not the developer. 1 = from MoPub.
     */
    private static final String LAT_LONG_FROM_SDK_KEY = "llsdk";

    /**
     * Timezone offset. e.g. Pacific Standard Time = -0800.
     */
    private static final String TIMEZONE_OFFSET_KEY = "z";

    /**
     * "p" for portrait, "l" for landscape
     */
    private static final String ORIENTATION_KEY = "o";

    /**
     * Density as represented by a float. See
     * https://developer.android.com/guide/practices/screens_support.html
     * for details on values this can be.
     */
    private static final String SCREEN_SCALE_KEY = "sc";

    /**
     * Whether or not this is using mraid. 1 = yes.
     */
    private static final String IS_MRAID_KEY = "mr";

    /**
     * mcc, the mobile country code, paired with the mobile network code,
     * uniquely identifies a carrier in a country.
     */
    private static final String MOBILE_COUNTRY_CODE_KEY = "mcc";
    private static final String MOBILE_NETWORK_CODE_KEY = "mnc";

    /**
     * The International Organization for Standardization's 2-character country code
     */
    private static final String COUNTRY_CODE_KEY = "iso";

    /**
     * String name of the carrier. e.g. "Verizon%20Wireless"
     */
    private static final String CARRIER_NAME_KEY = "cn";

    /**
     * Carrier type as in what kind of network this device is on.
     * See {@link android.net.ConnectivityManager} for constants.
     */
    private static final String CARRIER_TYPE_KEY = "ct";

    /**
     * Whether or not this ad is using third-party viewability tracking.
     * 0b000: viewability disabled
     * 0b100: OM SDK enabled
     */
    private static final String VIEWABILITY_KEY = "vv";

    /**
     * Version of the OM SDK
     */
    private static final String OMSDK_VERSION_KEY = "vver";

    /**
     * The advanced bidding token for each MoPubAdvancedBidder in JSON format.
     */
    private static final String ADVANCED_BIDDING_TOKENS_KEY = "abt";

    /**
     * Value {@link ResponseHeader#BACKOFF_MS} from previous ad response for this ad unit id. Optional.
     */
    private static final String BACKOFF_TIME_MS_KEY = "backoff_ms";

    /**
     * Value {@link ResponseHeader#BACKOFF_REASON} from previous ad response for this ad unit id. Optional.
     */
    private static final String BACKOFF_REASON_KEY = "backoff_reason";

    /**
     *  The SHA-256 hash of this ad's Creative Experience Settings.
     */
    private static final String CE_SETTINGS_HASH_KEY = "ce_settings_hash_key";

    protected Context mContext;
    protected String mAdUnitId;
    protected String mKeywords;
    protected String mUserDataKeywords;
    protected Point mRequestedAdSize;
    protected WindowInsets mWindowInsets;
    protected String mCeSettingsHash;
    @Nullable private final PersonalInfoManager mPersonalInfoManager;
    @Nullable private final ConsentData mConsentData;

    public AdUrlGenerator(Context context) {
        mContext = context;
        mPersonalInfoManager = MoPub.getPersonalInformationManager();
        if (mPersonalInfoManager == null) {
            mConsentData = null;
        } else {
            mConsentData = mPersonalInfoManager.getConsentData();
        }
    }

    public AdUrlGenerator withAdUnitId(String adUnitId) {
        mAdUnitId = adUnitId;
        return this;
    }

    public AdUrlGenerator withKeywords(String keywords) {
        mKeywords = keywords;
        return this;
    }

    public AdUrlGenerator withUserDataKeywords(String userDataKeywords) {
        mUserDataKeywords = userDataKeywords;
        return this;
    }

    public AdUrlGenerator withRequestedAdSize(final Point adSize) {
        mRequestedAdSize = adSize;
        return this;
    }

    public AdUrlGenerator withWindowInsets(final WindowInsets windowInsets) {
        mWindowInsets = windowInsets;
        return this;
    }

    public AdUrlGenerator withCeSettingsHash(String ceSettingsHash) {
        mCeSettingsHash = ceSettingsHash;
        return this;
    }

    protected void setAdUnitId(String adUnitId) {
        addParam(AD_UNIT_ID_KEY, adUnitId);
    }

    protected void setSdkVersion(String sdkVersion) {
        addParam(SDK_VERSION_KEY, sdkVersion);
    }

    protected void setKeywords(String keywords) {
        addParam(KEYWORDS_KEY, keywords);
    }

    protected void setUserDataKeywords(String userDataKeywords) {
        if (!MoPub.canCollectPersonalInformation()) {
            return;
        }
        addParam(USER_DATA_KEYWORDS_KEY, userDataKeywords);
    }

    protected void setLocation() {
        if (!MoPub.canCollectPersonalInformation()) {
            return;
        }

        final Location location = LocationService.getLastKnownLocation(mContext);

        if (location != null) {
            addParam(LAT_LONG_KEY, location.getLatitude() + "," + location.getLongitude());
            addParam(LAT_LONG_ACCURACY_KEY, String.valueOf((int) location.getAccuracy()));
            addParam(LAT_LONG_FRESHNESS_KEY,
                    String.valueOf(calculateLocationStalenessInMilliseconds(location)));
            addParam(LAT_LONG_FROM_SDK_KEY, "1");
        }
    }

    protected void setTimezone(String timeZoneOffsetString) {
        addParam(TIMEZONE_OFFSET_KEY, timeZoneOffsetString);
    }

    protected void setOrientation(String orientation) {
        addParam(ORIENTATION_KEY, orientation);
    }

    protected void setDensity(float density) {
        addParam(SCREEN_SCALE_KEY, "" + density);
    }

    protected void setMraidFlag(boolean mraid) {
        if (mraid) {
            addParam(IS_MRAID_KEY, "1");
        }
    }

    protected void setMccCode(String networkOperator) {
        String mcc = networkOperator == null
                ? ""
                : networkOperator.substring(0, mncPortionLength(networkOperator));
        addParam(MOBILE_COUNTRY_CODE_KEY, mcc);
    }

    protected void setMncCode(String networkOperator) {
        String mnc = networkOperator == null
                ? ""
                : networkOperator.substring(
                mncPortionLength(networkOperator));
        addParam(MOBILE_NETWORK_CODE_KEY, mnc);
    }

    protected void setIsoCountryCode(String networkCountryIso) {
        addParam(COUNTRY_CODE_KEY, networkCountryIso);
    }

    protected void setCarrierName(String networkOperatorName) {
        addParam(CARRIER_NAME_KEY, networkOperatorName);
    }

    protected void setNetworkType(MoPubNetworkType networkType) {
        addParam(CARRIER_TYPE_KEY, networkType);
    }

    protected void setBundleId(String bundleId) {
        if (!TextUtils.isEmpty(bundleId)) {
            addParam(BUNDLE_ID_KEY, bundleId);
        }
    }

    protected void setViewability() {
        final int omsdk = ViewabilityManager.isViewabilityEnabled() ?  0b100 : 0;
        addParam(VIEWABILITY_KEY, String.valueOf(omsdk));

        addParam(OMSDK_VERSION_KEY, ViewabilityManager.getOmidVersion());
    }

    protected void setAdvancedBiddingTokens() {
        final String adTokens = MoPub.getAdvancedBiddingTokensJson(mContext);
        addParam(ADVANCED_BIDDING_TOKENS_KEY, adTokens);
    }

    protected void setGdprApplies() {
        if (mPersonalInfoManager != null) {
            addParam(GDPR_APPLIES, mPersonalInfoManager.gdprApplies());
        }
    }

    protected void setForceGdprApplies() {
        if (mConsentData != null) {
            addParam(FORCE_GDPR_APPLIES, mConsentData.isForceGdprApplies());
        }
    }

    protected void setCurrentConsentStatus() {
        if (mPersonalInfoManager != null) {
            addParam(CURRENT_CONSENT_STATUS_KEY, mPersonalInfoManager.getPersonalInfoConsentStatus()
                    .getValue());
        }
    }

    protected void setConsentedPrivacyPolicyVersion() {
        if (mConsentData != null) {
            addParam(CONSENTED_PRIVACY_POLICY_VERSION_KEY,
                    mConsentData.getConsentedPrivacyPolicyVersion());
        }
    }

    protected void setConsentedVendorListVersion() {
        if (mConsentData != null) {
            addParam(CONSENTED_VENDOR_LIST_VERSION_KEY, mConsentData.getConsentedVendorListVersion());
        }
    }

    protected void setCeSettingsHashKey(@Nullable String ceSettingsHash) {
        if (!TextUtils.isEmpty(ceSettingsHash)) {
            addParam(CE_SETTINGS_HASH_KEY, ceSettingsHash);
        } else {
            addParam(CE_SETTINGS_HASH_KEY, "0");
        }
    }

    protected void addBaseParams(final ClientMetadata clientMetadata) {
        setAdUnitId(mAdUnitId);

        setSdkVersion(clientMetadata.getSdkVersion());
        appendAppEngineInfo();
        appendWrapperVersion();
        addParam(PLATFORM_KEY, Constants.ANDROID_PLATFORM);
        setDeviceInfo(clientMetadata.getDeviceOsVersion(),
                clientMetadata.getDeviceManufacturer(),
                clientMetadata.getDeviceModel(),
                clientMetadata.getDeviceProduct(),
                clientMetadata.getDeviceHardware());
        setBundleId(clientMetadata.getAppPackageName());

        setKeywords(mKeywords);

        if (MoPub.canCollectPersonalInformation()) {
            setUserDataKeywords(mUserDataKeywords);
            setLocation();
        }

        setTimezone(DateAndTime.getTimeZoneOffsetString());

        setOrientation(clientMetadata.getOrientationString());
        setDeviceDimensions(clientMetadata.getDeviceDimensions(), mRequestedAdSize, mWindowInsets);
        setDensity(clientMetadata.getDensity());

        final String networkOperator = clientMetadata.getNetworkOperatorForUrl();
        setMccCode(networkOperator);
        setMncCode(networkOperator);

        setIsoCountryCode(clientMetadata.getIsoCountryCode());
        setCarrierName(clientMetadata.getNetworkOperatorName());

        setNetworkType(clientMetadata.getActiveNetworkType());

        setAppVersion(clientMetadata.getAppVersion());

        setAdvancedBiddingTokens();

        appendAdvertisingInfoTemplates();

        setGdprApplies();

        setForceGdprApplies();

        setCurrentConsentStatus();

        setConsentedPrivacyPolicyVersion();

        setConsentedVendorListVersion();

        addRequestRateParameters();

        setViewability();

        setCeSettingsHashKey(mCeSettingsHash);
    }

    private void addParam(String key, MoPubNetworkType value) {
        addParam(key, value.toString());
    }

    private int mncPortionLength(String networkOperator) {
        return Math.min(3, networkOperator.length());
    }

    private static int calculateLocationStalenessInMilliseconds(final Location location) {
        Preconditions.checkNotNull(location);
        final long locationLastUpdatedInMillis = location.getTime();
        final long nowInMillis = System.currentTimeMillis();
        return (int) (nowInMillis - locationLastUpdatedInMillis);
    }

    private void addRequestRateParameters() {
        final RequestRateTracker rateTracker = RequestRateTracker.getInstance();
        final RequestRateTracker.TimeRecord record = rateTracker.getRecordForAdUnit(mAdUnitId);
        if (record == null || record.mBlockIntervalMs < 1) {
            return;
        }

        addParam(BACKOFF_TIME_MS_KEY, String.valueOf(record.mBlockIntervalMs));
        addParam(BACKOFF_REASON_KEY, record.mReason);
    }

    /**
     * @deprecated As of release 2.4
     */
    @Deprecated
    public AdUrlGenerator withFacebookSupported(boolean enabled) {
        return this;
    }
}
