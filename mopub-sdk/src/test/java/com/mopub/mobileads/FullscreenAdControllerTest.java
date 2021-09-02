// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.mopub.common.CloseableLayout;
import com.mopub.common.Constants;
import com.mopub.common.DataKeys;
import com.mopub.common.FullAdType;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.network.MoPubImageLoader;
import com.mopub.network.MoPubRequestQueue;
import com.mopub.network.Networking;
import com.mopub.network.TrackingRequest;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.shadows.ShadowView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Semaphore;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

@RunWith(SdkTestRunner.class)
public class FullscreenAdControllerTest {
    private static final String EXPECTED_HTML_DATA = "htmlData";
    private static final String COMPANION_RESOURCE = "resource";
    private static final int COMPANION_WIDTH = 300;
    private static final int COMPANION_HEIGHT = 250;
    private static final String COMPANION_CLICKTHROUGH_URL = "clickthrough";
    private static final int VIDEO_DURATION_MS = 29875;
    private static final String IMAGE_CLICKDESTINATION_URL = "click_destination";
    private static final String IMAGE_JSON =
            "{\"image\":\"imageurl\",\"w\":250,\"h\":200,\"clk\":\"" + IMAGE_CLICKDESTINATION_URL + "\"}";
    private static final String DSP_CREATIVE_ID = "dsp";

    private Activity activity;
    private long broadcastIdentifier;
    private AdData adData;
    private FullscreenAdController subject;
    private VastVideoConfig vastVideoConfig;
    private VastCompanionAdConfig vastCompanionAdConfig;
    private Set<VastCompanionAdConfig> vastCompanionAdConfigs;
    private List<VastTracker> companionClickTrackers;
    private List<VastTracker> companionCreativeViewTrackers;
    private BroadcastReceiver broadcastReceiver;
    private CreativeExperienceSettings creativeExperienceSettings;
    private JSONObject ceSettingsJsonObject;

    @Mock
    Intent mockIntent;
    @Mock
    Bundle mockBundle;
    @Mock
    MoPubRequestQueue mockRequestQueue;
    @Mock
    MoPubImageLoader mockImageLoader;
    @Mock
    CloseableLayout mockCloseableLayout;
    @Mock
    ImageView mockImageView;

    @Before
    public void setUp() throws Exception {
        Networking.clearForTesting();

        activity = spy(Robolectric.buildActivity(Activity.class).create().get());

        companionClickTrackers = new ArrayList<>();
        companionClickTrackers.add(new VastTracker("click1", VastTracker.MessageType.TRACKING_URL, false));
        companionClickTrackers.add(new VastTracker("click2", VastTracker.MessageType.TRACKING_URL, false));
        companionCreativeViewTrackers = new ArrayList<>();
        companionCreativeViewTrackers.add(new VastTracker("companion_view1", VastTracker.MessageType.TRACKING_URL, false));
        companionCreativeViewTrackers.add(new VastTracker("companion_view2", VastTracker.MessageType.TRACKING_URL, false));

        VastResource vastResource = new VastResource(
                COMPANION_RESOURCE,
                VastResource.Type.BLURRED_LAST_FRAME,
                VastResource.CreativeType.IMAGE,
                COMPANION_WIDTH,
                COMPANION_HEIGHT);
        vastCompanionAdConfig = new VastCompanionAdConfig(
                COMPANION_WIDTH,
                COMPANION_HEIGHT,
                vastResource,
                COMPANION_CLICKTHROUGH_URL,
                companionClickTrackers,
                companionCreativeViewTrackers,
                null);
        vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setNetworkMediaFileUrl("video_url");
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.addVastCompanionAdConfig(vastCompanionAdConfig);
        vastCompanionAdConfigs = new HashSet<>();
        vastCompanionAdConfigs.add(vastCompanionAdConfig);
        broadcastIdentifier = 112233;

        ceSettingsJsonObject = new JSONObject(CreativeExperienceSettingsParserTest.CE_SETTINGS_STRING);
        creativeExperienceSettings = CreativeExperienceSettingsParser
                .parse(ceSettingsJsonObject, false);
        adData = new AdData.Builder()
                .adPayload(EXPECTED_HTML_DATA)
                .broadcastIdentifier(broadcastIdentifier)
                .vastVideoConfig(vastVideoConfig.toJsonString())
                .dspCreativeId(DSP_CREATIVE_ID)
                .fullAdType(FullAdType.MRAID)
                .creativeExperienceSettings(creativeExperienceSettings)
                .build();
        final Bundle bundle = new Bundle();
        bundle.putParcelable(DataKeys.AD_DATA_KEY, adData);
        when(mockIntent.getExtras()).thenReturn(bundle);

        Networking.setRequestQueueForTesting(mockRequestQueue);
        Networking.setImageLoaderForTesting(mockImageLoader);

        subject = new FullscreenAdController(activity, mockBundle, mockIntent, adData);
        subject.setCloseableLayout(mockCloseableLayout);
        subject.setImageView(mockImageView);
    }

    @After
    public void tearDown() {
        LocalBroadcastManager.getInstance(activity).unregisterReceiver(broadcastReceiver);
        Networking.clearForTesting();
    }

    // region constructor
    @Test
    public void constructor_shouldSetShowCountdownTimerToMainAdConfigShowCountdownTimer() throws JSONException {
        setShowCd(ceSettingsJsonObject, true, true);
        subject = new FullscreenAdController(activity, mockBundle, mockIntent, adData);

        assertTrue(subject.getShowCountdownTimer());
    }

