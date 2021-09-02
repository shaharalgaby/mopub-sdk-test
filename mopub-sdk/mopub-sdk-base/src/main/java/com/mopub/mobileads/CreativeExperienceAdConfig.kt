// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads

import java.io.Serializable

/**
 * This class stores creative experience settings for an individual ad configuration.
 *
 * @property minTimeUntilNextActionSecs the minimum time in seconds before a user can perform the
 * next action.
 * @property countdownTimerDelaySecs the time in seconds before the countdown timer is shown.
 * @property showCountdownTimer whether or not the countdown timer should be shown.
 */
data class CreativeExperienceAdConfig(
    val minTimeUntilNextActionSecs: Int? = null,
    val countdownTimerDelaySecs: Int,
    val showCountdownTimer: Boolean,
) : Serializable {

    companion object {
        /**
         * Default values
         */
        private const val DEFAULT_MIN_TIME_UNTIL_NEXT_ACTION_REWARDED_SECS: Int = 30
        private const val DEFAULT_MIN_TIME_UNTIL_NEXT_ACTION_NON_REWARDED_SECS: Int = 0
        private const val DEFAULT_COUNTDOWN_TIMER_DELAY_REWARDED_SECS: Int = 0
        private const val DEFAULT_COUNTDOWN_TIMER_DELAY_NON_REWARDED_SECS: Int = 0
        private const val DEFAULT_SHOW_COUNTDOWN_REWARDED: Boolean = true
        private const val DEFAULT_SHOW_COUNTDOWN_NON_REWARDED: Boolean = true

        @JvmStatic
        fun getDefaultCEAdConfig(isRewarded: Boolean, isMainAd: Boolean) =
            CreativeExperienceAdConfig(
                if (isMainAd) getDefaultMinTimeUntilNextActionSecs(isRewarded) else null,
                getDefaultCountdownTimerDelaySecs(isRewarded),
                getDefaultShowCountdownTimer(isRewarded)
            )

        @JvmStatic
        fun getDefaultMinTimeUntilNextActionSecs(isRewarded: Boolean) =
            if (isRewarded) DEFAULT_MIN_TIME_UNTIL_NEXT_ACTION_REWARDED_SECS
            else DEFAULT_MIN_TIME_UNTIL_NEXT_ACTION_NON_REWARDED_SECS

        @JvmStatic
        fun getDefaultCountdownTimerDelaySecs(isRewarded: Boolean) =
            if (isRewarded) DEFAULT_COUNTDOWN_TIMER_DELAY_REWARDED_SECS
            else DEFAULT_COUNTDOWN_TIMER_DELAY_NON_REWARDED_SECS

        @JvmStatic
        fun getDefaultShowCountdownTimer(isRewarded: Boolean): Boolean =
            if (isRewarded) DEFAULT_SHOW_COUNTDOWN_REWARDED
            else DEFAULT_SHOW_COUNTDOWN_NON_REWARDED
    }

    override fun toString() = "CreativeExperienceAdConfig(" +
            "minTimeUntilNextActionSecs=$minTimeUntilNextActionSecs, " +
            "countdownTimerDelaySecs=$countdownTimerDelaySecs, " +
            "showCountdownTimer=$showCountdownTimer)"
}
