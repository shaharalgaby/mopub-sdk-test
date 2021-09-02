// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.GradientDrawable;
import android.view.View;

import com.mopub.common.test.support.SdkTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;

import static android.view.View.VISIBLE;
import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(SdkTestRunner.class)
public class VastVideoGradientStripWidgetTest {
    private Context context;
    private VastVideoGradientStripWidget subject;

    @Before
    public void setUp() throws Exception {
        context = Robolectric.buildActivity(Activity.class).create().get();
    }

    @Test
    public void constructor_whenForcePortrait_shouldBeInvisible() throws Exception {
        subject = new VastVideoGradientStripWidget(context);
        subject.setGradientOrientation(GradientDrawable.Orientation.TOP_BOTTOM);
        subject.setHasCompanionAd(true);
        subject.setVisibilityForCompanionAd(VISIBLE);
        subject.setAlwaysVisibleDuringVideo(false);

        context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_PORTRAIT;
        subject.onConfigurationChanged(context.getResources().getConfiguration());

        assertThat(subject.getVisibility()).isEqualTo(View.INVISIBLE);
    }

    @Test
    public void constructor_whenForcePortrait_withAlwaysVisibleDuringVideo_shouldBeVisible() throws Exception {
        subject = new VastVideoGradientStripWidget(context);
        subject.setGradientOrientation(GradientDrawable.Orientation.TOP_BOTTOM);
        subject.setHasCompanionAd(true);
        subject.setVisibilityForCompanionAd(VISIBLE);
        subject.setAlwaysVisibleDuringVideo(true);

        assertThat(subject.getVisibility()).isEqualTo(VISIBLE);
    }

    @Test
    public void constructor_whenForceLandscape_shouldBeInvisible() throws Exception {
        subject = new VastVideoGradientStripWidget(context);
        subject.setGradientOrientation(GradientDrawable.Orientation.TOP_BOTTOM);
        subject.setHasCompanionAd(true);
        subject.setVisibilityForCompanionAd(VISIBLE);
        subject.setAlwaysVisibleDuringVideo(false);

        // If not forcing orientation, visibility depends on device orientation,
        // which is initially ORIENTATION_UNDEFINED in tests
        context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_UNDEFINED;
        subject.onConfigurationChanged(context.getResources().getConfiguration());

        assertThat(subject.getVisibility()).isEqualTo(View.INVISIBLE);
    }

    @Test
    public void constructor_whenUseDeviceOrientation_shouldBeInvisible() throws Exception {
        subject = new VastVideoGradientStripWidget(context);
        subject.setGradientOrientation(GradientDrawable.Orientation.TOP_BOTTOM);
        subject.setHasCompanionAd(true);
        subject.setVisibilityForCompanionAd(VISIBLE);
        subject.setAlwaysVisibleDuringVideo(false);

        // If not forcing orientation, visibility depends on device orientation,
        // which is initially ORIENTATION_UNDEFINED in tests
        context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_UNDEFINED;
        subject.onConfigurationChanged(context.getResources().getConfiguration());

        assertThat(subject.getVisibility()).isEqualTo(View.INVISIBLE);
    }

    @Test
    public void constructor_whenForceOrientationUndefined_shouldBeInvisible() throws Exception {
        subject = new VastVideoGradientStripWidget(context);
        subject.setGradientOrientation(GradientDrawable.Orientation.TOP_BOTTOM);
        subject.setHasCompanionAd(true);
        subject.setVisibilityForCompanionAd(VISIBLE);
        subject.setAlwaysVisibleDuringVideo(false);

        // If force orientation undefined, visibility depends on device orientation,
        // which is initially ORIENTATION_UNDEFINED in tests
        context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_UNDEFINED;
        subject.onConfigurationChanged(context.getResources().getConfiguration());

        assertThat(subject.getVisibility()).isEqualTo(View.INVISIBLE);
    }

    // Video is still playing, forcing portrait orientation

