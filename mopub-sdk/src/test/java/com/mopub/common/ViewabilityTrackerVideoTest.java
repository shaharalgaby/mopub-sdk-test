// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common;

import android.app.Activity;
import android.view.View;

import com.iab.omid.library.mopub.adsession.AdEvents;
import com.iab.omid.library.mopub.adsession.AdSession;
import com.iab.omid.library.mopub.adsession.media.InteractionType;
import com.iab.omid.library.mopub.adsession.media.MediaEvents;
import com.iab.omid.library.mopub.adsession.media.PlayerState;
import com.iab.omid.library.mopub.adsession.media.VastProperties;
import com.mopub.common.test.support.SdkTestRunner;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.Robolectric;

import java.util.Collections;

import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(SdkTestRunner.class)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*", "org.json.*"})
@PrepareForTest({AdEvents.class, AdSession.class, MediaEvents.class})
public class ViewabilityTrackerVideoTest {
    private final float DURATION = 77f;

    private ViewabilityTrackerVideo subject;

    private AdSession mockAdSession;
    private AdEvents mockAdEvents;
    private View mockView;
    private MediaEvents mockMediaEvents;

    private Activity activity;

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    @Before
    public void setUp() throws Exception {
        activity = Robolectric.buildActivity(Activity.class).create().get();

        mockAdSession = PowerMockito.mock(AdSession.class);
        mockAdEvents = PowerMockito.mock(AdEvents.class);
        mockView = PowerMockito.mock(View.class);
        mockMediaEvents = PowerMockito.mock(MediaEvents.class);

        subject = new ViewabilityTrackerVideo(mockAdSession, mockAdEvents, mockView, mockMediaEvents);
    }

    @After
    public void tearDown() {
    }

    @Test
    public void createVastVideoTracker_withValidParameters_returnsViewabilityTracker() {
        ViewabilityManager.activate(activity);
        final ViewabilityVendor.Builder builder = new ViewabilityVendor.Builder("https://js.com");

        final ViewabilityTracker tracker = ViewabilityTrackerVideo.createVastVideoTracker(
                mockView,
                Collections.singleton(builder.build()));

        assertThat(tracker).isNotNull();
    }

    @Test(expected = IllegalArgumentException.class)
    public void createVastVideoTracker_withEmptyVendorsSet_throwsException() {
        ViewabilityManager.activate(activity);

        ViewabilityTrackerVideo.createVastVideoTracker(mockView, Collections.emptySet());
    }

    @Test
    public void constructor_validateState() throws Exception {
        assertThat(subject.isTracking()).isFalse();
        assertThat(subject.hasImpressionOccurred()).isFalse();
        PowerMockito.verifyPrivate(mockAdSession).invoke("registerAdView", mockView);
    }

    @Test
    public void startTracking_callsOmSdk_setsTrackingTrue() throws Exception {
        subject.startTracking();

        assertThat(subject.isTracking()).isTrue();
        assertThat(subject.hasImpressionOccurred()).isFalse();
        assertThat(subject.state).isEqualTo(ViewabilityTracker.STATE.STARTED_VIDEO);
        PowerMockito.verifyPrivate(mockAdSession).invoke("start");
        PowerMockito.verifyPrivate(mockAdEvents).invoke("loaded", Mockito.any(VastProperties.class));
    }

    @Test
    public void videoPrepared_whenTracking_callsMediaEventsStart() throws Exception {
        subject.startTracking();

        subject.videoPrepared(DURATION);

        PowerMockito.verifyPrivate(mockMediaEvents).invoke("start", DURATION, 1.0f);
    }

    @Test
    public void videoPrepared_whenNotTracking_doesNotCallMediaEvents() {
        assertThat(subject.state).isEqualTo(ViewabilityTracker.STATE.INIT);
        subject.videoPrepared(DURATION);

        PowerMockito.verifyNoMoreInteractions(mockMediaEvents);
    }

    @Test
    public void trackVideo_callsMediaEvents() throws Exception {
        subject.startTracking();

        subject.trackVideo(VideoEvent.AD_SKIPPED);
        PowerMockito.verifyPrivate(mockMediaEvents).invoke("skipped");

        subject.trackVideo(VideoEvent.AD_IMPRESSED);
        PowerMockito.verifyPrivate(mockAdEvents).invoke("impressionOccurred");

        subject.trackVideo(VideoEvent.AD_PAUSED);
        PowerMockito.verifyPrivate(mockMediaEvents).invoke("pause");

        subject.trackVideo(VideoEvent.AD_RESUMED);
        PowerMockito.verifyPrivate(mockMediaEvents).invoke("resume");

        subject.trackVideo(VideoEvent.AD_CLICK_THRU);
        PowerMockito.verifyPrivate(mockMediaEvents).invoke("adUserInteraction", InteractionType.CLICK);

        subject.trackVideo(VideoEvent.AD_BUFFER_START);
        PowerMockito.verifyPrivate(mockMediaEvents).invoke("bufferStart");

        subject.trackVideo(VideoEvent.AD_BUFFER_END);
        PowerMockito.verifyPrivate(mockMediaEvents).invoke("bufferFinish");

        subject.trackVideo(VideoEvent.AD_VIDEO_FIRST_QUARTILE);
        PowerMockito.verifyPrivate(mockMediaEvents).invoke("firstQuartile");

        subject.trackVideo(VideoEvent.AD_VIDEO_MIDPOINT);
        PowerMockito.verifyPrivate(mockMediaEvents).invoke("midpoint");

        subject.trackVideo(VideoEvent.AD_VIDEO_THIRD_QUARTILE);
        PowerMockito.verifyPrivate(mockMediaEvents).invoke("thirdQuartile");

        subject.trackVideo(VideoEvent.AD_COMPLETE);
        PowerMockito.verifyPrivate(mockMediaEvents).invoke("complete");

        subject.trackVideo(VideoEvent.AD_FULLSCREEN);
        PowerMockito.verifyPrivate(mockMediaEvents).invoke("playerStateChange", PlayerState.FULLSCREEN);

        subject.trackVideo(VideoEvent.AD_NORMAL);
        PowerMockito.verifyPrivate(mockMediaEvents).invoke("playerStateChange", PlayerState.NORMAL);
    }
}
