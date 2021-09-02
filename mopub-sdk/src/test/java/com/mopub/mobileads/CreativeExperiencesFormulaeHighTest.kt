// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads

import com.mopub.common.test.support.SdkTestRunner

import org.json.JSONObject
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(SdkTestRunner::class)
class CreativeExperiencesFormulaeHighTest {

    companion object {
        // Non-rewarded
        const val CE_SETTINGS_HIGH_NONREWARDED_PLAYABLE_STRING =
            """
                {
                    "hash": "12345",
                    "main_ad": {
                        "min_next_action_secs": 0,
                        "cd_delay_secs": 5,
                        "show_cd": 1
                    },
                    "end_card": {
                       "cd_delay_secs": 0,
                       "show_cd": 1
                    },
                    "max_ad_time_secs": 5,
                    "ec_durs_secs": {
                        "static": 0,
                        "interactive": 0,
                        "min_static": 5,
                        "min_interactive": 5
                    },
                    "video_skip_thresholds_secs": [
                        {
                            "min": 15,
                            "after": 5
                        }
                    ]
                }
            """

        const val CE_SETTINGS_HIGH_NONREWARDED_VAST_STRING =
            """
                {
                    "hash": "12345",
                    "main_ad": {
                        "min_next_action_secs": 0,
                        "cd_delay_secs": 5,
                        "show_cd": 0
                    },
                    "end_card": {
                       "cd_delay_secs": 0,
                       "show_cd": 1
                    },
                    "max_ad_time_secs": 5,
                    "ec_durs_secs": {
                        "static": 0,
                        "interactive": 0,
                        "min_static": 5,
                        "min_interactive": 5
                    },
                    "video_skip_thresholds_secs": [
                        {
                            "min": 15,
                            "after": 5
                        }
                    ]
                }
            """

        const val CE_SETTINGS_HIGH_NONREWARDED_DISPLAY_STRING =
            """
                {
                    "hash": "12345",
                    "main_ad": {
                        "min_next_action_secs": 0,
                        "cd_delay_secs": 0,
                        "show_cd": 1
                    },
                    "end_card": {
                       "cd_delay_secs": 0,
                       "show_cd": 1
                    },
                    "max_ad_time_secs": 5,
                    "ec_durs_secs": {
                        "static": 0,
                        "interactive": 0,
                        "min_static": 5,
                        "min_interactive": 5
                    },
                    "video_skip_thresholds_secs": [
                        {
                            "min": 15,
                            "after": 5
                        }
                    ]
                }
            """

        // Rewarded
        const val CE_SETTINGS_HIGH_REWARDED_DISPLAY_STRING =
            """
                {
                    "hash": "12345",
                    "main_ad": {
                        "min_next_action_secs": 30,
                        "cd_delay_secs": 2,
                        "show_cd": 1
                    },
                    "end_card": {
                       "cd_delay_secs": 5,
                       "show_cd": 1
                    },
                    "max_ad_time_secs": 30,
                    "ec_durs_secs": {
                        "static": 5,
                        "interactive": 5,
                        "min_static": 5,
                        "min_interactive": 5
                    },
                    "video_skip_thresholds_secs": [
                        {
                            "min": 0,
                            "after": 30
                        }
                    ]
                }
            """

        const val CE_SETTINGS_HIGH_REWARDED_PLAYABLE_STRING =
            """
                {
                    "hash": "12345",
                    "main_ad": {
                        "min_next_action_secs": 30,
                        "cd_delay_secs": 5,
                        "show_cd": 1
                    },
                    "end_card": {
                       "cd_delay_secs": 5,
                       "show_cd": 1
                    },
                    "max_ad_time_secs": 30,
                    "ec_durs_secs": {
                        "static": 5,
                        "interactive": 5,
                        "min_static": 5,
                        "min_interactive": 5
                    },
                    "video_skip_thresholds_secs": [
                        {
                            "min": 0,
                            "after": 30
                        }
                    ]
                }
            """

        const val CE_SETTINGS_HIGH_REWARDED_VAST_STRING =
            """
                {
                    "hash": "12345",
                    "main_ad": {
                        "min_next_action_secs": 30,
                        "cd_delay_secs": 7.5,
                        "show_cd": 1
                    },
                    "end_card": {
                       "cd_delay_secs": 5,
                       "show_cd": 1
                    },
                    "max_ad_time_secs": 30,
                    "ec_durs_secs": {
                        "static": 5,
                        "interactive": 5,
                        "min_static": 5,
                        "min_interactive": 5
                    },
                    "video_skip_thresholds_secs": [
                        {
                            "min": 0,
                            "after": 30
                        }
                    ]
                }
            """
    }

    //region HIGH CE Settings

    //region Non-rewarded 30s VAST - null (no) end card

    @Test
    fun `for high CE Settings for non-rewarded 30 second VAST with null end card`() {
        val ceSettingsJSONObject = JSONObject(CE_SETTINGS_HIGH_NONREWARDED_VAST_STRING)
        val ceSettings = CreativeExperienceSettingsParser.parse(ceSettingsJSONObject, false)

        val mainAdTimeUntilNextActionSecs = CreativeExperiencesFormulae.getTimeUntilNextActionSecs(
            isVast = true,
            isEndCard = false,
            endCardType = null,
            30,
            ceSettings)

        Assert.assertEquals(5, mainAdTimeUntilNextActionSecs)

        val mainAdCountdownDuration = CreativeExperiencesFormulae.getCountdownDuration(
            isVast = true,
            isEndCard = false,
            endCardType = null,
            30,
            elapsedTimeInAdSecs = 0, // beginning of the main ad
            ceSettings)

        Assert.assertEquals(5, mainAdCountdownDuration)

        val closeAfterSecs = CreativeExperiencesFormulae.getCloseAfterSecs(
            isVast = true,
            isEndCard = false,
            endCardType = null,
            30,
            ceSettings)

        Assert.assertEquals(5, closeAfterSecs)

        val endCardCountdownDuration = CreativeExperiencesFormulae.getCountdownDuration(
            isVast = false,
            isEndCard = true,
            endCardType = null,
            30,
            elapsedTimeInAdSecs = mainAdCountdownDuration, // user skips/closes at the first chance
            ceSettings)

        Assert.assertEquals(0, endCardCountdownDuration)

        val endCardTimeUntilNextActionSecs = CreativeExperiencesFormulae.getTimeUntilNextActionSecs(
            isVast = false, // end card is not VAST
            isEndCard = true,
            endCardType = null,
            30,
            ceSettings)

        Assert.assertEquals(0, endCardTimeUntilNextActionSecs)
    }

