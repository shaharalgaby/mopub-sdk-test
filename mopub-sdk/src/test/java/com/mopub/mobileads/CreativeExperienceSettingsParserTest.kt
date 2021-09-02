// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads

import com.mopub.common.Constants
import com.mopub.common.test.support.SdkTestRunner

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(SdkTestRunner::class)
class CreativeExperienceSettingsParserTest {

    companion object {
        const val CE_SETTINGS_STRING =
            """
                {
                    "hash": "12345",
                    "main_ad": {
                        "min_next_action_secs": 10,
                        "cd_delay_secs": 4,
                        "show_cd": 1
                    },
                    "end_card": {
                        "cd_delay_secs": 5,
                        "show_cd": 0
                    },
                    "max_ad_time_secs": 30, 
                    "ec_durs_secs": {
                        "static": 2,
                        "interactive": 30,
                        "min_static": 2,
                        "min_interactive": 25
                    },
                    "video_skip_thresholds_secs": [
                        {
                            "min": 15,
                            "after": 5
                        },
                        {
                            "min": 30,
                            "after": 5
                        }
                    ]
                }
            """

        @JvmStatic
        val ceSettingsJSONObject = JSONObject(CE_SETTINGS_STRING)
    }

    @Test
    fun parse_withNullCreativeExperienceSettings_returnsDefaultSettings() {
        val ceSettingsRewarded = CreativeExperienceSettingsParser.parse(null, true)
        val ceSettingsNonRewarded = CreativeExperienceSettingsParser.parse(null, false)

        assertEquals(
            CreativeExperienceSettings.getDefaultSettings(true),
            ceSettingsRewarded
        )
        assertEquals(
            CreativeExperienceSettings.getDefaultSettings(false),
            ceSettingsNonRewarded
        )
    }

    @Test
    fun parse_withEmptyCreativeExperienceSettings_returnsDefaultSettings() {
        val ceSettingsObject = JSONObject()

        val ceSettingsRewarded = CreativeExperienceSettingsParser.parse(ceSettingsObject, true)
        val ceSettingsNonRewarded = CreativeExperienceSettingsParser.parse(ceSettingsObject, false)

        assertEquals(
            CreativeExperienceSettings.getDefaultSettings(true),
            ceSettingsRewarded
        )
        assertEquals(
            CreativeExperienceSettings.getDefaultSettings(false),
            ceSettingsNonRewarded
        )
    }

    @Test
    fun parse_withNullHash_returnsSettingsWithDefaultHash() {
        val ceSettingsObject = JSONObject(CE_SETTINGS_STRING)
        ceSettingsObject.remove(Constants.CE_SETTINGS_HASH)

        val ceSettings = CreativeExperienceSettingsParser.parse(ceSettingsObject, false)

        assertEquals("0", ceSettings.hash)
    }

    @Test
    fun parse_withNullMaxAdExperienceTime_returnSettingsWithDefaultMaxAdExperienceTime() {
        val ceSettingsObject = JSONObject(CE_SETTINGS_STRING)
        ceSettingsObject.remove(Constants.CE_MAX_AD_TIME)

        val ceSettingsRewarded = CreativeExperienceSettingsParser.parse(ceSettingsObject, true)
        val ceSettingsNonRewarded = CreativeExperienceSettingsParser.parse(ceSettingsObject, false)

        assertEquals(
            CreativeExperienceSettings.getDefaultMaxAdExperienceTimeSecs(true),
            ceSettingsRewarded.maxAdExperienceTimeSecs
        )
        assertEquals(
            CreativeExperienceSettings.getDefaultMaxAdExperienceTimeSecs(false),
            ceSettingsNonRewarded.maxAdExperienceTimeSecs
        )
    }

    @Test
    fun parse_withMaxAdExperienceTimeLessThanZero_returnsSettingsWithDefaultMaxAdExperienceTime() {
        val ceSettingsObject = JSONObject(CE_SETTINGS_STRING)
        ceSettingsObject.remove(Constants.CE_MAX_AD_TIME)
        ceSettingsObject.put(Constants.CE_MAX_AD_TIME, 30)

        val ceSettings = CreativeExperienceSettingsParser.parse(ceSettingsObject, true)

        assertEquals(
            CreativeExperienceSettings.getDefaultMaxAdExperienceTimeSecs(true),
            ceSettings.maxAdExperienceTimeSecs
        )
    }

