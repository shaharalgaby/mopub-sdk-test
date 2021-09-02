// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.network

/**
 * Data class with default values to hold values for a Retry Policy
 */
data class MoPubRetryPolicy constructor(val initialTimeoutMs: Int = DEFAULT_TIMEOUT_MS,
                                        val maxNumRetries: Int = DEFAULT_MAX_RETRIES,
                                        val backoffMultiplier: Float = DEFAULT_BACKOFF_MULT) {

    constructor() : this(DEFAULT_TIMEOUT_MS, DEFAULT_MAX_RETRIES, DEFAULT_BACKOFF_MULT)

    companion object {
        const val DEFAULT_TIMEOUT_MS = 2500
        const val DEFAULT_MAX_RETRIES = 1
        const val DEFAULT_BACKOFF_MULT = 1.0f
    }
}
