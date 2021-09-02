// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.network

import com.mopub.common.Constants
import com.mopub.common.Mockable
import com.mopub.common.util.DeviceUtils
import com.mopub.common.util.ResponseHeader
import com.mopub.volley.AuthFailureError
import com.mopub.volley.Request
import com.mopub.volley.RequestQueue
import com.mopub.volley.toolbox.BasicNetwork
import com.mopub.volley.toolbox.DiskBasedCache
import com.mopub.volley.toolbox.HttpResponse
import com.mopub.volley.toolbox.HurlStack

import java.io.File
import java.io.IOException
import javax.net.ssl.SSLSocketFactory

/**
 * MoPub's custom implementation of the Google Volley RequestQueue.
 * @see [com.mopub.volley.RequestQueue]
 */
@Mockable
class MoPubRequestQueue internal constructor(
    userAgent: String?,
    sslSocketFactory: SSLSocketFactory,
    moPubUrlRewriter: MoPubUrlRewriter,
    volleyCacheDir: File
) {
    private val volleyRequestQueue: RequestQueue

    init {
        val volleyUrlRewriter = HurlStack.UrlRewriter { url -> moPubUrlRewriter.rewriteUrl(url) }

        val volleyHurlStack = object : HurlStack(volleyUrlRewriter, sslSocketFactory) {
            @Throws(IOException::class, AuthFailureError::class)
            override fun executeRequest(request: Request<*>?,
                                        additionalHeaders: MutableMap<String, String?>?): HttpResponse {
                // If the headers map is null or empty, make a new once since Collections.emptyMap()
                // returns an immutable map.
                val newAdditionalHeaders = if (additionalHeaders.isNullOrEmpty()) mutableMapOf() else additionalHeaders

                newAdditionalHeaders[ResponseHeader.USER_AGENT.key] = userAgent
                return super.executeRequest(request, newAdditionalHeaders)
            }
        }

        val network = BasicNetwork(volleyHurlStack)
        val cache = DiskBasedCache(volleyCacheDir,
                DeviceUtils.diskCacheSizeBytes(volleyCacheDir, Constants.TEN_MB.toLong()).toInt())

        volleyRequestQueue = RequestQueue(cache, network)
    }

    fun start() {
        volleyRequestQueue.start()
    }

    fun <T: Any>add(request: MoPubRequest<T>) {
        volleyRequestQueue.add(request.getVolleyRequest())
    }

    /**
     * Convenience method to cancel a single request.
     *
     * @param request The request to cancel.
     */
    fun <T: Any>cancel(request: MoPubRequest<T>) {
        volleyRequestQueue.cancelAll { it === request.getVolleyRequest() }
    }

    fun cancelAll(tag: Any) {
        volleyRequestQueue.cancelAll(tag)
    }

    internal fun getVolleyRequestQueue() : RequestQueue {
        return volleyRequestQueue
    }
}
