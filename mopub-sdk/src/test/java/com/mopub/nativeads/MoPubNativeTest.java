// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.nativeads;

import android.app.Activity;

import com.mopub.common.MoPub;
import com.mopub.common.SdkConfiguration;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.privacy.MoPubIdentifierTest;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.common.util.AsyncTasks;
import com.mopub.common.util.Reflection;
import com.mopub.common.util.test.support.TestMethodBuilderFactory;
import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.nativeads.MoPubNative.MoPubNativeNetworkListener;
import com.mopub.network.MoPubNetworkError;
import com.mopub.network.MoPubRequest;
import com.mopub.network.MoPubRequestQueue;
import com.mopub.network.Networking;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;
import org.robolectric.android.util.concurrent.RoboExecutorService;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.shadows.ShadowLooper;

import java.net.MalformedURLException;
import java.util.List;

import static android.Manifest.permission.ACCESS_NETWORK_STATE;
import static android.Manifest.permission.INTERNET;
import static com.mopub.common.MoPubRequestMatcher.isUrl;
import static com.mopub.common.util.Reflection.MethodBuilder;
import static com.mopub.nativeads.MoPubNative.EMPTY_NETWORK_LISTENER;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@RunWith(SdkTestRunner.class)
public class MoPubNativeTest {
    private MoPubNative subject;
    private MethodBuilder methodBuilder;
    private Activity context;
    private static final String adUnitId = "test_adunit_id";

    @Mock private MoPubNativeNetworkListener mockNetworkListener;
    @Mock private MoPubRequestQueue mockRequestQueue;
    @Mock private AdRendererRegistry mockAdRendererRegistry;
    @Mock private MoPubStaticNativeAdRenderer mockRenderer;

    @Before
    public void setup() throws Exception {
        context = Robolectric.buildActivity(Activity.class).create().get();
        AsyncTasks.setExecutor(new RoboExecutorService());
        MoPub.initializeSdk(context, new SdkConfiguration.Builder("adunit")
                .withLogLevel(MoPubLog.LogLevel.DEBUG)
                .build(), null);
        ShadowLooper.runUiThreadTasks();
        Reflection.getPrivateField(MoPub.class, "sSdkInitialized").setBoolean(null, true);

        MoPubIdentifierTest.writeAdvertisingInfoToSharedPreferences(context, false);
        Shadows.shadowOf(context).grantPermissions(ACCESS_NETWORK_STATE);
        Shadows.shadowOf(context).grantPermissions(INTERNET);
        subject = new MoPubNative(context, adUnitId, mockAdRendererRegistry, mockNetworkListener);
        methodBuilder = TestMethodBuilderFactory.getSingletonMock();
        Networking.setRequestQueueForTesting(mockRequestQueue);
    }

    @After
    public void tearDown() throws Exception {
        MoPubIdentifierTest.clearPreferences(context);
        reset(methodBuilder);
        new Reflection.MethodBuilder(null, "resetMoPub")
                .setStatic(MoPub.class)
                .setAccessible()
                .execute();
    }

    @Test
    public void registerAdRenderer_shouldCallAdRednererRegistryRegisterAdRenderer() throws Exception {
        subject.registerAdRenderer(mockRenderer);

        verify(mockAdRendererRegistry).registerAdRenderer(mockRenderer);
    }

    @Test
    public void destroy_shouldSetListenersToEmptyAndClearContext() {
        assertThat(subject.getContextOrDestroy()).isSameAs(context);
        assertThat(subject.getMoPubNativeNetworkListener()).isSameAs(mockNetworkListener);

        subject.destroy();

        assertThat(subject.getContextOrDestroy()).isNull();
        assertThat(subject.getMoPubNativeNetworkListener()).isSameAs(EMPTY_NETWORK_LISTENER);
    }

    @Test
    public void loadNativeAd_shouldReturnFast() {
        Robolectric.getForegroundThreadScheduler().pause();

        subject.destroy();
        subject.makeRequest();

        assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(0);
    }

    @Test
    public void requestNativeAd_shouldFireNetworkRequest() {
        subject.requestNativeAd("https://www.mopub.com", null);

        verify(mockNetworkListener, never()).onNativeFail(any(NativeErrorCode.class));
        verify(mockRequestQueue).add(argThat(isUrl("https://www.mopub.com")));
    }