    //endregion Non-rewarded 30s VAST - null (no) end card

    //region Non-rewarded 30s VAST - "none" end card

    @Test
    fun `for high CE Settings for non-rewarded 30 second VAST with end card of type NONE`() {
        val ceSettingsJSONObject = JSONObject(CE_SETTINGS_HIGH_NONREWARDED_VAST_STRING)
        val ceSettings = CreativeExperienceSettingsParser.parse(ceSettingsJSONObject, false)

        val mainAdTimeUntilNextActionSecs = CreativeExperiencesFormulae.getTimeUntilNextActionSecs(
            isVast = true,
            isEndCard = false,
            endCardType = EndCardType.NONE,
            30,
            ceSettings)

        Assert.assertEquals(5, mainAdTimeUntilNextActionSecs)

        val mainAdCountdownDuration = CreativeExperiencesFormulae.getCountdownDuration(
            isVast = true,
            isEndCard = false,
            endCardType = EndCardType.NONE,
            30,
            elapsedTimeInAdSecs = 0, // beginning of the main ad
            ceSettings)

        Assert.assertEquals(5, mainAdCountdownDuration)

        val closeAfterSecs = CreativeExperiencesFormulae.getCloseAfterSecs(
            isVast = true,
            isEndCard = false,
            endCardType = EndCardType.NONE,
            30,
            ceSettings)

        Assert.assertEquals(5, closeAfterSecs)

        val endCardCountdownDuration = CreativeExperiencesFormulae.getCountdownDuration(
            isVast = false,
            isEndCard = true,
            endCardType = EndCardType.NONE,
            30,
            elapsedTimeInAdSecs = mainAdCountdownDuration, // user skips/closes at the first chance
            ceSettings)

        Assert.assertEquals(0, endCardCountdownDuration)

        val endCardTimeUntilNextActionSecs = CreativeExperiencesFormulae.getTimeUntilNextActionSecs(
            isVast = false, // end card is not VAST
            isEndCard = true,
            endCardType = EndCardType.NONE,
            30,
            ceSettings)

        Assert.assertEquals(0, endCardTimeUntilNextActionSecs)
    }

    //endregion Non-rewarded 30s VAST - "none" end card

    //region Non-rewarded 30s VAST - static end card

    @Test
    fun `for high CE Settings for non-rewarded 30 second VAST with end card of type STATIC`() {
        val ceSettingsJSONObject = JSONObject(CE_SETTINGS_HIGH_NONREWARDED_VAST_STRING)
        val ceSettings = CreativeExperienceSettingsParser.parse(ceSettingsJSONObject, false)

        val mainAdTimeUntilNextActionSecs = CreativeExperiencesFormulae.getTimeUntilNextActionSecs(
            isVast = true,
            isEndCard = false,
            endCardType = EndCardType.STATIC,
            30,
            ceSettings)

        Assert.assertEquals(5, mainAdTimeUntilNextActionSecs)

        val mainAdCountdownDuration = CreativeExperiencesFormulae.getCountdownDuration(
            isVast = true,
            isEndCard = false,
            endCardType = EndCardType.STATIC,
            30,
            elapsedTimeInAdSecs = 0, // beginning of the main ad
            ceSettings)

        Assert.assertEquals(5, mainAdCountdownDuration)

        val closeAfterSecs = CreativeExperiencesFormulae.getCloseAfterSecs(
            isVast = true,
            isEndCard = false,
            endCardType = EndCardType.STATIC,
            30,
            ceSettings)

        Assert.assertEquals(5, closeAfterSecs)

        val endCardCountdownDuration = CreativeExperiencesFormulae.getCountdownDuration(
            isVast = false,
            isEndCard = true,
            endCardType = EndCardType.STATIC,
            30,
            elapsedTimeInAdSecs = mainAdCountdownDuration, // user skips/closes at the first chance
            ceSettings)

        Assert.assertEquals(5, endCardCountdownDuration)

        val endCardTimeUntilNextActionSecs = CreativeExperiencesFormulae.getTimeUntilNextActionSecs(
            isVast = false, // end card is not VAST
            isEndCard = true,
            endCardType = EndCardType.STATIC,
            30,
            ceSettings)

        Assert.assertEquals(5, endCardTimeUntilNextActionSecs)
    }

    //endregion Non-rewarded 30s VAST - static end card

    //region Non-rewarded 30s VAST - interactive end card

