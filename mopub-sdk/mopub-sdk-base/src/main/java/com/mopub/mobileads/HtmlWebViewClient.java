// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.content.Context;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;

import com.mopub.common.UrlAction;
import com.mopub.common.UrlHandler;

import java.util.EnumSet;

class HtmlWebViewClient extends WebViewClient {

    private final EnumSet<UrlAction> SUPPORTED_URL_ACTIONS = EnumSet.of(
            UrlAction.HANDLE_MOPUB_SCHEME,
            UrlAction.IGNORE_ABOUT_SCHEME,
            UrlAction.HANDLE_PHONE_SCHEME,
            UrlAction.OPEN_APP_MARKET,
            UrlAction.OPEN_NATIVE_BROWSER,
            UrlAction.OPEN_IN_APP_BROWSER,
            UrlAction.HANDLE_SHARE_TWEET,
            UrlAction.FOLLOW_DEEP_LINK_WITH_FALLBACK,
            UrlAction.FOLLOW_DEEP_LINK);

    private final Context mContext;
    private final String mDspCreativeId;
    private BaseHtmlWebView.BaseWebViewListener mBaseWebViewListener;
    private final BaseHtmlWebView mHtmlWebView;

    HtmlWebViewClient(BaseHtmlWebView htmlWebView,
                      BaseHtmlWebView.BaseWebViewListener baseWebViewListener,
                      String dspCreativeId) {
        mHtmlWebView = htmlWebView;
        mDspCreativeId = dspCreativeId;
        mContext = htmlWebView.getContext();
        mBaseWebViewListener = baseWebViewListener;
    }

    @Override
    public boolean shouldOverrideUrlLoading(final WebView view, final String url) {
        new UrlHandler.Builder()
                .withDspCreativeId(mDspCreativeId)
                .withSupportedUrlActions(SUPPORTED_URL_ACTIONS)
                .withResultActions(new UrlHandler.ResultActions() {
                    @Override
                    public void urlHandlingSucceeded(@NonNull String url,
                            @NonNull UrlAction urlAction) {
                        if (mHtmlWebView.wasClicked()) {
                            if (mBaseWebViewListener != null) {
                                mBaseWebViewListener.onClicked();
                            }
                            mHtmlWebView.onResetUserClick();
                        }
                    }

                    @Override
                    public void urlHandlingFailed(@NonNull String url,
                            @NonNull UrlAction lastFailedUrlAction) {
                    }
                })
                .withMoPubSchemeListener(new UrlHandler.MoPubSchemeListener() {
                    @Override
                    public void onFinishLoad() {
                        // Called when window.location="mopub://finishLoad"
                        mHtmlWebView.setPageLoaded();
                        if (mBaseWebViewListener != null) {
                            mBaseWebViewListener.onLoaded(mHtmlWebView);
                        }
                    }

                    @Override
                    public void onClose() {
                        if (mBaseWebViewListener != null) {
                            mBaseWebViewListener.onClose();
                        }
                    }

                    @Override
                    public void onFailLoad() {
                        // Called when window.location="mopub://failLoad"
                        mHtmlWebView.stopLoading();
                        if (mBaseWebViewListener != null) {
                            mBaseWebViewListener.onFailedToLoad(MoPubErrorCode.HTML_LOAD_ERROR);
                        }
                    }

                    @Override
                    public void onCrash() {
                        if (mBaseWebViewListener != null) {
                            mBaseWebViewListener.onFailed();
                        }
                    }
                })
                .build().handleUrl(mContext, url, mHtmlWebView.wasClicked());
        return true;
    }
}
