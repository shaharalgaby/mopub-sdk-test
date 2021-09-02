// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.app.Activity;
import android.view.MotionEvent;
import android.view.View;

import com.mopub.common.test.support.SdkTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowGestureDetector;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;


@RunWith(SdkTestRunner.class)
public class ViewGestureDetectorTest {
    private Activity context;
    private ViewGestureDetector subject;
    private ViewGestureDetector.GestureListener gestureListener;

    @Before
    public void setUp() throws Exception {
        context = Robolectric.buildActivity(Activity.class).create().get();

        gestureListener = mock(ViewGestureDetector.GestureListener.class);

        subject = new ViewGestureDetector(context);
        subject.setGestureListener(gestureListener);
    }

    @Test
    public void constructor_shouldDisableLongPressAndSetGestureListener() throws Exception {
        subject = new ViewGestureDetector(context);

        ShadowGestureDetector shadowGestureDetector = Shadows.shadowOf(subject);

        assertThat(subject.isLongpressEnabled()).isFalse();
        assertThat(shadowGestureDetector.getListener()).isNotNull();
        assertThat(shadowGestureDetector.getListener()).isInstanceOf(ViewGestureDetector.GestureListener.class);
    }

    @Test
    public void onTouchEvent_whenActionDown_shouldForwardOnTouchEvent() throws Exception {
        MotionEvent expectedMotionEvent = createMotionEvent(MotionEvent.ACTION_DOWN);

        subject.onTouchEvent(expectedMotionEvent);

        MotionEvent actualMotionEvent = Shadows.shadowOf(subject).getOnTouchEventMotionEvent();

        assertThat(actualMotionEvent).isEqualTo(expectedMotionEvent);
    }

    @Test
    public void onTouchEvent_whenActionMoveWithinView_shouldForwardOnTouchEvent() throws Exception {
        MotionEvent downEvent = createMotionEvent(MotionEvent.ACTION_DOWN);
        subject.onTouchEvent(downEvent);

        MotionEvent expectedMotionEvent = createActionMove(160);
        subject.onTouchEvent(expectedMotionEvent);

        MotionEvent actualMotionEvent = Shadows.shadowOf(subject).getOnTouchEventMotionEvent();

        assertThat(actualMotionEvent).isEqualTo(expectedMotionEvent);
        verify(gestureListener, never()).onResetUserClick();
    }

    private MotionEvent createActionMove(float x) {
        return MotionEvent.obtain(0, 0, MotionEvent.ACTION_MOVE, x, 0, 0);
    }

    private MotionEvent createMotionEvent(int action) {
        return MotionEvent.obtain(0, 0, action, 0, 0, 0);
    }
}
