// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common.privacy;

import android.app.Activity;

import com.mopub.common.Constants;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.network.MoPubNetworkError;
import com.mopub.network.MoPubNetworkResponse;
import com.mopub.network.MoPubResponse;
import com.mopub.network.MoPubRetryPolicy;
import com.mopub.network.Networking;
import com.mopub.network.PlayServicesUrlRewriter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;

import java.nio.charset.Charset;
import java.util.Collections;

import static com.mopub.network.MoPubRequest.DEFAULT_CONTENT_TYPE;
import static com.mopub.network.MoPubRequest.JSON_CONTENT_TYPE;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(SdkTestRunner.class)
public class ConsentDialogRequestTest {
    private static final String URL = "https://"+ Constants.HOST+"/m/gdpr_consent_dialog?id=testAdUnitId&nv=5.0.0&language=en";
    private static final String HTML = "html-body-text";
    private static final String BODY = "{ dialog_html : '" + HTML + "' }";

    @Mock
    private ConsentDialogRequest.Listener listener;

    private Activity activity;
    private ConsentDialogRequest subject;

    @Before
    public void setup() {
        activity = Robolectric.buildActivity(Activity.class).create().get();
        subject = new ConsentDialogRequest(activity, URL, listener);

        Networking.setUrlRewriter(new PlayServicesUrlRewriter());
    }

    @Test
    public void constructor_shouldSetParametersCorrectly() {
        MoPubRetryPolicy retryPolicy = subject.getRetryPolicy();

        assertThat(subject.getUrl()).isEqualTo(URL.substring(0, URL.indexOf('?')));
        assertThat(retryPolicy).isNotNull();
        assertThat(retryPolicy.getInitialTimeoutMs()).isEqualTo(MoPubRetryPolicy.DEFAULT_TIMEOUT_MS);
        assertThat(subject.getShouldCache()).isFalse();
    }

    @Test
    public void getParams_withMoPubRequest_shouldReturnParamMap() {
        assertNotNull(subject.getParams());
    }

    @Test
    public void getParams_withNonMoPubRequest_shouldReturnNull() {
        String nonMoPubUrl = "https://www.abcdefg.com/xyz";
        subject = new ConsentDialogRequest(activity, nonMoPubUrl, listener);

        assertNull(subject.getParams());
    }

    @Test
    public void getBodyContentType_withMoPubRequest_shouldReturnJsonContentType() {
        assertEquals(JSON_CONTENT_TYPE, subject.getBodyContentType());
    }

    @Test
    public void getBodyContentType_withNonMoPubRequest_shouldReturnDefaultContentType() {
        String nonMoPubUrl = "https://www.abcdefg.com/xyz";
        subject = new ConsentDialogRequest(activity, nonMoPubUrl, listener);

        assertEquals(DEFAULT_CONTENT_TYPE, subject.getBodyContentType());
    }

    @Test
    public void parseNetworkResponse_withValidBody_shouldSucceed() {
        MoPubNetworkResponse testResponse = new MoPubNetworkResponse(200, BODY.getBytes(Charset.defaultCharset()),
                Collections.emptyMap());
        final MoPubResponse<ConsentDialogResponse> response = subject.parseNetworkResponse(testResponse);

        assertThat(response).isNotNull();
        assertThat(response.getMoPubResult()).isNotNull();
        assertThat(response.getMoPubResult().getHtml()).isEqualTo(HTML);
    }

    @Test
    public void parseNetworkResponse_withEmptyBody_shouldReturnErrorBadBody() {
        MoPubNetworkResponse testResponse = new MoPubNetworkResponse(500, "".getBytes(Charset.defaultCharset()),
                Collections.emptyMap());
        final MoPubResponse<ConsentDialogResponse> response = subject.parseNetworkResponse(testResponse);

        assertThat(response).isNotNull();
        assertThat(response.getMoPubNetworkError()).isNotNull();
        assertEquals(MoPubNetworkError.Reason.BAD_BODY, response.getMoPubNetworkError().getReason());
    }

    @Test
    public void parseNetworkResponse_withBrokenJsonBody_shouldReturnErrorBadBody() {
        MoPubNetworkResponse testResponse = new MoPubNetworkResponse(500,
                "{ html - 'body' }".getBytes(Charset.defaultCharset()), Collections.emptyMap());
        final MoPubResponse<ConsentDialogResponse> response = subject.parseNetworkResponse(testResponse);

        assertThat(response).isNotNull();
        assertThat(response.getMoPubNetworkError()).isNotNull();
        assertEquals(MoPubNetworkError.Reason.BAD_BODY, response.getMoPubNetworkError().getReason());
    }

    @Test
    public void parseNetworkResponse_withJsonNoHtmlTag_shouldReturnErrorBadBody() {
        MoPubNetworkResponse testResponse = new MoPubNetworkResponse(500, "{ k: 1 }".getBytes(Charset.defaultCharset()),
                Collections.emptyMap());
        final MoPubResponse<ConsentDialogResponse> response = subject.parseNetworkResponse(testResponse);

        assertThat(response).isNotNull();
        assertThat(response.getMoPubNetworkError()).isNotNull();
        assertEquals(MoPubNetworkError.Reason.BAD_BODY, response.getMoPubNetworkError().getReason());
    }

    @Test
    public void deliverResponse_withValidListener_shouldCallListener() {
        ConsentDialogResponse response = new ConsentDialogResponse("html-text");
        subject.deliverResponse(response);

        verify(listener).onResponse(response);
    }

    @Test
    public void deliverResponse_withNullListener_shouldNotCrash() {
        subject = new ConsentDialogRequest(activity, URL, null);
        ConsentDialogResponse response = new ConsentDialogResponse("html-text");
        subject.deliverResponse(response);

        verify(listener, never()).onResponse(any(ConsentDialogResponse.class));
        verify(listener, never()).onErrorResponse(any(MoPubNetworkError.class));
    }
}
