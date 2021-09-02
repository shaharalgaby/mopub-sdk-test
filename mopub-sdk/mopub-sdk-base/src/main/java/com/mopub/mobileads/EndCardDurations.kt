// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads

import java.io.Serializable

/**
 * This data class stores creative experience end card durations.
 *
 * @property staticEndCardExperienceDurSecs the duration in seconds for a static end card.
 * @property interactiveEndCardExperienceDurSecs the duration in seconds for an interactive
 * (playable) end card.
 * @property minStaticEndCardDurSecs the minimum time in seconds a user will be locked into a static
 * end card experience.
 * @property minInteractiveEndCardDurSecs the minimum time in seconds a user will be locked into an
 * interactive end card experience.
 */
data class EndCardDurations(
    val staticEndCardExperienceDurSecs: Int,
    val interactiveEndCardExperienceDurSecs: Int,
    val minStaticEndCardDurSecs: Int,
    val minInteractiveEndCardDurSecs: Int,
) : Serializable {

    companion object {
        /**
         * Default values
         */
        private const val DEFAULT_STATIC_EC_EXPERIENCE_DUR_REWARDED_SECS: Int = 5
        private const val DEFAULT_STATIC_EC_EXPERIENCE_DUR_NON_REWARDED_SECS: Int = 0
        private const val DEFAULT_INTERACTIVE_EC_EXPERIENCE_DUR_REWARDED_SECS: Int = 10
        private const val DEFAULT_INTERACTIVE_EC_EXPERIENCE_DUR_NON_REWARDED_SECS: Int = 0
        private const val DEFAULT_MIN_STATIC_EC_DUR_REWARDED_SECS: Int = 0
        private const val DEFAULT_MIN_STATIC_EC_DUR_NON_REWARDED_SECS: Int = 0
        private const val DEFAULT_MIN_INTERACTIVE_EC_DUR_REWARDED_SECS: Int = 0
        private const val DEFAULT_MIN_INTERACTIVE_EC_DUR_NON_REWARDED_SECS: Int = 0

        @JvmStatic
        fun getDefaultEndCardDurations(isRewarded: Boolean) =
            EndCardDurations(
                getDefaultStaticEndCardExperienceDurSecs(isRewarded),
                getDefaultInteractiveEndCardExperienceDurSecs(isRewarded),
                getDefaultMinStaticEndCardDurSecs(isRewarded),
                getDefaultMinInteractiveEndCardDurSecs(isRewarded),
            )

        @JvmStatic
        fun getDefaultStaticEndCardExperienceDurSecs(isRewarded: Boolean) =
            if (isRewarded) DEFAULT_STATIC_EC_EXPERIENCE_DUR_REWARDED_SECS
            else DEFAULT_STATIC_EC_EXPERIENCE_DUR_NON_REWARDED_SECS

        @JvmStatic
        fun getDefaultInteractiveEndCardExperienceDurSecs(isRewarded: Boolean) =
            if (isRewarded) DEFAULT_INTERACTIVE_EC_EXPERIENCE_DUR_REWARDED_SECS
            else DEFAULT_INTERACTIVE_EC_EXPERIENCE_DUR_NON_REWARDED_SECS

        @JvmStatic
        fun getDefaultMinStaticEndCardDurSecs(isRewarded: Boolean) =
            if (isRewarded) DEFAULT_MIN_STATIC_EC_DUR_REWARDED_SECS
            else DEFAULT_MIN_STATIC_EC_DUR_NON_REWARDED_SECS

        @JvmStatic
        fun getDefaultMinInteractiveEndCardDurSecs(isRewarded: Boolean) =
            if (isRewarded) DEFAULT_MIN_INTERACTIVE_EC_DUR_REWARDED_SECS
            else DEFAULT_MIN_INTERACTIVE_EC_DUR_NON_REWARDED_SECS
    }

    override fun toString() = "EndCardDurations(" +
            "staticEndCardExperienceDurSecs=$staticEndCardExperienceDurSecs, " +
            "interactiveEndCardExperienceDurSecs=$interactiveEndCardExperienceDurSecs, " +
            "minStaticEndCardDurSecs=$minStaticEndCardDurSecs, " +
            "minInteractiveEndCardDurSecs=$minInteractiveEndCardDurSecs)"
}
