// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.VisibleForTesting;

import com.iab.omid.library.mopub.Omid;
import com.iab.omid.library.mopub.ScriptInjector;
import com.iab.omid.library.mopub.adsession.Partner;
import com.mopub.common.logging.MoPubLog;

import java.util.Set;

import static com.mopub.common.logging.MoPubLog.SdkLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.SdkLogEvent.CUSTOM_WITH_THROWABLE;

/**
 * Singleton to support OM SDK viewability verification.
 */
public class ViewabilityManager {

    private static final String PARTNER_NAME = "mopub";

    @NonNull
    private static final ViewabilityScriptLoader sScriptLoader = new ViewabilityScriptLoader();

    private static boolean sViewabilityEnabled = true;

    @Nullable
    private Partner mPartner;

    //region Singleton
    private ViewabilityManager() {
    }

    private static class Helper {
        @NonNull
        private static final ViewabilityManager sInstance = new ViewabilityManager();
    }

    @NonNull
    private static ViewabilityManager getInstance() {
        return Helper.sInstance;
    }
    //endregion

    //region Public API
    @UiThread
    public static void activate(@NonNull final Context applicationContext) {
        Preconditions.checkUiThread("activate() must be called on the UI thread.");
        Preconditions.checkNotNull(applicationContext);

        getInstance().init(applicationContext);
    }

    @UiThread
    static boolean isActive() {
        Preconditions.checkUiThread("isActive() must be called on the UI thread.");

        return getInstance().isActiveImpl();
    }

    @UiThread
    @NonNull
    static String getOmidVersion() {
        Preconditions.checkUiThread("getOmidVersion() must be called on the UI thread.");

        return Omid.getVersion();
    }

    static void disableViewability() {
        sViewabilityEnabled = false;
        MoPubLog.log(CUSTOM, "OMSDK Viewability has been disabled");
    }

    public static boolean isViewabilityEnabled() {
        return sViewabilityEnabled;
    }

    @NonNull
    static String getOmidJsServiceContent() {
        return sScriptLoader.getOmidJsServiceContent();
    }

    @Nullable
    static Partner getPartner() {
        return getInstance().mPartner;
    }

    @UiThread
    @NonNull
    public static String injectScriptContentIntoHtml(@NonNull final String adResponseHtml) {
        Preconditions.checkUiThread();
        Preconditions.checkNotNull(adResponseHtml);

        if (!isViewabilityEnabled()) {
            return adResponseHtml;
        }

        try {
            return ScriptInjector.injectScriptContentIntoHtml(
                    getOmidJsServiceContent(),
                    adResponseHtml);
        } catch (Throwable e) {
            MoPubLog.log(CUSTOM_WITH_THROWABLE, "Failed to inject OM script into HTML. ", e);
            return adResponseHtml;
        }
    }

    @NonNull
    static String injectScriptUrlIntoHtml(@NonNull final String adResponseHtml, @NonNull final String scriptUrl) {
        Preconditions.checkNotNull(adResponseHtml);
        Preconditions.checkNotNull(scriptUrl);

        if (!isViewabilityEnabled()) {
            return adResponseHtml;
        }

        final String hack_value = "cfc10ccaf0724c4cbc6122cf51421f03";
        final String placeholder_tag = "<script type=\"text/javascript\">" + hack_value + "</script>";
        final String valid_tag = "<script src=\"" + scriptUrl + "\"></script>";

        try {
            final String intermediate = ScriptInjector.injectScriptContentIntoHtml(
                    hack_value,
                    adResponseHtml);
            final String modifiedHtml = intermediate.replace(placeholder_tag, valid_tag);
            if (!modifiedHtml.equals(intermediate)) {
                return modifiedHtml;
            }
        } catch (Throwable e) {
            MoPubLog.log(CUSTOM_WITH_THROWABLE, "Failed to inject script URL into HTML. ", e);
        }

        return adResponseHtml;
    }

    @NonNull
    public static String injectVerificationUrlsIntoHtml(@NonNull final String adResponseHtml, @Nullable final Set<ViewabilityVendor> vendors) {
        Preconditions.checkNotNull(adResponseHtml);

        if (vendors == null || TextUtils.isEmpty(adResponseHtml)) {
            return adResponseHtml;
        }

        String responseHtml = adResponseHtml;
        for (final ViewabilityVendor vendor : vendors) {
            if (vendor != null) {
                responseHtml = injectScriptUrlIntoHtml(responseHtml, vendor.getJavascriptResourceUrl().toString());
            }
        }

        return responseHtml;
    }
    //endregion

    //region Internal Implementation
    @UiThread
    private void init(@NonNull final Context applicationContext) {
        if (mPartner != null) {
            MoPubLog.log(CUSTOM, "ViewabilityManager already initialized.");
            return;
        }

        try {
            Omid.activate(applicationContext.getApplicationContext());

            mPartner = Partner.createPartner(PARTNER_NAME, MoPub.SDK_VERSION);
        } catch (IllegalArgumentException e) {
            MoPubLog.log(CUSTOM_WITH_THROWABLE, "createPartner() ", e);
        }
    }

    @UiThread
    private boolean isActiveImpl() {
        try {
            return sViewabilityEnabled && Omid.isActive() && mPartner != null;
        } catch (Exception ex) {
            MoPubLog.log(CUSTOM_WITH_THROWABLE, "isActive() ", ex);
        }

        return false;
    }
    //endregion

    @VisibleForTesting
    public static void setViewabilityEnabled(boolean enabled) {
        sViewabilityEnabled = enabled;
    }
}