    @Test
    fun `for high CE Settings for non-rewarded 30 second VAST with end card of type INTERACTIVE`() {
        val ceSettingsJSONObject = JSONObject(CE_SETTINGS_HIGH_NONREWARDED_VAST_STRING)
        val ceSettings = CreativeExperienceSettingsParser.parse(ceSettingsJSONObject, false)

        val mainAdTimeUntilNextActionSecs = CreativeExperiencesFormulae.getTimeUntilNextActionSecs(
            isVast = true,
            isEndCard = false,
            endCardType = EndCardType.INTERACTIVE,
            30,
            ceSettings)

        Assert.assertEquals(5, mainAdTimeUntilNextActionSecs)

        val mainAdCountdownDuration = CreativeExperiencesFormulae.getCountdownDuration(
            isVast = true,
            isEndCard = false,
            endCardType = EndCardType.INTERACTIVE,
            30,
            elapsedTimeInAdSecs = 0, // beginning of the main ad
            ceSettings)

        Assert.assertEquals(5, mainAdCountdownDuration)

        val closeAfterSecs = CreativeExperiencesFormulae.getCloseAfterSecs(
            isVast = true,
            isEndCard = false,
            endCardType = EndCardType.INTERACTIVE,
            30,
            ceSettings)

        Assert.assertEquals(5, closeAfterSecs)

        val endCardCountdownDuration = CreativeExperiencesFormulae.getCountdownDuration(
            isVast = false,
            isEndCard = true,
            endCardType = EndCardType.INTERACTIVE,
            30,
            elapsedTimeInAdSecs = mainAdCountdownDuration, // user skips/closes at the first chance
            ceSettings)

        Assert.assertEquals(5, endCardCountdownDuration)

        val endCardTimeUntilNextActionSecs = CreativeExperiencesFormulae.getTimeUntilNextActionSecs(
            isVast = false, // end card is not VAST
            isEndCard = true,
            endCardType = EndCardType.INTERACTIVE,
            30,
            ceSettings)

        Assert.assertEquals(5, endCardTimeUntilNextActionSecs)
    }

    //endregion Non-rewarded 30s VAST - interactive end card

    //region Non-rewarded 15s VAST - null (no) end card

    @Test
    fun `for high CE Settings for non-rewarded 15 second VAST with null end card`() {
        val ceSettingsJSONObject = JSONObject(CE_SETTINGS_HIGH_NONREWARDED_VAST_STRING)
        val ceSettings = CreativeExperienceSettingsParser.parse(ceSettingsJSONObject, false)

        val mainAdTimeUntilNextActionSecs = CreativeExperiencesFormulae.getTimeUntilNextActionSecs(
            isVast = true,
            isEndCard = false,
            endCardType = null,
            15,
            ceSettings)

        Assert.assertEquals(5, mainAdTimeUntilNextActionSecs)

        val mainAdCountdownDuration = CreativeExperiencesFormulae.getCountdownDuration(
            isVast = true,
            isEndCard = false,
            endCardType = null,
            15,
            elapsedTimeInAdSecs = 0, // beginning of the main ad
            ceSettings)

        Assert.assertEquals(5, mainAdCountdownDuration)

        val closeAfterSecs = CreativeExperiencesFormulae.getCloseAfterSecs(
            isVast = true,
            isEndCard = false,
            endCardType = null,
            15,
            ceSettings)

        Assert.assertEquals(5, closeAfterSecs)

        val endCardCountdownDuration = CreativeExperiencesFormulae.getCountdownDuration(
            isVast = false,
            isEndCard = true,
            endCardType = null,
            15,
            elapsedTimeInAdSecs = mainAdCountdownDuration, // user skips/closes at the first chance
            ceSettings)

        Assert.assertEquals(0, endCardCountdownDuration)

        val endCardTimeUntilNextActionSecs = CreativeExperiencesFormulae.getTimeUntilNextActionSecs(
            isVast = false, // end card is not VAST
            isEndCard = true,
            endCardType = null,
            15,
            ceSettings)

        Assert.assertEquals(0, endCardTimeUntilNextActionSecs)
    }

    //endregion Non-rewarded 15s VAST - null (no) end card

    //region Non-rewarded 15s VAST - "none" end card

    @Test
    fun `for high CE Settings for non-rewarded 15 second VAST with end card of type NONE`() {
        val ceSettingsJSONObject = JSONObject(CE_SETTINGS_HIGH_NONREWARDED_VAST_STRING)
        val ceSettings = CreativeExperienceSettingsParser.parse(ceSettingsJSONObject, false)

        val mainAdTimeUntilNextActionSecs = CreativeExperiencesFormulae.getTimeUntilNextActionSecs(
            isVast = true,
            isEndCard = false,
            endCardType = EndCardType.NONE,
            15,
            ceSettings)

        Assert.assertEquals(5, mainAdTimeUntilNextActionSecs)

        val mainAdCountdownDuration = CreativeExperiencesFormulae.getCountdownDuration(
            isVast = true,
            isEndCard = false,
            endCardType = EndCardType.NONE,
            15,
            elapsedTimeInAdSecs = 0, // beginning of the main ad
            ceSettings)

        Assert.assertEquals(5, mainAdCountdownDuration)

        val closeAfterSecs = CreativeExperiencesFormulae.getCloseAfterSecs(
            isVast = true,
            isEndCard = false,
            endCardType = EndCardType.NONE,
            15,
            ceSettings)

        Assert.assertEquals(5, closeAfterSecs)

        val endCardCountdownDuration = CreativeExperiencesFormulae.getCountdownDuration(
            isVast = false,
            isEndCard = true,
            endCardType = EndCardType.NONE,
            15,
            elapsedTimeInAdSecs = mainAdCountdownDuration, // user skips/closes at the first chance
            ceSettings)

        Assert.assertEquals(0, endCardCountdownDuration)

        val endCardTimeUntilNextActionSecs = CreativeExperiencesFormulae.getTimeUntilNextActionSecs(
            isVast = false, // end card is not VAST
            isEndCard = true,
            endCardType = EndCardType.NONE,
            15,
            ceSettings)

        Assert.assertEquals(0, endCardTimeUntilNextActionSecs)
    }

    //endregion Non-rewarded 15s VAST - "none" end card

    //region Non-rewarded 15s VAST - static end card

