// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.simpleadsdemo

import android.net.Uri

object LastAdRequestUrlManager {

    private const val AD_REQUEST_LOG_PREFIX = "Ad requesting from AdServer: "

    private const val AD_REQUEST_ID_QUERY_PARAM = "id"

    private var lastRequest: Pair<String, String>? = null

    fun getAdRequestUrl(adUnitId: String?) = if (lastRequest?.first.equals(adUnitId)) {
        lastRequest?.second
    } else null

    fun isAdRequestLogMessage(message: String) = message.startsWith(AD_REQUEST_LOG_PREFIX)

    fun updateAdRequestUrl(message: String) {
        val url = message.substring(AD_REQUEST_LOG_PREFIX.length).substringBefore('\n')

        if (url.isEmpty()) {
            return
        }

        Uri.parse(url).getQueryParameter(AD_REQUEST_ID_QUERY_PARAM)?.let { adUnitId ->
            if (adUnitId.isNotEmpty()) {
                lastRequest = Pair(adUnitId, url)
            }
        }
    }
}
