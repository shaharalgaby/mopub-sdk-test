// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.nativeads;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import com.mopub.common.ClientMetadata;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.nativeads.MoPubNativeAdPositioning.MoPubClientPositioning;
import com.mopub.nativeads.PositioningSource.PositioningListener;
import com.mopub.network.MoPubNetworkError;
import com.mopub.network.MoPubRequest;
import com.mopub.network.MoPubRequestQueue;
import com.mopub.network.Networking;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.shadows.ShadowLog;

import java.util.HashSet;
import java.util.List;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
public class ServerPositioningSourceTest {
    @Mock PositioningListener mockPositioningListener;
    @Captor ArgumentCaptor<PositioningRequest> positionRequestCaptor;
    @Mock Context mockContext;
    @Mock ClientMetadata mockClientMetaData;
    @Mock MoPubRequestQueue mockRequestQueue;

    ServerPositioningSource subject;
    private Activity spyActivity;

    @Before
    public void setUp() {
        Activity activity = Robolectric.buildActivity(Activity.class).create().get();
        spyActivity = spy(activity);

        subject = new ServerPositioningSource(spyActivity);
        setupClientMetadata();
        Networking.setRequestQueueForTesting(mockRequestQueue);
        MoPubLog.setLogLevel(MoPubLog.LogLevel.DEBUG);
    }

    private void setupClientMetadata() {
        when(mockClientMetaData.getSdkVersion()).thenReturn("sdk_version");
        when(mockClientMetaData.getAppName()).thenReturn("app_name");
        when(mockClientMetaData.getAppPackageName()).thenReturn("app_package_name");
        when(mockClientMetaData.getAppVersion()).thenReturn("app_version");
        when(mockClientMetaData.getDeviceManufacturer()).thenReturn("device_manufacturer");
        when(mockClientMetaData.getDeviceModel()).thenReturn("device_model");
        when(mockClientMetaData.getDeviceProduct()).thenReturn("device_product");
        when(mockClientMetaData.getDeviceHardware()).thenReturn("device_hardware");
        when(mockClientMetaData.getDeviceOsVersion()).thenReturn("device_os_version");
        when(mockClientMetaData.getDeviceScreenWidthDip()).thenReturn(1337);
        when(mockClientMetaData.getDeviceScreenHeightDip()).thenReturn(70707);
        when(mockClientMetaData.getActiveNetworkType()).thenReturn(ClientMetadata.MoPubNetworkType.WIFI);
        when(mockClientMetaData.getNetworkOperator()).thenReturn("network_operator");
        when(mockClientMetaData.getNetworkOperatorName()).thenReturn("network_operator_name");
        when(mockClientMetaData.getIsoCountryCode()).thenReturn("network_iso_country_code");
        when(mockClientMetaData.getSimOperator()).thenReturn("network_sim_operator");
        when(mockClientMetaData.getSimOperatorName()).thenReturn("network_sim_operator_name");
        when(mockClientMetaData.getSimIsoCountryCode()).thenReturn("network_sim_iso_country_code");
        ClientMetadata.setInstance(mockClientMetaData);
    }

    @Test
    public void loadPositions_shouldAddToRequestQueue() {
        subject.loadPositions("test_ad_unit", mockPositioningListener);
        verify(mockRequestQueue).add(any(MoPubRequest.class));
    }

    @Test
    public void loadPositionsTwice_shouldCancelPreviousRequest_shouldNotCallListener() {
        subject.loadPositions("test_ad_unit", mockPositioningListener);
        subject.loadPositions("test_ad_unit", mockPositioningListener);
        verify(mockRequestQueue, times(2)).add(any(MoPubRequest.class));

        verify(mockPositioningListener, never()).onFailed();
        verify(mockPositioningListener, never()).onLoad(any(MoPubClientPositioning.class));
    }

    @Test
    public void loadPositionsTwice_afterSuccess_shouldNotCancelPreviousRequest() {
        subject.loadPositions("test_ad_unit", mockPositioningListener);
        verify(mockRequestQueue).add(positionRequestCaptor.capture());
        reset(mockRequestQueue);

        subject.loadPositions("test_ad_unit", mockPositioningListener);
        verify(mockRequestQueue).add(any(MoPubRequest.class));
    }

