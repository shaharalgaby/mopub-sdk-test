// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.network;

import androidx.annotation.Nullable;

import com.mopub.common.logging.MoPubLog;

import static com.mopub.common.logging.MoPubLog.SdkLogEvent.CUSTOM;

public class SingleImpression {
    @Nullable
    private final String mAdUnitId;
    @Nullable
    private final ImpressionData mImpressionData;

    public SingleImpression(@Nullable String adUnitid, @Nullable ImpressionData data) {
        mAdUnitId = adUnitid;
        mImpressionData = data;
    }

    public void sendImpression() {
        if (mAdUnitId != null) {
            ImpressionsEmitter.send(mAdUnitId, mImpressionData);
        } else {
            MoPubLog.log(CUSTOM, "SingleImpression ad unit id may not be null.");
        }
    }
}
