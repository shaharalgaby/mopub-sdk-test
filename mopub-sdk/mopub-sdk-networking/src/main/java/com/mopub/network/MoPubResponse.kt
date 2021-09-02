// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.network

import com.mopub.common.VisibleForTesting
import com.mopub.volley.Cache
import com.mopub.volley.Response
import com.mopub.volley.toolbox.HttpHeaderParser

/**
 * Custom implementation of a Volley Response
 */
class MoPubResponse<T: Any> {
    private var volleyResponse: Response<T>

    @VisibleForTesting
    var moPubResult: T? = null
        private set

    @VisibleForTesting
    var moPubNetworkError: MoPubNetworkError? = null
        private set

    companion object {
        @JvmStatic
        fun <T: Any>success(result: T, response: MoPubNetworkResponse) : MoPubResponse<T> {
            return MoPubResponse(result, HttpHeaderParser.parseCacheHeaders(response.getVolleyNetworkResponse()))
        }

        @JvmStatic
        fun <T: Any>error(networkError: MoPubNetworkError) : MoPubResponse<T> {
            return MoPubResponse(networkError)
        }
    }

    private constructor(networkError: MoPubNetworkError) {
        moPubNetworkError = networkError
        volleyResponse = Response.error(networkError.getVolleyErrorFromMoPubNetworkError())
    }

    private constructor(result: T, cacheEntry: Cache.Entry?) {
        moPubResult = result
        volleyResponse = Response.success(result, cacheEntry)
    }

    internal fun getVolleyResponse() : Response<T> {
        return volleyResponse
    }

    interface Listener<T: Any> {
        @JvmDefault
        fun onErrorResponse(networkError: MoPubNetworkError) {}
        @JvmDefault
        fun onResponse(response: T) {}
    }
}