    @Test
    public void loadPositions_thenComplete_withValidResponse_shouldCallOnLoadListener() {
        subject.loadPositions("test_ad_unit", mockPositioningListener);

        verify(mockRequestQueue).add(positionRequestCaptor.capture());

        final PositioningRequest value = positionRequestCaptor.getValue();
        final MoPubClientPositioning response = new MoPubClientPositioning().enableRepeatingPositions(3);
        value.deliverResponse(response);

        verify(mockPositioningListener).onLoad(eq(response));
    }

    @Test
    public void loadPositions_thenComplete_withErrorResponse_shouldRetry() {
        subject.loadPositions("test_ad_unit", mockPositioningListener);

        verify(mockRequestQueue).add(positionRequestCaptor.capture());
        reset(mockRequestQueue);

        // We get VolleyErrors for invalid JSON, 404s, 5xx, and {"error": "WARMING_UP"}
        MoPubNetworkError error = new MoPubNetworkError.Builder().build();
        positionRequestCaptor.getValue().getMoPubListener().onErrorResponse(error);

        Robolectric.getForegroundThreadScheduler().advanceToLastPostedRunnable();
        verify(mockRequestQueue).add(any(MoPubRequest.class));
    }


    @Test
    public void loadPositions_withPendingRetry_shouldNotRetry() {
        subject.loadPositions("test_ad_unit", mockPositioningListener);

        verify(mockRequestQueue).add(positionRequestCaptor.capture());
        reset(mockRequestQueue);

        MoPubNetworkError error = new MoPubNetworkError.Builder("testError").build();
        positionRequestCaptor.getValue().getMoPubListener().onErrorResponse(error);

        subject.loadPositions("test_ad_unit", mockPositioningListener);
        Robolectric.getForegroundThreadScheduler().advanceToLastPostedRunnable();
        // If a retry happened, we'd have two here.
        verify(mockRequestQueue).add(any(MoPubRequest.class));
    }

    @Test
    public void loadPositions_thenFailAfterMaxRetryTime_shouldCallFailureHandler() {
        subject.loadPositions("test_ad_unit", mockPositioningListener);
        // Simulate failure after max time.
        subject.setMaximumRetryTimeMilliseconds(999);

        verify(mockRequestQueue).add(positionRequestCaptor.capture());
        MoPubNetworkError error = new MoPubNetworkError.Builder("test error", null).build();
        positionRequestCaptor.getValue().getMoPubListener().onErrorResponse(error);
        verify(mockPositioningListener).onFailed();
    }

    @Test
    public void loadPositions_withNoConnection_shouldLogMoPubErrorCodeNoConnection_shouldCallFailureHandler() {
        MoPubLog.setLogLevel(MoPubLog.LogLevel.DEBUG);

        when(mockContext.checkCallingOrSelfPermission(anyString()))
                .thenReturn(PackageManager.PERMISSION_DENIED);
        when(spyActivity.getApplicationContext()).thenReturn(mockContext);
        // Reinit the subject so we get our mocked context.
        subject = new ServerPositioningSource(spyActivity);

        // Simulate failure after max time.
        subject.setMaximumRetryTimeMilliseconds(999);
        subject.loadPositions("test_ad_unit", mockPositioningListener);

        verify(mockRequestQueue).add(positionRequestCaptor.capture());
        MoPubNetworkError error = new MoPubNetworkError.Builder().build();
        positionRequestCaptor.getValue().getMoPubListener().onErrorResponse(error);

        verify(mockPositioningListener).onFailed();

        final List<ShadowLog.LogItem> allLogItems = ShadowLog.getLogs();
        HashSet<String> allLogMessages = new HashSet<>(allLogItems.size());

        for (ShadowLog.LogItem logItem : allLogItems) {
            allLogMessages.add(logItem.msg.trim());
        }

        assertThat(allLogMessages).contains("[com.mopub.nativeads.ServerPositioningSourceTest][loadPositions_withNo" +
                "Connection_shouldLogMoPubErrorCodeNoConnection_shouldCallFailureHandler] SDK Log - " +
                        MoPubErrorCode.NO_CONNECTION.toString());
        assertThat(allLogMessages).contains("[com.mopub.nativeads.ServerPositioningSource]" +
                "[access$300] SDK Log - Error downloading positioning information");
    }
}
