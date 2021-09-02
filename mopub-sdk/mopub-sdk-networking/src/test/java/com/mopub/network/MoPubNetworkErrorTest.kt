// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.network

import com.mopub.test.support.NetworkingTestRunner
import com.mopub.volley.NetworkResponse
import com.mopub.volley.NoConnectionError
import com.mopub.volley.VolleyError

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(NetworkingTestRunner::class)
class MoPubNetworkErrorTest {

    @Test
    fun volleyErrorToMoPubNetworkError_withNoConnectionVolleyError_shouldReturnNoConnectionError() {
        val error = NoConnectionError()

        val moPubNetworkError = MoPubNetworkError.volleyErrorToMoPubNetworkError(error)

        assertEquals(MoPubNetworkError.Reason.NO_CONNECTION, moPubNetworkError.reason)
    }

    @Test
    fun volleyErrorToMoPubNetworkError__withInternalVolleyError_withReason_shouldPreserveReason() {
        val error = MoPubNetworkError.InternalVolleyError(MoPubNetworkError.Reason.BAD_BODY)

        val moPubNetworkError = MoPubNetworkError.volleyErrorToMoPubNetworkError(error)

        assertEquals(MoPubNetworkError.Reason.BAD_BODY, moPubNetworkError.reason)
    }

    @Test
    fun volleyErrorToMoPubNetworkError_withNetworkResponse_shouldPreserveResponse() {
        val volleyNetworkResponse = NetworkResponse(byteArrayOf())
        val error = VolleyError(volleyNetworkResponse)

        val moPubNetworkError = MoPubNetworkError.volleyErrorToMoPubNetworkError(error)

        assertNotNull(moPubNetworkError.networkResponse)
        assertEquals(volleyNetworkResponse.statusCode, moPubNetworkError.networkResponse?.statusCode)
        assertEquals(volleyNetworkResponse.data, moPubNetworkError.networkResponse?.data)
        assertEquals(volleyNetworkResponse.headers, moPubNetworkError.networkResponse?.headers)
    }

    @Test
    fun volleyErrorToMoPubNetworkError_withInternalVolleyError_withRefreshTime_shouldPreserveRefreshTime() {
        val error = MoPubNetworkError.InternalVolleyError(refreshTimeMillis = 1000)

        val moPubNetworkError = MoPubNetworkError.volleyErrorToMoPubNetworkError(error)

        assertEquals(1000, moPubNetworkError.refreshTimeMillis)
    }

    @Test
    fun volleyErrorToMoPubNetworkError_withInternalVolleyError__withNullValues_shouldReturnErrorWithNullValues() {
        val error = MoPubNetworkError.InternalVolleyError(null, null, null, null, null)

        val moPubNetworkError = MoPubNetworkError.volleyErrorToMoPubNetworkError(error)

        assertNull(moPubNetworkError.reason)
        assertNull(moPubNetworkError.message)
        assertNull(moPubNetworkError.cause)
        assertNull(moPubNetworkError.networkResponse)
        assertNull(moPubNetworkError.refreshTimeMillis)
    }

    @Test
    fun volleyErrorToMoPubNetworkError_withNullVolleyError_shouldReturnErrorWithNullValues() {
        val moPubNetworkError = MoPubNetworkError.volleyErrorToMoPubNetworkError(null)

        assertNull(moPubNetworkError.reason)
        assertNull(moPubNetworkError.message)
        assertNull(moPubNetworkError.cause)
        assertNull(moPubNetworkError.networkResponse)
        assertNull(moPubNetworkError.refreshTimeMillis)
    }
}
