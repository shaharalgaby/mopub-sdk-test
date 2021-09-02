// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.app.Activity;
import android.view.View;

import com.mopub.common.ExternalViewabilitySessionManager;
import com.mopub.common.ViewabilityManager;
import com.mopub.common.ViewabilityObstruction;
import com.mopub.common.test.support.SdkTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(SdkTestRunner.class)
public class BaseWebViewViewabilityTest {
    private Activity context;
    private BaseWebViewViewability subject;

    @Before
    public void setup() {
        context = Robolectric.buildActivity(Activity.class).create().postResume().get();
        ViewabilityManager.setViewabilityEnabled(true);
    }

    @Test
    public void constructor_checkState() {
        subject = new BaseWebViewViewability(context);

        assertEquals(ViewabilityManager.isViewabilityEnabled(), subject.viewabilityEnabled);
        assertTrue(subject.automaticImpression);
        assertFalse(subject.pageLoaded);
        assertFalse(subject.pageVisible);
        assertTrue(subject.delayDestroy);
    }

    @Test
    public void constructor_whenViewabilityDisabled_checkState() {
        ViewabilityManager.setViewabilityEnabled(false);
        subject = new BaseWebViewViewability(context);

        assertFalse(subject.viewabilityEnabled);
        assertTrue(subject.automaticImpression);
        assertFalse(subject.pageLoaded);
        assertFalse(subject.pageVisible);
        assertFalse(subject.delayDestroy);
    }

    @Test
    public void onAttachedToWindow_whenPageLoadedFalse_whenViewabilityEnabledTrue_makesNoChanges() {
        final ExternalViewabilitySessionManager mockExternalTracker = mock(ExternalViewabilitySessionManager.class);
        subject = new BaseWebViewViewability(context);
        subject.setMockExternalTracker(mockExternalTracker);
        subject.pageLoaded = false;

        subject.viewabilityEnabled = true;
        subject.onAttachedToWindow();

        assertFalse(subject.pageLoaded);
        assertFalse(subject.pageVisible);
        assertTrue(subject.automaticImpression);
        assertTrue(subject.viewabilityEnabled);
        assertEquals(BaseWebViewViewability.State.INIT, subject.state);
        verifyNoMoreInteractions(mockExternalTracker);
    }

    @Test
    public void onAttachedToWindow_whenPageLoadedTrue_doesCallStartSession_changesStateToStarted() {
        final ExternalViewabilitySessionManager mockExternalTracker = mock(ExternalViewabilitySessionManager.class);
        subject = new BaseWebViewViewability(context);
        subject.setMockExternalTracker(mockExternalTracker);
        subject.pageLoaded = true;

        subject.onAttachedToWindow();

        assertTrue(subject.pageLoaded);
        assertFalse(subject.pageVisible);
        assertTrue(subject.automaticImpression);
        assertTrue(subject.viewabilityEnabled);
        assertEquals(BaseWebViewViewability.State.STARTED, subject.state);
        verify(mockExternalTracker).createWebViewSession(subject);
        verify(mockExternalTracker).startSession();
    }

    @Test
    public void onAttachedToWindow_whenPageLoadedTrue_whenNotInInitState_makesNoChanges() {
        final ExternalViewabilitySessionManager mockExternalTracker = mock(ExternalViewabilitySessionManager.class);
        subject = new BaseWebViewViewability(context);
        subject.setMockExternalTracker(mockExternalTracker);
        subject.pageLoaded = true;

        subject.state = BaseWebViewViewability.State.STARTED;
        subject.onAttachedToWindow();
        assertTrue(subject.pageLoaded);
        assertFalse(subject.pageVisible);
        assertTrue(subject.automaticImpression);
        assertTrue(subject.viewabilityEnabled);
        verifyNoMoreInteractions(mockExternalTracker);
        assertEquals(BaseWebViewViewability.State.STARTED, subject.state);

        subject.state = BaseWebViewViewability.State.IMPRESSED;
        subject.onAttachedToWindow();
        assertTrue(subject.pageLoaded);
        assertFalse(subject.pageVisible);
        assertTrue(subject.automaticImpression);
        assertTrue(subject.viewabilityEnabled);
        verifyNoMoreInteractions(mockExternalTracker);
        assertEquals(BaseWebViewViewability.State.IMPRESSED, subject.state);

        subject.state = BaseWebViewViewability.State.STOPPED;
        subject.onAttachedToWindow();
        assertTrue(subject.pageLoaded);
        assertFalse(subject.pageVisible);
        assertTrue(subject.automaticImpression);
        assertTrue(subject.viewabilityEnabled);
        verifyNoMoreInteractions(mockExternalTracker);
        assertEquals(BaseWebViewViewability.State.STOPPED, subject.state);
    }

