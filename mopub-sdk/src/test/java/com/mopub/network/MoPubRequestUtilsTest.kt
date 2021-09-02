// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.network

import com.mopub.common.Constants
import com.mopub.common.test.support.SdkTestRunner

import org.fest.assertions.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(SdkTestRunner::class)
class MoPubRequestUtilsTest {

    @Test
    fun chooseMethod_withMoPubUrl_shouldReturnPost() {
        val result = MoPubRequestUtils.chooseMethod("https://" + Constants.HOST)

        assertThat(result).isEqualTo(MoPubRequest.Method.POST)
    }

    @Test
    fun chooseMethod_withNonMoPubUrl_shouldReturnGet() {
        val result = MoPubRequestUtils.chooseMethod("https://www.someurl.com")

        assertThat(result).isEqualTo(MoPubRequest.Method.GET)
    }

    @Test
    fun isMoPubRequest_withHttpsMoPubUrl_shouldReturnTrue() {
        val result = MoPubRequestUtils.isMoPubRequest("https://ads.mopub.com/m/ad")
        assertThat(result).isTrue
    }

    @Test
    fun isMoPubRequest_withHttpMoPubUrl_shouldReturnFalse() {
        val result = MoPubRequestUtils.isMoPubRequest("http://ads.mopub.com/m/imp")

        assertThat(result).isFalse
    }

    @Test
    fun isMoPubRequest_withHttpsNonMoPubUrl_shouldReturnFalse() {
        val result = MoPubRequestUtils.isMoPubRequest("https://www.abcdefg.com/xyz")

        assertThat(result).isFalse
    }

    @Test
    fun isMoPubRequest_withHttpNonMoPubUrl_shouldReturnFalse() {
        val result = MoPubRequestUtils.isMoPubRequest("http://www.notmopub.com/hi")

        assertThat(result).isFalse
    }

    @Test
    fun truncateQueryParamsIfPost_withQueryParams_shouldStripQuery() {
        val result = MoPubRequestUtils.truncateQueryParamsIfPost(
            "https://ads.mopub.com/m/ad?query1=abc&query2=def%20ghi&query3=jkl#fragment"
        )

        assertThat(result).isEqualTo("https://ads.mopub.com/m/ad")
    }

    @Test
    fun truncateQueryParamsIfPost_withNonMoPubUrl_shouldDoNothing() {
        val result = MoPubRequestUtils.truncateQueryParamsIfPost(
            "https://www.notmopub.com/m/ad?query1=abc&query2=def%20ghi&query3=jkl#fragment"
        )

        assertThat(result).isEqualTo(
            "https://www.notmopub.com/m/ad?query1=abc&query2=def%20ghi&query3=jkl#fragment"
        )
    }

    @Test
    fun truncateQueryParamsIfPost_withIntent_shouldDoNothing() {
        val result = MoPubRequestUtils.truncateQueryParamsIfPost("geo:37.777328,-122.416544")

        assertThat(result).isEqualTo("geo:37.777328,-122.416544")
    }
}