    @Test
    fun `for high CE Settings for non-rewarded 15 second VAST with end card of type STATIC`() {
        val ceSettingsJSONObject = JSONObject(CE_SETTINGS_HIGH_NONREWARDED_VAST_STRING)
        val ceSettings = CreativeExperienceSettingsParser.parse(ceSettingsJSONObject, false)

        val mainAdTimeUntilNextActionSecs = CreativeExperiencesFormulae.getTimeUntilNextActionSecs(
            isVast = true,
            isEndCard = false,
            endCardType = EndCardType.STATIC,
            15,
            ceSettings)

        Assert.assertEquals(5, mainAdTimeUntilNextActionSecs)

        val mainAdCountdownDuration = CreativeExperiencesFormulae.getCountdownDuration(
            isVast = true,
            isEndCard = false,
            endCardType = EndCardType.STATIC,
            15,
            elapsedTimeInAdSecs = 0, // beginning of the main ad
            ceSettings)

        Assert.assertEquals(5, mainAdCountdownDuration)

        val closeAfterSecs = CreativeExperiencesFormulae.getCloseAfterSecs(
            isVast = true,
            isEndCard = false,
            endCardType = EndCardType.STATIC,
            15,
            ceSettings)

        Assert.assertEquals(5, closeAfterSecs)

        val endCardCountdownDuration = CreativeExperiencesFormulae.getCountdownDuration(
            isVast = false,
            isEndCard = true,
            endCardType = EndCardType.STATIC,
            15,
            elapsedTimeInAdSecs = mainAdCountdownDuration, // user skips/closes at the first chance
            ceSettings)

        Assert.assertEquals(5, endCardCountdownDuration)

        val endCardTimeUntilNextActionSecs = CreativeExperiencesFormulae.getTimeUntilNextActionSecs(
            isVast = false, // end card is not VAST
            isEndCard = true,
            endCardType = EndCardType.STATIC,
            15,
            ceSettings)

        Assert.assertEquals(5, endCardTimeUntilNextActionSecs)
    }

    //endregion Non-rewarded 15s VAST - static end card

    //region Non-rewarded 15s VAST - interactive end card

    @Test
    fun `for high CE Settings for non-rewarded 15 second VAST with end card of type INTERACTIVE`() {
        val ceSettingsJSONObject = JSONObject(CE_SETTINGS_HIGH_NONREWARDED_VAST_STRING)
        val ceSettings = CreativeExperienceSettingsParser.parse(ceSettingsJSONObject, false)

        val mainAdTimeUntilNextActionSecs = CreativeExperiencesFormulae.getTimeUntilNextActionSecs(
            isVast = true,
            isEndCard = false,
            endCardType = EndCardType.INTERACTIVE,
            15,
            ceSettings)

        Assert.assertEquals(5, mainAdTimeUntilNextActionSecs)

        val mainAdCountdownDuration = CreativeExperiencesFormulae.getCountdownDuration(
            isVast = true,
            isEndCard = false,
            endCardType = EndCardType.INTERACTIVE,
            15,
            elapsedTimeInAdSecs = 0, // beginning of the main ad
            ceSettings)

        Assert.assertEquals(5, mainAdCountdownDuration)

        val closeAfterSecs = CreativeExperiencesFormulae.getCloseAfterSecs(
            isVast = true,
            isEndCard = false,
            endCardType = EndCardType.INTERACTIVE,
            15,
            ceSettings)

        Assert.assertEquals(5, closeAfterSecs)

        val endCardCountdownDuration = CreativeExperiencesFormulae.getCountdownDuration(
            isVast = false,
            isEndCard = true,
            endCardType = EndCardType.INTERACTIVE,
            15,
            elapsedTimeInAdSecs = mainAdCountdownDuration, // user skips/closes at the first chance
            ceSettings)

        Assert.assertEquals(5, endCardCountdownDuration)

        val endCardTimeUntilNextActionSecs = CreativeExperiencesFormulae.getTimeUntilNextActionSecs(
            isVast = false, // end card is not VAST
            isEndCard = true,
            endCardType = EndCardType.INTERACTIVE,
            15,
            ceSettings)

        Assert.assertEquals(5, endCardTimeUntilNextActionSecs)
    }

    //endregion Non-rewarded 15s VAST - interactive end card

    //region Non-rewarded Playable - null (no) end card

    @Test
    fun `for high CE Settings for non-rewarded playable with null end card`() {
        val ceSettingsJSONObject = JSONObject(CE_SETTINGS_HIGH_NONREWARDED_PLAYABLE_STRING)
        val ceSettings = CreativeExperienceSettingsParser.parse(ceSettingsJSONObject, false)

        val mainAdTimeUntilNextActionSecs = CreativeExperiencesFormulae.getTimeUntilNextActionSecs(
            isVast = false,
            isEndCard = false,
            endCardType = null,
            0,
            ceSettings)

        Assert.assertEquals(0, mainAdTimeUntilNextActionSecs)

        val mainAdCountdownDuration = CreativeExperiencesFormulae.getCountdownDuration(
            isVast = false,
            isEndCard = false,
            endCardType = null,
            0,
            elapsedTimeInAdSecs = 0, // beginning of the main ad
            ceSettings)

        Assert.assertEquals(5, mainAdCountdownDuration)

        val closeAfterSecs = CreativeExperiencesFormulae.getCloseAfterSecs(
            isVast = false,
            isEndCard = false,
            endCardType = null,
            0,
            ceSettings)

        Assert.assertEquals(5, closeAfterSecs)

        val endCardCountdownDuration = CreativeExperiencesFormulae.getCountdownDuration(
            isVast = false,
            isEndCard = true,
            endCardType = null,
            0,
            elapsedTimeInAdSecs = mainAdCountdownDuration, // user skips/closes at the first chance
            ceSettings)

        Assert.assertEquals(0, endCardCountdownDuration)

        val endCardTimeUntilNextActionSecs = CreativeExperiencesFormulae.getTimeUntilNextActionSecs(
            isVast = false, // end card is not VAST
            isEndCard = true,
            endCardType = null,
            0,
            ceSettings)

        Assert.assertEquals(0, endCardTimeUntilNextActionSecs)
    }