    @Test
    public void constructor_withJsonImage_shouldSetImageView_shouldSetClickDestinationUrl_shouldSetImageViewOnClickListener() {
        adData.setFullAdType(FullAdType.JSON);
        adData.setAdPayload(IMAGE_JSON);

        subject = new FullscreenAdController(activity, mockBundle, mockIntent, adData);

        assertNotNull(subject.getImageView());
        assertEquals(subject.getImageView(), subject.getCloseableLayout().getChildAt(0));
        assertEquals(IMAGE_CLICKDESTINATION_URL, subject.getImageClickDestinationUrl());
        assertEquals(FullscreenAdController.ControllerState.IMAGE, subject.getState());
        assertTrue(subject.getImageView().hasOnClickListeners());
    }

    @Test
    public void constructor_withRewardedJsonImage_shouldNotShowCloseButton() {
        adData.setFullAdType(FullAdType.JSON);
        adData.setAdPayload(IMAGE_JSON);
        adData.setRewarded(true);

        subject = new FullscreenAdController(activity, mockBundle, mockIntent, adData);

        assertFalse(subject.getCloseableLayout().isCloseVisible());
    }

    @Test
    public void constructor_withMraid_shouldSetUpMoPubWebViewController() {
        MoPubWebViewController webViewController = subject.getMoPubWebViewController();
        assertNotNull(webViewController);
        assertThat(webViewController.mWeakActivity.get()).isEqualTo(activity);
        assertThat(webViewController.mDspCreativeId).isEqualTo(DSP_CREATIVE_ID);
        assertNotNull(webViewController.mBaseWebViewListener);
        assertNotNull(webViewController.mWebView);
        assertTrue(webViewController.mIsPaused);
        assertThat(subject.getState()).isEqualTo(FullscreenAdController.ControllerState.MRAID);
    }

    @Test
    public void constructor_withVast_shouldSetUpVastVideoViewController() {
        adData.setFullAdType(FullAdType.VAST);

        subject = new FullscreenAdController(activity, mockBundle, mockIntent, adData);

        assertThat(subject.getVideoViewController()).isInstanceOf(VastVideoViewController.class);
        assertThat(subject.getState()).isEqualTo(FullscreenAdController.ControllerState.VIDEO);
    }

    @Test
    public void constructor_whenCountdownTimeIsLessThanOrEqualToZero_shouldShowCloseButton_shouldSetBackButtonEnabledTrue_shouldNotInitializeCountdownTimer() {
        // these ce settings ensure the calculated countdown time = 0
        creativeExperienceSettings = CreativeExperienceSettings.getDefaultSettings(false);
        adData.setCreativeExperienceSettings(creativeExperienceSettings);

        subject = new FullscreenAdController(activity, mockBundle, mockIntent, adData);

        assertEquals(0, subject.getCountdownTimeMillis());
        assertTrue(subject.getCloseableLayout().isCloseVisible());
        assertTrue(subject.backButtonEnabled());
        assertNull(subject.getRadialCountdownWidget());
    }

    @Test
    public void constructor_whenCountdownTimeIsGreaterThanZero_whenShowCountdownTimerIsTrue_whenShowCountdownTimerDelayIsLessThanCountdownTime_shouldSetShowCountdownTimerDelayToMainAdConfigCountdownTimerDelay() throws JSONException {
        setShowCd(ceSettingsJsonObject, true, true);

        subject = new FullscreenAdController(activity, mockBundle, mockIntent, adData);

        /*
        mShowCountdownTimerDelayMillis = adData.getCreativeExperienceSettings().getMainAdConfig()
            .getCountdownTimerDelaySecs() * MILLIS_IN_SECOND;
         */
        CreativeExperienceSettings ceSettings = adData.getCreativeExperienceSettings();
        assertNotNull(ceSettings.getMainAdConfig().getMinTimeUntilNextActionSecs());
        assertTrue(subject.getCountdownTimeMillis() > 0);
        assertTrue(subject.getShowCountdownTimer());
        assertTrue(subject.getShowCountdownTimerDelaysMillis() < subject.getCountdownTimeMillis());
        assertEquals(ceSettings.getMainAdConfig().getCountdownTimerDelaySecs() * 1000,
                subject.getShowCountdownTimerDelaysMillis());
    }

    @Test
    public void constructor_whenCountdownTimeIsGreaterThanZero_whenShowCountdownTimerFalse_shouldSetShowCountdownTimerDelayToCountdownTime_shouldSetShowCountdownTimerToFalse() throws JSONException {
        setShowCd(ceSettingsJsonObject, true, false);

        subject = new FullscreenAdController(activity, mockBundle, mockIntent, adData);

        /*
        if (!mShowCountdownTimer || mShowCountdownTimerDelayMillis >= mCountdownTimeMillis) {
            // Countdown timer is never shown
            mShowCountdownTimerDelayMillis = mCountdownTimeMillis;
            mShowCountdownTimer = false;
        }
        */
        assertTrue(subject.getCountdownTimeMillis() > 0);
        assertEquals(subject.getCountdownTimeMillis(), subject.getShowCountdownTimerDelaysMillis());
        assertFalse(subject.getShowCountdownTimer());
    }

