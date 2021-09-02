// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.mopub.common.MoPubBrowser;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.test.support.VastUtils;
import com.mopub.network.MoPubRequestQueue;
import com.mopub.network.Networking;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;

import java.util.Arrays;
import java.util.Collections;

import static com.mopub.common.MoPubRequestMatcher.isUrl;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.robolectric.Shadows.shadowOf;

@RunWith(SdkTestRunner.class)
public class VastCompanionAdConfigTest {

    private static final String RESOLVED_CLICKTHROUGH_URL = "https://www.mopub.com/en";
    private static final String CLICKTHROUGH_URL = "deeplink+://navigate?" +
            "&primaryUrl=bogus%3A%2F%2Furl" +
            "&fallbackUrl=" + Uri.encode(RESOLVED_CLICKTHROUGH_URL);
    private static final int WIDTH_DP = 123;
    private static final int HEIGHT_DP = 456;

    private VastCompanionAdConfig subject;
    private Context context;
    @Mock
    private MoPubRequestQueue mockRequestQueue;

    @Before
    public void setup() {
        subject = new VastCompanionAdConfig(WIDTH_DP, HEIGHT_DP,
                new VastResource("resource", VastResource.Type.STATIC_RESOURCE, VastResource
                        .CreativeType.IMAGE, WIDTH_DP, HEIGHT_DP),
                CLICKTHROUGH_URL,
                VastUtils.stringsToVastTrackers("clickTrackerOne", "clickTrackerTwo"),
                VastUtils.stringsToVastTrackers("viewTrackerOne", "viewTrackerTwo"),
                null
        );
        context = Robolectric.buildActivity(Activity.class).create().get();
        Networking.setRequestQueueForTesting(mockRequestQueue);
    }

    @Test
    public void constructor_shouldSetParamsCorrectly() throws Exception {
        assertThat(subject.getWidth()).isEqualTo(WIDTH_DP);
        assertThat(subject.getHeight()).isEqualTo(HEIGHT_DP);
        assertThat(subject.getVastResource().getResource()).isEqualTo("resource");
        assertThat(subject.getVastResource().getType()).isEqualTo(VastResource.Type.STATIC_RESOURCE);
        assertThat(subject.getVastResource().getCreativeType())
                .isEqualTo(VastResource.CreativeType.IMAGE);
        assertThat(subject.getClickThroughUrl()).isEqualTo(CLICKTHROUGH_URL);
        assertThat(VastUtils.vastTrackersToStrings(subject.getClickTrackers()))
                .containsOnly("clickTrackerOne", "clickTrackerTwo");
        assertThat(VastUtils.vastTrackersToStrings(subject.getCreativeViewTrackers()))
                .containsOnly("viewTrackerOne", "viewTrackerTwo");
    }

    @Test
    public void handleImpression_shouldTrackImpression() throws Exception {
        subject.handleImpression(context, 123);

        verify(mockRequestQueue).add(argThat(isUrl("viewTrackerOne")));
        verify(mockRequestQueue).add(argThat(isUrl("viewTrackerTwo")));
    }

    @Test
    public void handleClick_shouldNotTrackClick() throws Exception {
        subject.handleClick(context, 1, null, "dsp_creative_id");

        verifyNoMoreInteractions(mockRequestQueue);
    }

    @Test
    public void handleClick_shouldOpenMoPubBrowser() throws Exception {
        subject.handleClick(context, 1, null, "dsp_creative_id");

        Robolectric.flushBackgroundThreadScheduler();
        Intent startedActivity = shadowOf((Activity) context).getNextStartedActivity();
        assertThat(startedActivity.getComponent().getClassName())
                .isEqualTo("com.mopub.common.MoPubBrowser");
        assertThat(startedActivity.getStringExtra(MoPubBrowser.DESTINATION_URL_KEY))
                .isEqualTo(RESOLVED_CLICKTHROUGH_URL);
        assertThat(startedActivity.getStringExtra(MoPubBrowser.DSP_CREATIVE_ID))
                .isEqualTo("dsp_creative_id");
        assertThat(startedActivity.getData()).isNull();
    }

