// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.network

import com.mopub.test.support.NetworkingTestRunner
import org.fest.assertions.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(NetworkingTestRunner::class)
class MoPubNetworkUtilsTest {

    private lateinit var params: Map<String, String>

    @Before
    fun setUp() {
        params = hashMapOf(
            "query1" to "value1",
            "query2" to "value2,value3,value4",
            "query3" to "",
            "query4" to "value5%20value6"
        )
    }

    @Test
    fun parseCharsetFromContentType_withContentType_shouldReturnCharset() {
        val result = MoPubNetworkUtils.parseCharsetFromContentType(
            mapOf("Content-Type" to "text/html; charset=UTF-8")
        )

        assertThat(result).isEqualTo("UTF-8")
    }

    @Test
    fun parseCharsetFromContentType_withoutContentType_shouldReturnDefaultCharset() {
        val result = MoPubNetworkUtils.parseCharsetFromContentType(mapOf("" to ""))

        assertThat(result).isEqualTo("ISO-8859-1")
    }

    @Test
    fun parseCharsetFromContentType_withoutHeaders_shouldReturnDefaultCharset() {
        val result = MoPubNetworkUtils.parseCharsetFromContentType(null)

        assertThat(result).isEqualTo("ISO-8859-1")
    }

    @Test
    fun convertQueryToMap_withAdRequest_shouldReturnQueryMap() {
        val result = MoPubNetworkUtils.convertQueryToMap(
            "https://ads.mopub.com/m/ad?query1=abc&query2=def%20ghi&query3=jkl&query1=mno&query4&query4&query4#fragment"
        )

        assertThat(result.size).isEqualTo(4)
        assertThat(result["query1"]).isEqualTo("abc,mno")
        assertThat(result["query2"]).isEqualTo("def ghi")
        assertThat(result["query3"]).isEqualTo("jkl")
        assertThat(result["query4"]).isEqualTo(",,")
    }

    @Test
    fun convertQueryToMap_withNoQueryParams_shouldReturnEmptyMap() {
        val result = MoPubNetworkUtils.convertQueryToMap("https://ads.mopub.com/m/ad")

        assertThat(result).isEmpty()
    }

    @Test
    fun convertQueryToMap_withBadUriString_shouldReturnEmptyMap() {
        val result= MoPubNetworkUtils.convertQueryToMap("https://*#&(%*$&")

        assertThat(result).isEmpty()
    }

    @Test
    fun generateBodyFromParams_withParamsMap_shouldGenerateJsonString() {
        val result = MoPubNetworkUtils.generateBodyFromParams(params)

        assertThat(result).contains("\"query1\":\"value1\"")
        assertThat(result).contains("\"query2\":\"value2,value3,value4\"")
        assertThat(result).contains("\"query3\":\"\"")
        // Values have already been decoded and should not be decoded again.
        assertThat(result).contains("\"query4\":\"value5%20value6\"")
        assertThat(result?.length).isEqualTo(90)
    }

    @Test
    fun generateBodyFromParams_withEmptyParamsMap_shouldReturnNull() {
        val result = MoPubNetworkUtils.generateBodyFromParams(emptyMap())

        assertThat(result).isNull()
    }

    @Test
    fun generateBodyFromParams_withoutParamsMap_shouldReturnNull() {
        val result = MoPubNetworkUtils.generateBodyFromParams(null)

        assertThat(result).isNull()
    }
}
