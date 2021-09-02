// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.FrameLayout;

import com.mopub.common.DataKeys;
import com.mopub.common.test.support.SdkTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;

import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(SdkTestRunner.class)
public class BaseInterstitialActivityTest {
    private BaseInterstitialActivity subject;
    private long broadcastIdentifier;
    private Intent intent;

    // Make a concrete version of the abstract class for testing purposes.
    private static class TestInterstitialActivity extends BaseInterstitialActivity {
        View view;

        @Override
        public View getAdView() {
            if (view == null) {
                view = new View(this);
            }
            return view;
        }
    }

    @Before
    public void setup() {
        broadcastIdentifier = 2222;
        Context context = Robolectric.buildActivity(Activity.class).create().get();
        intent = new Intent(context, TestInterstitialActivity.class);
        intent.putExtra(DataKeys.AD_DATA_KEY, new AdData.Builder().build());
    }

    @Test
    public void onCreate_shouldCreateView() throws Exception {
        subject = Robolectric.buildActivity(TestInterstitialActivity.class, intent).create().get();

        View adView = getContentView(subject).getChildAt(0);

        assertThat(adView).isNotNull();
    }

    @Test
    public void onDestroy_shouldCleanUpContentView() throws Exception {
        subject = Robolectric.buildActivity(TestInterstitialActivity.class, intent).create().destroy().get();

        // the close button should still be present
        assertThat(getContentView(subject).getChildCount()).isEqualTo(1);
    }

    @Test
    public void getBroadcastIdentifier_shouldReturnBroadcastIdFromIntent() throws Exception {
        intent.putExtra(DataKeys.AD_DATA_KEY, new AdData.Builder().broadcastIdentifier(broadcastIdentifier).build());

        subject = Robolectric.buildActivity(TestInterstitialActivity.class, intent)
                .create().get();
        assertThat(subject.getBroadcastIdentifier()).isEqualTo(2222L);
    }

    @Test
    public void getBroadcastIdentifier_withMissingBroadCastId_shouldReturn0() throws Exception {
        // This intent is missing a broadcastidentifier extra.

        subject = Robolectric.buildActivity(TestInterstitialActivity.class, intent)
                .create().get();

        assertThat(subject.getBroadcastIdentifier()).isZero();
    }

    protected FrameLayout getContentView(BaseInterstitialActivity subject) {
        return subject.getCloseableLayout();
    }
}
