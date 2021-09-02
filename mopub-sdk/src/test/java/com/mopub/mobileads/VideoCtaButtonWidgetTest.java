// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.view.View;

import com.mopub.common.test.support.SdkTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;

import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(SdkTestRunner.class)
public class VideoCtaButtonWidgetTest {
    private Context context;
    private VideoCtaButtonWidget subject;

    @Before
    public void setUp() throws Exception {
        context = Robolectric.buildActivity(Activity.class).create().get();
    }

    @Test
    public void constructor_withCompanionAd_shouldBeVisible() {
        subject = new VideoCtaButtonWidget(context, null);
        subject.setHasCompanionAd(true);
        subject.setHasClickthroughUrl(true);

        assertThat(subject.getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void constructor_withoutCompanionAd_shouldBeVisible() {
        subject = new VideoCtaButtonWidget(context, null);
        subject.setHasCompanionAd(true);
        subject.setHasClickthroughUrl(true);

        assertThat(subject.getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void constructor_withCompanionAd_withNoClickthroughUrl_shouldBeGone() {
        subject = new VideoCtaButtonWidget(context, null);
        subject.setHasCompanionAd(true);
        subject.setHasClickthroughUrl(false);

        assertThat(subject.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void constructor_withoutCompanionAd_withNoClickthroughUrl_shouldBeGone() {
        subject = new VideoCtaButtonWidget(context, null);
        subject.setHasCompanionAd(false);
        subject.setHasClickthroughUrl(false);

        assertThat(subject.getVisibility()).isEqualTo(View.GONE);
    }

    // Video is complete, has companion ad, CTA button already visible

    @Test
    public void notifyVideoComplete_withCompanionAdAndInPortrait_shouldBeGone() {
        context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_PORTRAIT;
        subject = new VideoCtaButtonWidget(context, null);
        subject.setHasCompanionAd(true);
        subject.setHasClickthroughUrl(true);
        subject.setVisibility(View.VISIBLE);

        subject.notifyVideoComplete();

        assertThat(subject.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void notifyVideoComplete_withCompanionAdAndInLandscape_shouldBeGone() {
        context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_LANDSCAPE;
        subject = new VideoCtaButtonWidget(context, null);
        subject.setHasCompanionAd(true);
        subject.setHasClickthroughUrl(true);
        subject.setVisibility(View.VISIBLE);

        subject.notifyVideoComplete();

        assertThat(subject.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void notifyVideoComplete_withCompanionAdAndOrientationUndefined_shouldBeGone() {
        context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_UNDEFINED;
        subject = new VideoCtaButtonWidget(context, null);
        subject.setHasCompanionAd(true);
        subject.setHasClickthroughUrl(true);
        subject.setVisibility(View.VISIBLE);

        subject.notifyVideoComplete();

        assertThat(subject.getVisibility()).isEqualTo(View.GONE);
    }

    // Video is complete, no companion ad, has clickthrough url, CTA button already visible

    @Test
    public void notifyVideoComplete_withoutCompanionAdAndInPortrait_shouldBeVisible() {
        context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_PORTRAIT;
        subject = new VideoCtaButtonWidget(context, null);
        subject.setHasCompanionAd(false);
        subject.setHasClickthroughUrl(true);
        subject.setVisibility(View.VISIBLE);

        subject.notifyVideoComplete();

        assertThat(subject.getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void notifyVideoComplete_withoutCompanionAdAndInLandscape_shouldBeVisible() {
        context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_LANDSCAPE;
        subject = new VideoCtaButtonWidget(context, null);
        subject.setHasCompanionAd(false);
        subject.setHasClickthroughUrl(true);
        subject.setVisibility(View.VISIBLE);

        subject.notifyVideoComplete();

        assertThat(subject.getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void notifyVideoComplete_withoutCompanionAdAndOrientationUndefined_shouldBeVisible() {
        context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_UNDEFINED;
        subject = new VideoCtaButtonWidget(context, null);
        subject.setHasCompanionAd(false);
        subject.setHasClickthroughUrl(true);
        subject.setVisibility(View.VISIBLE);

        subject.notifyVideoComplete();

        assertThat(subject.getVisibility()).isEqualTo(View.VISIBLE);
    }

    // No clickthrough url means never show cta button

    @Test
    public void notifyVideoComplete_withoutClickthroughUrl_shouldBeGone() {
        subject = new VideoCtaButtonWidget(context, null);
        subject.setHasCompanionAd(true);
        subject.setHasClickthroughUrl(false);
        subject.setVisibility(View.VISIBLE);

        subject.notifyVideoComplete();

        assertThat(subject.getVisibility()).isEqualTo(View.GONE);
    }
}