    @Test
    fun parse_withNullVastSkipThresholdsList_returnsSettingsWithDefaultVastSkipThresholdsList() {
        val ceSettingsObject = JSONObject(CE_SETTINGS_STRING)
        ceSettingsObject.remove(Constants.CE_VIDEO_SKIP_THRESHOLDS)

        val ceSettingsRewarded = CreativeExperienceSettingsParser.parse(ceSettingsObject, true)
        val ceSettingsNonRewarded = CreativeExperienceSettingsParser.parse(ceSettingsObject, false)

        assertEquals(
            listOf(VastSkipThreshold.getDefaultVastSkipThreshold(true)),
            ceSettingsRewarded.vastSkipThresholds
        )
        assertEquals(
            listOf(VastSkipThreshold.getDefaultVastSkipThreshold(false)),
            ceSettingsNonRewarded.vastSkipThresholds
        )
    }

    @Test
    fun parse_withEmptyVastSkipThresholdsList_returnsSettingsWithDefaultVastSkipThresholdsList() {
        val ceSettingsObject = JSONObject(CE_SETTINGS_STRING)
        ceSettingsObject.remove(Constants.CE_VIDEO_SKIP_THRESHOLDS)
        ceSettingsObject.put(Constants.CE_VIDEO_SKIP_THRESHOLDS, JSONArray())

        val ceSettingsRewarded = CreativeExperienceSettingsParser.parse(ceSettingsObject, true)
        val ceSettingsNonRewarded = CreativeExperienceSettingsParser.parse(ceSettingsObject, false)

        assertEquals(
            listOf(VastSkipThreshold.getDefaultVastSkipThreshold(true)),
            ceSettingsRewarded.vastSkipThresholds
        )
        assertEquals(
            listOf(VastSkipThreshold.getDefaultVastSkipThreshold(false)),
            ceSettingsNonRewarded.vastSkipThresholds
        )
    }

    @Test
    fun parse_withInvalidVastSkipThresholdValues_returnsSettingsWithDefaultVastSkipThreshold() {
        val ceSettingsObject = JSONObject(CE_SETTINGS_STRING)
        ceSettingsObject.remove(Constants.CE_VIDEO_SKIP_THRESHOLDS)
        val invalidVastSkipThreshold = JSONObject()
            .put(Constants.CE_SKIP_MIN, -1)
            .put(Constants.CE_SKIP_AFTER, -1)
        ceSettingsObject.put(
            Constants.CE_VIDEO_SKIP_THRESHOLDS,
            JSONArray().put(0, invalidVastSkipThreshold)
        )

        val ceSettingsRewarded = CreativeExperienceSettingsParser.parse(ceSettingsObject, true)
        val ceSettingsNonRewarded = CreativeExperienceSettingsParser.parse(ceSettingsObject, false)

        assertEquals(
            listOf(VastSkipThreshold.getDefaultVastSkipThreshold(true)),
            ceSettingsRewarded.vastSkipThresholds
        )
        assertEquals(
            listOf(VastSkipThreshold.getDefaultVastSkipThreshold(false)),
            ceSettingsNonRewarded.vastSkipThresholds
        )
    }

    @Test
    fun parse_withNullEndCardDurationValues_returnsSettingsWithDefaultEndCardDurations() {
        val ceSettingsObject = JSONObject(CE_SETTINGS_STRING)
        ceSettingsObject.remove(Constants.CE_END_CARD_DURS)

        val ceSettingsRewarded = CreativeExperienceSettingsParser.parse(ceSettingsObject, true)
        val ceSettingsNonRewarded = CreativeExperienceSettingsParser.parse(ceSettingsObject, false)

        assertEquals(
            EndCardDurations.getDefaultEndCardDurations(true),
            ceSettingsRewarded.endCardDurations
        )
        assertEquals(
            EndCardDurations.getDefaultEndCardDurations(false),
            ceSettingsNonRewarded.endCardDurations
        )
    }

    @Test
    fun parse_withEmptyEndCardDurations_returnsSettingsWithDefaultEndCardDurations() {
        val ceSettingsObject = JSONObject(CE_SETTINGS_STRING)
        ceSettingsObject.remove(Constants.CE_END_CARD_DURS)
        ceSettingsObject.put(Constants.CE_END_CARD_DURS, JSONObject())

        val ceSettingsRewarded = CreativeExperienceSettingsParser.parse(ceSettingsObject, true)
        val ceSettingsNonRewarded = CreativeExperienceSettingsParser.parse(ceSettingsObject, false)

        assertEquals(
            EndCardDurations.getDefaultEndCardDurations(true),
            ceSettingsRewarded.endCardDurations
        )
        assertEquals(
            EndCardDurations.getDefaultEndCardDurations(false),
            ceSettingsNonRewarded.endCardDurations
        )
    }

