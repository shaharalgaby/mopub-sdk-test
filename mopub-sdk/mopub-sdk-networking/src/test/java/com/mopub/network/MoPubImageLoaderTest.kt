// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.network

import android.graphics.Bitmap

import com.mopub.test.support.NetworkingTestRunner

import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock

@RunWith(NetworkingTestRunner::class)
class MoPubImageLoaderTest {
    @Mock
    private lateinit var mockRequestQueue: MoPubRequestQueue
    private lateinit var imageCache: MoPubImageLoader.ImageCache

    @Before
    fun setup() {
        imageCache = object : MoPubImageLoader.ImageCache {
            override fun getBitmap(key: String) = Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8)
            override fun putBitmap(key: String, bitmap: Bitmap) {}
        }
    }

    @Test
    fun fetch_shouldReceiveOnResponseImageContainer() {
        val imageLoader = MoPubImageLoader(mockRequestQueue, imageCache)
        var containerResponse: MoPubImageLoader.ImageContainer? = null
        val imageListener = object : MoPubImageLoader.ImageListener {
            override fun onResponse(imageContainer: MoPubImageLoader.ImageContainer, isImmediate: Boolean) {
                containerResponse = imageContainer
            }
        }

        try {
            imageLoader.fetch("test", imageListener)
        } catch (e: NullPointerException) {
            // request queue is null due to mock
        }

        assertNotNull(containerResponse)
    }
}
