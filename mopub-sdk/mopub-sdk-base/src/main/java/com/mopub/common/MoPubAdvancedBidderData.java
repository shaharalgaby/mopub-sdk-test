// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common;

import androidx.annotation.NonNull;

import com.mopub.common.logging.MoPubLog;

import org.json.JSONException;
import org.json.JSONObject;

import static com.mopub.common.logging.MoPubLog.SdkLogEvent.CUSTOM;

/**
 * Data object holding advanced bidding data.
 * {"[mCreativeNetworkName]" : {"token" : "[mToken]"}}
 */
public class MoPubAdvancedBidderData {
    private static final String TOKEN_KEY = "token";

    @NonNull final String mToken;
    @NonNull final String mCreativeNetworkName;

    public MoPubAdvancedBidderData(@NonNull final String token,
            @NonNull final String creativeNetworkName) {
        Preconditions.checkNotNull(token);
        Preconditions.checkNotNull(creativeNetworkName);

        mToken = token;
        mCreativeNetworkName = creativeNetworkName;
    }

    @NonNull
    public JSONObject toJson(){
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(TOKEN_KEY, mToken);
        } catch (JSONException e) {
            MoPubLog.log(CUSTOM, "Invalid token format: " + mToken);
        }
        return jsonObject;
    }
}