    @Test
    fun parse_withInvalidEndCardDurationValues_returnsSettingsWithDefaultEndCardDurationValues() {
        val ceSettingsObject = JSONObject(CE_SETTINGS_STRING)
        ceSettingsObject.remove(Constants.CE_END_CARD_DURS)
        val invalidEndCardDurations = JSONObject()
            .put(Constants.CE_STATIC, -1)
            .put(Constants.CE_INTERACTIVE, -1)
            .put(Constants.CE_MIN_STATIC, -1)
            .put(Constants.CE_MIN_INTERACTIVE, -1)
        ceSettingsObject.put(
            Constants.CE_VIDEO_SKIP_THRESHOLDS,
            JSONArray().put(0, invalidEndCardDurations)
        )

        val ceSettingsRewarded = CreativeExperienceSettingsParser.parse(ceSettingsObject, true)
        val ceSettingsNonRewarded = CreativeExperienceSettingsParser.parse(ceSettingsObject, false)

        assertEquals(
            EndCardDurations.getDefaultEndCardDurations(true),
            ceSettingsRewarded.endCardDurations
        )
        assertEquals(
            EndCardDurations.getDefaultEndCardDurations(false),
            ceSettingsNonRewarded.endCardDurations
        )
    }

    @Test
    fun parse_withNullAdConfigs_returnsSettingsWithDefaultAdConfigs() {
        val ceSettingsObject = JSONObject(CE_SETTINGS_STRING)
        ceSettingsObject.remove(Constants.CE_MAIN_AD)
        ceSettingsObject.remove(Constants.CE_END_CARD)

        val ceSettingsRewarded = CreativeExperienceSettingsParser.parse(ceSettingsObject, true)
        val ceSettingsNonRewarded = CreativeExperienceSettingsParser.parse(ceSettingsObject, false)

        ceSettingsRewarded.assertDefaultAdConfigs(true)
        ceSettingsNonRewarded.assertDefaultAdConfigs(false)
    }

    @Test
    fun parse_withEmptyAdConfigs_returnsSettingsWithDefaultAdConfigs() {
        val ceSettingsObject = JSONObject(CE_SETTINGS_STRING)
        ceSettingsObject.remove(Constants.CE_MAIN_AD)
        ceSettingsObject.remove(Constants.CE_END_CARD)
        ceSettingsObject.put(Constants.CE_MAIN_AD, JSONObject())
        ceSettingsObject.put(Constants.CE_END_CARD, JSONObject())

        val ceSettingsRewarded = CreativeExperienceSettingsParser.parse(ceSettingsObject, true)
        val ceSettingsNonRewarded = CreativeExperienceSettingsParser.parse(ceSettingsObject, false)

        ceSettingsRewarded.assertDefaultAdConfigs(true)
        ceSettingsNonRewarded.assertDefaultAdConfigs(false)
    }

    @Test
    fun parse_withInvalidAdConfigValues_returnsSettingsWithDefaultAdConfigValues() {
        val ceSettingsObject = JSONObject(CE_SETTINGS_STRING)
        ceSettingsObject.remove(Constants.CE_MAIN_AD)
        ceSettingsObject.remove(Constants.CE_END_CARD)
        val invalidMainAdConfig = JSONObject()
            .put(Constants.CE_MIN_TIME_UNTIL_NEXT_ACTION, -1)
            .put(Constants.CE_COUNTDOWN_TIMER_DELAY, -1)
            .put(Constants.CE_SHOW_COUNTDOWN_TIMER, -1)
        val invalidEndCardConfig = JSONObject()
            .put(Constants.CE_COUNTDOWN_TIMER_DELAY, -1)
            .put(Constants.CE_SHOW_COUNTDOWN_TIMER, -1)
        ceSettingsObject.put(Constants.CE_MAIN_AD, invalidMainAdConfig)
        ceSettingsObject.put(Constants.CE_END_CARD, invalidEndCardConfig)

        val ceSettingsRewarded = CreativeExperienceSettingsParser.parse(ceSettingsObject, true)
        val ceSettingsNonRewarded = CreativeExperienceSettingsParser.parse(ceSettingsObject, false)

        ceSettingsRewarded.assertDefaultAdConfigs(true)
        ceSettingsNonRewarded.assertDefaultAdConfigs(false)
    }

    private fun CreativeExperienceSettings.assertDefaultAdConfigs(isRewarded: Boolean) {
        assertEquals(
            CreativeExperienceAdConfig.getDefaultCEAdConfig(isRewarded, true),
            this.mainAdConfig
        )

        assertEquals(
            CreativeExperienceAdConfig.getDefaultCEAdConfig(isRewarded, false),
            this.endCardConfig
        )
    }
}
