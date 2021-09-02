// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.nativeads;

import android.content.Context;
import androidx.annotation.NonNull;

import com.mopub.common.BaseUrlGenerator;
import com.mopub.common.ClientMetadata;
import com.mopub.common.Constants;

class PositioningUrlGenerator extends BaseUrlGenerator {
    private static final String POSITIONING_API_VERSION = "1";

    @NonNull private final Context mContext;
    @NonNull private String mAdUnitId;

    public PositioningUrlGenerator(@NonNull Context context) {
        mContext = context;
    }

    @NonNull
    public PositioningUrlGenerator withAdUnitId(@NonNull final String adUnitId) {
        mAdUnitId = adUnitId;
        return this;
    }

    @Override
    public String generateUrlString(@NonNull final String serverHostname) {
        initUrlString(serverHostname, Constants.POSITIONING_HANDLER);

        setAdUnitId(mAdUnitId);

        setApiVersion(POSITIONING_API_VERSION);

        ClientMetadata clientMetadata = ClientMetadata.getInstance(mContext);

        addParam(SDK_VERSION_KEY, clientMetadata.getSdkVersion());

        appendAppEngineInfo();

        appendWrapperVersion();

        addParam(PLATFORM_KEY, Constants.ANDROID_PLATFORM);
        setDeviceInfo(clientMetadata.getDeviceOsVersion(),
                clientMetadata.getDeviceManufacturer(),
                clientMetadata.getDeviceModel(),
                clientMetadata.getDeviceProduct(),
                clientMetadata.getDeviceHardware());

        setAppVersion(clientMetadata.getAppVersion());

        appendAdvertisingInfoTemplates();

        return getFinalUrlString();
    }

    private void setAdUnitId(@NonNull String adUnitId) {
        addParam("id", adUnitId);
    }
}
