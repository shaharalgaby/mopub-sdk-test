// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.network

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.os.Build
import com.mopub.common.Constants.HOST
import com.mopub.common.Constants.HTTPS

import com.mopub.network.MoPubRequest.Companion.DEFAULT_CONTENT_TYPE
import com.mopub.network.MoPubRequest.VolleyRequest.Companion.getVolleyMethod
import com.mopub.volley.DefaultRetryPolicy
import com.mopub.volley.NetworkResponse
import com.mopub.volley.Request
import com.mopub.volley.VolleyError

import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.anyString
import org.powermock.api.mockito.PowerMockito
import org.powermock.api.mockito.PowerMockito.`when`
import org.powermock.api.mockito.PowerMockito.verifyStatic
import org.powermock.core.classloader.annotations.PowerMockIgnore
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.rule.PowerMockRule
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@PowerMockIgnore("org.mockito.*", "org.robolectric.*",  "android.*", "com.sun.org.apache.xerces.internal.jaxp.*")
@PrepareForTest(MoPubNetworkUtils::class)
class MoPubRequestTest {
    private lateinit var context: Activity
    private lateinit var subject: TestMoPubRequest
    private var responseReceived: String? = null
    private var errorReceived: MoPubNetworkError? = null
    private val url = "$HTTPS://$HOST/m/ad?query1=abc&query2=def&query3=ghi"

    @get:Rule
    val rule = PowerMockRule()

    private var listener = object : MoPubResponse.Listener<String> {
        override fun onResponse(response: String) {
            responseReceived = response
        }

        override fun onErrorResponse(networkError: MoPubNetworkError) {
            errorReceived = networkError
        }
    }

    @Before
    fun setup() {
        context = Robolectric.buildActivity(Activity::class.java).create().get()
        subject = TestMoPubRequest(context, listener, url)

        PowerMockito.mockStatic(MoPubNetworkUtils::class.java)
        `when`(MoPubNetworkUtils.parseCharsetFromContentType(mutableMapOf()))
            .thenReturn("ISO-8859-1")
        `when`(MoPubNetworkUtils.convertQueryToMap(url))
            .thenReturn(mapOf("query1" to "abc", "query2" to "def", "query3" to "ghi"))
        Networking.urlRewriter = object : MoPubUrlRewriter {}
    }

    @Test
    fun setShouldCache_shouldSetVolleyShouldCache() {
        subject.shouldCache = true

        assertTrue(subject.getVolleyRequest().shouldCache())

        subject.shouldCache = false

        assertFalse(subject.getVolleyRequest().shouldCache())
    }

    @Test
    fun setRetryPolicy_shouldSetVolleyRetryPolicy() {
        subject.retryPolicy = MoPubRetryPolicy(1000, 1000, 1.0F)

        val moPubRetryPolicy = subject.retryPolicy
        val volleyRetryPolicy = subject.getVolleyRequest().retryPolicy as DefaultRetryPolicy
        val maxNumRetriesField = volleyRetryPolicy::class.java.getDeclaredField("mMaxNumRetries")
        maxNumRetriesField.isAccessible = true
        val maxNumRetries = maxNumRetriesField.get(volleyRetryPolicy)
        assertEquals(moPubRetryPolicy.initialTimeoutMs, volleyRetryPolicy.currentTimeout)
        assertEquals(moPubRetryPolicy.maxNumRetries, maxNumRetries)
        assertEquals(moPubRetryPolicy.backoffMultiplier, volleyRetryPolicy.backoffMultiplier)
    }

    @Test
    fun volleyParseNetworkResponse_shouldCallMoPubRequestParseNetworkResponse() {
        val parseNetworkResponseMethod = subject.getVolleyRequest().javaClass
            .getDeclaredMethod("parseNetworkResponse", NetworkResponse::class.java)

        val response = parseNetworkResponseMethod.invoke(
            subject.getVolleyRequest(), NetworkResponse(byteArrayOf())
        )

        assertNotNull(response)
    }

