// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common;

import android.app.Activity;
import android.content.Context;
import android.util.Pair;
import android.view.View;

import com.iab.omid.library.mopub.adsession.AdEvents;
import com.iab.omid.library.mopub.adsession.AdSession;
import com.mopub.common.test.support.SdkTestRunner;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.Robolectric;

import java.util.HashSet;
import java.util.Set;

import static com.mopub.common.ViewabilityObstruction.CLOSE_BUTTON;
import static com.mopub.common.ViewabilityObstruction.CTA_BUTTON;
import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(SdkTestRunner.class)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*", "org.json.*"})
@PrepareForTest({AdEvents.class, AdSession.class, View.class})
public class ViewabilityTrackerTest {

    private ViewabilityTracker subject;

    private AdSession mockAdSession;
    private AdEvents mockAdEvents;
    private View mockView;

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    @Before
    public void setUp() throws Exception {
        Context context = Robolectric.buildActivity(Activity.class).create().get().getApplicationContext();
        ViewabilityManager.activate(context);

        mockAdSession = PowerMockito.mock(AdSession.class);
        mockAdEvents = PowerMockito.mock(AdEvents.class);
        mockView = PowerMockito.mock(View.class);
    }

    @After
    public void tearDown() {
    }

    @Test
    public void constructor_callsRegisterAdView_internalStateIsCorrect() throws Exception {
        subject = new ViewabilityTracker(mockAdSession, mockAdEvents, mockView);

        assertThat(subject.isTracking()).isFalse();
        assertThat(subject.hasImpressionOccurred()).isFalse();
        assertThat(subject.state).isEqualTo(ViewabilityTracker.STATE.INIT);
        PowerMockito.verifyPrivate(mockAdSession).invoke("registerAdView", mockView);
    }

    @Test
    public void registerFriendlyObstruction_callsAdSession() throws Exception {
        subject = new ViewabilityTracker(mockAdSession, mockAdEvents, mockView);
        View obstruction1 = PowerMockito.mock(View.class);

        subject.registerFriendlyObstruction(obstruction1, CLOSE_BUTTON);

        PowerMockito.verifyPrivate(mockAdSession).invoke("addFriendlyObstruction",
                obstruction1, CLOSE_BUTTON.value, " ");
    }

    @Test
    public void removeFriendlyObstruction_callsAdSession() throws Exception {
        subject = new ViewabilityTracker(mockAdSession, mockAdEvents, mockView);
        View mockViewObstruction = PowerMockito.mock(View.class);

        subject.removeFriendlyObstruction(mockViewObstruction);

        PowerMockito.verifyPrivate(mockAdSession).invoke("removeFriendlyObstruction", mockViewObstruction);
    }

    @Test
    public void registerFriendlyObstructions_callsAdSessionForEachObject() throws Exception {
        subject = new ViewabilityTracker(mockAdSession, mockAdEvents, mockView);
        View obstruction1 = PowerMockito.mock(View.class);
        View obstruction2 = PowerMockito.mock(View.class);
        Set<Pair<View, ViewabilityObstruction>> obstructions = new HashSet<>();
        obstructions.add(new Pair<>(obstruction1, CLOSE_BUTTON));
        obstructions.add(new Pair<>(obstruction2, CTA_BUTTON));

        subject.registerFriendlyObstructions(obstructions);

        PowerMockito.verifyPrivate(mockAdSession).invoke("addFriendlyObstruction",
                obstruction1, CLOSE_BUTTON.value, " ");
        PowerMockito.verifyPrivate(mockAdSession).invoke("addFriendlyObstruction",
                obstruction2, CTA_BUTTON.value, " ");
    }

    @Test
    public void registerTrackedView_callsAdSessionToTrackView() throws Exception {
        subject = new ViewabilityTracker(mockAdSession, mockAdEvents, mockView);
        View newView = PowerMockito.mock(View.class);

        subject.registerTrackedView(newView);

        PowerMockito.verifyPrivate(mockAdSession).invoke("registerAdView", newView);
    }

    @Test
    public void startTracking_startsAdSession() throws Exception {
        subject = new ViewabilityTracker(mockAdSession, mockAdEvents, mockView);

        assertThat(subject.isTracking()).isFalse();
        assertThat(subject.state).isEqualTo(ViewabilityTracker.STATE.INIT);

        subject.startTracking();

        assertThat(subject.isTracking()).isTrue();
        assertThat(subject.state).isEqualTo(ViewabilityTracker.STATE.STARTED);
        PowerMockito.verifyPrivate(mockAdSession).invoke("start");
        PowerMockito.verifyPrivate(mockAdEvents).invoke("loaded");
    }

    @Test
    public void startTracking_secondTime_doesNotChangeState_doesNotCallOmid() throws Exception {
        subject = new ViewabilityTracker(mockAdSession, mockAdEvents, mockView);
        subject.startTracking();

        assertThat(subject.isTracking()).isTrue();
        assertThat(subject.state).isEqualTo(ViewabilityTracker.STATE.STARTED);
        PowerMockito.verifyPrivate(mockAdSession).invoke("start");
        PowerMockito.verifyPrivate(mockAdEvents).invoke("loaded");

        subject.startTracking();

        assertThat(subject.isTracking()).isTrue();
        assertThat(subject.state).isEqualTo(ViewabilityTracker.STATE.STARTED);
        PowerMockito.verifyNoMoreInteractions(mockAdEvents);
    }

    @Test
    public void stopTracking_finishesAdSession_changesStateToStopped() throws Exception {
        subject = new ViewabilityTracker(mockAdSession, mockAdEvents, mockView);
        subject.startTracking();
        assertThat(subject.isTracking()).isTrue();
        assertThat(subject.state).isEqualTo(ViewabilityTracker.STATE.STARTED);
        PowerMockito.verifyPrivate(mockAdSession).invoke("start");
        PowerMockito.verifyPrivate(mockAdEvents).invoke("loaded");

        subject.stopTracking();

        assertThat(subject.isTracking()).isFalse();
        assertThat(subject.state).isEqualTo(ViewabilityTracker.STATE.STOPPED);
        PowerMockito.verifyPrivate(mockAdSession).invoke("finish");
    }

    @Test
    public void stopTracking_whenNotStarted_doesNotChangeState() {
        subject = new ViewabilityTracker(mockAdSession, mockAdEvents, mockView);
        assertThat(subject.state).isEqualTo(ViewabilityTracker.STATE.INIT);

        subject.stopTracking();

        assertThat(subject.isTracking()).isFalse();
        assertThat(subject.state).isEqualTo(ViewabilityTracker.STATE.INIT);
    }

    @Test
    public void trackImpression_callsAdEVentsToRegisterImpression() throws Exception {
        subject = new ViewabilityTracker(mockAdSession, mockAdEvents, mockView);
        subject.startTracking();
        assertThat(subject.isTracking()).isTrue();
        assertThat(subject.hasImpressionOccurred()).isFalse();
        PowerMockito.verifyPrivate(mockAdSession).invoke("start");
        PowerMockito.verifyPrivate(mockAdEvents).invoke("loaded");

        subject.trackImpression();

        PowerMockito.verifyPrivate(mockAdEvents).invoke("impressionOccurred");
        assertThat(subject.hasImpressionOccurred()).isTrue();
    }
}