    @Test
    public void constructor_whenCountdownTimeIsGreaterThanZero_whenShowCountdownTimerDelayIsGreaterThanOrEqualToCountdownTime_shouldSetShowCountdownTimerDelayToCountdownTime_shouldSetShowCountdownTimerToFalse() throws JSONException {
        // mainAdConfig.countdownTimerDelaySecs >= max(adTimeRemainingSecs, timeUntilNextActionSecs) = 30
        setShowCdDelay(ceSettingsJsonObject, true, 30);

        subject = new FullscreenAdController(activity, mockBundle, mockIntent, adData);

        /*
        if (!mShowCountdownTimer || mShowCountdownTimerDelayMillis >= mCountdownTimeMillis) {
            // Countdown timer is never shown
            mShowCountdownTimerDelayMillis = mCountdownTimeMillis;
            mShowCountdownTimer = false;
        }
        */
        assertTrue(subject.getCountdownTimeMillis() > 0);
        assertEquals(subject.getCountdownTimeMillis(), subject.getShowCountdownTimerDelaysMillis());
        assertFalse(subject.getShowCountdownTimer());
    }

    @Test
    public void constructor_whenCountdownTimeIsGreaterThanZero_shouldInitializeRadialCountdownWidget_shouldInitializeCountdownRunnable_shouldNotShowCloseButton() {
        subject = new FullscreenAdController(activity, mockBundle, mockIntent, adData);

        assertTrue(subject.getCountdownTimeMillis() > 0);
        RadialCountdownWidget radialCountdownWidget = subject.getRadialCountdownWidget();
        assertNotNull(radialCountdownWidget);
        assertTrue(subject.isCalibrationDone());
        assertEquals(View.INVISIBLE, radialCountdownWidget.getVisibility());
        assertEquals(subject.getCountdownTimeMillis(),
                radialCountdownWidget.getImageViewDrawable().getInitialCountdownMilliseconds());
        assertNotNull(subject.getCountdownRunnable());
        assertFalse(subject.getCloseableLayout().isCloseVisible());
        assertFalse(subject.backButtonEnabled());
    }
    // endregion constructor

    @Test
    public void pause_shouldStopRunnables() {
        adData.setRewarded(true);
        subject = new FullscreenAdController(activity, mockBundle, mockIntent, adData);
        subject.resume();

        subject.pause();

        assertThat(subject.getCountdownRunnable().isRunning()).isFalse();
    }

    @Test
    public void resume_shouldStartRunnables() {
        adData.setRewarded(true);
        subject = new FullscreenAdController(activity, mockBundle, mockIntent, adData);

        subject.resume();

        assertThat(subject.getCountdownRunnable().isRunning()).isTrue();
    }

    @Test
    public void destroy_shouldStopRunnables() {
        adData.setRewarded(true);
        subject = new FullscreenAdController(activity, mockBundle, mockIntent, adData);
        subject.resume();

        subject.destroy();

        assertThat(subject.getCountdownRunnable().isRunning()).isFalse();
    }

    @Test
    public void showCloseButton_shouldToggleVisibilityStatesAndFireEvents() {
        adData.setRewarded(true);
        subject = new FullscreenAdController(activity, mockBundle, mockIntent, adData);

        RadialCountdownWidget radialCountdownWidget = subject.getRadialCountdownWidget();

        assertThat(subject.getCloseableLayout().isCloseVisible()).isFalse();
        assertThat(radialCountdownWidget.getVisibility()).isEqualTo(View.INVISIBLE);
        assertThat(subject.isShowCloseButtonEventFired()).isFalse();
        assertThat(subject.isRewarded()).isFalse();
        subject.resume();

        subject.showCloseButton();

        assertThat(subject.getCloseableLayout().isCloseVisible()).isTrue();
        assertThat(radialCountdownWidget.getVisibility()).isEqualTo(View.GONE);
        assertThat(subject.isShowCloseButtonEventFired()).isTrue();
        assertThat(subject.isRewarded()).isTrue();
    }

    @Test
    public void onCompanionAdReady_withVastCompanionAdConfig_withBlurredLastVideoFrame_shouldSetUpCompanionAd() {
        subject.onCompanionAdReady(vastCompanionAdConfig, VIDEO_DURATION_MS);

        final ImageView blurredLastVideoFrameImageView = subject.getImageView();
        assertNull(blurredLastVideoFrameImageView.getParent());
        assertThat(blurredLastVideoFrameImageView.getVisibility()).isEqualTo(View.VISIBLE);
        final ShadowView blurredLastVideoFrameImageViewShadow = shadowOf(blurredLastVideoFrameImageView);
        // This has been changed for the new player which allows a click on the blurred frame
        assertThat(blurredLastVideoFrameImageViewShadow.getOnClickListener()).isNotNull();
        assertNull(subject.getImageClickDestinationUrl());
        assertNotNull(subject.getBlurLastVideoFrameTask());
        assertThat(subject.getSelectedVastCompanionAdConfig()).isEqualTo(vastCompanionAdConfig);
    }

