// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.content.Context;

import com.mopub.common.AdUrlGenerator;
import com.mopub.common.ClientMetadata;
import com.mopub.common.Constants;

public class WebViewAdUrlGenerator extends AdUrlGenerator {

    public WebViewAdUrlGenerator(Context context) {
        super(context);
    }

    @Override
    public String generateUrlString(String serverHostname) {
        initUrlString(serverHostname, Constants.AD_HANDLER);

        setApiVersion("6");

        final ClientMetadata clientMetadata = ClientMetadata.getInstance(mContext);
        addBaseParams(clientMetadata);

        setMraidFlag(true);

        return getFinalUrlString();
    }
}