    @Test
    public void onAttachedToWindow_whenPageLoadedTrue_whenViewabilityEnabledFalse_makesNoChanges() {
        final ExternalViewabilitySessionManager mockExternalTracker = mock(ExternalViewabilitySessionManager.class);
        subject = new BaseWebViewViewability(context);
        subject.setMockExternalTracker(mockExternalTracker);
        subject.pageLoaded = true;

        subject.viewabilityEnabled = false;
        subject.onAttachedToWindow();

        assertTrue(subject.pageLoaded);
        assertFalse(subject.pageVisible);
        assertTrue(subject.automaticImpression);
        assertFalse(subject.viewabilityEnabled);
        assertEquals(BaseWebViewViewability.State.INIT, subject.state);
        verifyNoMoreInteractions(mockExternalTracker);
    }

    @Test
    public void onDetachedFromWindow_whenViewabilityEnabledTrue_callsEndSession_changesStateToStopped() {
        final ExternalViewabilitySessionManager mockExternalTracker = mock(ExternalViewabilitySessionManager.class);
        subject = new BaseWebViewViewability(context);
        subject.setMockExternalTracker(mockExternalTracker);
        subject.state = BaseWebViewViewability.State.STARTED;

        subject.onDetachedFromWindow();

        assertFalse(subject.pageLoaded);
        assertTrue(subject.automaticImpression);
        assertTrue(subject.viewabilityEnabled);
        assertEquals(BaseWebViewViewability.State.STOPPED, subject.state);
        verify(mockExternalTracker).endSession();
    }

    @Test
    public void onDetachedFromWindow_whenNotInRightState_makesNoChanges() {
        final ExternalViewabilitySessionManager mockExternalTracker = mock(ExternalViewabilitySessionManager.class);
        subject = new BaseWebViewViewability(context);
        subject.setMockExternalTracker(mockExternalTracker);
        subject.viewabilityEnabled = true;

        subject.state = BaseWebViewViewability.State.INIT;
        subject.onDetachedFromWindow();
        assertTrue(subject.automaticImpression);
        assertTrue(subject.viewabilityEnabled);
        verifyNoMoreInteractions(mockExternalTracker);
        assertEquals(BaseWebViewViewability.State.INIT, subject.state);

        subject.state = BaseWebViewViewability.State.STOPPED;
        subject.onDetachedFromWindow();
        assertTrue(subject.automaticImpression);
        assertTrue(subject.viewabilityEnabled);
        verifyNoMoreInteractions(mockExternalTracker);
        assertEquals(BaseWebViewViewability.State.STOPPED, subject.state);
    }

    @Test
    public void onDetachedFromWindow_whenViewabilityEnabledFalse_makesNoChanges() {
        final ExternalViewabilitySessionManager mockExternalTracker = mock(ExternalViewabilitySessionManager.class);
        subject = new BaseWebViewViewability(context);
        subject.setMockExternalTracker(mockExternalTracker);
        subject.state = BaseWebViewViewability.State.STARTED;

        subject.viewabilityEnabled = false;
        subject.onDetachedFromWindow();

        assertTrue(subject.automaticImpression);
        assertFalse(subject.viewabilityEnabled);
        assertEquals(BaseWebViewViewability.State.STARTED, subject.state);
        verifyNoMoreInteractions(mockExternalTracker);
    }

    @Test
    public void onVisibilityChanged_whenVisible_callsTrackImpression_changesStateToImpressed() {
        final ExternalViewabilitySessionManager mockExternalTracker = mock(ExternalViewabilitySessionManager.class);
        subject = new BaseWebViewViewability(context);
        subject.setMockExternalTracker(mockExternalTracker);
        subject.state = BaseWebViewViewability.State.STARTED;

        subject.onVisibilityChanged(subject, View.VISIBLE);

        assertTrue(subject.pageVisible);
        assertTrue(subject.automaticImpression);
        assertTrue(subject.viewabilityEnabled);
        assertEquals(BaseWebViewViewability.State.IMPRESSED, subject.state);
        verify(mockExternalTracker).trackImpression();
    }

    @Test
    public void onVisibilityChanged_whenNotVisible_makesNoChanges() {
        final ExternalViewabilitySessionManager mockExternalTracker = mock(ExternalViewabilitySessionManager.class);
        subject = new BaseWebViewViewability(context);
        subject.setMockExternalTracker(mockExternalTracker);
        subject.state = BaseWebViewViewability.State.STARTED;

        subject.onVisibilityChanged(subject, View.INVISIBLE);

        assertFalse(subject.pageVisible);
        assertTrue(subject.automaticImpression);
        assertTrue(subject.viewabilityEnabled);
        assertEquals(BaseWebViewViewability.State.STARTED, subject.state);
        verifyNoMoreInteractions(mockExternalTracker);
    }

    @Test
    public void onVisibilityChanged_whenAutomaticImpressionFalse_makesNoChanges() {
        final ExternalViewabilitySessionManager mockExternalTracker = mock(ExternalViewabilitySessionManager.class);
        subject = new BaseWebViewViewability(context);
        subject.setMockExternalTracker(mockExternalTracker);
        subject.state = BaseWebViewViewability.State.STARTED;
        subject.disableAutomaticImpression();

        subject.onVisibilityChanged(subject, View.VISIBLE);

        assertFalse(subject.automaticImpression);
        assertTrue(subject.viewabilityEnabled);
        assertEquals(BaseWebViewViewability.State.STARTED, subject.state);
        verifyNoMoreInteractions(mockExternalTracker);
    }

