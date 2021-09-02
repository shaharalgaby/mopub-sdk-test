// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.network

import android.graphics.Bitmap
import android.widget.ImageView.ScaleType

import com.mopub.common.Mockable
import com.mopub.network.MoPubNetworkError.Companion.volleyErrorToMoPubNetworkError
import com.mopub.volley.VolleyError
import com.mopub.volley.toolbox.ImageLoader

/**
 * Custom implementation of Volley's ImageLoader
 */
@Mockable
class MoPubImageLoader(queue: MoPubRequestQueue?, cache: ImageCache)  {
    private val volleyImageLoader: ImageLoader
    private lateinit var volleyImageListener: ImageLoader.ImageListener

    init {
        val volleyImageCache = object : ImageLoader.ImageCache {
            override fun getBitmap(key: String?): Bitmap? {
                return key?.let { cache.getBitmap(it) }
            }

            override fun putBitmap(key: String?, bitmap: Bitmap?) {
                key?.let {
                    if (bitmap != null) {
                        cache.putBitmap(it, bitmap)
                    }
                }
            }
        }

        volleyImageLoader = object : ImageLoader(queue?.getVolleyRequestQueue(), volleyImageCache) {}
    }

    @JvmOverloads
    fun fetch(
        requestUrl: String?,
        listener: ImageListener,
        maxWidth: Int = 0,
        maxHeight: Int = 0,
        scaleType: ScaleType = ScaleType.CENTER_INSIDE
    ) {
        volleyImageListener = object : ImageLoader.ImageListener {
            override fun onResponse(imageContainer: ImageLoader.ImageContainer, isImmediate: Boolean) {
                // Convert Volley ImageContainer to MoPubImageContainer
                val moPubImageContainer = ImageContainer(imageContainer.bitmap)
                listener.onResponse(moPubImageContainer, isImmediate)
            }

            override fun onErrorResponse(volleyError: VolleyError?) {
                // Convert VolleyError to MoPubNetworkError
                val moPubNetworkError = volleyErrorToMoPubNetworkError(volleyError)
                listener.onErrorResponse(moPubNetworkError)
            }
        }

        volleyImageLoader.get(requestUrl, volleyImageListener, maxWidth, maxHeight, scaleType)
    }

    @Mockable
    data class ImageContainer(val bitmap: Bitmap?)

    interface ImageListener : MoPubResponse.Listener<ImageContainer> {
        fun onResponse(imageContainer: ImageContainer, isImmediate: Boolean)
    }

    interface ImageCache {
        fun getBitmap(key: String): Bitmap?
        fun putBitmap(key: String, bitmap: Bitmap)
    }
}
