// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.view.View;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.resource.RadialCountdownDrawable;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@RunWith(SdkTestRunner.class)
public class RadialCountdownWidgetTest {
    private Context context;
    private RadialCountdownWidget subject;
    private RadialCountdownDrawable radialCountdownDrawableSpy;

    @Before
    public void setUp() throws Exception {
        context = Robolectric.buildActivity(Activity.class).create().get();
        subject = new RadialCountdownWidget(context, null);
        radialCountdownDrawableSpy = spy(subject.getImageViewDrawable());
        subject.setImageViewDrawable(radialCountdownDrawableSpy);
    }

    @Test
    public void calibrate_shouldSetInitialCountdownAndMakeInvisible() {
        subject.calibrate(10000);

        Assert.assertEquals(View.INVISIBLE, subject.getVisibility());
        Assert.assertEquals(10000, radialCountdownDrawableSpy.getInitialCountdownMilliseconds());
        verify(radialCountdownDrawableSpy).setInitialCountdown(10000);
    }

    @Test
    public void updateCountdownProgress_shouldUpdateDrawable() throws Exception {
        subject.setVisibility(View.VISIBLE);

        subject.updateCountdownProgress(10000, 1000);

        assertThat(subject.getVisibility()).isEqualTo(View.VISIBLE);
        verify(radialCountdownDrawableSpy).updateCountdownProgress(1000);
    }

    @Test
    public void updateCountdownProgress_whenProgressIsGreaterThanInitialCountdown_shouldHideAndNotUpdateDrawable() throws Exception {
        subject.setVisibility(View.VISIBLE);

        subject.updateCountdownProgress(10000, 10001);

        assertThat(subject.getVisibility()).isEqualTo(View.GONE);
        verify(radialCountdownDrawableSpy, never()).updateCountdownProgress(anyInt());
    }

    @Test
    public void updateCountdownProgress_whenCurrentProgressGreaterThanPreviousProgress_shouldUpdateDrawable() throws Exception {
        subject.setVisibility(View.VISIBLE);

        // Set mLastProgressMilliseconds to 1000
        subject.updateCountdownProgress(10000, 1000);
        reset(radialCountdownDrawableSpy);

        subject.updateCountdownProgress(10000, 1001);

        assertThat(subject.getVisibility()).isEqualTo(View.VISIBLE);
        verify(radialCountdownDrawableSpy).updateCountdownProgress(1001);
    }

    @Test
    public void updateCountdownProgress_whenCurrentProgressLessThanPreviousProgress_shouldNotChangeVisibilityOrUpdateDrawable() throws Exception {
        subject.setVisibility(View.VISIBLE);

        // Set mLastProgressMilliseconds to 1000
        subject.updateCountdownProgress(10000, 1000);
        reset(radialCountdownDrawableSpy);

        subject.updateCountdownProgress(10000, 999);

        assertThat(subject.getVisibility()).isEqualTo(View.VISIBLE);
        verify(radialCountdownDrawableSpy, never()).updateCountdownProgress(anyInt());
    }
}
