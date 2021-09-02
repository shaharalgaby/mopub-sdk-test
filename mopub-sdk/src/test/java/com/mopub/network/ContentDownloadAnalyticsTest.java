// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.network;

import android.app.Activity;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.MoPubErrorCode;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.robolectric.Robolectric;

import java.util.List;
import java.util.Arrays;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
public class ContentDownloadAnalyticsTest {
    private static final List<String> BEFORE_LOAD_URLS = Arrays.asList("beforeload1", "beforeload2");

    private static final List<String> AFTER_LOAD_URLS =
            Arrays.asList("https://ads.mopub.com/m/load?load_duration_ms=%%LOAD_DURATION_MS%%&load_result=%%LOAD_RESULT%%");

    private static final List<String> AFTER_LOAD_SUCCESS_URLS =
            Arrays.asList("https://ads.mopub.com/m/load?load_duration_ms=%%LOAD_DURATION_MS%%&load_result=%%LOAD_RESULT%%");

    private static final List<String> AFTER_LOAD_FAIL_URLS =
            Arrays.asList("https://ads.mopub.com/m/load?load_duration_ms=%%LOAD_DURATION_MS%%&load_result=%%LOAD_RESULT%%");

    @Mock
    private MoPubRequestQueue mockRequestQueue;
    @Mock
    private AdResponse mockAdResponse;
    @Mock
    private AdResponse mockInvalidAdResponse;

    private Activity activity;
    private ContentDownloadAnalytics subject;

    @Before
    public void setup() {
        activity = Robolectric.buildActivity(Activity.class).create().get();
        when(mockAdResponse.getBeforeLoadUrls()).thenReturn(BEFORE_LOAD_URLS);
        when(mockAdResponse.getAfterLoadUrls()).thenReturn(AFTER_LOAD_URLS);
        when(mockAdResponse.getAfterLoadSuccessUrls()).thenReturn(AFTER_LOAD_SUCCESS_URLS);
        when(mockAdResponse.getAfterLoadFailUrls()).thenReturn(AFTER_LOAD_FAIL_URLS);
        Networking.setRequestQueueForTesting(mockRequestQueue);
    }

    @Test
    public void reportBeforeLoad_withValidAdResponse_shouldAddRequestToNetworkQueue(){
        subject = new ContentDownloadAnalytics(mockAdResponse);

        subject.reportBeforeLoad(activity);

        assertThat(subject.mBeforeLoadTime).isNotZero();
        ArgumentCaptor<MoPubRequest> requestCaptor = ArgumentCaptor.forClass(MoPubRequest.class);
        verify(mockRequestQueue, times(2)).add(requestCaptor.capture());
        MoPubRequest moPubRequest = requestCaptor.getAllValues().get(0);
        assertThat(moPubRequest.getOriginalUrl()).isEqualTo("beforeload1");
        moPubRequest = requestCaptor.getAllValues().get(1);
        assertThat(moPubRequest.getOriginalUrl()).isEqualTo("beforeload2");
    }

    @Test
    public void reportBeforeLoad_withEmptyUrl_shouldNotSendRequest(){
        subject = new ContentDownloadAnalytics(mockInvalidAdResponse);

        subject.reportBeforeLoad(activity);

        verify(mockRequestQueue, never()).add(any(MoPubRequest.class));
        assertThat(subject.mBeforeLoadTime).isNotZero();
    }

    @Test
    public void reportAfterLoad_withValidAdResponse_withNoError_shouldSendNoErrorRequest(){
        subject = new ContentDownloadAnalytics(mockAdResponse);
        subject.reportBeforeLoad(activity);
        reset(mockRequestQueue);

        subject.reportAfterLoad(activity, MoPubErrorCode.AD_SUCCESS);

        ArgumentCaptor<MoPubRequest> reqeustCaptor = ArgumentCaptor.forClass(MoPubRequest.class);
        verify(mockRequestQueue).add(reqeustCaptor.capture());
        MoPubRequest moPubRequest = reqeustCaptor.getValue();
        assertThat(moPubRequest.getOriginalUrl().indexOf("ad_loaded")).isNotNegative();
    }

