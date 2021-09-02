// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.network

import android.app.Activity
import android.net.SSLCertificateSocketFactory
import android.webkit.WebSettings

import com.mopub.volley.CacheDispatcher
import com.mopub.volley.RequestQueue

import junit.framework.Assert.*

import org.fest.assertions.api.Assertions.assertThat
import org.junit.*
import org.junit.runner.RunWith
import org.mockito.Mockito.never
import org.powermock.api.mockito.PowerMockito.*
import org.powermock.core.classloader.annotations.PowerMockIgnore
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.rule.PowerMockRule
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@PowerMockIgnore("org.robolectric.*", "android.*", "com.sun.org.apache.xerces.internal.jaxp.*")
@PrepareForTest(WebSettings::class, SSLCertificateSocketFactory::class)
class NetworkingTest {
    private lateinit var context: Activity
    private lateinit var mockRequestQueue: MoPubRequestQueue
    private lateinit var mockImageLoader: MoPubImageLoader

    @get:Rule
    val rule = PowerMockRule()

    @Before
    fun setUp() {
        context = Robolectric.buildActivity(Activity::class.java).create().get()
        mockStatic(WebSettings::class.java)
        mockStatic(SSLCertificateSocketFactory::class.java)
        mockRequestQueue = mock(MoPubRequestQueue::class.java)
        mockImageLoader = mock(MoPubImageLoader::class.java)

        `when`(WebSettings.getDefaultUserAgent(context))
            .thenReturn("some android user agent")
    }

    @After
    fun tearDown() {
        Networking.clearForTesting()
    }

    @Test
    fun getUserAgent_usesCachedUserAgent() {
        Networking.setUserAgentForTesting("some cached user agent")

        val userAgent = Networking.getUserAgent(context)

        assertThat(userAgent).isEqualTo("some cached user agent")
    }

    @Test
    fun getUserAgent_shouldIncludeAndroid() {
        val userAgent = Networking.getUserAgent(context)

        assertThat(userAgent).isEqualTo("some android user agent")
    }

    @Test
    @Throws(InterruptedException::class)
    fun getUserAgent_whenOnABackgroundThread_shouldReturnHttpAgent() {
        val userAgent = arrayOfNulls<String>(1)
        val latch = CountDownLatch(1)

        object : Thread() {
            override fun run() {
                userAgent[0] = Networking.getUserAgent(context)
                latch.countDown()
            }
        }.start()
        latch.await(500, TimeUnit.MILLISECONDS)

        // Robolectric's default http agent is null which gets rewritten to an empty String.
        assertThat(userAgent[0]).isEqualTo("")
    }

    @Test
    fun getCachedUserAgent_usesCachedUserAgent() {
        Networking.setUserAgentForTesting("some cached user agent")

        val userAgent = Networking.cachedUserAgent

        assertThat(userAgent).isEqualTo("some cached user agent")
    }

    @Test
    fun getCachedUserAgent_shouldReturnDefaultAgent() {
        val userAgent = Networking.cachedUserAgent

        // Robolectric's default http agent is null which gets rewritten to an empty String.
        assertThat(userAgent).isEqualTo("")
    }

    @Test
    fun getRequestQueue_shouldReturnCachedRequestQueue() {
        Networking.setRequestQueueForTesting(mockRequestQueue)

        Assert.assertEquals(
            mockRequestQueue,
            Networking.getRequestQueue(context)
        )
        verifyStatic(never())
        Networking.getUserAgent(context.applicationContext)
    }

    @Test
    fun getRequestQueue_whenUrlRewriterIsNotNull_shouldInitializeNewRequestQueue_shouldSaveInstance() {
        Networking.urlRewriter = object : MoPubUrlRewriter {}

        val requestQueue = Networking.getRequestQueue(context)

        // Verify request queue initialization
        verifyStatic()
        Networking.getUserAgent(context.applicationContext)
        // Verify the request queue instance is saved
        assertEquals(Networking.requestQueue, requestQueue)
        // verify the cache dispatcher is not null since requestQueue.start() is called
        val cacheDispatcherField = RequestQueue::class.java.getDeclaredField("mCacheDispatcher")
        cacheDispatcherField.isAccessible = true
        val volleyCacheDispatcher = cacheDispatcherField
            .get(requestQueue.getVolleyRequestQueue()) as? CacheDispatcher
        assertNotNull(volleyCacheDispatcher)
    }

    @Test
    fun getRequestQueue_whenUrlRewriterIsNull_shouldReturnUnusableRequestQueue_shouldNotSaveInstance() {
        Networking.urlRewriter = null

        val volleyRequestQueue = Networking.getRequestQueue(context).getVolleyRequestQueue()

        // Verify the request queue instance is not saved
        assertNull(Networking.requestQueue)
        // Verify the cache dispatcher is null since requestQueue.start() is not called
        val cacheDispatcherField = RequestQueue::class.java.getDeclaredField("mCacheDispatcher")
        cacheDispatcherField.isAccessible = true
        val volleyCacheDispatcher = cacheDispatcherField.get(volleyRequestQueue) as? CacheDispatcher
        assertNull(volleyCacheDispatcher)
    }

    @Test
    fun getImageLoader_shouldReturnCachedImageLoader() {
        Networking.setImageLoaderForTesting(mockImageLoader)

        Assert.assertEquals(
            mockImageLoader,
            Networking.getImageLoader(context)
        )
        verifyStatic(never())
        Networking.getRequestQueue(context)
    }

    @Test
    fun getImageLoader_shouldInitializeImageLoader() {
        Networking.getImageLoader(context)

        verifyStatic()
        Networking.getRequestQueue(context.applicationContext)
    }
}