    @Test
    fun volleyDeliverResponse_shouldCallMoPubRequestDeliverResponse() {
        val deliverResponseMethod = subject.getVolleyRequest().javaClass
            .getDeclaredMethod("deliverResponse", Object::class.java)

        deliverResponseMethod.invoke(subject.getVolleyRequest(), "success")

        assertEquals("success", responseReceived)
    }

    @Test
    fun volleyGetParams_shouldCallMoPubRequestGetParams() {
        subject.getParamsFromRequest()

        verifyStatic(MoPubNetworkUtils::class.java)
        MoPubNetworkUtils.convertQueryToMap(anyString())
    }

    @Test
    fun volleyGetBody_shouldCallMoPubRequestGetBody() {
        subject.getBody()

        verifyStatic(MoPubNetworkUtils::class.java)
        MoPubNetworkUtils.generateBodyFromParams(subject.getParamsFromRequest())
    }

    @Test
    fun volleyGetBodyContentType_shouldCallMoPubRequestGetBodyContentType() {
        val result = subject.getBodyContentTypeFromRequest()

        assertEquals(DEFAULT_CONTENT_TYPE, result)
    }

    @Test
    fun volleyErrorListener_shouldCallMoPubListener() {
        subject.getVolleyRequest().errorListener?.onErrorResponse(
            VolleyError("test error")
        )

        assertNotNull(errorReceived)
        assertEquals("test error", errorReceived?.message)
    }

    @Test
    fun parseStringBody_withBody_shouldParseBody() {
        val body = "{ dialog_html : 'html-body-text' }"
        val testResponse = MoPubNetworkResponse(200, body.toByteArray(), emptyMap())

        val result = subject.parseStringBodyFromRequest(testResponse)

        assertEquals("{ dialog_html : 'html-body-text' }", result)
    }

    @Test
    fun parseStringBody_withUnsupportedCharset_shouldParseWithDefaultCharset() {
        `when`(
            MoPubNetworkUtils.parseCharsetFromContentType(
                mapOf("Content-Type" to "text/html; charset=Unsupported")
            )
        ).thenReturn("Unsupported")
        val testResponse = MoPubNetworkResponse(
            200, "body".toByteArray(), mapOf("Content-Type" to "text/html; charset=Unsupported")
        )

        val result = subject.parseStringBodyFromRequest(testResponse)

        assertEquals("body", result)
    }

    @Test
    fun parseStringBody_withEmptyBody_shouldReturnEmptyString() {
        val testResponse = MoPubNetworkResponse(200, "".toByteArray(), emptyMap())

        val result = subject.parseStringBodyFromRequest(testResponse)

        assertEquals("", result)
    }

    @Test()
    fun parseStringBody_withNullBody_shouldReturnEmptyString() {
        val testResponse = MoPubNetworkResponse(200, null, emptyMap())

        val result = subject.parseStringBodyFromRequest(testResponse)

        assertEquals("", result)
    }

    @Test
    fun cancel_shouldCancelVolleyRequest() {
        subject.cancel()

        assertTrue(subject.isCanceled())
    }

    @Test
    fun setTag_shouldSetVolleyRequestTag() {
        subject.setTag("test tag")

        assertEquals("test tag", subject.getVolleyRequest().tag)
    }

    @Test
    fun getParams_whenUrlHasParams_whenUrlRewriterIsNotNull_shouldReturnParamMap() {
        val result = subject.getParamsFromRequest()

        verifyStatic(MoPubNetworkUtils::class.java)
        MoPubNetworkUtils.convertQueryToMap(url)
        assertEquals(
            mapOf("query1" to "abc", "query2" to "def", "query3" to "ghi"),
            result
        )
    }

    @Test
    fun getParams_whenUrlDoesNotHaveParams_whenUrlRewriterIsNotNull_shouldReturnEmptyMap() {
        assertNotNull(Networking.urlRewriter)
        subject = TestMoPubRequest(context, listener, "https://ads.mopub.com/m/ad")

        assertTrue(subject.getParamsFromRequest()!!.isEmpty())
    }

