// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common.privacy;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.mopub.common.CloseableLayout;
import com.mopub.common.Constants;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.Intents;
import com.mopub.exceptions.IntentNotResolvableException;

import static com.mopub.common.logging.MoPubLog.SdkLogEvent.CUSTOM;
import static com.mopub.common.privacy.ConsentStatus.EXPLICIT_NO;
import static com.mopub.common.privacy.ConsentStatus.EXPLICIT_YES;
import static com.mopub.mobileads.MoPubErrorCode.RENDER_PROCESS_GONE_UNSPECIFIED;
import static com.mopub.mobileads.MoPubErrorCode.RENDER_PROCESS_GONE_WITH_CRASH;

class ConsentDialogLayout extends CloseableLayout {
    static int FINISHED_LOADING = 101;

    final static String URL_CONSENT_YES = "mopub://consent?yes";
    final static String URL_CONSENT_NO = "mopub://consent?no";
    final static String URL_CLOSE = "mopub://close";

    interface ConsentListener {
        void onConsentClick(ConsentStatus state);
        void onCloseClick();
    }

    interface PageLoadListener {
        void onLoadProgress(int progress);
    }

    @NonNull
    private final WebView mWebView;
    @Nullable
    private PageLoadListener mLoadListener;
    @Nullable
    private ConsentListener mConsentListener;

    public ConsentDialogLayout(@NonNull Context context) {
        this(context, null);
    }

    public ConsentDialogLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ConsentDialogLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mWebView = initWebView();
    }

    void startLoading(@NonNull final String htmlData, @Nullable final PageLoadListener listener) {
        Preconditions.checkNotNull(htmlData);

        mLoadListener = listener;
        setupEventsListeners(mWebView);

        mWebView.loadDataWithBaseURL(Constants.HTTPS + "://" + Constants.HOST + "/",
                htmlData, "text/html", "UTF-8", null);
    }

    void setConsentClickListener(@NonNull final ConsentListener consentListener) {
        Preconditions.checkNotNull(consentListener);
        mConsentListener = consentListener;
    }

    @SuppressLint("SetJavaScriptEnabled")
    private WebView initWebView() {
        WebView webView = new WebView(getContext());
        webView.setVerticalScrollBarEnabled(false);
        webView.setHorizontalScrollBarEnabled(false);

        WebSettings webSettings = webView.getSettings();
        webSettings.setSupportZoom(false);
        webSettings.setBuiltInZoomControls(false);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setLoadWithOverviewMode(true);

        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAppCacheEnabled(true);
        webSettings.setAppCachePath(getContext().getCacheDir().getAbsolutePath());
        webSettings.setAllowFileAccess(false);
        webSettings.setAllowContentAccess(false);

        webSettings.setAllowUniversalAccessFromFileURLs(false);
        webView.setId(View.generateViewId());

        setCloseVisible(false);

        addView(webView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        return webView;
    }

    private void setupEventsListeners(@NonNull final WebView webView) {
        webView.setWebViewClient(webViewClient);
        setOnCloseListener(new CloseableLayout.OnCloseListener() {
            @Override
            public void onClose() {
                if (mConsentListener != null) {
                    mConsentListener.onCloseClick();
                }
            }
        });
    }

    private final WebViewClient webViewClient = new WebViewClient() {
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            if (mLoadListener != null) {
                mLoadListener.onLoadProgress(0);
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            if (mLoadListener != null) {
                mLoadListener.onLoadProgress(FINISHED_LOADING);
            }
            super.onPageFinished(view, url);
        }

        @RequiresApi(Build.VERSION_CODES.O)
        @Override
        public boolean onRenderProcessGone(@Nullable final WebView view, @Nullable final RenderProcessGoneDetail detail) {
            MoPubLog.log(CUSTOM, (detail != null && detail.didCrash())
                    ? RENDER_PROCESS_GONE_WITH_CRASH
                    : RENDER_PROCESS_GONE_UNSPECIFIED);
            return true;
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (URL_CONSENT_YES.equals(url)) {
                if (mConsentListener != null) {
                    mConsentListener.onConsentClick(EXPLICIT_YES);
                }
                return true;
            } else if (URL_CONSENT_NO.equals(url)) {
                if (mConsentListener != null) {
                    mConsentListener.onConsentClick(EXPLICIT_NO);
                }
                return true;
            } else if (URL_CLOSE.equals(url)) {
                if (mConsentListener != null) {
                    mConsentListener.onCloseClick();
                }
                return true;
            } else if (!TextUtils.isEmpty(url)) {
                try {
                    Intents.launchActionViewIntent(getContext(), Uri.parse(url), "Cannot open native browser for " + url);
                    return true;
                } catch (IntentNotResolvableException e) {
                    MoPubLog.log(CUSTOM, e.getMessage());
                }
            }
            return super.shouldOverrideUrlLoading(view, url);
        }
    };
}
