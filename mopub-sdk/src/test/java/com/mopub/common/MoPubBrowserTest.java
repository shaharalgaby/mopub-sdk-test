// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common;

import android.view.View;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.common.util.UtilsTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(SdkTestRunner.class)
public class MoPubBrowserTest {

    private MoPubBrowser subject;
    private WebView mockWebView;

    @Before
    public void setUp() {
        subject = Robolectric.buildActivity(MoPubBrowser.class).create().visible().start().get();
        CookieSyncManager.createInstance(subject);

        mockWebView = mock(WebView.class);
        subject.setWebView(mockWebView);
    }

    @Test
    public void create_callsHideNavigationBar(){
        View decorView = subject.getWindow().getDecorView();

        assertThat(decorView.getSystemUiVisibility()).isEqualTo(UtilsTest.FLAGS);
    }

    @Test
    public void onPause_withIsFinishingTrue_shouldStopLoading_shouldLoadBlankUrl_shouldPauseWebView() throws Exception {
        // We have to manually call #onPause here after #finish since the activity is not being managed by Android
        // Even if the activity was being managed by Android we would likely have to call onPause since the test would
        // complete before the UI thread had a chance to invoke the lifecycle events
        subject.finish();
        subject.onPause();

        verify(mockWebView).stopLoading();
        verify(mockWebView).loadUrl("");
        verify(mockWebView).onPause();
    }

    @Test
    public void onPause_withIsFinishingFalse_shouldPauseWebView() throws Exception {
        subject.onPause();

        verify(mockWebView, never()).stopLoading();
        verify(mockWebView, never()).loadUrl("");
        verify(mockWebView).onPause();
    }

    @Test
    public void onResume_shouldResumeWebView() throws Exception {
        subject.onResume();

        verify(mockWebView).onResume();
    }

    @Test
    public void onDestroy_shouldDestroyWebView() throws Exception {
        subject.onDestroy();

        verify(mockWebView).destroy();
    }
}
