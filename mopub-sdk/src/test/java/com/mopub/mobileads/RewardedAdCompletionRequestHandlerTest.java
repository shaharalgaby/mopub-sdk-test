// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;

import com.mopub.common.MoPub;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.network.MoPubNetworkError;
import com.mopub.network.MoPubNetworkResponse;
import com.mopub.network.MoPubRequestQueue;
import com.mopub.network.Networking;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;

import java.util.Collections;

import static com.mopub.common.MoPubRequestMatcher.isUrl;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

@RunWith(SdkTestRunner.class)
public class RewardedAdCompletionRequestHandlerTest {
    @Mock
    private MoPubRequestQueue mockRequestQueue;
    private Context context;
    private String url;
    private String customerId;
    private String rewardName;
    private String rewardAmount;
    private String className;
    private String customData;

    @Before
    public void setup() {
        context = Robolectric.buildActivity(Activity.class).create().get();
        url = "testUrl";
        customerId = "customer id";
        rewardName = "gold coins";
        rewardAmount = "25";
        className = "com.mopub.mobileads.MoPubFullscreen";
        customData = "custom data";
        Networking.setRequestQueueForTesting(mockRequestQueue);
    }

    @Test
    public void makeRewardedAdCompletionRequest_shouldAddMacros_shouldMakeVideoCompletionRequest() {
        RewardedAdCompletionRequestHandler.makeRewardedAdCompletionRequest(context, url,
                customerId, rewardName, rewardAmount, className, customData);

        verify(mockRequestQueue).add(argThat(isUrl(
                "testUrl&customer_id=customer%20id&rcn=gold%20coins&rca=25"
                        + "&nv=" + Uri.encode(MoPub.SDK_VERSION)
                        + "&v=" + MoPubRewardedAdManager.API_VERSION
                        + "&cec=com.mopub.mobileads.MoPubFullscreen"
                        + "&rcd=custom%20data"
                ))
        );
    }

    @Test
    public void makeRewardedAdCompletionRequest_withNullContext_shouldNotMakeVideoCompletionRequest() {
        RewardedAdCompletionRequestHandler.makeRewardedAdCompletionRequest(null, url,
                customerId, rewardName, rewardAmount, className, customData);
        verifyZeroInteractions(mockRequestQueue);
    }

    @Test
    public void makeRewardedAdCompletionRequest_withNullUrl_shouldNotMakeVideoCompletionRequest() {
        RewardedAdCompletionRequestHandler.makeRewardedAdCompletionRequest(context, null,
                customerId, rewardName, rewardAmount, className, customData);
        verifyZeroInteractions(mockRequestQueue);
    }

    @Test
    public void makeRewardedAdCompletionRequest_withEmptyUrl_shouldNotMakeVideoCompletionRequest() {
        RewardedAdCompletionRequestHandler.makeRewardedAdCompletionRequest(context, "",
                customerId, rewardName, rewardAmount, className, customData);
        verifyZeroInteractions(mockRequestQueue);
    }

    @Test
    public void makeRewardedAdCompletionRequest_withNullRewardName_shouldNotMakeVideoCompletionRequest() {
        RewardedAdCompletionRequestHandler.makeRewardedAdCompletionRequest(context, url,
                customerId, null, rewardAmount, className, customData);
        verifyZeroInteractions(mockRequestQueue);
    }

    @Test
    public void makeRewardedAdCompletionRequest_withNullRewardAmount_shouldNotMakeVideoCompletionRequest() {
        RewardedAdCompletionRequestHandler.makeRewardedAdCompletionRequest(context, url,
                customerId, rewardName, null, className, customData);
        verifyZeroInteractions(mockRequestQueue);
    }

    @Test
    public void makeRewardedAdCompletionRequest_withNullCustomEvent_shouldPassEmptyCustomEventQueryParam_shouldMakeVideoCompletionRequest() {
        RewardedAdCompletionRequestHandler.makeRewardedAdCompletionRequest(context, url,
                customerId, rewardName, rewardAmount, null, customData);

        verify(mockRequestQueue).add(argThat(isUrl(
                "testUrl&customer_id=customer%20id&rcn=gold%20coins&rca=25"
                        + "&nv=" + Uri.encode(MoPub.SDK_VERSION)
                        + "&v=" + MoPubRewardedAdManager.API_VERSION
                        + "&cec="
                        + "&rcd=custom%20data"
                ))
        );
    }

