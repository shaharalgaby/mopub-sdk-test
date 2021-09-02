// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads

import java.io.Serializable

/**
 * This class stores a creative experience video skip threshold.
 *
 * @property skipMinSecs the minimum length in seconds a video must be in order to be skipped after
 * skipAfter seconds.
 * @property skipAfterSecs the time in seconds after which a video of at least skipMin length can be
 * skipped.
 */
data class VastSkipThreshold(
    val skipMinSecs: Int,
    val skipAfterSecs: Int,
) : Serializable {

    companion object {
        /**
         * Default Values
         */
        private const val DEFAULT_SKIP_MIN_REWARDED_SECS = 0
        private const val DEFAULT_SKIP_MIN_NON_REWARDED_SECS = 16
        private const val DEFAULT_SKIP_AFTER_REWARDED_SECS = 30
        private const val DEFAULT_SKIP_AFTER_NON_REWARDED_SECS = 5

        fun getDefaultVastSkipThreshold(isRewarded: Boolean) =
            VastSkipThreshold(
                getDefaultSkipMinSecs(isRewarded),
                getDefaultSkipAfterSecs(isRewarded)
            )

        fun getDefaultSkipMinSecs(isRewarded: Boolean) =
            if (isRewarded) DEFAULT_SKIP_MIN_REWARDED_SECS else DEFAULT_SKIP_MIN_NON_REWARDED_SECS

        fun getDefaultSkipAfterSecs(isRewarded: Boolean) =
            if (isRewarded) DEFAULT_SKIP_AFTER_REWARDED_SECS
            else DEFAULT_SKIP_AFTER_NON_REWARDED_SECS
    }

    override fun toString() = "VastSkipThreshold(" +
            "skipMinSecs=$skipMinSecs, " +
            "skipAfterSecs=$skipAfterSecs)"
}
