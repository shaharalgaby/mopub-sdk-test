// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common;

import android.text.TextUtils;
import android.util.Pair;
import android.view.View;
import android.webkit.WebView;

import androidx.annotation.NonNull;

import com.iab.omid.library.mopub.adsession.AdEvents;
import com.iab.omid.library.mopub.adsession.AdSession;
import com.iab.omid.library.mopub.adsession.AdSessionConfiguration;
import com.iab.omid.library.mopub.adsession.AdSessionContext;
import com.iab.omid.library.mopub.adsession.CreativeType;
import com.iab.omid.library.mopub.adsession.ImpressionType;
import com.iab.omid.library.mopub.adsession.Owner;
import com.iab.omid.library.mopub.adsession.Partner;
import com.iab.omid.library.mopub.adsession.VerificationScriptResource;
import com.iab.omid.library.mopub.adsession.media.Position;
import com.iab.omid.library.mopub.adsession.media.VastProperties;
import com.mopub.common.logging.MoPubLog;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static com.mopub.common.logging.MoPubLog.SdkLogEvent.CUSTOM;

class ViewabilityTracker {
    // STARTED_VIDEO is equal to STARTED
    protected enum STATE {INIT, STARTED, STARTED_VIDEO, IMPRESSED, STOPPED}

    private static final String CUSTOM_REFERENCE_DATA = "";
    private static final String CONTENT_URL = "";

    @NonNull
    private final AdSession adSession;
    @NonNull
    private final AdEvents adEvents;
    private boolean impressionOccurred = false;
    protected boolean tracking = false;

    protected STATE state;

    private static final AtomicInteger sessionCounter = new AtomicInteger(0);
    int sessionID;

    //region Create ViewabilityTracker

    /**
     * Factory method to create ViewabilityTracker to track viewability of a WebView
     *
     * @param webView to track viewability on
     * @throws IllegalArgumentException error
     */
    @NonNull
    static ViewabilityTracker createWebViewTracker(@NonNull final WebView webView) throws IllegalArgumentException {
        final Partner partner = ViewabilityManager.getPartner();
        if (partner == null) {
            throw new IllegalArgumentException("Parameter 'partner' may not be null.");
        }

        final AdSessionContext adSessionContext =
                AdSessionContext.createHtmlAdSessionContext(partner,
                        webView,
                        CONTENT_URL,
                        CUSTOM_REFERENCE_DATA);

        final AdSessionConfiguration adSessionConfiguration = AdSessionConfiguration.createAdSessionConfiguration(
                CreativeType.HTML_DISPLAY,
                ImpressionType.BEGIN_TO_RENDER,
                Owner.NATIVE,
                Owner.NONE,
                false);

        final AdSession adSession = AdSession.createAdSession(adSessionConfiguration, adSessionContext);

        final AdEvents adEvents = AdEvents.createAdEvents(adSession);
        return new ViewabilityTracker(adSession, adEvents, webView);
    }

    /**
     * Factory method to create ViewabilityTracker to track viewability of native ads
     *
     * @param adView             to track viewability on
     * @param viewabilityVendors list of third party viewability vendors
     * @throws IllegalArgumentException error
     */
    @NonNull
    static ViewabilityTracker createNativeTracker(@NonNull final View adView,
                                                  @NonNull final Set<ViewabilityVendor> viewabilityVendors)
            throws IllegalArgumentException {
        if (viewabilityVendors.size() == 0) {
            throw new IllegalArgumentException("Empty viewability vendors list.");
        }

        final AdSession adSession = createAdSession(
                CreativeType.NATIVE_DISPLAY,
                viewabilityVendors,
                Owner.NONE);

        final AdEvents adEvents = AdEvents.createAdEvents(adSession);
        return new ViewabilityTracker(adSession, adEvents, adView);
    }

    static AdSession createAdSession(@NonNull final CreativeType creativeType,
                                     @NonNull final Set<ViewabilityVendor> viewabilityVendors,
                                     @NonNull final Owner mediaEventsOwner) {
        Preconditions.checkNotNull(creativeType);
        Preconditions.checkNotNull(viewabilityVendors);
        Preconditions.checkNotNull(mediaEventsOwner);

        final List<VerificationScriptResource> verificationScriptResources =
                createVerificationResources(viewabilityVendors);

        if (verificationScriptResources.isEmpty()) {
            throw new IllegalArgumentException("verificationScriptResources is empty");
        }

        final Partner partner = ViewabilityManager.getPartner();
        if (partner == null) {
            throw new IllegalArgumentException("Parameter 'partner' may not be null.");
        }

        final AdSessionContext adSessionContext =
                AdSessionContext.createNativeAdSessionContext(partner,
                        ViewabilityManager.getOmidJsServiceContent(),
                        verificationScriptResources,
                        CONTENT_URL,
                        CUSTOM_REFERENCE_DATA);

        final AdSessionConfiguration adSessionConfiguration =
                AdSessionConfiguration.createAdSessionConfiguration(
                        creativeType,
                        ImpressionType.BEGIN_TO_RENDER,
                        Owner.NATIVE,
                        mediaEventsOwner,
                        false);

        return AdSession.createAdSession(adSessionConfiguration, adSessionContext);
    }