    @Test
    public void makeRewardedAdCompletionRequest_withAlreadyEncodedCustomData_shouldDoubleEncodeCustomData_shouldMakeVideoCompletionRequest() {
        RewardedAdCompletionRequestHandler.makeRewardedAdCompletionRequest(context, url,
                customerId, rewardName, rewardAmount, className, Uri.encode(customData));

        verify(mockRequestQueue).add(argThat(isUrl(
                "testUrl&customer_id=customer%20id&rcn=gold%20coins&rca=25"
                        + "&nv=" + Uri.encode(MoPub.SDK_VERSION)
                        + "&v=" + MoPubRewardedAdManager.API_VERSION
                        + "&cec=com.mopub.mobileads.MoPubFullscreen"
                        + "&rcd=custom%2520data"
                ))
        );
    }

    @Test
    public void makeRewardedAdCompletionRequest_withNullCustomData_shouldAddAllOtherMacros_shouldMakeVideoCompletionRequest() {
        RewardedAdCompletionRequestHandler.makeRewardedAdCompletionRequest(context, url,
                customerId, rewardName, rewardAmount, className, null);

        verify(mockRequestQueue).add(argThat(isUrl(
                "testUrl&customer_id=customer%20id&rcn=gold%20coins&rca=25"
                        + "&nv=" + Uri.encode(MoPub.SDK_VERSION)
                        + "&v=" + MoPubRewardedAdManager.API_VERSION
                        + "&cec=com.mopub.mobileads.MoPubFullscreen"
                ))
        );
    }

    @Test
    public void makeRewardedAdCompletionRequest_withEmptyCustomData_shouldAddAllOtherMacros_shouldMakeVideoCompletionRequest() {
        RewardedAdCompletionRequestHandler.makeRewardedAdCompletionRequest(context, url,
                customerId, rewardName, rewardAmount, className, "");

        verify(mockRequestQueue).add(argThat(isUrl(
                "testUrl&customer_id=customer%20id&rcn=gold%20coins&rca=25"
                        + "&nv=" + Uri.encode(MoPub.SDK_VERSION)
                        + "&v=" + MoPubRewardedAdManager.API_VERSION
                        + "&cec=com.mopub.mobileads.MoPubFullscreen"
                ))
        );
    }

    @Test
    public void getTimeout_shouldReturnCorrectTimeoutBasedOnRetry() {
        final int maxTimeout = RewardedAdCompletionRequestHandler.RETRY_TIMES[RewardedAdCompletionRequestHandler.RETRY_TIMES.length - 1];

        assertThat(RewardedAdCompletionRequestHandler.getTimeout(-1)).isEqualTo(maxTimeout);

        assertThat(RewardedAdCompletionRequestHandler.getTimeout(0)).isEqualTo(
                RewardedAdCompletionRequestHandler.RETRY_TIMES[0]);

        assertThat(RewardedAdCompletionRequestHandler.getTimeout(1)).isEqualTo(
                RewardedAdCompletionRequestHandler.RETRY_TIMES[1]);

        assertThat(RewardedAdCompletionRequestHandler.getTimeout(1234567)).isEqualTo(
                maxTimeout);
    }

    @Test
    public void retryTimes_shouldAllBeGreaterThanRequestTimeoutDelay() {
        for (int retryTime : RewardedAdCompletionRequestHandler.RETRY_TIMES) {
            assertThat(
                    retryTime - RewardedAdCompletionRequestHandler.REQUEST_TIMEOUT_DELAY)
                    .isGreaterThanOrEqualTo(0);
        }
    }