    //endregion Non-rewarded Playable - null (no) end card

    //region Non-rewarded Display - null (no) end card

    @Test
    fun `for high CE Settings for non-rewarded display with null end card`() {
        val ceSettingsJSONObject = JSONObject(CE_SETTINGS_HIGH_NONREWARDED_DISPLAY_STRING)
        val ceSettings = CreativeExperienceSettingsParser.parse(ceSettingsJSONObject, false)

        val mainAdTimeUntilNextActionSecs = CreativeExperiencesFormulae.getTimeUntilNextActionSecs(
            isVast = false,
            isEndCard = false,
            endCardType = null,
            0,
            ceSettings)

        Assert.assertEquals(0, mainAdTimeUntilNextActionSecs)

        val mainAdCountdownDuration = CreativeExperiencesFormulae.getCountdownDuration(
            isVast = false,
            isEndCard = false,
            endCardType = null,
            0,
            elapsedTimeInAdSecs = 0, // beginning of the main ad
            ceSettings)

        Assert.assertEquals(5, mainAdCountdownDuration)

        val closeAfterSecs = CreativeExperiencesFormulae.getCloseAfterSecs(
            isVast = false,
            isEndCard = false,
            endCardType = null,
            0,
            ceSettings)

        Assert.assertEquals(5, closeAfterSecs)

        val endCardCountdownDuration = CreativeExperiencesFormulae.getCountdownDuration(
            isVast = false,
            isEndCard = true,
            endCardType = null,
            0,
            elapsedTimeInAdSecs = mainAdCountdownDuration, // user skips/closes at the first chance
            ceSettings)

        Assert.assertEquals(0, endCardCountdownDuration)

        val endCardTimeUntilNextActionSecs = CreativeExperiencesFormulae.getTimeUntilNextActionSecs(
            isVast = false, // end card is not VAST
            isEndCard = true,
            endCardType = null,
            0,
            ceSettings)

        Assert.assertEquals(0, endCardTimeUntilNextActionSecs)
    }

    //endregion Non-rewarded Display - null (no) end card

    //region Rewarded Display - null (no) end card

    @Test
    fun `for high CE Settings for rewarded display with null end card`() {
        val ceSettingsJSONObject = JSONObject(CE_SETTINGS_HIGH_REWARDED_DISPLAY_STRING)
        val ceSettings = CreativeExperienceSettingsParser.parse(ceSettingsJSONObject, true)

        val mainAdTimeUntilNextActionSecs = CreativeExperiencesFormulae.getTimeUntilNextActionSecs(
            isVast = false,
            isEndCard = false,
            endCardType = null,
            0,
            ceSettings)

        Assert.assertEquals(30, mainAdTimeUntilNextActionSecs)

        val mainAdCountdownDuration = CreativeExperiencesFormulae.getCountdownDuration(
            isVast = false,
            isEndCard = false,
            endCardType = null,
            0,
            elapsedTimeInAdSecs = 0, // beginning of the main ad
            ceSettings)

        Assert.assertEquals(30, mainAdCountdownDuration)

        val closeAfterSecs = CreativeExperiencesFormulae.getCloseAfterSecs(
            isVast = false,
            isEndCard = false,
            endCardType = null,
            0,
            ceSettings)

        Assert.assertEquals(30, closeAfterSecs)

        val endCardCountdownDuration = CreativeExperiencesFormulae.getCountdownDuration(
            isVast = false,
            isEndCard = true,
            endCardType = null,
            0,
            elapsedTimeInAdSecs = mainAdCountdownDuration, // user skips/closes at the first chance
            ceSettings)

        Assert.assertEquals(0, endCardCountdownDuration)

        val endCardTimeUntilNextActionSecs = CreativeExperiencesFormulae.getTimeUntilNextActionSecs(
            isVast = false, // end card is not VAST
            isEndCard = true,
            endCardType = null,
            0,
            ceSettings)

        Assert.assertEquals(0, endCardTimeUntilNextActionSecs)
    }

    //endregion Rewarded Display - null (no) end card

    //region Rewarded 30s VAST - null (no) end card

    @Test
    fun `for high CE Settings for rewarded 30 second VAST with null end card`() {
        val ceSettingsJSONObject = JSONObject(CE_SETTINGS_HIGH_REWARDED_VAST_STRING)
        val ceSettings = CreativeExperienceSettingsParser.parse(ceSettingsJSONObject, true)

        val mainAdTimeUntilNextActionSecs = CreativeExperiencesFormulae.getTimeUntilNextActionSecs(
            isVast = true,
            isEndCard = false,
            endCardType = null,
            30,
            ceSettings)

        Assert.assertEquals(30, mainAdTimeUntilNextActionSecs)

        val mainAdCountdownDuration = CreativeExperiencesFormulae.getCountdownDuration(
            isVast = true,
            isEndCard = false,
            endCardType = null,
            30,
            elapsedTimeInAdSecs = 0, // beginning of the main ad
            ceSettings)

        Assert.assertEquals(30, mainAdCountdownDuration)

        val closeAfterSecs = CreativeExperiencesFormulae.getCloseAfterSecs(
            isVast = true,
            isEndCard = false,
            endCardType = null,
            30,
            ceSettings)

        Assert.assertEquals(30, closeAfterSecs)

        val endCardCountdownDuration = CreativeExperiencesFormulae.getCountdownDuration(
            isVast = false,
            isEndCard = true,
            endCardType = null,
            30,
            elapsedTimeInAdSecs = mainAdCountdownDuration, // user skips/closes at the first chance
            ceSettings)

        Assert.assertEquals(0, endCardCountdownDuration)

        val endCardTimeUntilNextActionSecs = CreativeExperiencesFormulae.getTimeUntilNextActionSecs(
            isVast = false, // end card is not VAST
            isEndCard = true,
            endCardType = null,
            30,
            ceSettings)

        Assert.assertEquals(0, endCardTimeUntilNextActionSecs)
    }