    @Test
    public void onAdClicked_withNoCompanionAd_withMraid_shouldBroadcastClick() throws InterruptedException {
        final Semaphore semaphore = new Semaphore(0);
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                assertThat(intent.getAction()).isEqualTo("com.mopub.action.fullscreen.click");
                assertThat(intent.getLongExtra(DataKeys.BROADCAST_IDENTIFIER_KEY, -1)).isEqualTo(broadcastIdentifier);

                semaphore.release();
            }
        };
        LocalBroadcastManager.getInstance(activity).registerReceiver(broadcastReceiver,
                new EventForwardingBroadcastReceiver(null, broadcastIdentifier).getIntentFilter());

        subject.onAdClicked(activity, adData);

        semaphore.acquire();
    }

    @Test
    public void onAdClicked_withNoCompanionAd_withImage_shouldBroadcastClick() throws InterruptedException {
        final Semaphore semaphore = new Semaphore(0);
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                assertThat(intent.getAction()).isEqualTo("com.mopub.action.fullscreen.click");
                assertThat(intent.getLongExtra(DataKeys.BROADCAST_IDENTIFIER_KEY, -1)).isEqualTo(broadcastIdentifier);

                semaphore.release();
            }
        };
        LocalBroadcastManager.getInstance(activity).registerReceiver(broadcastReceiver,
                new EventForwardingBroadcastReceiver(null, broadcastIdentifier).getIntentFilter());
        adData.setFullAdType(FullAdType.JSON);
        adData.setAdPayload(IMAGE_JSON);
        subject = new FullscreenAdController(activity, mockBundle, mockIntent, adData);

        subject.onAdClicked(activity, adData);

        semaphore.acquire();
    }

    @Test
    public void onAdClicked_withBlurredLastFrameCompanion_shouldBroadcastClick() throws InterruptedException {
        final Semaphore semaphore = new Semaphore(0);
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                assertThat(intent.getAction()).isEqualTo("com.mopub.action.fullscreen.click");
                assertThat(intent.getLongExtra(DataKeys.BROADCAST_IDENTIFIER_KEY, -1)).isEqualTo(broadcastIdentifier);

                semaphore.release();
            }
        };
        LocalBroadcastManager.getInstance(activity).registerReceiver(broadcastReceiver,
                new EventForwardingBroadcastReceiver(null, broadcastIdentifier).getIntentFilter());
        subject.onCompanionAdReady(vastCompanionAdConfig, VIDEO_DURATION_MS);

        subject.onAdClicked(activity, adData);

        semaphore.acquire();
    }

    @Test
    public void onAdClicked_withStaticImageCompanion_shouldBroadcastClick() throws InterruptedException {
        setCompanionResource(VastResource.Type.STATIC_RESOURCE, VastResource.CreativeType.IMAGE);
        vastCompanionAdConfigs.clear();
        vastCompanionAdConfigs.add(vastCompanionAdConfig);
        final Semaphore semaphore = new Semaphore(0);
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                assertThat(intent.getAction()).isEqualTo("com.mopub.action.fullscreen.click");
                assertThat(intent.getLongExtra(DataKeys.BROADCAST_IDENTIFIER_KEY, -1)).isEqualTo(broadcastIdentifier);

                semaphore.release();
            }
        };
        LocalBroadcastManager.getInstance(activity).registerReceiver(broadcastReceiver,
                new EventForwardingBroadcastReceiver(null, broadcastIdentifier).getIntentFilter());
        subject.onCompanionAdReady(vastCompanionAdConfig, VIDEO_DURATION_MS);

        subject.onAdClicked(activity, adData);

        semaphore.acquire();
    }

    @Test
    public void onAdClicked_withJavascriptCompanion_shouldBroadcastClick() throws InterruptedException {
        setCompanionResource(VastResource.Type.STATIC_RESOURCE, VastResource.CreativeType.JAVASCRIPT);
        vastCompanionAdConfigs.clear();
        vastCompanionAdConfigs.add(vastCompanionAdConfig);
        final Semaphore semaphore = new Semaphore(0);
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                assertThat(intent.getAction()).isEqualTo("com.mopub.action.fullscreen.click");
                assertThat(intent.getLongExtra(DataKeys.BROADCAST_IDENTIFIER_KEY, -1)).isEqualTo(broadcastIdentifier);

                semaphore.release();
            }
        };
        LocalBroadcastManager.getInstance(activity).registerReceiver(broadcastReceiver,
                new EventForwardingBroadcastReceiver(null, broadcastIdentifier).getIntentFilter());
        subject.onCompanionAdReady(vastCompanionAdConfig, VIDEO_DURATION_MS);

        subject.onAdClicked(activity, adData);

        semaphore.acquire();
    }

    @Test
    public void onAdClicked_withHtmlCompanion_shouldBroadcastClick() throws InterruptedException {
        setCompanionResource(VastResource.Type.HTML_RESOURCE, VastResource.CreativeType.NONE);
        vastCompanionAdConfigs.clear();
        vastCompanionAdConfigs.add(vastCompanionAdConfig);
        final Semaphore semaphore = new Semaphore(0);
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                assertThat(intent.getAction()).isEqualTo("com.mopub.action.fullscreen.click");
                assertThat(intent.getLongExtra(DataKeys.BROADCAST_IDENTIFIER_KEY, -1)).isEqualTo(broadcastIdentifier);

                semaphore.release();
            }
        };
        LocalBroadcastManager.getInstance(activity).registerReceiver(broadcastReceiver,
                new EventForwardingBroadcastReceiver(null, broadcastIdentifier).getIntentFilter());
        subject.onCompanionAdReady(vastCompanionAdConfig, VIDEO_DURATION_MS);

        subject.onAdClicked(activity, adData);

        semaphore.acquire();
    }

    @Test
    public void onAdClicked_withIframeCompanion_shouldBroadcastClick() throws InterruptedException {
        setCompanionResource(VastResource.Type.IFRAME_RESOURCE, VastResource.CreativeType.NONE);
        vastCompanionAdConfigs.clear();
        vastCompanionAdConfigs.add(vastCompanionAdConfig);
        final Semaphore semaphore = new Semaphore(0);
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                assertThat(intent.getAction()).isEqualTo("com.mopub.action.fullscreen.click");
                assertThat(intent.getLongExtra(DataKeys.BROADCAST_IDENTIFIER_KEY, -1)).isEqualTo(broadcastIdentifier);

                semaphore.release();
            }
        };
        LocalBroadcastManager.getInstance(activity).registerReceiver(broadcastReceiver,
                new EventForwardingBroadcastReceiver(null, broadcastIdentifier).getIntentFilter());
        subject.onCompanionAdReady(vastCompanionAdConfig, VIDEO_DURATION_MS);

        subject.onAdClicked(activity, adData);

        semaphore.acquire();
    }

    // region onVideoFinish
    @Test
    public void onVideoFinish_withNullCloseableLayout_shouldFinishActivity_shouldNotChangeVideoTimeElapsed() {
        subject.setCloseableLayout(null);
        subject.setVideoTimeElapsed(Integer.MIN_VALUE);

        subject.onVideoFinish(Integer.MAX_VALUE);

        verify(activity).finish();

        assertThat(subject.getVideoTimeElapsed()).isEqualTo(Integer.MIN_VALUE);
    }

    @Test
    public void onVideoFinish_withOnVideoFinishCalledTrue_shouldNotChangeVideoTimeElapsed() {
        subject.setSelectedVastCompanionAdConfig(vastCompanionAdConfig);
        subject.setOnVideoFinishCalled(true);
        subject.setVideoTimeElapsed(Integer.MIN_VALUE);

        subject.onVideoFinish(Integer.MAX_VALUE);

        assertThat(subject.getVideoTimeElapsed()).isEqualTo(Integer.MIN_VALUE);

        verify(activity, never()).finish();
    }

    @Test
    public void onVideoFinish_withOnVideoFinishCalledFalse_shouldChangeVideoTimeElapsed() {
        subject.setSelectedVastCompanionAdConfig(vastCompanionAdConfig);
        subject.setOnVideoFinishCalled(false);
        subject.setVideoTimeElapsed(Integer.MIN_VALUE);

        // This needs a real CloseableLayout
        final CloseableLayout closeableLayout = new CloseableLayout(activity, null);
        subject.setCloseableLayout(closeableLayout);

        subject.onVideoFinish(Integer.MAX_VALUE);

        assertThat(subject.getVideoTimeElapsed()).isEqualTo(Integer.MAX_VALUE);
        assertTrue(subject.getOnVideoFinishCalled());
    }

    @Test
    public void onVideoFinish_withNullSelectedVastCompanionAdConfig_shouldFinishActivity_shouldNotChangeVideoTimeElapsed() {
        subject.setSelectedVastCompanionAdConfig(null);
        subject.setVideoTimeElapsed(Integer.MIN_VALUE);

        subject.onVideoFinish(Integer.MAX_VALUE);

        verify(activity).finish();

        assertThat(subject.getVideoTimeElapsed()).isEqualTo(Integer.MIN_VALUE);
    }

    @Test
    public void onVideoFinish_withNonNullVideoViewController_shouldPauseVideoViewController_shouldDestroyVideoViewController() {
        final BaseVideoViewController mockVideoViewController = mock(BaseVideoViewController.class);
        subject.setVideoViewController(mockVideoViewController);

        subject.setSelectedVastCompanionAdConfig(vastCompanionAdConfig);

        // This needs a real CloseableLayout
        final CloseableLayout closeableLayout = new CloseableLayout(activity, null);
        subject.setCloseableLayout(closeableLayout);

        subject.onVideoFinish(Integer.MAX_VALUE);

        verify(mockVideoViewController).onPause();
        verify(mockVideoViewController).onDestroy();
    }

    @Test
    public void onVideoFinish_withStaticResourceType_withImageCreativeType_withNullImageView_shouldSetControllerState_shouldFinishActivity() {
        final CloseableLayout mockCloseableLayout = mock(CloseableLayout.class);
        subject.setCloseableLayout(mockCloseableLayout);

        subject.setImageView(null);

        VastResource vastResource = new VastResource(
                COMPANION_RESOURCE,
                VastResource.Type.STATIC_RESOURCE,
                VastResource.CreativeType.IMAGE,
                COMPANION_WIDTH,
                COMPANION_HEIGHT);

        VastCompanionAdConfig companionAdConfig = new VastCompanionAdConfig(
                COMPANION_WIDTH,
                COMPANION_HEIGHT,
                vastResource,
                COMPANION_CLICKTHROUGH_URL,
                companionClickTrackers,
                companionCreativeViewTrackers,
                null);

        subject.setSelectedVastCompanionAdConfig(companionAdConfig);

        subject.onVideoFinish(Integer.MAX_VALUE);

        verify(mockCloseableLayout).removeAllViews();
        verify(mockCloseableLayout).setOnCloseListener(any(CloseableLayout.OnCloseListener.class));

        /*
        if (VastResource.Type.STATIC_RESOURCE.equals(vastResource.getType()) &&
                VastResource.CreativeType.IMAGE.equals(vastResource.getCreativeType()) ||
                VastResource.Type.BLURRED_LAST_FRAME.equals(vastResource.getType())) {
         */

        assertThat(subject.getState()).isEqualTo(FullscreenAdController.ControllerState.IMAGE);

        verify(activity).finish();
    }

    @Test
    public void onVideoFinish_withBlurredLastFrameType_withNullImageView_shouldSetControllerState_shouldFinishActivity() {
        final CloseableLayout mockCloseableLayout = mock(CloseableLayout.class);
        subject.setCloseableLayout(mockCloseableLayout);

        subject.setImageView(null);

        VastResource vastResource = new VastResource(
                COMPANION_RESOURCE,
                VastResource.Type.BLURRED_LAST_FRAME,
                VastResource.CreativeType.IMAGE,
                COMPANION_WIDTH,
                COMPANION_HEIGHT);

        VastCompanionAdConfig companionAdConfig = new VastCompanionAdConfig(
                COMPANION_WIDTH,
                COMPANION_HEIGHT,
                vastResource,
                COMPANION_CLICKTHROUGH_URL,
                companionClickTrackers,
                companionCreativeViewTrackers,
                null);

        subject.setSelectedVastCompanionAdConfig(companionAdConfig);

        subject.onVideoFinish(Integer.MAX_VALUE);

        verify(mockCloseableLayout).removeAllViews();
        verify(mockCloseableLayout).setOnCloseListener(any(CloseableLayout.OnCloseListener.class));

        /*
        if (VastResource.Type.STATIC_RESOURCE.equals(vastResource.getType()) &&
                VastResource.CreativeType.IMAGE.equals(vastResource.getCreativeType()) ||
                VastResource.Type.BLURRED_LAST_FRAME.equals(vastResource.getType())) {
         */

        assertThat(subject.getState()).isEqualTo(FullscreenAdController.ControllerState.IMAGE);

        verify(activity).finish();
    }

    @Test
    public void onVideoFinish_shouldSetShowCountdownTimerToEndCardShowCdValue() throws JSONException {
        setShowCd(ceSettingsJsonObject, false, true);
        // Using an interactive resource to ensure show cd timer delay < show close button delay
        setCompanionResource(VastResource.Type.HTML_RESOURCE, VastResource.CreativeType.NONE);

        subject.onVideoFinish(Integer.MAX_VALUE);

        // mShowCountdownTimer = endCardAdConfig.getShowCountdownTimer();
        assertTrue(subject.getShowCountdownTimer());
    }

    @Test
    public void onVideoFinish_whenCountdownTimeLessThanOrEqualToZero_shouldSetCloseAlwaysInteractableTrue_shouldShowCloseButton_shouldNotInitializeCountdownTimer_shouldHandleImpression() {
        // these ce settings ensure the calculated countdown time = 0
        creativeExperienceSettings = CreativeExperienceSettings.getDefaultSettings(true);
        adData.setCreativeExperienceSettings(creativeExperienceSettings);
        setCompanionResource(VastResource.Type.STATIC_RESOURCE, VastResource.CreativeType.IMAGE);

        final ImageButton mockImageButton = mock(ImageButton.class);
        when(mockCloseableLayout.findViewById(com.mopub.mobileads.base.R.id.mopub_closeable_layout_close_button))
                .thenReturn(mockImageButton);

        final VideoCtaButtonWidget mockVideoCtaButtonWidget = mock(VideoCtaButtonWidget.class);
        when(mockCloseableLayout.findViewById(com.mopub.mobileads.base.R.id.mopub_fullscreen_video_cta_button))
                .thenReturn(mockVideoCtaButtonWidget);

        final RadialCountdownWidget radialCW = new RadialCountdownWidget(activity, null);
        when(mockCloseableLayout.findViewById(com.mopub.mobileads.base.R.id.mopub_fullscreen_radial_countdown))
                .thenReturn(radialCW);

        subject.onVideoFinish(Integer.MAX_VALUE);

        assertEquals(0, subject.getCountdownTimeMillis());
        verify(mockCloseableLayout).setCloseAlwaysInteractable(true);
        verify(mockCloseableLayout).setCloseVisible(true);
        assertEquals(View.GONE, subject.getRadialCountdownWidget().getVisibility());

        // Called twice for two creative view trackers
        verify(mockRequestQueue, times(2)).add(any(TrackingRequest.class));
    }

    @Test
    public void onVideoFinish_whenCountdownTimeGreaterThanZero_whenShowCountdownTimerIsTrue_whenShowCountdownTimerDelayIsLessThanCountdownTime_shouldSetShowCountdownTimerDelayToEndCardConfigShowCountdownDelay() throws JSONException {
        setShowCd(ceSettingsJsonObject, false, true);
        // Using an interactive resource to ensure show cd timer delay < show close button delay
        setCompanionResource(VastResource.Type.HTML_RESOURCE, VastResource.CreativeType.NONE);

        subject.onVideoFinish(Integer.MAX_VALUE);

        // mShowCountdownTimerDelayMillis = endCardAdConfig.getCountdownTimerDelaySecs() * MILLIS_IN_SECOND;
        CreativeExperienceAdConfig endCardConfig = creativeExperienceSettings.getEndCardConfig();
        assertTrue(subject.getCountdownTimeMillis() > 0);
        assertTrue(subject.getShowCountdownTimer());
        assertTrue(subject.getShowCountdownTimerDelaysMillis() < subject.getCountdownTimeMillis());
        assertEquals(endCardConfig.getCountdownTimerDelaySecs() * 1000,
                subject.getShowCountdownTimerDelaysMillis());
    }

    @Test
    public void onVideoFinish_whenCloseButtonDelayGreaterThanZero_whenShowCountdownTimerFalse_shouldSetShowCountdownTimerDelayToCountdownTime_shouldSetShowCountdownTimerToFalse() throws JSONException {
        setShowCd(ceSettingsJsonObject, false, false);
        setCompanionResource(VastResource.Type.STATIC_RESOURCE, VastResource.CreativeType.IMAGE);

        // This needs a real CloseableLayout
        final CloseableLayout closeableLayout = new CloseableLayout(activity, null);
        subject.setCloseableLayout(closeableLayout);

        subject.onVideoFinish(Integer.MAX_VALUE);

        /*
        if (!mShowCountdownTimer || ...) {
            // Countdown timer is never shown
            mShowCountdownTimerDelayMillis = mCountdownTimeMillis;
            mShowCountdownTimer = false;
         }
         */
        assertTrue(subject.getCountdownTimeMillis() > 0);
        assertFalse(subject.getShowCountdownTimer());
        assertEquals(subject.getCountdownTimeMillis(), subject.getShowCountdownTimerDelaysMillis());
    }

    @Test
    public void onVideoFinish_withCloseButtonDelayGreaterThanZero_whenShowCountdownTimerDelayIsGreaterThanOrEqualToCountdownTime_shouldSetShowCountdownTimerDelayToCountdownTime_shouldSetShowCountdownTimerToFalse() {
        setCompanionResource(VastResource.Type.STATIC_RESOURCE, VastResource.CreativeType.IMAGE);

        // This needs a real CloseableLayout
        final CloseableLayout closeableLayout = new CloseableLayout(activity, null);
        subject.setCloseableLayout(closeableLayout);

        // Current CE settings will set mShowCountdownTimerDelayMillis = 5000, mCountdownTimeMillis = 2000
        subject.onVideoFinish(Integer.MAX_VALUE);

        /*
        if (... || mShowCountdownTimerDelayMillis >= mCountdownTimeMillis) {
            // Countdown timer is never shown
            mShowCountdownTimerDelayMillis = mCountdownTimeMillis;
            mShowCountdownTimer = false;
        }
         */
        assertTrue(subject.getCountdownTimeMillis() > 0);
        assertFalse(subject.getShowCountdownTimer());
        assertEquals(subject.getCountdownTimeMillis(), subject.getShowCountdownTimerDelaysMillis());
    }

    @Test
    public void onVideoFinish_whenCountdownTimeGreaterThanZero_shouldInitializeRadialCountdownWidget_shouldInitializeAndStartCountdownRunnable_shouldNotShowCloseButton_shouldHandleImpression() {
        setCompanionResource(VastResource.Type.STATIC_RESOURCE, VastResource.CreativeType.IMAGE);

        final ImageButton mockImageButton = mock(ImageButton.class);
        when(mockCloseableLayout.findViewById(com.mopub.mobileads.base.R.id.mopub_closeable_layout_close_button))
                .thenReturn(mockImageButton);

        final VideoCtaButtonWidget mockVideoCtaButtonWidget = mock(VideoCtaButtonWidget.class);
        when(mockCloseableLayout.findViewById(com.mopub.mobileads.base.R.id.mopub_fullscreen_video_cta_button))
                .thenReturn(mockVideoCtaButtonWidget);

        final RadialCountdownWidget radialCW = new RadialCountdownWidget(activity, null);
        when(mockCloseableLayout.findViewById(com.mopub.mobileads.base.R.id.mopub_fullscreen_radial_countdown))
                .thenReturn(radialCW);

        subject.onVideoFinish(Integer.MAX_VALUE);

        assertTrue(subject.getCountdownTimeMillis() > 0);
        verify(mockCloseableLayout).setCloseAlwaysInteractable(false);
        verify(mockCloseableLayout).setCloseVisible(false);

        RadialCountdownWidget radialCountdownWidget = subject.getRadialCountdownWidget();
        assertNotNull(radialCountdownWidget);
        assertTrue(subject.isCalibrationDone());
        assertEquals(subject.getCountdownTimeMillis(),
                radialCountdownWidget.getImageViewDrawable().getInitialCountdownMilliseconds());
        assertNotNull(subject.getCountdownRunnable());
        assertTrue(subject.getCountdownRunnable().isRunning());
        // Called twice for two creative view trackers
        verify(mockRequestQueue, times(2)).add(any(TrackingRequest.class));
    }

    /* Inside updateCountdown
    // Make the countdown timer visible if the show countdown timer delay has passed
    if (!mShowCloseButtonEventFired && mShowCountdownTimer
            && mRadialCountdownWidget.getVisibility() != View.VISIBLE
            && currentElapsedTimeMillis >= mShowCountdownTimerDelayMillis) {
        mRadialCountdownWidget.setVisibility(View.VISIBLE);
    }
    */
    @Test
    public void onVideoFinish_whenShowCloseButtonNotFired_shouldNotChangeCountdownVisibility() {
        setCompanionResource(VastResource.Type.STATIC_RESOURCE, VastResource.CreativeType.IMAGE);

        assertFalse(subject.isShowCloseButtonEventFired());

        // This needs a real CloseableLayout
        final CloseableLayout closeableLayout = new CloseableLayout(activity, null);
        subject.setCloseableLayout(closeableLayout);

        subject.onVideoFinish(1000);

        assertTrue(subject.isCalibrationDone());
        RadialCountdownWidget radialCountdownWidget = subject.getRadialCountdownWidget();
        assertNotNull(radialCountdownWidget);
        assertEquals(View.INVISIBLE, radialCountdownWidget.getVisibility());
    }

    @Test
    public void onVideoFinish_whenShowCountdownTimerFalse_shouldNotChangeCountdownVisibility() throws JSONException {
        setShowCd(ceSettingsJsonObject, false, false);
        setCompanionResource(VastResource.Type.STATIC_RESOURCE, VastResource.CreativeType.IMAGE);

        // This needs a real CloseableLayout
        final CloseableLayout closeableLayout = new CloseableLayout(activity, null);
        subject.setCloseableLayout(closeableLayout);

        subject.onVideoFinish(1000);

        assertTrue(subject.isCalibrationDone());
        RadialCountdownWidget radialCountdownWidget = subject.getRadialCountdownWidget();
        assertNotNull(radialCountdownWidget);
        assertEquals(View.INVISIBLE, radialCountdownWidget.getVisibility());
    }

    @Test
    public void onVideoFinish_whenTimeElapsedIsLessThanShowCountdownTimerDelay_shouldNotMakeCountdownVisible() throws JSONException {
        setShowCd(ceSettingsJsonObject, false, true);
        setCompanionResource(VastResource.Type.STATIC_RESOURCE, VastResource.CreativeType.IMAGE);

        // This needs a real CloseableLayout
        final CloseableLayout closeableLayout = new CloseableLayout(activity, null);
        subject.setCloseableLayout(closeableLayout);

        subject.onVideoFinish(1000);

        assertTrue(subject.getCurrentTimeElapsedMillis() <
                subject.getShowCountdownTimerDelaysMillis());
        assertTrue(subject.isCalibrationDone());
        RadialCountdownWidget radialCountdownWidget = subject.getRadialCountdownWidget();
        assertNotNull(radialCountdownWidget);
        assertEquals(View.INVISIBLE, radialCountdownWidget.getVisibility());
    }

    @Test
    public void onVideoFinish_whenTimeElapsedIsGreaterThanOrEqualToShowCountdownTimerDelay_whenShowCloseButtonEventNotFired_whenCountdownTimerIsNotVisible_whenShowCountdownTimerTrue_shouldMakeCountdownVisible() throws JSONException {
        setShowCd(ceSettingsJsonObject, false, true);
        setShowCdDelay(ceSettingsJsonObject, false, 0);
        setCompanionResource(VastResource.Type.STATIC_RESOURCE, VastResource.CreativeType.IMAGE);

        // This needs a real CloseableLayout
        final CloseableLayout closeableLayout = new CloseableLayout(activity, null);
        subject.setCloseableLayout(closeableLayout);

        subject.onVideoFinish(1000);

        assertTrue(subject.isCalibrationDone());
        RadialCountdownWidget radialCountdownWidget = subject.getRadialCountdownWidget();
        assertNotNull(radialCountdownWidget);
        assertEquals(View.VISIBLE, radialCountdownWidget.getVisibility());
    }

    // endregion onVideoFinish

    @Test
    public void destroy_withBlurLastVideoFrameTaskStillPending_shouldCancelTask() {

        VastVideoBlurLastVideoFrameTask mockBlurLastVideoFrameTask = mock(VastVideoBlurLastVideoFrameTask.class);
        when(mockBlurLastVideoFrameTask.getStatus()).thenReturn(AsyncTask.Status.PENDING);
        subject.setBlurLastVideoFrameTask(mockBlurLastVideoFrameTask);

        subject.destroy();

        verify(mockBlurLastVideoFrameTask).cancel(true);
    }

    @Test
    public void destroy_withBlurLastVideoFrameTaskFinished_shouldCancelTask() {

        VastVideoBlurLastVideoFrameTask mockBlurLastVideoFrameTask = mock(VastVideoBlurLastVideoFrameTask.class);
        when(mockBlurLastVideoFrameTask.getStatus()).thenReturn(AsyncTask.Status.FINISHED);
        subject.setBlurLastVideoFrameTask(mockBlurLastVideoFrameTask);

        subject.destroy();

        verify(mockBlurLastVideoFrameTask).cancel(anyBoolean());
    }

    private void setShowCd(JSONObject ceSettings,
                           boolean isMainAd,
                           boolean showCd) throws JSONException {
        String key = isMainAd ? Constants.CE_MAIN_AD : Constants.CE_END_CARD;
        JSONObject adConfig = ceSettings.getJSONObject(key);
        adConfig.remove(Constants.CE_SHOW_COUNTDOWN_TIMER);
        adConfig.put(Constants.CE_SHOW_COUNTDOWN_TIMER, showCd ? "1" :"0");
        ceSettings.remove(key);
        ceSettings.put(key, adConfig);
        creativeExperienceSettings = CreativeExperienceSettingsParser
                .parse(ceSettings, false);
        adData.setCreativeExperienceSettings(creativeExperienceSettings);
    }

    private void setShowCdDelay(JSONObject ceSettings,
                                boolean isMainAd,
                                int delay) throws JSONException {
        String key = isMainAd ? Constants.CE_MAIN_AD : Constants.CE_END_CARD;
        JSONObject adConfig = ceSettings.getJSONObject(key);
        adConfig.remove(Constants.CE_COUNTDOWN_TIMER_DELAY);
        adConfig.put(Constants.CE_COUNTDOWN_TIMER_DELAY, delay);
        ceSettings.remove(key);
        ceSettings.put(key, adConfig);
        creativeExperienceSettings = CreativeExperienceSettingsParser
                .parse(ceSettings, false);
        adData.setCreativeExperienceSettings(creativeExperienceSettings);
    }

    void setCompanionResource(VastResource.Type resourceType,
                              VastResource.CreativeType creativeType) {
        VastResource vastResource = new VastResource(
                COMPANION_RESOURCE,
                resourceType,
                creativeType,
                COMPANION_WIDTH,
                COMPANION_HEIGHT);

        VastCompanionAdConfig companionAdConfig  = new VastCompanionAdConfig(
                COMPANION_WIDTH,
                COMPANION_HEIGHT,
                vastResource,
                COMPANION_CLICKTHROUGH_URL,
                companionClickTrackers,
                companionCreativeViewTrackers,
                null);

        subject.setSelectedVastCompanionAdConfig(companionAdConfig);
    }
}