    @Test
    public void onErrorResponse_shouldSetShouldStopToTrueWhenResponseNot500To599() {
        RewardedAdCompletionRequestHandler subject =
                new RewardedAdCompletionRequestHandler(context, url, customerId, rewardName,
                        rewardAmount, className, customData);

        assertThat(subject.getShouldStop()).isEqualTo(false);

        subject.onErrorResponse(new MoPubNetworkError.Builder().networkResponse(
                new MoPubNetworkResponse(500, null, Collections.emptyMap())).build());
        assertThat(subject.getShouldStop()).isEqualTo(false);

        subject = new RewardedAdCompletionRequestHandler(context, url, customerId, rewardName,
                rewardAmount, className, customData);
        subject.onErrorResponse(new MoPubNetworkError.Builder().networkResponse(
                new MoPubNetworkResponse(501, null, Collections.emptyMap())).build());
        assertThat(subject.getShouldStop()).isEqualTo(false);

        subject = new RewardedAdCompletionRequestHandler(context, url, customerId, rewardName,
                rewardAmount, className, customData);
        subject.onErrorResponse(new MoPubNetworkError.Builder().networkResponse(
                new MoPubNetworkResponse(599, null, Collections.emptyMap())).build());
        assertThat(subject.getShouldStop()).isEqualTo(false);

        subject = new RewardedAdCompletionRequestHandler(context, url, customerId, rewardName,
                rewardAmount, className, customData);
        subject.onErrorResponse(new MoPubNetworkError.Builder().networkResponse(
                new MoPubNetworkResponse(200, null, Collections.emptyMap())).build());
        assertThat(subject.getShouldStop()).isEqualTo(true);

        subject = new RewardedAdCompletionRequestHandler(context, url, customerId, rewardName,
                rewardAmount, className, customData);
        subject.onErrorResponse(new MoPubNetworkError.Builder().networkResponse(
                new MoPubNetworkResponse(499, null, Collections.emptyMap())).build());
        assertThat(subject.getShouldStop()).isEqualTo(true);

        subject = new RewardedAdCompletionRequestHandler(context, url, customerId, rewardName,
                rewardAmount, className, customData);
        subject.onErrorResponse(new MoPubNetworkError.Builder().networkResponse(
                new MoPubNetworkResponse(600, null, Collections.emptyMap())).build());
        assertThat(subject.getShouldStop()).isEqualTo(true);
    }

    @Test
    public void onResponse_shouldSetShouldStopToTrueWhenResponseNot500To599() {
        RewardedAdCompletionRequestHandler subject =
                new RewardedAdCompletionRequestHandler(context, url, customerId, rewardName,
                        rewardAmount, className, customData);

        assertThat(subject.getShouldStop()).isEqualTo(false);

        subject.onResponse(500);
        assertThat(subject.getShouldStop()).isEqualTo(false);

        subject = new RewardedAdCompletionRequestHandler(context, url, customerId, rewardName,
                rewardAmount, className, customData);
        subject.onResponse(501);
        assertThat(subject.getShouldStop()).isEqualTo(false);

        subject = new RewardedAdCompletionRequestHandler(context, url, customerId, rewardName,
                rewardAmount, className, customData);
        subject.onResponse(599);
        assertThat(subject.getShouldStop()).isEqualTo(false);

        subject = new RewardedAdCompletionRequestHandler(context, url, customerId, rewardName,
                rewardAmount, className, customData);
        subject.onResponse(200);
        assertThat(subject.getShouldStop()).isEqualTo(true);

        subject = new RewardedAdCompletionRequestHandler(context, url, customerId, rewardName,
                rewardAmount, className, customData);
        subject.onResponse(499);
        assertThat(subject.getShouldStop()).isEqualTo(true);

        subject = new RewardedAdCompletionRequestHandler(context, url, customerId, rewardName,
                rewardAmount, className, customData);
        subject.onResponse(600);
        assertThat(subject.getShouldStop()).isEqualTo(true);
    }

    @Test
    public void makeRewardedAdCompletionRequest_shouldRetry() {
        Handler mockHandler = mock(Handler.class);
        RewardedAdCompletionRequestHandler subject =
                new RewardedAdCompletionRequestHandler(context, url, customerId, rewardName,
                        rewardAmount, className, customData, mockHandler);

        subject.makeRewardedAdCompletionRequest();

        assertThat(subject.getRetryCount()).isEqualTo(1);
        verify(mockHandler).postDelayed(any(Runnable.class),
                eq((long) RewardedAdCompletionRequestHandler.RETRY_TIMES[0]));
    }

    @Test
    public void makeRewardedAdCompletionRequest_shouldNotRetryIfShouldStopIsSetToTrue() {
        Handler mockHandler = mock(Handler.class);
        RewardedAdCompletionRequestHandler subject =
                new RewardedAdCompletionRequestHandler(context, url, customerId, rewardName,
                        rewardAmount, className, customData, mockHandler);
        // This should set shouldStop to true.
        subject.onResponse(200);

        subject.makeRewardedAdCompletionRequest();

        assertThat(subject.getShouldStop()).isTrue();
        verifyZeroInteractions(mockHandler);
    }

    @Test
    public void makeRewardedAdCompletionRequest_shouldNotRetryIfMaxRetriesReached() {
        Handler mockHandler = mock(Handler.class);
        RewardedAdCompletionRequestHandler subject =
                new RewardedAdCompletionRequestHandler(context, url, customerId, rewardName,
                        rewardAmount, className, customData, mockHandler);
        subject.setRetryCount(RewardedAdCompletionRequestHandler.MAX_RETRIES);

        subject.makeRewardedAdCompletionRequest();

        verifyZeroInteractions(mockHandler);
    }
}