    @Test
    public void calculateScore_withSameSizes_shouldWeightHtmlResourceHighest() {
        final int containerWidth = 480;
        final int containerHeight = 320;
        final double staticImageScore = subject.calculateScore(containerWidth, containerHeight);

        subject = new VastCompanionAdConfig(WIDTH_DP, HEIGHT_DP,
                new VastResource("html_resource", VastResource.Type.HTML_RESOURCE, VastResource
                        .CreativeType.NONE, WIDTH_DP, HEIGHT_DP),
                CLICKTHROUGH_URL,
                Collections.emptyList(),
                Collections.emptyList(),
                null
        );
        final double htmlScore = subject.calculateScore(containerWidth, containerHeight);

        subject = new VastCompanionAdConfig(WIDTH_DP, HEIGHT_DP,
                new VastResource("html_resource", VastResource.Type.IFRAME_RESOURCE, VastResource
                        .CreativeType.NONE, WIDTH_DP, HEIGHT_DP),
                CLICKTHROUGH_URL,
                Collections.emptyList(),
                Collections.emptyList(),
                null
        );
        final double iframeScore = subject.calculateScore(containerWidth, containerHeight);

        subject = new VastCompanionAdConfig(WIDTH_DP, HEIGHT_DP,
                new VastResource("html_resource", VastResource.Type.BLURRED_LAST_FRAME, VastResource
                        .CreativeType.IMAGE, WIDTH_DP, HEIGHT_DP),
                CLICKTHROUGH_URL,
                Collections.emptyList(),
                Collections.emptyList(),
                null
        );
        final double blurredLastFrameScore = subject.calculateScore(containerWidth, containerHeight);

        subject = new VastCompanionAdConfig(WIDTH_DP, HEIGHT_DP,
                new VastResource("html_resource", VastResource.Type.STATIC_RESOURCE, VastResource
                        .CreativeType.JAVASCRIPT, WIDTH_DP, HEIGHT_DP),
                CLICKTHROUGH_URL,
                Collections.emptyList(),
                Collections.emptyList(),
                null
        );
        final double javascriptScore = subject.calculateScore(containerWidth, containerHeight);

        double[] scores = new double[]{staticImageScore, htmlScore, iframeScore, blurredLastFrameScore, javascriptScore};
        Arrays.sort(scores);

        assertThat(scores).isEqualTo(new double[]{blurredLastFrameScore, staticImageScore, javascriptScore, iframeScore, htmlScore});
    }

    @Test
    public void calculateScore_withVariousSizes_with270dpScreen_shouldPickClosestInSize() {
        final int containerWidth = 480;
        final int containerHeight = 270;
        double score123x456 = subject.calculateScore(containerWidth, containerHeight);

        subject = new VastCompanionAdConfig(470, 265,
                new VastResource("resource", VastResource.Type.STATIC_RESOURCE, VastResource
                        .CreativeType.IMAGE, 0, 0),
                CLICKTHROUGH_URL, Collections.emptyList(), Collections.emptyList(), null
        );
        double score470x265 = subject.calculateScore(containerWidth, containerHeight);

        subject = new VastCompanionAdConfig(460, 255,
                new VastResource("resource", VastResource.Type.STATIC_RESOURCE, VastResource
                        .CreativeType.IMAGE, 0, 0),
                CLICKTHROUGH_URL, Collections.emptyList(), Collections.emptyList(), null
        );
        double score460x255 = subject.calculateScore(containerWidth, containerHeight);

        subject = new VastCompanionAdConfig(270, 480,
                new VastResource("resource", VastResource.Type.STATIC_RESOURCE, VastResource
                        .CreativeType.IMAGE, 0, 0),
                CLICKTHROUGH_URL, Collections.emptyList(), Collections.emptyList(), null
        );
        double score270x480 = subject.calculateScore(containerWidth, containerHeight);

        double[] scores = new double[]{score123x456, score470x265, score460x255, score270x480};
        Arrays.sort(scores);

        assertThat(scores).isEqualTo(new double[]{score123x456, score270x480, score460x255, score470x265});
    }

    @Test
    public void calculateScore_withZeroHeight_shouldReturn0() {
        subject = new VastCompanionAdConfig(WIDTH_DP, 0,
                new VastResource("resource", VastResource.Type.STATIC_RESOURCE, VastResource
                        .CreativeType.IMAGE, 0, 0),
                CLICKTHROUGH_URL, Collections.emptyList(), Collections.emptyList(), null
        );
        double result = subject.calculateScore(WIDTH_DP, HEIGHT_DP);
        assertThat(result).isEqualTo(0);

        subject = new VastCompanionAdConfig(0, 0,
                new VastResource("resource", VastResource.Type.STATIC_RESOURCE, VastResource
                        .CreativeType.IMAGE, 0, 0),
                CLICKTHROUGH_URL, Collections.emptyList(), Collections.emptyList(), null
        );
        result = subject.calculateScore(WIDTH_DP, HEIGHT_DP);
        assertThat(result).isEqualTo(0);

        subject = new VastCompanionAdConfig(WIDTH_DP, HEIGHT_DP,
                new VastResource("resource", VastResource.Type.STATIC_RESOURCE, VastResource
                        .CreativeType.IMAGE, 0, 0),
                CLICKTHROUGH_URL, Collections.emptyList(), Collections.emptyList(), null
        );
        result = subject.calculateScore(WIDTH_DP, 0);
        assertThat(result).isEqualTo(0);

        subject = new VastCompanionAdConfig(0, 0,
                new VastResource("resource", VastResource.Type.STATIC_RESOURCE, VastResource
                        .CreativeType.IMAGE, 0, 0),
                CLICKTHROUGH_URL, Collections.emptyList(), Collections.emptyList(), null
        );
        result = subject.calculateScore(WIDTH_DP, 0);
        assertThat(result).isEqualTo(0);
    }

    @Test
    public void calculateScore_withBlurredLastFrame_shouldReturn0() {
        subject = new VastCompanionAdConfig(WIDTH_DP, HEIGHT_DP,
                new VastResource("resource", VastResource.Type.BLURRED_LAST_FRAME, VastResource
                        .CreativeType.IMAGE, WIDTH_DP, HEIGHT_DP),
                CLICKTHROUGH_URL, Collections.emptyList(), Collections.emptyList(), null
        );

        double result = subject.calculateScore(WIDTH_DP, HEIGHT_DP);

        assertThat(result).isEqualTo(0);
    }
}