    @Test
    public void reportAfterLoad_withValidAdResponse_withTimeoutError_shouldSendTimeoutErrorRequest(){
        subject = new ContentDownloadAnalytics(mockAdResponse);
        subject.reportBeforeLoad(activity);
        reset(mockRequestQueue);

        subject.reportAfterLoad(activity, MoPubErrorCode.NETWORK_TIMEOUT);

        ArgumentCaptor<MoPubRequest> reqeustCaptor = ArgumentCaptor.forClass(MoPubRequest.class);
        verify(mockRequestQueue).add(reqeustCaptor.capture());
        MoPubRequest moPubRequest = reqeustCaptor.getValue();
        assertThat(moPubRequest.getOriginalUrl().indexOf("ad_loaded")).isNegative();
        assertThat(moPubRequest.getOriginalUrl().indexOf("timeout")).isNotNegative();
    }

    @Test
    public void reportAfterLoad_withEmptyUrl_shouldNotSendRequest(){
        when(mockInvalidAdResponse.getBeforeLoadUrls()).thenReturn(BEFORE_LOAD_URLS);
        subject = new ContentDownloadAnalytics(mockInvalidAdResponse);
        subject.reportBeforeLoad(activity);
        reset(mockRequestQueue);

        subject.reportAfterLoad(activity, MoPubErrorCode.NETWORK_TIMEOUT);

        verify(mockRequestQueue, never()).add(any(MoPubRequest.class));
    }

    @Test
    public void reportAfterLoadSuccess_withEmptyUrl_shouldNotSendRequest(){
        when(mockInvalidAdResponse.getBeforeLoadUrls()).thenReturn(BEFORE_LOAD_URLS);
        subject = new ContentDownloadAnalytics(mockInvalidAdResponse);
        subject.reportBeforeLoad(activity);
        reset(mockRequestQueue);

        subject.reportAfterLoadSuccess(activity);

        verify(mockRequestQueue, never()).add(any(MoPubRequest.class));
    }

    @Test
    public void reportAfterLoadFail_withEmptyUrl_shouldNotSendRequest(){
        when(mockInvalidAdResponse.getBeforeLoadUrls()).thenReturn(BEFORE_LOAD_URLS);
        subject = new ContentDownloadAnalytics(mockInvalidAdResponse);
        subject.reportBeforeLoad(activity);
        reset(mockRequestQueue);

        subject.reportAfterLoadFail(activity, MoPubErrorCode.NETWORK_TIMEOUT);

        verify(mockRequestQueue, never()).add(any(MoPubRequest.class));
    }

    @Test
    public void reportAfterLoad_withoutCallingReportBeforeLoad_shouldNotSendRequest(){
        subject = new ContentDownloadAnalytics(mockAdResponse);

        subject.reportAfterLoad(activity, MoPubErrorCode.NETWORK_TIMEOUT);

        verify(mockRequestQueue, never()).add(any(MoPubRequest.class));
    }

    @Test
    public void reportAfterLoadSuccess_withoutCallingReportBeforeLoad_shouldNotSendRequest(){
        subject = new ContentDownloadAnalytics(mockAdResponse);

        subject.reportAfterLoadSuccess(activity);

        verify(mockRequestQueue, never()).add(any(MoPubRequest.class));
    }

    @Test
    public void reportAfterLoadFail_withoutCallingReportBeforeLoad_shouldNotSendRequest(){
        subject = new ContentDownloadAnalytics(mockAdResponse);

        subject.reportAfterLoadFail(activity, MoPubErrorCode.NETWORK_TIMEOUT);

        verify(mockRequestQueue, never()).add(any(MoPubRequest.class));
    }
}
