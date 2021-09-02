// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads

import kotlin.math.max
import kotlin.math.min

object CreativeExperiencesFormulae {

    fun getCloseAfterSecs(
        isVast: Boolean,
        isEndCard: Boolean,
        endCardType: EndCardType?,
        videoDurationSecs: Int,
        ceSettings: CreativeExperienceSettings
    ): Int {

        if (!isVast && !isEndCard) {
            return ceSettings.maxAdExperienceTimeSecs
        }

        val endCardDur = when (endCardType) {
            EndCardType.INTERACTIVE -> ceSettings.endCardDurations.interactiveEndCardExperienceDurSecs
            EndCardType.STATIC -> ceSettings.endCardDurations.staticEndCardExperienceDurSecs
            else -> 0
        }

        return min(
            videoDurationSecs + endCardDur,
            ceSettings.maxAdExperienceTimeSecs
        )
    }

    fun getTimeUntilNextActionSecs(
        isVast: Boolean,
        isEndCard: Boolean,
        endCardType: EndCardType?,
        videoDurationSecs: Int,
        ceSettings: CreativeExperienceSettings
    ): Int {
        return when {
            isVast -> ceSettings.vastSkipThresholds.sortedByDescending { threshold ->
                threshold.skipMinSecs
            }.firstOrNull { threshold ->
                videoDurationSecs >= threshold.skipMinSecs
            }?.skipAfterSecs?.let { skipAfterSecs ->
                min(skipAfterSecs, videoDurationSecs)
            } ?: videoDurationSecs

            isEndCard -> when (endCardType) {
                EndCardType.INTERACTIVE -> ceSettings.endCardDurations.minInteractiveEndCardDurSecs
                EndCardType.STATIC -> ceSettings.endCardDurations.minStaticEndCardDurSecs
                else -> 0
            }

            else -> requireNotNull(ceSettings.mainAdConfig.minTimeUntilNextActionSecs) {
                "Min time until next action for a main ad config cannot be null."
            }
        }
    }

    @JvmStatic
    fun getCountdownDuration(
        isVast: Boolean,
        isEndCard: Boolean,
        endCardType: EndCardType?,
        videoDurationSecs: Int,
        elapsedTimeInAdSecs: Int,
        ceSettings: CreativeExperienceSettings
    ): Int {
        val closeAfterSecs = getCloseAfterSecs(
            isVast,
            isEndCard,
            endCardType,
            videoDurationSecs,
            ceSettings
        )
        val timeUntilNextActionSecs = getTimeUntilNextActionSecs(
            isVast,
            isEndCard,
            endCardType,
            videoDurationSecs,
            ceSettings
        )

        // Blurred last frame end card
        if (isEndCard && (endCardType == null || endCardType == EndCardType.NONE)) {
            return 0
        }

        // VAST without end card
        if (isVast && (endCardType == null || endCardType == EndCardType.NONE)) {
            return max(timeUntilNextActionSecs, closeAfterSecs)
        }

        // VAST with end card
        if (isVast) {
            return timeUntilNextActionSecs
        }

        // All other ads (non-VAST and non-blurred last frame end cards)
        val adTimeRemainingSecs = closeAfterSecs - elapsedTimeInAdSecs

        return max(adTimeRemainingSecs, timeUntilNextActionSecs)
    }

}
