// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common;

import android.app.Activity;
import android.view.MotionEvent;

import com.mopub.common.CloseableLayout.OnCloseListener;
import com.mopub.common.test.support.SdkTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(SdkTestRunner.class)
public class CloseableLayoutTest {
    private CloseableLayout subject;

    @Mock private OnCloseListener mockCloseListener;

    private MotionEvent contentRegionDown;
    private MotionEvent contentRegionUp;

    @Before
    public void setup() {
        Activity activity = Robolectric.buildActivity(Activity.class).create().get();
        subject = new CloseableLayout(activity, null);

        // Fake the close bounds, which allows us to set up close regions
        contentRegionDown = MotionEvent.obtain(
                100, 200, MotionEvent.ACTION_DOWN, 0, 0, 0);
        contentRegionUp = MotionEvent.obtain(
                100, 200, MotionEvent.ACTION_UP, 0, 0, 0);
    }

    @Test
    public void setOnCloseListener_thenTouchCloseRegion_shouldCallOnClick() {
        subject.setOnCloseListener(mockCloseListener);
        subject.clickCloseButton();

        verify(mockCloseListener).onClose();
    }

    @Test
    public void setOnCloseListener_thenTouchContentRegion_shouldNotCallCloseListener() {
        subject.setOnCloseListener(mockCloseListener);
        subject.onTouchEvent(contentRegionDown);
        subject.onTouchEvent(contentRegionUp);

        verify(mockCloseListener, never()).onClose();
    }

    @Test
    public void setCloseVisible_shouldToggleCloseDrawable() {
        subject.setCloseVisible(false);
        assertThat(subject.isCloseVisible()).isFalse();

        subject.setCloseVisible(true);
        assertThat(subject.isCloseVisible()).isTrue();
    }

    @Test
    public void onCloseButtonClicked_shouldTogglePressedStateAfterDelay() {
        assertThat(subject.isClosePressed()).isFalse();

        subject.clickCloseButton();
        assertThat(subject.isClosePressed()).isTrue();

        Robolectric.getForegroundThreadScheduler().advanceToLastPostedRunnable();
        assertThat(subject.isClosePressed()).isFalse();
    }

    @Test
    public void shouldAllowPress_shouldRespectSetCloseAlwaysInteractable() {
        subject.setCloseVisible(false);
        subject.setCloseAlwaysInteractable(false);
        assertThat(subject.shouldAllowPress()).isFalse();

        subject.setCloseVisible(false);
        subject.setCloseAlwaysInteractable(true);
        assertThat(subject.shouldAllowPress()).isTrue();

        subject.setCloseVisible(true);
        subject.setCloseAlwaysInteractable(false);
        assertThat(subject.shouldAllowPress()).isTrue();

        subject.setCloseVisible(true);
        subject.setCloseAlwaysInteractable(true);
        assertThat(subject.shouldAllowPress()).isTrue();
    }
}