    @Test
    public void requestNativeAd_whenRequestQueueDeliversUnknownError_shouldFireNativeFail() {
        reset(mockRequestQueue);
        doAnswer(invocation -> {
            MoPubNetworkError error = new MoPubNetworkError.Builder(null, new MalformedURLException()).build();
            ((MoPubRequest<?>) invocation.getArguments()[0]).getMoPubListener().onErrorResponse(error);
            return null;
        }).when(mockRequestQueue).add(any(MoPubRequest.class));

        subject.requestNativeAd("//\\//\\::::", null);

        verify(mockNetworkListener).onNativeFail(any(NativeErrorCode.class));
    }

    @Test
    public void requestNativeAd_withNullUrl_shouldFireNativeFail() {
        Robolectric.getForegroundThreadScheduler().pause();

        subject.requestNativeAd(null, null);

        verify(mockNetworkListener).onNativeFail(any(NativeErrorCode.class));
        verify(mockRequestQueue, never()).add(any(MoPubRequest.class));
    }

    @Test
    public void onAdError_shouldNotifyListener() {
        subject.onAdError(new MoPubNetworkError.Builder()
                .reason(MoPubNetworkError.Reason.BAD_BODY)
                .build());

        verify(mockNetworkListener).onNativeFail(eq(NativeErrorCode.INVALID_RESPONSE));
    }

    @Test
    public void onAdError_whenNotMoPubError_shouldNotifyListener() {
        subject.onAdError(new MoPubNetworkError.Builder("generic").build());

        verify(mockNetworkListener).onNativeFail(eq(NativeErrorCode.UNSPECIFIED));
    }

    @Test
    public void onAdError_withWarmingUpError_shouldLogMoPubErrorCodeWarmup_shouldNotifyListener() {
        MoPubLog.setLogLevel(MoPubLog.LogLevel.DEBUG);

        subject.onAdError(new MoPubNetworkError.Builder()
                .reason(MoPubNetworkError.Reason.WARMING_UP)
                .build());

        final List<ShadowLog.LogItem> allLogMessages = ShadowLog.getLogs();
        final ShadowLog.LogItem latestLogMessage = allLogMessages.get(allLogMessages.size() - 1);

        // All log messages end with a newline character.
        assertThat(latestLogMessage.msg.trim()).isEqualTo(
                "[com.mopub.nativeads.MoPubNativeTest][onAdError_withWarmingUpError_shouldLogMoPubErrorCodeWarmup_" +
                        "shouldNotifyListener] Ad Log - " + MoPubErrorCode.WARMUP.toString());
        verify(mockNetworkListener).onNativeFail(eq(NativeErrorCode.EMPTY_AD_RESPONSE));
    }

    @Test
    public void onAdError_withNoConnection_shouldLogMoPubErrorCodeNoConnection_shouldNotifyListener() {
        MoPubLog.setLogLevel(MoPubLog.LogLevel.DEBUG);
        Shadows.shadowOf(context).denyPermissions(INTERNET);

        subject.onAdError(new MoPubNetworkError.Builder().build());

        final List<ShadowLog.LogItem> allLogMessages = ShadowLog.getLogs();
        final ShadowLog.LogItem latestLogMessage = allLogMessages.get(allLogMessages.size() - 1);

        // All log messages end with a newline character.
        assertThat(latestLogMessage.msg.trim()).isEqualTo(
                "[com.mopub.nativeads.MoPubNativeTest][onAdError_withNoConnection_shouldLogMoPubErrorCodeNoConnection" +
                        "_shouldNotifyListener] Ad Log - " + MoPubErrorCode.NO_CONNECTION.toString());
        verify(mockNetworkListener).onNativeFail(eq(NativeErrorCode.CONNECTION_ERROR));
    }

    @Test
    public void onAdError_withRateLimiting_shouldLogMoPubErrorCodeTooManyRequests_shouldNotifyListener() {
        MoPubLog.setLogLevel(MoPubLog.LogLevel.DEBUG);

        subject.onAdError(new MoPubNetworkError.Builder().reason(MoPubNetworkError.Reason.TOO_MANY_REQUESTS).build());

        verify(mockNetworkListener).onNativeFail(eq(NativeErrorCode.TOO_MANY_REQUESTS));
    }
}