    @Test
    public void onConfigurationChanged_whenForcePortraitAndDeviceInPortrait_shouldBeInvisible() throws Exception {
        subject = new VastVideoGradientStripWidget(context);
        subject.setGradientOrientation(GradientDrawable.Orientation.TOP_BOTTOM);
        subject.setHasCompanionAd(true);
        subject.setVisibilityForCompanionAd(VISIBLE);
        subject.setAlwaysVisibleDuringVideo(false);
        context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_PORTRAIT;

        subject.onConfigurationChanged(context.getResources().getConfiguration());

        assertThat(subject.getVisibility()).isEqualTo(View.INVISIBLE);
    }

    @Test
    public void onConfigurationChanged_whenForcePortraitAndDeviceInPortrait_withAlwaysVisibleDuringVideo_shouldBeVisible() throws Exception {
        subject = new VastVideoGradientStripWidget(context);
        subject.setGradientOrientation(GradientDrawable.Orientation.TOP_BOTTOM);
        subject.setHasCompanionAd(true);
        subject.setVisibilityForCompanionAd(VISIBLE);
        subject.setAlwaysVisibleDuringVideo(true);
        context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_PORTRAIT;

        subject.onConfigurationChanged(context.getResources().getConfiguration());

        assertThat(subject.getVisibility()).isEqualTo(VISIBLE);
    }

    @Test
    public void onConfigurationChanged_whenForcePortraitAndDeviceInLandscape_shouldBeVisible() throws Exception {
        subject = new VastVideoGradientStripWidget(context);
        subject.setGradientOrientation(GradientDrawable.Orientation.TOP_BOTTOM);
        subject.setHasCompanionAd(true);
        subject.setVisibilityForCompanionAd(VISIBLE);
        subject.setAlwaysVisibleDuringVideo(false);
        context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_LANDSCAPE;

        subject.onConfigurationChanged(context.getResources().getConfiguration());

        assertThat(subject.getVisibility()).isEqualTo(VISIBLE);
    }

    @Test
    public void onConfigurationChanged_whenForcePortraitAndDeviceOrientationUndefined_shouldBeInvisible() throws Exception {
        subject = new VastVideoGradientStripWidget(context);
        subject.setGradientOrientation(GradientDrawable.Orientation.TOP_BOTTOM);
        subject.setHasCompanionAd(true);
        subject.setVisibilityForCompanionAd(VISIBLE);
        subject.setAlwaysVisibleDuringVideo(false);
        context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_UNDEFINED;

        subject.onConfigurationChanged(context.getResources().getConfiguration());

        assertThat(subject.getVisibility()).isEqualTo(View.INVISIBLE);
    }

    // Video is still playing, forcing landscape orientation

    @Test
    public void onConfigurationChanged_whenForceLandscapeAndDeviceInPortrait_shouldBeInvisible() throws Exception {
        subject = new VastVideoGradientStripWidget(context);
        subject.setGradientOrientation(GradientDrawable.Orientation.TOP_BOTTOM);
        subject.setHasCompanionAd(true);
        subject.setVisibilityForCompanionAd(VISIBLE);
        subject.setAlwaysVisibleDuringVideo(false);
        context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_PORTRAIT;

        subject.onConfigurationChanged(context.getResources().getConfiguration());

        assertThat(subject.getVisibility()).isEqualTo(View.INVISIBLE);
    }

    @Test
    public void onConfigurationChanged_whenForceLandscapeAndDeviceInLandscape_shouldBeVisible() throws Exception {
        subject = new VastVideoGradientStripWidget(context);
        subject.setGradientOrientation(GradientDrawable.Orientation.TOP_BOTTOM);
        subject.setHasCompanionAd(true);
        subject.setVisibilityForCompanionAd(VISIBLE);
        subject.setAlwaysVisibleDuringVideo(false);
        context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_LANDSCAPE;

        subject.onConfigurationChanged(context.getResources().getConfiguration());

        assertThat(subject.getVisibility()).isEqualTo(VISIBLE);
    }

    @Test
    public void onConfigurationChanged_whenForceLandscapeAndDeviceOrientationUndefined_shouldBeInvisible() throws Exception {
        subject = new VastVideoGradientStripWidget(context);
        subject.setGradientOrientation(GradientDrawable.Orientation.TOP_BOTTOM);
        subject.setHasCompanionAd(true);
        subject.setVisibilityForCompanionAd(VISIBLE);
        subject.setAlwaysVisibleDuringVideo(false);
        context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_UNDEFINED;

        subject.onConfigurationChanged(context.getResources().getConfiguration());

        assertThat(subject.getVisibility()).isEqualTo(View.INVISIBLE);
    }

