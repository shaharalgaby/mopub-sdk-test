// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.network

import android.app.Activity
import android.content.Context

import com.mopub.common.Constants.HOST
import com.mopub.common.Constants.HTTPS
import com.mopub.common.MoPubRequestMatcher.Companion.isUrl
import com.mopub.common.test.support.KtArgumentCaptor.argumentCaptor
import com.mopub.common.test.support.KtMatchers.any
import com.mopub.common.test.support.KtMatchers.argThat
import com.mopub.common.test.support.SdkTestRunner
import com.mopub.mobileads.VastErrorCode
import com.mopub.mobileads.VastTracker
import com.mopub.network.Networking.setRequestQueueForTesting
import junit.framework.Assert.*

import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.verify
import org.robolectric.Robolectric

@RunWith(SdkTestRunner::class)
class TrackingRequestTest {

    @Mock
    private lateinit var mockRequestQueue: MoPubRequestQueue
    private lateinit var context: Context
    private val url = "$HTTPS://$HOST"
    private val trackingRequestCaptor = argumentCaptor<TrackingRequest>()

    @Before
    fun setup() {
        context = Robolectric.buildActivity(Activity::class.java).create().get()
        setRequestQueueForTesting(mockRequestQueue)
        Networking.urlRewriter = PlayServicesUrlRewriter()
    }

    @Test
    fun getParams_withMoPubRequest_shouldReturnParamMap() {
        TrackingRequest.makeTrackingHttpRequest(url, context)

        verify(mockRequestQueue).add(trackingRequestCaptor.capture())
        assertNotNull(trackingRequestCaptor.firstValue.getParams())
    }

    @Test
    fun getParams_withNonMoPubRequest_shouldReturnNull() {
        val nonMoPubUrl = "https://www.abcdefg.com/xyz"
        TrackingRequest.makeTrackingHttpRequest(nonMoPubUrl, context)

        verify(mockRequestQueue).add(trackingRequestCaptor.capture())
        assertNull(trackingRequestCaptor.firstValue.getParams())
    }

    @Test
    fun getBodyContentType_withMoPubRequest_shouldReturnJsonContentType() {
        doNothing().`when`(mockRequestQueue).add(any<TrackingRequest>())
        TrackingRequest.makeTrackingHttpRequest(url, context)

        verify(mockRequestQueue).add(trackingRequestCaptor.capture())
        Assert.assertEquals(
            MoPubRequest.JSON_CONTENT_TYPE,
            trackingRequestCaptor.firstValue.getBodyContentType()
        )
    }

    @Test
    fun getBodyContentType_withNonMoPubRequest_shouldReturnDefaultContentType() {
        val nonMoPubUrl = "https://www.abcdefg.com/xyz"

        doNothing().`when`(mockRequestQueue).add(any<TrackingRequest>())
        TrackingRequest.makeTrackingHttpRequest(nonMoPubUrl, context)

        verify(mockRequestQueue).add(trackingRequestCaptor.capture())
        Assert.assertEquals(
            MoPubRequest.DEFAULT_CONTENT_TYPE,
            trackingRequestCaptor.firstValue.getBodyContentType()
        )
    }

    @Test
    @Throws(Exception::class)
    fun makeTrackingHttpRequest_shouldMakeTrackingHttpRequestWithWebViewUserAgent() {
        doNothing().`when`(mockRequestQueue).add(any<TrackingRequest>())
        TrackingRequest.makeTrackingHttpRequest(url, context)

        verify(mockRequestQueue).add(any<TrackingRequest>())
    }

    @Test
    fun makeTrackingHttpRequest_withNullUrl_shouldNotMakeTrackingHttpRequest() {
        TrackingRequest.makeTrackingHttpRequest(null as String?, context)

        verify(mockRequestQueue, Mockito.never()).add(any<TrackingRequest>())
    }

    @Test
    @Throws(Exception::class)
    fun makeTrackingHttpRequest_withNullContext_shouldNotMakeTrackingHttpRequest() {
        TrackingRequest.makeTrackingHttpRequest(url, null)

        verify(mockRequestQueue, Mockito.never()).add(any<TrackingRequest>())
    }

    @Test
    @Throws(Exception::class)
    fun makeVastTrackingTwoHttpRequest_shouldSubstituteMacros_shouldMakeSingleRequest() {
        val vastTracker =
            VastTracker.Builder("uri?errorcode=[ERRORCODE]&contentplayhead=[CONTENTPLAYHEAD]&asseturi=[ASSETURI]")
                .build()
        TrackingRequest.makeVastTrackingHttpRequest(
            listOf(vastTracker),
            VastErrorCode.UNDEFINED_ERROR,
            123,
            "assetUri",
            context
        )

        verify(mockRequestQueue).add(
            argThat(isUrl("uri?errorcode=900&contentplayhead=00:00:00.123&asseturi=assetUri"))
        )

        TrackingRequest.makeVastTrackingHttpRequest(
            listOf(vastTracker),
            VastErrorCode.UNDEFINED_ERROR,
            123,
            "assetUri",
            context
        )
        Mockito.verifyNoMoreInteractions(mockRequestQueue)
    }

    @Test
    @Throws(Exception::class)
    fun makeVastTrackingTwoHttpRequest_withRepeatableRequest_shouldMakeMultipleTrackingRequests() {
        val vastTracker =
            VastTracker.Builder("uri?errorcode=[ERRORCODE]&contentplayhead=[CONTENTPLAYHEAD]&asseturi=[ASSETURI]")
                .build()
        TrackingRequest.makeVastTrackingHttpRequest(
            listOf(vastTracker),
            VastErrorCode.UNDEFINED_ERROR,
            123,
            "assetUri",
            context
        )

        verify(mockRequestQueue).add(
            argThat(isUrl("uri?errorcode=900&contentplayhead=00:00:00.123&asseturi=assetUri"))
        )

        TrackingRequest.makeVastTrackingHttpRequest(
            listOf(vastTracker),
            VastErrorCode.UNDEFINED_ERROR,
            123,
            "assetUri",
            context
        )

        verify(mockRequestQueue).add(
            argThat(isUrl("uri?errorcode=900&contentplayhead=00:00:00.123&asseturi=assetUri"))
        )
    }
}