    //endregion Rewarded 30s VAST - null (no) end card

    //region Rewarded 30s VAST - "none" end card

    @Test
    fun `for high CE Settings for rewarded 30 second VAST with end card of type NONE`() {
        val ceSettingsJSONObject = JSONObject(CE_SETTINGS_HIGH_REWARDED_VAST_STRING)
        val ceSettings = CreativeExperienceSettingsParser.parse(ceSettingsJSONObject, true)

        val mainAdTimeUntilNextActionSecs = CreativeExperiencesFormulae.getTimeUntilNextActionSecs(
            isVast = true,
            isEndCard = false,
            endCardType = EndCardType.NONE,
            30,
            ceSettings)

        Assert.assertEquals(30, mainAdTimeUntilNextActionSecs)

        val mainAdCountdownDuration = CreativeExperiencesFormulae.getCountdownDuration(
            isVast = true,
            isEndCard = false,
            endCardType = EndCardType.NONE,
            30,
            elapsedTimeInAdSecs = 0, // beginning of the main ad
            ceSettings)

        Assert.assertEquals(30, mainAdCountdownDuration)

        val closeAfterSecs = CreativeExperiencesFormulae.getCloseAfterSecs(
            isVast = true,
            isEndCard = false,
            endCardType = EndCardType.NONE,
            30,
            ceSettings)

        Assert.assertEquals(30, closeAfterSecs)

        val endCardCountdownDuration = CreativeExperiencesFormulae.getCountdownDuration(
            isVast = false,
            isEndCard = true,
            endCardType = EndCardType.NONE,
            30,
            elapsedTimeInAdSecs = mainAdCountdownDuration, // user skips/closes at the first chance
            ceSettings)

        Assert.assertEquals(0, endCardCountdownDuration)

        val endCardTimeUntilNextActionSecs = CreativeExperiencesFormulae.getTimeUntilNextActionSecs(
            isVast = false, // end card is not VAST
            isEndCard = true,
            endCardType = EndCardType.NONE,
            30,
            ceSettings)

        Assert.assertEquals(0, endCardTimeUntilNextActionSecs)
    }

    //endregion Rewarded 30s VAST - "none" end card

    //region Rewarded 30s VAST - static end card

    @Test
    fun `for high CE Settings for rewarded 30 second VAST with end card of type STATIC`() {
        val ceSettingsJSONObject = JSONObject(CE_SETTINGS_HIGH_REWARDED_VAST_STRING)
        val ceSettings = CreativeExperienceSettingsParser.parse(ceSettingsJSONObject, true)

        val mainAdTimeUntilNextActionSecs = CreativeExperiencesFormulae.getTimeUntilNextActionSecs(
            isVast = true,
            isEndCard = false,
            endCardType = EndCardType.STATIC,
            30,
            ceSettings)

        Assert.assertEquals(30, mainAdTimeUntilNextActionSecs)

        val mainAdCountdownDuration = CreativeExperiencesFormulae.getCountdownDuration(
            isVast = true,
            isEndCard = false,
            endCardType = EndCardType.STATIC,
            30,
            elapsedTimeInAdSecs = 0, // beginning of the main ad
            ceSettings)

        Assert.assertEquals(30, mainAdCountdownDuration)

        val closeAfterSecs = CreativeExperiencesFormulae.getCloseAfterSecs(
            isVast = true,
            isEndCard = false,
            endCardType = EndCardType.STATIC,
            30,
            ceSettings)

        Assert.assertEquals(30, closeAfterSecs)

        val endCardCountdownDuration = CreativeExperiencesFormulae.getCountdownDuration(
            isVast = false,
            isEndCard = true,
            endCardType = EndCardType.STATIC,
            30,
            elapsedTimeInAdSecs = mainAdCountdownDuration, // user skips/closes at the first chance
            ceSettings)

        Assert.assertEquals(5, endCardCountdownDuration)

        val endCardTimeUntilNextActionSecs = CreativeExperiencesFormulae.getTimeUntilNextActionSecs(
            isVast = false, // end card is not VAST
            isEndCard = true,
            endCardType = EndCardType.STATIC,
            30,
            ceSettings)

        Assert.assertEquals(5, endCardTimeUntilNextActionSecs)
    }

    //endregion Rewarded 30s VAST - static end card

    //region Rewarded 30s VAST - interactive end card

