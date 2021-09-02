// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.network

import com.mopub.test.support.NetworkingTestRunner
import com.mopub.volley.Header
import com.mopub.volley.NetworkResponse

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(NetworkingTestRunner::class)
class MoPubNetworkResponseTest {

    @Test
    fun init_withVolleyResponse_shouldSetStatusCode_shouldSetData_shouldCopyHeaderKeysAndValues() {
        val response = MoPubNetworkResponse(
            200,
            byteArrayOf(0x2E, 0X38),
            mapOf(
                "header1" to "value1",
                "header2" to "value2",
            )
        )

        val volleyNetworkResponseField = response::class.java.getDeclaredField("volleyNetworkResponse")
        volleyNetworkResponseField.isAccessible = true
        val volleyNetworkResponse = volleyNetworkResponseField.get(response) as NetworkResponse
        assertEquals(response.statusCode, volleyNetworkResponse.statusCode)
        assertEquals(response.data, volleyNetworkResponse.data)
        assertEquals(response.headers, volleyNetworkResponse.headers)
        val allHeaders = response.headers.map { Header(it.key, it.value) }
        assertEquals(allHeaders.size, volleyNetworkResponse.allHeaders.size)
        assertEquals(allHeaders[0].name, volleyNetworkResponse.allHeaders[0].name)
        assertEquals(allHeaders[0].value, volleyNetworkResponse.allHeaders[0].value)
        assertEquals(allHeaders[1].name, volleyNetworkResponse.allHeaders[1].name)
        assertEquals(allHeaders[1].value, volleyNetworkResponse.allHeaders[1].value)
    }
}
