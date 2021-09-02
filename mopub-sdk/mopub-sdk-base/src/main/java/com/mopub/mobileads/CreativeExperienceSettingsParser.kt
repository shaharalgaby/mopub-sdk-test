// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads

import com.mopub.common.Constants

import org.json.JSONObject

object CreativeExperienceSettingsParser {

    @JvmStatic
    fun parse(
        creativeExperienceSettings: JSONObject?,
        isRewarded: Boolean
    ): CreativeExperienceSettings {
        if (creativeExperienceSettings == null) {
            return CreativeExperienceSettings.getDefaultSettings(isRewarded)
        }

        // Current Creative Experience Settings Hash
        val currentCESettingsHash =
            creativeExperienceSettings.optString(Constants.CE_SETTINGS_HASH, "0")

        // Max Ad Experience Time
        val defaultMaxAdExperienceTime =
            CreativeExperienceSettings.getDefaultMaxAdExperienceTimeSecs(isRewarded)
        val maxAdExperienceTime =
            creativeExperienceSettings.optInt(Constants.CE_MAX_AD_TIME, defaultMaxAdExperienceTime)
                .let {
                    if (it < 0) defaultMaxAdExperienceTime else it
                }

        // Video Skip Thresholds
        val vastSkipThresholds: MutableList<VastSkipThreshold> = mutableListOf()
        val vastSkipThresholdsJSONArray =
            creativeExperienceSettings.optJSONArray(Constants.CE_VIDEO_SKIP_THRESHOLDS)
        val defaultSkipMin = VastSkipThreshold.getDefaultSkipMinSecs(isRewarded)
        val defaultSkipAfter = VastSkipThreshold.getDefaultSkipAfterSecs(isRewarded)

        if (vastSkipThresholdsJSONArray != null) {
            for (i in 0 until vastSkipThresholdsJSONArray.length()) {
                val skipThresholdJSONObject = vastSkipThresholdsJSONArray.getJSONObject(i)

                val skipMin =
                    skipThresholdJSONObject.optInt(Constants.CE_SKIP_MIN, defaultSkipMin).let {
                        if (it < 0) defaultSkipMin else it
                    }

                val skipAfter =
                    skipThresholdJSONObject.optInt(Constants.CE_SKIP_AFTER, defaultSkipAfter).let {
                        if (it < 0) defaultSkipAfter else it
                    }

                vastSkipThresholds.add(
                    VastSkipThreshold(skipMin, skipAfter)
                )
            }
        }
        vastSkipThresholds.addIfEmpty(
            VastSkipThreshold.getDefaultVastSkipThreshold(isRewarded)
        )

        // End Card Durations
        val defaultStatic = EndCardDurations.getDefaultStaticEndCardExperienceDurSecs(isRewarded)
        val defaultInteractive = EndCardDurations.getDefaultInteractiveEndCardExperienceDurSecs(isRewarded)
        val defaultMinStatic = EndCardDurations.getDefaultMinStaticEndCardDurSecs(isRewarded)
        val defaultMinInteractive = EndCardDurations.getDefaultMinInteractiveEndCardDurSecs(isRewarded)
        val endCardDurations: EndCardDurations
        val endCardDurationsJSONObject = creativeExperienceSettings.optJSONObject(
            Constants.CE_END_CARD_DURS
        )

        val static =
            endCardDurationsJSONObject?.optInt(Constants.CE_STATIC, defaultStatic)?.let {
                if (it < 0) defaultStatic else it
            } ?: defaultStatic

        val interactive =
            endCardDurationsJSONObject?.optInt(Constants.CE_INTERACTIVE, defaultInteractive)?.let {
                if (it < 0) defaultInteractive else it
            } ?: defaultInteractive

        val minStatic =
            endCardDurationsJSONObject?.optInt(Constants.CE_MIN_STATIC, defaultMinStatic)?.let {
                if (it < 0) defaultMinStatic else it
            } ?: defaultMinStatic

        val minInteractive =
            endCardDurationsJSONObject?.optInt(Constants.CE_MIN_INTERACTIVE, defaultMinInteractive)?.let {
                if (it < 0) defaultMinInteractive else it
            } ?: defaultMinInteractive

        endCardDurations = EndCardDurations(
            static,
            interactive,
            minStatic,
            minInteractive
        )

        // Ad Configs
        val mainAdConfig = creativeExperienceSettings.optJSONObject(
            Constants.CE_MAIN_AD
        )?.let {
            parseAdConfig(it, isRewarded, true)
        } ?: CreativeExperienceAdConfig.getDefaultCEAdConfig(isRewarded, true)

        val endCardConfig = creativeExperienceSettings.optJSONObject(
            Constants.CE_END_CARD
        )?.let {
            parseAdConfig(it, isRewarded, false)
        } ?: CreativeExperienceAdConfig.getDefaultCEAdConfig(isRewarded, false)

        return CreativeExperienceSettings(
            currentCESettingsHash,
            maxAdExperienceTime,
            vastSkipThresholds,
            endCardDurations,
            mainAdConfig,
            endCardConfig
        )
    }

    private fun parseAdConfig(
        adConfigJsonObject: JSONObject,
        isRewarded: Boolean,
        isMainAd: Boolean
    ): CreativeExperienceAdConfig {
        val defaultMinTimeUntilNextAction =
            CreativeExperienceAdConfig.getDefaultMinTimeUntilNextActionSecs(isRewarded)
        val minTimeUntilNextAction =
            adConfigJsonObject.optInt(
                Constants.CE_MIN_TIME_UNTIL_NEXT_ACTION,
                defaultMinTimeUntilNextAction
            ).let {
                if (it < 0) defaultMinTimeUntilNextAction else it
            }

        val defaultCountdownTimerDelay =
            CreativeExperienceAdConfig.getDefaultCountdownTimerDelaySecs(isRewarded)
        val countdownTimerDelay =
            adConfigJsonObject.optInt(
                Constants.CE_COUNTDOWN_TIMER_DELAY,
                defaultCountdownTimerDelay
            ).let {
                if (it < 0) defaultCountdownTimerDelay else it
            }

        val defaultShowCountdownTimer =
            if (CreativeExperienceAdConfig.getDefaultShowCountdownTimer(isRewarded)) 1
            else 0
        val showCountdownTimer = adConfigJsonObject.optInt(
            Constants.CE_SHOW_COUNTDOWN_TIMER,
            defaultShowCountdownTimer
        ).let {
            when (it) {
                1 -> true
                0 -> false
                else -> defaultShowCountdownTimer == 1
            }
        }

        return CreativeExperienceAdConfig(
            if (isMainAd) minTimeUntilNextAction else null,
            countdownTimerDelay,
            showCountdownTimer
        )
    }

    private fun <T> MutableList<T>.addIfEmpty(element: T) {
        if (this.isEmpty()) {
            this.add(element)
        }
    }
}