    // Video is still playing, use device orientation

    @Test
    public void onConfigurationChanged_whenUseDeviceOrientationAndDeviceInPortrait_shouldBeInvisible() throws Exception {
        subject = new VastVideoGradientStripWidget(context);
        subject.setGradientOrientation(GradientDrawable.Orientation.TOP_BOTTOM);
        subject.setHasCompanionAd(true);
        subject.setVisibilityForCompanionAd(VISIBLE);
        subject.setAlwaysVisibleDuringVideo(false);
        context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_PORTRAIT;

        subject.onConfigurationChanged(context.getResources().getConfiguration());

        assertThat(subject.getVisibility()).isEqualTo(View.INVISIBLE);
    }

    @Test
    public void onConfigurationChanged_whenUseDeviceOrientationAndDeviceInPortrait_withAlwaysVisibleDuringVideo_shouldBeVisible() throws Exception {
        subject = new VastVideoGradientStripWidget(context);
        subject.setGradientOrientation(GradientDrawable.Orientation.TOP_BOTTOM);
        subject.setHasCompanionAd(true);
        subject.setVisibilityForCompanionAd(VISIBLE);
        subject.setAlwaysVisibleDuringVideo(true);
        context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_PORTRAIT;

        subject.onConfigurationChanged(context.getResources().getConfiguration());

        assertThat(subject.getVisibility()).isEqualTo(VISIBLE);
    }

    @Test
    public void onConfigurationChanged_whenUseDeviceOrientationAndDeviceInLandscape_shouldBeVisible() throws Exception {
        subject = new VastVideoGradientStripWidget(context);
        subject.setGradientOrientation(GradientDrawable.Orientation.TOP_BOTTOM);
        subject.setHasCompanionAd(true);
        subject.setVisibilityForCompanionAd(VISIBLE);
        subject.setAlwaysVisibleDuringVideo(false);
        context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_LANDSCAPE;

        subject.onConfigurationChanged(context.getResources().getConfiguration());

        assertThat(subject.getVisibility()).isEqualTo(VISIBLE);
    }

    @Test
    public void onConfigurationChanged_whenUseDeviceOrientationAndDeviceOrientationUndefined_shouldBeInvisible() throws Exception {
        subject = new VastVideoGradientStripWidget(context);
        subject.setGradientOrientation(GradientDrawable.Orientation.TOP_BOTTOM);
        subject.setHasCompanionAd(true);
        subject.setVisibilityForCompanionAd(VISIBLE);
        subject.setAlwaysVisibleDuringVideo(false);
        context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_UNDEFINED;

        subject.onConfigurationChanged(context.getResources().getConfiguration());

        assertThat(subject.getVisibility()).isEqualTo(View.INVISIBLE);
    }

    // Video is still playing, force orientation undefined

    @Test
    public void onConfigurationChanged_whenForceOrientationUndefinedAndDeviceInPortrait_shouldBeInvisible() throws Exception {
        subject = new VastVideoGradientStripWidget(context);
        subject.setGradientOrientation(GradientDrawable.Orientation.TOP_BOTTOM);
        subject.setHasCompanionAd(true);
        subject.setVisibilityForCompanionAd(VISIBLE);
        subject.setAlwaysVisibleDuringVideo(false);
        context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_PORTRAIT;

        subject.onConfigurationChanged(context.getResources().getConfiguration());

        assertThat(subject.getVisibility()).isEqualTo(View.INVISIBLE);
    }

    @Test
    public void onConfigurationChanged_whenForceOrientationUndefinedAndDeviceInPortrait_withAlwaysVisibleDuringVideo_shouldBeVisible() throws Exception {
        subject = new VastVideoGradientStripWidget(context);
        subject.setGradientOrientation(GradientDrawable.Orientation.TOP_BOTTOM);
        subject.setHasCompanionAd(true);
        subject.setVisibilityForCompanionAd(VISIBLE);
        subject.setAlwaysVisibleDuringVideo(true);
        context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_PORTRAIT;

        subject.onConfigurationChanged(context.getResources().getConfiguration());

        assertThat(subject.getVisibility()).isEqualTo(VISIBLE);
    }

