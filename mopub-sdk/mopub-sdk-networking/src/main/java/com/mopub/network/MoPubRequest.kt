// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.network

import android.content.Context
import android.os.Build
import android.text.TextUtils

import com.mopub.common.util.ResponseHeader
import com.mopub.network.MoPubNetworkError.Companion.volleyErrorToMoPubNetworkError
import com.mopub.volley.*

import java.nio.charset.Charset
import java.nio.charset.UnsupportedCharsetException
import java.util.*

/**
 * Changes the type of request it is based on whether or not the request is going to MoPub's ad
 * server. If the request is for ad server in some way, reconstruct it as a POST request and
 * set the body and content type to json.
 */
abstract class MoPubRequest<T: Any>(val context: Context,
                                    val originalUrl: String,
                                    val truncatedUrl: String,
                                    val method: Method,
                                    val moPubListener: MoPubResponse.Listener<T>?) {

    enum class Method {
        GET,
        POST
    }

    private var volleyRequest : VolleyRequest<T>

    var shouldCache : Boolean = false
        set(value) {
            field = value
            volleyRequest.setShouldCache(value)
        }

    var retryPolicy : MoPubRetryPolicy = MoPubRetryPolicy()
        set(value) {
            field = value
            volleyRequest.retryPolicy = DefaultRetryPolicy(
                value.initialTimeoutMs,
                value.maxNumRetries,
                value.backoffMultiplier
            )
        }

    companion object {
        const val JSON_CONTENT_TYPE = "application/json; charset=UTF-8"
        const val DEFAULT_CONTENT_TYPE = "application/x-www-form-urlencoded; charset=UTF-8"
    }

    init {

        val volleyErrorListener = Response.ErrorListener { volleyError ->
            val moPubNetworkError = volleyErrorToMoPubNetworkError(volleyError)
            moPubListener?.onErrorResponse(moPubNetworkError)
        }

        volleyRequest = object : VolleyRequest<T>(context, method, truncatedUrl, volleyErrorListener) {
            override fun parseNetworkResponse(networkResponse: NetworkResponse?): Response<T>? {
                // convert Volley NetworkResponse to MoPubNetworkResponse
                var moPubNetworkResponse: MoPubNetworkResponse? = null
                networkResponse?.let {
                    moPubNetworkResponse = MoPubNetworkResponse(it.statusCode, it.data, it.headers)
                }
                return this@MoPubRequest.parseNetworkResponse(moPubNetworkResponse)?.getVolleyResponse()
            }

            override fun deliverResponse(response: T) {
                return this@MoPubRequest.deliverResponse(response)
            }

            override fun getParams(): Map<String, String>? {
                return this@MoPubRequest.getParams()
            }

            override fun getBody(): ByteArray? {
                return this@MoPubRequest.getBody()
            }

            override fun getBodyContentType(): String {
                return this@MoPubRequest.getBodyContentType()
            }
        }
    }

    protected fun parseStringBody(response: MoPubNetworkResponse): String {
        val dataBytes = response.data ?: byteArrayOf()
        return try {
            String(dataBytes, Charset.forName(MoPubNetworkUtils.parseCharsetFromContentType(response.headers)))
        } catch (e: UnsupportedCharsetException) {
            String(dataBytes)
        }
    }

    fun cancel() {
        volleyRequest.cancel()
    }

    fun isCanceled() = volleyRequest.isCanceled

    fun setTag(tag: String) {
        volleyRequest.tag = tag
    }

    open fun getUrl(): String {
        return volleyRequest.url
    }

    fun getHeaders() : MutableMap<String, String> = volleyRequest.headers

    internal fun getVolleyRequest() : VolleyRequest<T> {
        return volleyRequest
    }

    protected open fun getParams() : Map<String, String>? {
        // Checks for isMoPubRequest are moved out of the networking module
        return Networking.urlRewriter?.let {
            MoPubNetworkUtils.convertQueryToMap(it.rewriteUrl(originalUrl))
        }
    }

    open fun getBody() : ByteArray? {
        // Checks for isMoPubRequest are moved out of the networking module
        val body = MoPubNetworkUtils.generateBodyFromParams(getParams()) ?: return null
        return body.toByteArray()
    }

    protected open fun getBodyContentType() : String {
        // Checks for isMoPubRequest are moved out of the networking module
        return DEFAULT_CONTENT_TYPE
    }

    protected abstract fun deliverResponse(response: T)

    protected abstract fun parseNetworkResponse(networkResponse: MoPubNetworkResponse?) : MoPubResponse<T>?

    internal abstract class VolleyRequest<T: Any>(
        val context: Context,
        method: MoPubRequest.Method,
        url: String,
        listener: Response.ErrorListener?
    ) : Request<T>(getVolleyMethod(method), url, listener) {

        companion object {
            @JvmStatic
            fun getVolleyMethod(method: MoPubRequest.Method) : Int  {
                return when (method) {
                    MoPubRequest.Method.GET -> Method.GET
                    MoPubRequest.Method.POST -> Method.POST
                }
            }
        }

        override fun getHeaders(): MutableMap<String, String> {
            val headers = TreeMap<String, String>()

            var userLocale: Locale? = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val list = context.resources.configuration.locales
                if (list.size() > 0) {
                    userLocale = list.get(0)
                }
            } else {
                userLocale = context.resources.configuration.locale
            }

            val language: String
            val country: String
            if (userLocale != null && !TextUtils.isEmpty(userLocale.toString().trim())) {
                // If user's preferred locale is available
                language = userLocale.language.trim()
                country = userLocale.country.trim()
            } else {
                // Use default locale
                language = Locale.getDefault().language.trim()
                country = Locale.getDefault().country.trim()
            }

            var languageCode: String
            if (!TextUtils.isEmpty(language)) {
                languageCode = language
                if (!TextUtils.isEmpty(country)) {
                    languageCode += "-" + country.toLowerCase()
                }
                headers[ResponseHeader.ACCEPT_LANGUAGE.key] = languageCode
            }

            return headers
        }
    }
}
