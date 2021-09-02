// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.content.Context;

public class HtmlWebView extends BaseHtmlWebView {

    public HtmlWebView(Context context) {
        super(context);
    }

    public void init(BaseWebViewListener baseWebViewListener, String dspCreativeId) {
        super.init();
        final HtmlWebViewClient mHtmlWebViewClient =
                new HtmlWebViewClient(this, baseWebViewListener, dspCreativeId);
        setWebViewClient(mHtmlWebViewClient);
    }
}
