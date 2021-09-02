// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.network

import com.mopub.volley.NoConnectionError
import com.mopub.volley.VolleyError

/**
 * This class represents a network error in the SDK and retains an instance of VolleyError for conversion purposes
 */
data class MoPubNetworkError(
    val reason: Reason?,
    override val message: String?,
    override val cause: Throwable?,
    val networkResponse: MoPubNetworkResponse?,
    val refreshTimeMillis: Int?
) : Exception(message, cause) {

    private val volleyError = InternalVolleyError(reason, message, cause, networkResponse, refreshTimeMillis)

    enum class Reason(val code: Int) {
        WARMING_UP(0),
        NO_FILL(1),
        BAD_HEADER_DATA(2),
        BAD_BODY(3),
        TRACKING_FAILURE(4),
        UNSPECIFIED(5),
        NO_CONNECTION(6),
        TOO_MANY_REQUESTS(7)
    }

    class Builder @JvmOverloads constructor(private val message: String? = null, private val cause: Throwable? = null) {
        private var reason: Reason? = null
        private var networkResponse: MoPubNetworkResponse? = null
        private var refreshTimeMillis: Int? = null

        fun reason(reason: Reason?) = apply { this.reason = reason }
        fun networkResponse(networkResponse: MoPubNetworkResponse?) = apply { this.networkResponse = networkResponse }
        fun refreshTimeMillis(refreshTimeMillis: Int?) = apply { this.refreshTimeMillis = refreshTimeMillis }

        fun build() = MoPubNetworkError(reason, message, cause, networkResponse, refreshTimeMillis)
    }

    internal class InternalVolleyError(val reason: Reason? = null,
                                       override val message: String? = null,
                                       override val cause: Throwable? = null,
                                       val moPubNetworkResponse: MoPubNetworkResponse? = null,
                                       val refreshTimeMillis: Int? = null) : VolleyError(message, cause)

    internal fun getVolleyErrorFromMoPubNetworkError() : VolleyError {
        return volleyError
    }

    companion object {
        internal fun volleyErrorToMoPubNetworkError(volleyError: VolleyError?): MoPubNetworkError {
            // Convert VolleyError to MoPubNetworkError
            // NoConnectionError should have its own Reason
            val reason = when (volleyError) {
                is NoConnectionError -> Reason.NO_CONNECTION
                is InternalVolleyError -> volleyError.reason
                else -> null
            }

            val networkResponse = if (volleyError is InternalVolleyError) {
                volleyError.moPubNetworkResponse
            } else {
                volleyError?.networkResponse?.let {
                    MoPubNetworkResponse(it.statusCode, it.data, it.headers)
                }
            }

            val refreshTimeMillis = if (volleyError is InternalVolleyError) {
                volleyError.refreshTimeMillis
            } else {
                null
            }

            return Builder(volleyError?.message, volleyError?.cause)
                .reason(reason)
                .networkResponse(networkResponse)
                .refreshTimeMillis(refreshTimeMillis)
                .build()
        }
    }
}
