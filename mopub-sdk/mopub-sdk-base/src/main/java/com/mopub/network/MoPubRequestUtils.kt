// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.network

import com.mopub.common.Constants

/**
 * Utility methods regarding MoPub specific requests
 */
object MoPubRequestUtils {

    @JvmStatic
    fun truncateQueryParamsIfPost(url: String): String {
        if (!isMoPubRequest(url)) {
            return url
        }

        url.indexOf('?').let {
            return if (it == -1) url else url.substring(0, it)
        }
    }

    @JvmStatic
    fun isMoPubRequest(url: String) : Boolean {
        val httpsHost = Constants.HTTPS + "://" + Constants.HOST
        return url.startsWith(httpsHost)
    }

    @JvmStatic
    fun chooseMethod(url: String) = if (isMoPubRequest(url)) MoPubRequest.Method.POST else MoPubRequest.Method.GET
}