    /**
     * @param adSession OM SDK Session
     * @param adEvents  OM SDK Events
     * @param adView    UI object to track viewability on
     */
    ViewabilityTracker(@NonNull final AdSession adSession,
                       @NonNull final AdEvents adEvents,
                       @NonNull final View adView) {
        Preconditions.checkNotNull(adSession);
        Preconditions.checkNotNull(adEvents);
        Preconditions.checkNotNull(adView);

        state = STATE.INIT;
        this.adSession = adSession;
        this.adEvents = adEvents;
        sessionID = sessionCounter.incrementAndGet();

        registerTrackedView(adView);
    }
    //endregion

    boolean isTracking() {
        return tracking;
    }

    void changeState(@NonNull final STATE newState) {
        boolean modified = false;

        if (ViewabilityManager.isActive()) {
            switch (newState) {
                case STARTED:
                    if (state == STATE.INIT) {
                        // Start the session
                        adSession.start();
                        adEvents.loaded();
                        tracking = true;
                        modified = true;
                    }
                    break;
                case STARTED_VIDEO:
                    if (state == STATE.INIT) {
                        // Start the video session
                        adSession.start();
                        final VastProperties vProps = VastProperties.createVastPropertiesForNonSkippableMedia(true, Position.STANDALONE);
                        adEvents.loaded(vProps);
                        tracking = true;
                        modified = true;
                    }
                    break;
                case IMPRESSED:
                    if (impressionOccurred) {
                        break;
                    }
                    if (state == STATE.STARTED || state == STATE.STARTED_VIDEO) {
                        adEvents.impressionOccurred();
                        impressionOccurred = true;
                        modified = true;
                    }
                    break;
                case STOPPED:
                    if (state != STATE.INIT && state != STATE.STOPPED) {
                        adSession.finish();
                        tracking = false;
                        modified = true;
                    }
                    break;
            }
        }

        if (modified) {
            state = newState;
            log("new state: " + state.name() + " " + sessionID);
        } else {
            log("skip transition from: " + state + " to " + newState);
        }
    }

    void startTracking() {
        log("startTracking(): " + sessionID);
        changeState(STATE.STARTED);
    }

    void stopTracking() {
        log("stopTracking(): " + sessionID);
        changeState(STATE.STOPPED);
    }

    public void trackImpression() {
        log("trackImpression(): " + sessionID);
        changeState(STATE.IMPRESSED);
    }

    boolean hasImpressionOccurred() {
        return impressionOccurred;
    }

    void registerTrackedView(@NonNull final View adView) {
        adSession.registerAdView(adView);
    }

    /**
     * @param obstructions List of friendly obstruction objects
     */
    void registerFriendlyObstructions(@NonNull final Iterable<Pair<View, ViewabilityObstruction>> obstructions) {
        for (Pair<View, ViewabilityObstruction> pair : obstructions) {
            try {
                registerFriendlyObstruction(pair.first, pair.second);
            } catch (IllegalArgumentException ex) {
                MoPubLog.log(CUSTOM, "registerFriendlyObstructions() " + ex.getLocalizedMessage());
            }
        }
    }

    /**
     * @param view    obstruction view
     * @param purpose reason to have this view on top of the Ad.
     */
    void registerFriendlyObstruction(@NonNull final View view, @NonNull ViewabilityObstruction purpose) {
        log("registerFriendlyObstruction(): " + sessionID);
        adSession.addFriendlyObstruction(view, purpose.value, " ");
    }

    /**
     * @param view friendly obstruction to remove
     */
    void removeFriendlyObstruction(@NonNull final View view) {
        Preconditions.checkNotNull(view);

        log("removeFriendlyObstruction(): " + sessionID);
        adSession.removeFriendlyObstruction(view);
    }

    @NonNull
    private static List<VerificationScriptResource> createVerificationResources(
            @NonNull final Set<ViewabilityVendor> viewabilityVendors) {
        final ArrayList<VerificationScriptResource> list = new ArrayList<>();

        for (ViewabilityVendor vendor : viewabilityVendors) {
            try {
                if (TextUtils.isEmpty(vendor.getVendorKey()) || TextUtils.isEmpty(vendor.getVerificationParameters())) {
                    list.add(VerificationScriptResource.createVerificationScriptResourceWithoutParameters(vendor.getJavascriptResourceUrl()));
                } else {
                    list.add(VerificationScriptResource.createVerificationScriptResourceWithParameters(
                            vendor.getVendorKey(),
                            vendor.getJavascriptResourceUrl(),
                            vendor.getVerificationParameters()));
                }
            } catch (Exception ex) {
                // no need for message
            }
        }

        return list;
    }

    void videoPrepared(final float duration) {
    }


    void trackVideo(@NonNull final VideoEvent videoEvent) {
    }

    void log(@NonNull final String message) {
        if (ViewabilityManager.isViewabilityEnabled()) {
            MoPubLog.log(CUSTOM, "OMSDK " + message);
        }
    }
}
