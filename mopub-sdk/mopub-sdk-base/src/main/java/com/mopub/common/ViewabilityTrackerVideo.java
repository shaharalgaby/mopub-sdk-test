// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common;

import android.view.View;

import androidx.annotation.NonNull;

import com.iab.omid.library.mopub.adsession.AdEvents;
import com.iab.omid.library.mopub.adsession.AdSession;
import com.iab.omid.library.mopub.adsession.CreativeType;
import com.iab.omid.library.mopub.adsession.Owner;
import com.iab.omid.library.mopub.adsession.media.InteractionType;
import com.iab.omid.library.mopub.adsession.media.MediaEvents;
import com.iab.omid.library.mopub.adsession.media.PlayerState;

import java.util.Set;

class ViewabilityTrackerVideo extends ViewabilityTracker {

    @NonNull
    private MediaEvents mediaEvents;

    /**
     * Factory method to create ViewabilityTracker to track viewability of video ads
     *
     * @param videoView          to track viewability on
     * @param viewabilityVendors list of third party viewability vendors
     * @throws IllegalArgumentException error
     */
    @NonNull
    static ViewabilityTracker createVastVideoTracker(@NonNull final View videoView,
                                                     @NonNull final Set<ViewabilityVendor> viewabilityVendors)
            throws IllegalArgumentException {
        final AdSession adSession = createAdSession(
                CreativeType.VIDEO,
                viewabilityVendors,
                Owner.NATIVE);

        final AdEvents adEvents = AdEvents.createAdEvents(adSession);
        return new ViewabilityTrackerVideo(adSession, adEvents, videoView);
    }

    /**
     * private constructor. Use {@link ViewabilityTrackerVideo#createVastVideoTracker createVastVideoTracker}
     * to create ViewabilityTrackerVideo object
     *
     * @param adSession OM SDK Session
     * @param adEvents  OM SDK Events
     * @param videoView to track viewability on
     * @throws IllegalArgumentException error
     */
    private ViewabilityTrackerVideo(@NonNull AdSession adSession,
                                    @NonNull AdEvents adEvents,
                                    @NonNull final View videoView) throws IllegalArgumentException, IllegalStateException {
        this(adSession, adEvents, videoView, MediaEvents.createMediaEvents(adSession));
    }

    /**
     * private constructor. Use {@link ViewabilityTrackerVideo#createVastVideoTracker createVastVideoTracker}
     * to create ViewabilityTrackerVideo object
     *
     * @param adSession   OM SDK Session
     * @param adEvents    OM SDK Events
     * @param videoView   to track viewability on
     * @param mediaEvents MediaEvents object
     * @throws IllegalArgumentException error
     */
    @VisibleForTesting
    ViewabilityTrackerVideo(@NonNull AdSession adSession,
                            @NonNull AdEvents adEvents,
                            @NonNull final View videoView,
                            @NonNull MediaEvents mediaEvents) throws IllegalArgumentException, IllegalStateException {
        super(adSession, adEvents, videoView);

        this.mediaEvents = mediaEvents;
        log("ViewabilityTrackerVideo() sesseionId:" + sessionID);
    }

    @Override
    public void startTracking() {
        log("ViewabilityTrackerVideo.startTracking() sesseionId: " + sessionID);
        changeState(STATE.STARTED_VIDEO);
    }

    @Override
    public void videoPrepared(final float durationSeconds) {
        log("videoPrepared() duration= " + durationSeconds);
        if (!isTracking()) {
            log("videoPrepared() not tracking yet: " + sessionID);
            return;
        }
        mediaEvents.start(durationSeconds, 1.0f);
    }

    @Override
    public void trackVideo(@NonNull final VideoEvent videoEvent) {
        if (!isTracking()) {
            log("trackVideo() skip event: " + videoEvent.name());
            return;
        }

        log("trackVideo() event: " + videoEvent.name() + " " + sessionID);
        switch (videoEvent) {
            case AD_IMPRESSED:
                trackImpression();
                break;
            case AD_PAUSED:
                mediaEvents.pause();
                break;
            case AD_RESUMED:
                mediaEvents.resume();
                break;
            case AD_SKIPPED:
                mediaEvents.skipped();
                break;

            case AD_CLICK_THRU:
                mediaEvents.adUserInteraction(InteractionType.CLICK);
                break;
            case RECORD_AD_ERROR:
                mediaEvents.skipped();
                break;

            case AD_BUFFER_START:
                mediaEvents.bufferStart();
                break;
            case AD_BUFFER_END:
                mediaEvents.bufferFinish();
                break;

            case AD_VIDEO_FIRST_QUARTILE:
                mediaEvents.firstQuartile();
                break;
            case AD_VIDEO_MIDPOINT:
                mediaEvents.midpoint();
                break;
            case AD_VIDEO_THIRD_QUARTILE:
                mediaEvents.thirdQuartile();
                break;
            case AD_COMPLETE:
                mediaEvents.complete();
                break;

            case AD_FULLSCREEN:
                mediaEvents.playerStateChange(PlayerState.FULLSCREEN);
                break;
            case AD_NORMAL:
                mediaEvents.playerStateChange(PlayerState.NORMAL);
                break;
            case AD_VOLUME_CHANGE:
                mediaEvents.volumeChange(1.0f);
                break;
        }
    }
}
