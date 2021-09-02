// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common;

import android.view.View;
import android.webkit.WebView;

import androidx.annotation.NonNull;

import com.mopub.common.test.support.SdkTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
public class ExternalViewabilitySessionManagerTest {

    private ExternalViewabilitySessionManager subject;

    @Before
    public void setup() {
        ExternalViewabilitySessionManager.setCreator(null);
        subject = ExternalViewabilitySessionManager.create();
        subject.createWebViewSession(mock(WebView.class));
    }

    @Test
    public void startSession_callsViewabilityTracker_startTracking() {
        final ViewabilityTracker viewabilityTracker = mockViewabilityTracker(subject);

        subject.startSession();

        verify(viewabilityTracker).startTracking();
    }

    @Test
    public void startSession_movesInternalObstructionsSet_toViewabilityTracker() {
        subject.registerFriendlyObstruction(mock(View.class), ViewabilityObstruction.CLOSE_BUTTON);
        assertEquals(1, subject.mObstructions.size());
        subject.registerFriendlyObstruction(mock(View.class), ViewabilityObstruction.CTA_BUTTON);
        assertEquals(2, subject.mObstructions.size());

        final ViewabilityTracker viewabilityTracker = mockViewabilityTracker(subject);
        subject.startSession();

        assertEquals(0, subject.mObstructions.size());
        verify(viewabilityTracker).registerFriendlyObstructions(subject.mObstructions);
    }

    @Test
    public void isTracking_callsViewabilityTracker_isTracking() {
        final ViewabilityTracker viewabilityTracker = mockViewabilityTracker(subject);

        when(viewabilityTracker.isTracking()).thenReturn(true);
        assertTrue(subject.isTracking());

        when(viewabilityTracker.isTracking()).thenReturn(false);
        assertFalse(subject.isTracking());

        verify(viewabilityTracker, times(2)).isTracking();
    }

    @Test
    public void trackImpression_callsViewabilityTracker_trackImpression() {
        final ViewabilityTracker viewabilityTracker = mockViewabilityTracker(subject);

        subject.trackImpression();

        verify(viewabilityTracker).trackImpression();
    }

    @Test
    public void hasImpressionOccured_callsViewabilityTracker_isImpressionOccurred() {
        final ViewabilityTracker viewabilityTracker = mockViewabilityTracker(subject);

        when(viewabilityTracker.hasImpressionOccurred()).thenReturn(true);
        assertTrue(subject.hasImpressionOccurred());

        when(viewabilityTracker.hasImpressionOccurred()).thenReturn(false);
        assertFalse(subject.hasImpressionOccurred());

        verify(viewabilityTracker, times(2)).hasImpressionOccurred();
    }

    @Test
    public void endSession_callsViewabilityTracker_stopTracking() {
        final ViewabilityTracker viewabilityTracker = mockViewabilityTracker(subject);

        subject.endSession();

        verify(viewabilityTracker).stopTracking();
    }

    @Test
    public void onVideoPrepared_callsViewabilityTracker_videoPrepared() {
        final ViewabilityTracker viewabilityTracker = mockViewabilityTracker(subject);

        subject.onVideoPrepared(6789);

        verify(viewabilityTracker).videoPrepared(6.789f);
    }

    @Test
    public void recordVideoEvent_callsViewabilityTracker_trackVideo() {
        final ViewabilityTracker viewabilityTracker = mockViewabilityTracker(subject);

        subject.recordVideoEvent(VideoEvent.AD_BUFFER_END, 100);

        verify(viewabilityTracker).trackVideo(VideoEvent.AD_BUFFER_END);
    }

    @Test
    public void registerFriendlyObstruction_whenSessionNotStarted_addsObstructionToInternalSet() {
        subject.registerFriendlyObstruction(mock(View.class), ViewabilityObstruction.CLOSE_BUTTON);
        assertEquals(1, subject.mObstructions.size());

        subject.registerFriendlyObstruction(mock(View.class), ViewabilityObstruction.CTA_BUTTON);
        assertEquals(2, subject.mObstructions.size());

        subject.registerFriendlyObstruction(mock(View.class), ViewabilityObstruction.OTHER);
        assertEquals(3, subject.mObstructions.size());
    }

    @Test
    public void registerFriendlyObstruction_whenSessionStarted_callsViewabilityTracker_doesNotAddToInternalSet() {
        final ViewabilityTracker viewabilityTracker = mockViewabilityTracker(subject);
        final View mockView = mock(View.class);

        subject.registerFriendlyObstruction(mockView, ViewabilityObstruction.CLOSE_BUTTON);
        assertEquals(0, subject.mObstructions.size());
        verify(viewabilityTracker).registerFriendlyObstruction(eq(mockView), eq(ViewabilityObstruction.CLOSE_BUTTON));
    }

    // utility
    public ViewabilityTracker mockViewabilityTracker(final @NonNull ExternalViewabilitySessionManager sessionManager) {
        final ViewabilityTracker mockViewabilityTracker = mock(ViewabilityTracker.class);
        sessionManager.setMockViewabilityTracker(mockViewabilityTracker);
        return mockViewabilityTracker;
    }
}
