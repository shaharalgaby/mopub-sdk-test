// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common.util;

import android.app.Activity;
import android.view.View;
import android.view.Window;

import com.mopub.common.test.support.SdkTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashSet;
import java.util.Set;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@RunWith(SdkTestRunner.class)
public class UtilsTest {

    private Activity mockActivity;
    private Window mockWindow;
    private View mockView;

    public static final int FLAGS = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN;

    @Before
    public void setup() {
        mockActivity = mock(Activity.class);
        mockWindow = mock(Window.class);
        mockView = mock(View.class);

        when(mockActivity.getWindow()).thenReturn(mockWindow);
        when(mockWindow.getDecorView()).thenReturn(mockView);
    }

    @Test
    public void generateUniqueId_withMultipleInvocations_shouldReturnUniqueValues() {
        final int expectedIdCount = 100;

        Set<Long> ids = new HashSet<Long>(expectedIdCount);
        for (int i = 0; i < expectedIdCount; i++) {
            final long id = Utils.generateUniqueId();
            ids.add(id);
        }

        assertThat(ids).hasSize(expectedIdCount);
    }

    @Test
    public void sha1_shouldReturnSha1Value() {
        final String input = "test";

        final String result = Utils.sha1(input);

        assertThat(result).isEqualToIgnoringCase("A94A8FE5CCB19BA61C4C0873D391E987982FBBD3");
    }

    @Test
    public void hideNavigation_setsCorrectFlags() {
        Utils.hideNavigation(mockView);

        verify(mockView).setSystemUiVisibility(eq(FLAGS));
    }

    @Test
    public void createNavigationListener_createsUIVisibilityChangeListener() {
        View.OnSystemUiVisibilityChangeListener listener = Utils.createHideNavigationListener(mockView);
        listener.onSystemUiVisibilityChange(0);

        verify(mockView).setSystemUiVisibility(eq(FLAGS));
    }

    @Test
    public void hideNavigationBar_createsListenerForDecorView() {
        Utils.hideNavigationBar(mockActivity);

        verify(mockWindow).getDecorView();
        verify(mockView).setOnSystemUiVisibilityChangeListener(any(View.OnSystemUiVisibilityChangeListener.class));
    }
}
