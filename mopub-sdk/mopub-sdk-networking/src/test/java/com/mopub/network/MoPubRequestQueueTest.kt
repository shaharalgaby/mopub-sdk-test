// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.network

import android.content.Context

import com.mopub.network.CustomSSLSocketFactory.Companion.getDefault
import com.mopub.test.support.NetworkingTestRunner
import com.mopub.volley.CacheDispatcher
import com.mopub.volley.Request
import com.mopub.volley.RequestQueue

import org.fest.assertions.api.Assertions.assertThat
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock

import java.io.File

@RunWith(NetworkingTestRunner::class)
class MoPubRequestQueueTest {
    private lateinit var subject: MoPubRequestQueue
    private lateinit var volleyRequestQueue: RequestQueue
    private lateinit var moPubRequest: TestMoPubRequest
    @Mock
    private lateinit var volleyCacheDir: File
    @Mock
    private lateinit var context: Context

    @Before
    fun setup() {
        val customSSLSocketFactory = getDefault(0)
        val urlRewriter = object : MoPubUrlRewriter {}
        subject = MoPubRequestQueue("user-agent", customSSLSocketFactory, urlRewriter, volleyCacheDir)
        volleyRequestQueue = subject.getVolleyRequestQueue()
        moPubRequest = TestMoPubRequest(context, null, "test")
    }

    @Test
    fun start_shouldStartVolleyRequestQueue() {
        subject.start()

        val cacheDispatcherField = RequestQueue::class.java.getDeclaredField("mCacheDispatcher")
        cacheDispatcherField.isAccessible = true
        val cacheDispatcher = cacheDispatcherField.get(subject.getVolleyRequestQueue())
        val cacheDispatcherQuitField = CacheDispatcher::class.java.getDeclaredField("mQuit")
        cacheDispatcherQuitField.isAccessible = true
        val cacheDispatcherQuit = cacheDispatcherQuitField.get(cacheDispatcher) as Boolean
        assertFalse(cacheDispatcherQuit)
    }

    @Test
    fun add_shouldAddRequestToVolleyRequestQueue() {
        subject.add(moPubRequest)

        val requestsField = RequestQueue::class.java.getDeclaredField("mCurrentRequests")
        requestsField.isAccessible = true
        val currentRequests = requestsField.get(subject.getVolleyRequestQueue()) as Set<Request<*>>
        assertTrue(currentRequests.contains(moPubRequest.getVolleyRequest()))
    }

    @Test
    fun cancel_shouldCancelRequestInVolleyRequestQueue() {
        subject.add(moPubRequest)

        subject.cancel(moPubRequest)

        val requestsField = RequestQueue::class.java.getDeclaredField("mCurrentRequests")
        requestsField.isAccessible = true
        val currentRequests = requestsField.get(subject.getVolleyRequestQueue()) as Set<Request<*>>
        val volleyRequest = currentRequests.find {
            it === moPubRequest.getVolleyRequest()
        }
        assertThat(volleyRequest?.isCanceled).isTrue
    }

    @Test
    fun cancelAll_withTag_shouldCancelRequestWithSameTagInVolleyRequestQueue() {
        moPubRequest.setTag("tag1")
        val moPubRequest2 = TestMoPubRequest(context, null, "test")
        moPubRequest2.setTag("tag2")
        subject.add(moPubRequest)
        subject.add(moPubRequest2)

        subject.cancelAll("tag2")

        val requestsField = RequestQueue::class.java.getDeclaredField("mCurrentRequests")
        requestsField.isAccessible = true
        val currentRequests = requestsField.get(subject.getVolleyRequestQueue()) as Set<Request<*>>
        currentRequests.forEach {
            if (it.tag == "tag2") {
                assertTrue(it.isCanceled)
            } else {
                assertFalse(it.isCanceled)
            }
        }
    }

    class TestMoPubRequest(
        context: Context,
        listener: MoPubResponse.Listener<String>?,
        url: String
    ) : MoPubRequest<String>(
        context,
        url,
        "https://ads.mopub.com/m/ad",
        Method.POST,
        listener
    ) {
        override fun deliverResponse(response: String) {}

        override fun parseNetworkResponse(networkResponse: MoPubNetworkResponse?): MoPubResponse<String>? {
            return networkResponse?.let {
                MoPubResponse.success("test", MoPubNetworkResponse(it.statusCode, it.data, it.headers))
            }
        }
    }
}