    @Test
    fun `for high CE Settings for rewarded 30 second VAST with end card of type INTERACTIVE`() {
        val ceSettingsJSONObject = JSONObject(CE_SETTINGS_HIGH_REWARDED_VAST_STRING)
        val ceSettings = CreativeExperienceSettingsParser.parse(ceSettingsJSONObject, true)

        val mainAdTimeUntilNextActionSecs = CreativeExperiencesFormulae.getTimeUntilNextActionSecs(
            isVast = true,
            isEndCard = false,
            endCardType = EndCardType.INTERACTIVE,
            30,
            ceSettings)

        Assert.assertEquals(30, mainAdTimeUntilNextActionSecs)

        val mainAdCountdownDuration = CreativeExperiencesFormulae.getCountdownDuration(
            isVast = true,
            isEndCard = false,
            endCardType = EndCardType.INTERACTIVE,
            30,
            elapsedTimeInAdSecs = 0, // beginning of the main ad
            ceSettings)

        Assert.assertEquals(30, mainAdCountdownDuration)

        val closeAfterSecs = CreativeExperiencesFormulae.getCloseAfterSecs(
            isVast = true,
            isEndCard = false,
            endCardType = EndCardType.INTERACTIVE,
            30,
            ceSettings)

        Assert.assertEquals(30, closeAfterSecs)

        val endCardCountdownDuration = CreativeExperiencesFormulae.getCountdownDuration(
            isVast = false,
            isEndCard = true,
            endCardType = EndCardType.INTERACTIVE,
            30,
            elapsedTimeInAdSecs = mainAdCountdownDuration, // user skips/closes at the first chance
            ceSettings)

        Assert.assertEquals(5, endCardCountdownDuration)

        val endCardTimeUntilNextActionSecs = CreativeExperiencesFormulae.getTimeUntilNextActionSecs(
            isVast = false, // end card is not VAST
            isEndCard = true,
            endCardType = EndCardType.INTERACTIVE,
            30,
            ceSettings)

        Assert.assertEquals(5, endCardTimeUntilNextActionSecs)
    }

    //endregion Rewarded 30s VAST - interactive end card

    //region Rewarded 15s VAST - null (no) end card

    @Test
    fun `for high CE Settings for rewarded 15 second VAST with null end card`() {
        val ceSettingsJSONObject = JSONObject(CE_SETTINGS_HIGH_REWARDED_VAST_STRING)
        val ceSettings = CreativeExperienceSettingsParser.parse(ceSettingsJSONObject, true)

        val mainAdTimeUntilNextActionSecs = CreativeExperiencesFormulae.getTimeUntilNextActionSecs(
            isVast = true,
            isEndCard = false,
            endCardType = null,
            15,
            ceSettings)

        Assert.assertEquals(15, mainAdTimeUntilNextActionSecs)

        val mainAdCountdownDuration = CreativeExperiencesFormulae.getCountdownDuration(
            isVast = true,
            isEndCard = false,
            endCardType = null,
            15,
            elapsedTimeInAdSecs = 0, // beginning of the main ad
            ceSettings)

        Assert.assertEquals(15, mainAdCountdownDuration)

        val closeAfterSecs = CreativeExperiencesFormulae.getCloseAfterSecs(
            isVast = true,
            isEndCard = false,
            endCardType = null,
            15,
            ceSettings)

        Assert.assertEquals(15, closeAfterSecs)

        val endCardCountdownDuration = CreativeExperiencesFormulae.getCountdownDuration(
            isVast = false,
            isEndCard = true,
            endCardType = null,
            15,
            elapsedTimeInAdSecs = mainAdCountdownDuration, // user skips/closes at the first chance
            ceSettings)

        Assert.assertEquals(0, endCardCountdownDuration)

        val endCardTimeUntilNextActionSecs = CreativeExperiencesFormulae.getTimeUntilNextActionSecs(
            isVast = false, // end card is not VAST
            isEndCard = true,
            endCardType = null,
            15,
            ceSettings)

        Assert.assertEquals(0, endCardTimeUntilNextActionSecs)
    }

    //endregion Rewarded 15s VAST - null (no) end card

    //region Rewarded 15s VAST - "none" end card

    @Test
    fun `for high CE Settings for rewarded 15 second VAST with end card of type NONE`() {
        val ceSettingsJSONObject = JSONObject(CE_SETTINGS_HIGH_REWARDED_VAST_STRING)
        val ceSettings = CreativeExperienceSettingsParser.parse(ceSettingsJSONObject, true)

        val mainAdTimeUntilNextActionSecs = CreativeExperiencesFormulae.getTimeUntilNextActionSecs(
            isVast = true,
            isEndCard = false,
            endCardType = EndCardType.NONE,
            15,
            ceSettings)

        Assert.assertEquals(15, mainAdTimeUntilNextActionSecs)

        val mainAdCountdownDuration = CreativeExperiencesFormulae.getCountdownDuration(
            isVast = true,
            isEndCard = false,
            endCardType = EndCardType.NONE,
            15,
            elapsedTimeInAdSecs = 0, // beginning of the main ad
            ceSettings)

        Assert.assertEquals(15, mainAdCountdownDuration)

        val closeAfterSecs = CreativeExperiencesFormulae.getCloseAfterSecs(
            isVast = true,
            isEndCard = false,
            endCardType = EndCardType.NONE,
            15,
            ceSettings)

        Assert.assertEquals(15, closeAfterSecs)

        val endCardCountdownDuration = CreativeExperiencesFormulae.getCountdownDuration(
            isVast = false,
            isEndCard = true,
            endCardType = EndCardType.NONE,
            15,
            elapsedTimeInAdSecs = mainAdCountdownDuration, // user skips/closes at the first chance
            ceSettings)

        Assert.assertEquals(0, endCardCountdownDuration)

        val endCardTimeUntilNextActionSecs = CreativeExperiencesFormulae.getTimeUntilNextActionSecs(
            isVast = false, // end card is not VAST
            isEndCard = true,
            endCardType = EndCardType.NONE,
            15,
            ceSettings)

        Assert.assertEquals(0, endCardTimeUntilNextActionSecs)
    }

    //endregion Rewarded 15s VAST - "none" end card

    //region Rewarded 15s VAST - static end card