    @Test
    public void onConfigurationChanged_whenForceOrientationUndefinedAndDeviceInLandscape_shouldBeVisible() throws Exception {
        subject = new VastVideoGradientStripWidget(context);
        subject.setGradientOrientation(GradientDrawable.Orientation.TOP_BOTTOM);
        subject.setHasCompanionAd(true);
        subject.setVisibilityForCompanionAd(VISIBLE);
        subject.setAlwaysVisibleDuringVideo(false);
        context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_LANDSCAPE;

        subject.onConfigurationChanged(context.getResources().getConfiguration());

        assertThat(subject.getVisibility()).isEqualTo(VISIBLE);
    }

    @Test
    public void onConfigurationChanged_whenForceOrientationUndefinedAndDeviceOrientationUndefined_shouldBeInvisible() throws Exception {
        subject = new VastVideoGradientStripWidget(context);
        subject.setGradientOrientation(GradientDrawable.Orientation.TOP_BOTTOM);
        subject.setHasCompanionAd(true);
        subject.setVisibilityForCompanionAd(VISIBLE);
        subject.setAlwaysVisibleDuringVideo(false);
        context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_UNDEFINED;

        subject.onConfigurationChanged(context.getResources().getConfiguration());

        assertThat(subject.getVisibility()).isEqualTo(View.INVISIBLE);
    }

    @Test
    public void onConfigurationChanged_whenForceOrientationUndefinedAndDeviceOrientationUndefined_withAlwaysVisibleDuringVideo_shouldBeVisible() throws Exception {
        subject = new VastVideoGradientStripWidget(context);
        subject.setGradientOrientation(GradientDrawable.Orientation.TOP_BOTTOM);
        subject.setHasCompanionAd(true);
        subject.setVisibilityForCompanionAd(VISIBLE);
        subject.setAlwaysVisibleDuringVideo(true);
        context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_UNDEFINED;

        subject.onConfigurationChanged(context.getResources().getConfiguration());

        assertThat(subject.getVisibility()).isEqualTo(VISIBLE);
    }

    // Video is complete

    @Test
    public void notifyVideoComplete_withCompanionAd_shouldSetVisibilityForCompanionAd() throws Exception {
        final int visibilityForCompanionAd = VISIBLE;
        subject = new VastVideoGradientStripWidget(context);
        subject.setGradientOrientation(GradientDrawable.Orientation.TOP_BOTTOM);
        subject.setHasCompanionAd(true);
        subject.setVisibilityForCompanionAd(visibilityForCompanionAd);
        subject.setAlwaysVisibleDuringVideo(false);
        context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_UNDEFINED;

        subject.notifyVideoComplete();

        assertThat(subject.getVisibility()).isEqualTo(VISIBLE);
    }

    @Test
    public void notifyVideoComplete_withoutCompanionAd_shouldBeGone() throws Exception {
        final int visibilityForCompanionAd = VISIBLE;
        subject = new VastVideoGradientStripWidget(context);
        subject.setGradientOrientation(GradientDrawable.Orientation.TOP_BOTTOM);
        subject.setHasCompanionAd(false);
        subject.setVisibilityForCompanionAd(visibilityForCompanionAd);
        subject.setAlwaysVisibleDuringVideo(false);
        context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_UNDEFINED;

        subject.notifyVideoComplete();

        assertThat(subject.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void notifyVideoComplete_withoutCompanionAd_withAlwaysVisibleDuringVideo_shouldBeGone() throws Exception {
        final int visibilityForCompanionAd = VISIBLE;
        subject = new VastVideoGradientStripWidget(context);
        subject.setGradientOrientation(GradientDrawable.Orientation.TOP_BOTTOM);
        subject.setHasCompanionAd(false);
        subject.setVisibilityForCompanionAd(visibilityForCompanionAd);
        subject.setAlwaysVisibleDuringVideo(true);
        context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_UNDEFINED;

        subject.notifyVideoComplete();

        assertThat(subject.getVisibility()).isEqualTo(View.GONE);
    }
}
