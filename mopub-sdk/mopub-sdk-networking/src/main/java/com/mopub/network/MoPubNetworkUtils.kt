// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.network

import android.net.Uri
import android.text.TextUtils

import com.mopub.common.logging.MoPubLog

import org.json.JSONException
import org.json.JSONObject

/**
 * Utility methods for Networking
 */
object MoPubNetworkUtils {

    private const val DEFAULT_CONTENT_CHARSET = "ISO-8859-1"

    @JvmStatic
    fun parseCharsetFromContentType(headers: Map<String, String>?): String {
        val contentType = headers?.get("Content-Type")
        if (contentType != null) {
            val params = contentType.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

            for (i in 1 until params.size) {
                val pair = params[i].trim { it <= ' ' }
                    .split("=".toRegex())
                    .dropLastWhile { it.isEmpty() }
                    .toTypedArray()
                if (pair.size == 2 && pair[0].toLowerCase() == "charset") {
                    return pair[1]
                }
            }
        }

        return DEFAULT_CONTENT_CHARSET
    }

    @JvmStatic
    fun convertQueryToMap(url: String): Map<String, String> {
        val uri = Uri.parse(url)
        return getQueryParamMap(uri)
    }

    @JvmStatic
    fun getQueryParamMap(uri: Uri): Map<String, String> {
        val params = HashMap<String, String>()
        for (queryParam in uri.queryParameterNames) {
            params[queryParam] = TextUtils.join(",", uri.getQueryParameters(queryParam))
        }

        return params
    }

    @JvmStatic
    fun generateBodyFromParams(params: Map<String, String>?): String? {
        if (params == null || params.isEmpty()) {
            return null
        }

        val jsonBody = JSONObject()

        for (queryName in params.keys) {
            try {
                jsonBody.put(queryName, params[queryName])
            } catch (e: JSONException) {
                MoPubLog.log(MoPubLog.SdkLogEvent.CUSTOM, "Unable to add $queryName to JSON body.")
            }
        }

        return jsonBody.toString()
    }
}
