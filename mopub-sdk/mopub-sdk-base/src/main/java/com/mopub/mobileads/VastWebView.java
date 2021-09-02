// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.content.Context;
import android.graphics.Color;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.Constants;
import com.mopub.common.Preconditions;
import com.mopub.common.VisibleForTesting;
import com.mopub.network.Networking;

/**
 * A WebView customized for Vast video needs.
 */
public class VastWebView extends BaseWebView {
    interface VastWebViewClickListener {
        void onVastWebViewClick();
    }

    @Nullable VastWebViewClickListener mVastWebViewClickListener;

    VastWebView(Context context) {
        super(context);

        disableScrollingAndZoom();
        getSettings().setJavaScriptEnabled(true);

        setBackgroundColor(Color.TRANSPARENT);
        setOnTouchListener(new VastWebViewOnTouchListener());
        setId(View.generateViewId());
    }

    void loadData(String data) {
        loadDataWithBaseURL(Networking.getScheme() + "://" + Constants.HOST + "/",
                data, "text/html", "utf-8", null);
    }

    void setVastWebViewClickListener(@NonNull VastWebViewClickListener vastWebViewClickListener) {
        mVastWebViewClickListener = vastWebViewClickListener;
    }

    private void disableScrollingAndZoom() {
        setHorizontalScrollBarEnabled(false);
        setHorizontalScrollbarOverlay(false);
        setVerticalScrollBarEnabled(false);
        setVerticalScrollbarOverlay(false);
        getSettings().setSupportZoom(false);
        setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
    }

    /**
     * Creates and populates a webview.
     *
     * @param context      the context.
     * @param vastResource A resource describing the contents of the webview
     * @return a fully populated webview
     */
    @NonNull
    static VastWebView createView(@NonNull final Context context,
                                  @NonNull final VastResource vastResource) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(vastResource);

        VastWebView webView = new VastWebView(context);
        vastResource.initializeWebView(webView);

        return webView;
    }

    /**
     * Custom on touch listener to easily detect clicks on the entire WebView.
     */
    class VastWebViewOnTouchListener implements View.OnTouchListener {
        private boolean mClickStarted;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mClickStarted = true;
                    break;
                case MotionEvent.ACTION_UP:
                    if (!mClickStarted) {
                        return false;
                    }
                    mClickStarted = false;
                    if (mVastWebViewClickListener != null) {
                        mVastWebViewClickListener.onVastWebViewClick();
                    }
            }

            return false;
        }
    }

    @VisibleForTesting
    @Deprecated
    @NonNull
    VastWebViewClickListener getVastWebViewClickListener() {
        return mVastWebViewClickListener;
    }
}
