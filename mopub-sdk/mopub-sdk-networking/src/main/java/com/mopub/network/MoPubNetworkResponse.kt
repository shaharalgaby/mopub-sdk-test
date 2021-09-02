// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.network

import com.mopub.volley.Header
import com.mopub.volley.NetworkResponse

/**
 * This class stores all the necessary network response data needed from Volley's NetworkResponse
 */
class MoPubNetworkResponse(val statusCode: Int, val data: ByteArray?, val headers: Map<String, String>) {

    private val volleyNetworkResponse: NetworkResponse

    init {
        volleyNetworkResponse = NetworkResponse(statusCode, data, false, 0L, toAllHeaderList(headers))
    }

    internal fun getVolleyNetworkResponse() : NetworkResponse {
        return volleyNetworkResponse
    }

    private fun toAllHeaderList(headers: Map<String, String>) : List<Header> {
        return headers.map { Header(it.key, it.value) }
    }
}