    @Test
    fun getParams_whenUrlRewriterIsNull_shouldReturnNull() {
        Networking.urlRewriter = null

        assertNull(subject.getParamsFromRequest())
    }

    @Test
    fun getBody_withParams_shouldReturnBody() {
        `when`(
            MoPubNetworkUtils.generateBodyFromParams(
                mapOf("query1" to "abc", "query2" to "def", "query3" to "ghi")
            )
        ).thenReturn("test")

        val result = subject.getBody()

        assertTrue(result.contentEquals("test".toByteArray()))
    }

    @Test
    fun getBody_withEmptyParams_shouldReturnNull() {
        `when`(MoPubNetworkUtils.generateBodyFromParams(emptyMap()))
            .thenReturn(null)
        val request = TestMoPubRequest(context, listener, "https://ads.mopub.com/m/ad")

        val result = request.getBody()

        assertNull(result)
    }

    @Test
    fun getBody_withNullParams_shouldReturnNull() {
        `when`(MoPubNetworkUtils.generateBodyFromParams(null))
            .thenReturn(null)
        val request = TestMoPubRequest(context, listener, "https://ads.mopub.com/m/ad")

        val result = request.getBody()

        assertNull(result)
    }

    @Test
    fun getVolleyMethod_withGet_shouldReturnGet() {
        val method = getVolleyMethod(MoPubRequest.Method.GET)

        assertEquals(Request.Method.GET, method)
    }

    @Test
    fun getVolleyMethod_withPost_shouldReturnPost() {
        val method = getVolleyMethod(MoPubRequest.Method.POST)

        assertEquals(Request.Method.POST, method)
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Config(sdk = [Build.VERSION_CODES.LOLLIPOP], qualifiers = "zh")
    @Test
    fun getHeaders_withApi23OrBelow_withLocaleSet_shouldReturnLocale() {
        val result = subject.getHeaders()

        assertEquals(mapOf("accept-language" to "zh"), result)
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Config(sdk = [Build.VERSION_CODES.LOLLIPOP], qualifiers = "")
    @Test
    fun getHeaders_withApi23OrBelow_withEmptyLocale_shouldReturnDefaultLocale() {
        val result = subject.getHeaders()

        assertEquals(mapOf("accept-language" to "en-us"), result)
    }

    @TargetApi(Build.VERSION_CODES.N)
    @Config(sdk = [Build.VERSION_CODES.N], qualifiers = "zh")
    @Test
    fun getHeaders_withApi24OrAbove__withLocaleSet_shouldReturnLocale() {
        val result = subject.getHeaders()

        assertEquals(mapOf("accept-language" to "zh"), result)
    }

    @TargetApi(Build.VERSION_CODES.N)
    @Config(sdk = [Build.VERSION_CODES.N], qualifiers = (""))
    @Test
    fun getHeaders_withApi24OrAbove_withEmptyLocale_shouldReturnDefaultLocale() {
        val result = subject.getHeaders()

        assertEquals(mapOf("accept-language" to "en-us"), result)
    }

    class TestMoPubRequest(
        context: Context,
        listener: MoPubResponse.Listener<String>,
        url: String
    ) : MoPubRequest<String>(
        context,
        url,
        "https://ads.mopub.com/m/ad",
        Method.POST,
        listener
    ) {
        override fun deliverResponse(response: String) {
            moPubListener?.onResponse(response)
        }

        override fun parseNetworkResponse(networkResponse: MoPubNetworkResponse?): MoPubResponse<String>? {
            return networkResponse?.let {
                MoPubResponse.success("test", MoPubNetworkResponse(it.statusCode, it.data, it.headers))
            }
        }

        // the wrappers below are needed to test protected methods
        fun getParamsFromRequest() = getParams()

        fun getBodyContentTypeFromRequest() = getBodyContentType()

        fun parseStringBodyFromRequest(response: MoPubNetworkResponse) = parseStringBody(response)
    }
}