    @Test
    fun `for high CE Settings for rewarded 15 second VAST with end card of type STATIC`() {
        val ceSettingsJSONObject = JSONObject(CE_SETTINGS_HIGH_REWARDED_VAST_STRING)
        val ceSettings = CreativeExperienceSettingsParser.parse(ceSettingsJSONObject, true)

        val mainAdTimeUntilNextActionSecs = CreativeExperiencesFormulae.getTimeUntilNextActionSecs(
            isVast = true,
            isEndCard = false,
            endCardType = EndCardType.STATIC,
            15,
            ceSettings)

        Assert.assertEquals(15, mainAdTimeUntilNextActionSecs)

        val mainAdCountdownDuration = CreativeExperiencesFormulae.getCountdownDuration(
            isVast = true,
            isEndCard = false,
            endCardType = EndCardType.STATIC,
            15,
            elapsedTimeInAdSecs = 0, // beginning of the main ad
            ceSettings)

        Assert.assertEquals(15, mainAdCountdownDuration)

        val closeAfterSecs = CreativeExperiencesFormulae.getCloseAfterSecs(
            isVast = true,
            isEndCard = false,
            endCardType = EndCardType.STATIC,
            15,
            ceSettings)

        Assert.assertEquals(20, closeAfterSecs)

        val endCardCountdownDuration = CreativeExperiencesFormulae.getCountdownDuration(
            isVast = false,
            isEndCard = true,
            endCardType = EndCardType.STATIC,
            15,
            elapsedTimeInAdSecs = mainAdCountdownDuration, // user skips/closes at the first chance
            ceSettings)

        Assert.assertEquals(5, endCardCountdownDuration)

        val endCardTimeUntilNextActionSecs = CreativeExperiencesFormulae.getTimeUntilNextActionSecs(
            isVast = false, // end card is not VAST
            isEndCard = true,
            endCardType = EndCardType.STATIC,
            15,
            ceSettings)

        Assert.assertEquals(5, endCardTimeUntilNextActionSecs)
    }

    //endregion Rewarded 15s VAST - static end card

    //region Rewarded 15s VAST - interactive end card

    @Test
    fun `for high CE Settings for rewarded 15 second VAST with end card of type INTERACTIVE`() {
        val ceSettingsJSONObject = JSONObject(CE_SETTINGS_HIGH_REWARDED_VAST_STRING)
        val ceSettings = CreativeExperienceSettingsParser.parse(ceSettingsJSONObject, true)

        val mainAdTimeUntilNextActionSecs = CreativeExperiencesFormulae.getTimeUntilNextActionSecs(
            isVast = true,
            isEndCard = false,
            endCardType = EndCardType.INTERACTIVE,
            15,
            ceSettings)

        Assert.assertEquals(15, mainAdTimeUntilNextActionSecs)

        val mainAdCountdownDuration = CreativeExperiencesFormulae.getCountdownDuration(
            isVast = true,
            isEndCard = false,
            endCardType = EndCardType.INTERACTIVE,
            15,
            elapsedTimeInAdSecs = 0, // beginning of the main ad
            ceSettings)

        Assert.assertEquals(15, mainAdCountdownDuration)

        val closeAfterSecs = CreativeExperiencesFormulae.getCloseAfterSecs(
            isVast = true,
            isEndCard = false,
            endCardType = EndCardType.INTERACTIVE,
            15,
            ceSettings)

        Assert.assertEquals(20, closeAfterSecs)

        val endCardCountdownDuration = CreativeExperiencesFormulae.getCountdownDuration(
            isVast = false,
            isEndCard = true,
            endCardType = EndCardType.INTERACTIVE,
            15,
            elapsedTimeInAdSecs = mainAdCountdownDuration, // user skips/closes at the first chance
            ceSettings)

        Assert.assertEquals(5, endCardCountdownDuration)

        val endCardTimeUntilNextActionSecs = CreativeExperiencesFormulae.getTimeUntilNextActionSecs(
            isVast = false, // end card is not VAST
            isEndCard = true,
            endCardType = EndCardType.INTERACTIVE,
            15,
            ceSettings)

        Assert.assertEquals(5, endCardTimeUntilNextActionSecs)
    }

    //endregion Rewarded 15s VAST - interactive end card

    //region Rewarded Playable - null (no) end card

    @Test
    fun `for high CE Settings for rewarded playable with null end card`() {
        val ceSettingsJSONObject = JSONObject(CE_SETTINGS_HIGH_REWARDED_PLAYABLE_STRING)
        val ceSettings = CreativeExperienceSettingsParser.parse(ceSettingsJSONObject, true)

        val mainAdTimeUntilNextActionSecs = CreativeExperiencesFormulae.getTimeUntilNextActionSecs(
            isVast = false,
            isEndCard = false,
            endCardType = null,
            0,
            ceSettings)

        Assert.assertEquals(30, mainAdTimeUntilNextActionSecs)

        val mainAdCountdownDuration = CreativeExperiencesFormulae.getCountdownDuration(
            isVast = false,
            isEndCard = false,
            endCardType = null,
            0,
            elapsedTimeInAdSecs = 0, // beginning of the main ad
            ceSettings)

        Assert.assertEquals(30, mainAdCountdownDuration)

        val closeAfterSecs = CreativeExperiencesFormulae.getCloseAfterSecs(
            isVast = false,
            isEndCard = false,
            endCardType = null,
            0,
            ceSettings)

        Assert.assertEquals(30, closeAfterSecs)

        val endCardCountdownDuration = CreativeExperiencesFormulae.getCountdownDuration(
            isVast = false,
            isEndCard = true,
            endCardType = null,
            0,
            elapsedTimeInAdSecs = mainAdCountdownDuration, // user skips/closes at the first chance
            ceSettings)

        Assert.assertEquals(0, endCardCountdownDuration)

        val endCardTimeUntilNextActionSecs = CreativeExperiencesFormulae.getTimeUntilNextActionSecs(
            isVast = false, // end card is not VAST
            isEndCard = true,
            endCardType = null,
            0,
            ceSettings)

        Assert.assertEquals(0, endCardTimeUntilNextActionSecs)
    }

    //endregion Rewarded Playable - null (no) end card

    //endregion HIGH CE Settings
}