    @Test
    public void onVisibilityChanged_whenNotInStartedState_makesNoChanges() {
        final ExternalViewabilitySessionManager mockExternalTracker = mock(ExternalViewabilitySessionManager.class);
        subject = new BaseWebViewViewability(context);
        subject.setMockExternalTracker(mockExternalTracker);

        subject.state = BaseWebViewViewability.State.INIT;
        subject.onVisibilityChanged(subject, View.VISIBLE);
        assertTrue(subject.automaticImpression);
        assertTrue(subject.viewabilityEnabled);
        verifyNoMoreInteractions(mockExternalTracker);
        assertEquals(BaseWebViewViewability.State.INIT, subject.state);

        subject.state = BaseWebViewViewability.State.IMPRESSED;
        subject.onVisibilityChanged(subject, View.VISIBLE);
        assertTrue(subject.automaticImpression);
        assertTrue(subject.viewabilityEnabled);
        verifyNoMoreInteractions(mockExternalTracker);
        assertEquals(BaseWebViewViewability.State.IMPRESSED, subject.state);

        subject.state = BaseWebViewViewability.State.STOPPED;
        subject.onVisibilityChanged(subject, View.VISIBLE);
        assertTrue(subject.automaticImpression);
        assertTrue(subject.viewabilityEnabled);
        verifyNoMoreInteractions(mockExternalTracker);
        assertEquals(BaseWebViewViewability.State.STOPPED, subject.state);
    }

    @Test
    public void setPageLoaded_whenPageVisibleFalse_createsViewabilitySession() {
        final ExternalViewabilitySessionManager mockExternalTracker = mock(ExternalViewabilitySessionManager.class);
        subject = new BaseWebViewViewability(context);
        subject.setMockExternalTracker(mockExternalTracker);
        subject.pageLoaded = false;

        subject.setPageLoaded();

        assertTrue(subject.pageLoaded);
        assertFalse(subject.pageVisible);
        assertTrue(subject.automaticImpression);
        assertTrue(subject.viewabilityEnabled);
        assertEquals(BaseWebViewViewability.State.STARTED, subject.state);
        verify(mockExternalTracker).createWebViewSession(subject);
        verify(mockExternalTracker).startSession();
    }

    @Test
    public void setPageLoaded_whenPageVisibleTrue_createsViewabilitySession_tracksImpression() {
        final ExternalViewabilitySessionManager mockExternalTracker = mock(ExternalViewabilitySessionManager.class);
        subject = new BaseWebViewViewability(context);
        subject.setMockExternalTracker(mockExternalTracker);
        subject.pageVisible = true;
        subject.pageLoaded = false;

        subject.setPageLoaded();

        assertTrue(subject.pageLoaded);
        assertTrue(subject.automaticImpression);
        assertTrue(subject.viewabilityEnabled);
        assertEquals(BaseWebViewViewability.State.IMPRESSED, subject.state);
        verify(mockExternalTracker).createWebViewSession(subject);
        verify(mockExternalTracker).startSession();
        verify(mockExternalTracker).trackImpression();
    }

    @Test
    public void registerFriendlyObstruction_callsViewabilityTracker() {
        final ExternalViewabilitySessionManager mockExternalTracker = mock(ExternalViewabilitySessionManager.class);
        subject = new BaseWebViewViewability(context);
        subject.setMockExternalTracker(mockExternalTracker);
        final View obstruction = new View(context);

        subject.registerFriendlyObstruction(obstruction, ViewabilityObstruction.CLOSE_BUTTON);

        verify(mockExternalTracker).registerFriendlyObstruction(obstruction, ViewabilityObstruction.CLOSE_BUTTON);
    }

    @Test
    public void disableTracking_turnsOffViewability() {
        subject = new BaseWebViewViewability(context);
        subject.viewabilityEnabled = true;

        subject.disableTracking();

        assertFalse(subject.viewabilityEnabled);
    }

    @Test
    public void notifyImpression_whenPageVisible_changesStateToImpessed_callsViewabilityTracker() {
        final ExternalViewabilitySessionManager mockExternalTracker = mock(ExternalViewabilitySessionManager.class);
        subject = new BaseWebViewViewability(context);
        subject.setMockExternalTracker(mockExternalTracker);
        subject.disableAutomaticImpression();
        subject.setPageLoaded(); // to change state to STARTED

        assertTrue(subject.pageLoaded);
        assertFalse(subject.automaticImpression);
        assertTrue(subject.viewabilityEnabled);
        assertEquals(BaseWebViewViewability.State.STARTED, subject.state);
        verify(mockExternalTracker).createWebViewSession(subject);
        verify(mockExternalTracker).startSession();

        subject.pageVisible = true;
        subject.notifyImpression();

        assertTrue(subject.pageLoaded);
        assertFalse(subject.automaticImpression);
        assertTrue(subject.viewabilityEnabled);
        assertEquals(BaseWebViewViewability.State.IMPRESSED, subject.state);
        verify(mockExternalTracker).trackImpression();
    }
}
