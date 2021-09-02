// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads

import com.mopub.common.test.support.SdkTestRunner

import org.json.JSONObject
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PowerMockIgnore
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.rule.PowerMockRule

import kotlin.math.min

@RunWith(SdkTestRunner::class)
@PowerMockIgnore("org.mockito.*", "org.robolectric.*", "android.*", "org.json.*")
@PrepareForTest(CreativeExperiencesFormulae::class)
class CreativeExperiencesFormulaeBaseTest {

    @get:Rule
    var rule = PowerMockRule()

    companion object {
        // skeleton config
        const val CE_SETTINGS_SKELETON_STRING =
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
                    "max_ad_time_secs": 0,
                    "ec_durs_secs": {
                        "static": 0,
                        "interactive": 0,
                        "min_static": 0,
                        "min_interactive": 0
                    },
                    "video_skip_thresholds_secs": []
                }
            """
    }

    //region Base unit tests

    //region getCloseAfterSecs
    @Test
    fun `getCloseAfterSecs for end card should return minimum of maxAdExperienceTime and (videoDuration + endCardDuration)`() {
        val isVast = false
        val isEndCard = true
        val endCardType: EndCardType = EndCardType.STATIC
        val videoDurationSecs = 15
        val maxAdExperienceTime = videoDurationSecs - 1

        val ceSettingsJSONObject = JSONObject(CE_SETTINGS_SKELETON_STRING)

        ceSettingsJSONObject.put("max_ad_time_secs", maxAdExperienceTime)
        val ceSettings = CreativeExperienceSettingsParser.parse(ceSettingsJSONObject, false)

        val closeAfterSecs = CreativeExperiencesFormulae.getCloseAfterSecs(
            isVast,
            isEndCard,
            endCardType,
            videoDurationSecs,
            ceSettings
        )

        val endCardDuration = ceSettings.endCardDurations.staticEndCardExperienceDurSecs
        val expectedCloseAfterSecs = min(maxAdExperienceTime, videoDurationSecs + endCardDuration)
        Assert.assertEquals(expectedCloseAfterSecs, closeAfterSecs)
    }

    @Test
    fun `getCloseAfterSecs for VAST with null end card and maxAdExperienceTime less than videoDurationSecs should return maxAdExperienceTime`() {
        val isVast = true
        val isEndCard = false
        val endCardType: EndCardType? = null
        val videoDurationSecs = 15
        val maxAdExperienceTime = videoDurationSecs - 1

        val ceSettingsJSONObject = JSONObject(CE_SETTINGS_SKELETON_STRING)

        ceSettingsJSONObject.put("max_ad_time_secs", maxAdExperienceTime)
        val ceSettings = CreativeExperienceSettingsParser.parse(ceSettingsJSONObject, false)

        val closeAfterSecs = CreativeExperiencesFormulae.getCloseAfterSecs(
            isVast,
            isEndCard,
            endCardType,
            videoDurationSecs,
            ceSettings
        )

        Assert.assertEquals(maxAdExperienceTime, closeAfterSecs)
    }

    @Test
    fun `getCloseAfterSecs for VAST with null end card and maxAdExperienceTime greater than videoDurationSecs should return videoDurationSecs`() {
        val isVast = true
        val isEndCard = false
        val endCardType: EndCardType? = null
        val videoDurationSecs = 15
        val maxAdExperienceTime = videoDurationSecs + 1

        val ceSettingsJSONObject = JSONObject(CE_SETTINGS_SKELETON_STRING)

        ceSettingsJSONObject.put("max_ad_time_secs", maxAdExperienceTime)
        val ceSettings = CreativeExperienceSettingsParser.parse(ceSettingsJSONObject, false)

        val closeAfterSecs = CreativeExperiencesFormulae.getCloseAfterSecs(
            isVast,
            isEndCard,
            endCardType,
            videoDurationSecs,
            ceSettings
        )

        Assert.assertEquals(videoDurationSecs, closeAfterSecs)
    }

    @Test
    fun `getCloseAfterSecs for VAST with NONE end card and maxAdExperienceTime less than videoDurationSecs should return maxAdExperienceTime`() {
        val isVast = true
        val isEndCard = false
        val endCardType: EndCardType = EndCardType.NONE
        val videoDurationSecs = 15
        val maxAdExperienceTime = videoDurationSecs - 1

        val ceSettingsJSONObject = JSONObject(CE_SETTINGS_SKELETON_STRING)

        ceSettingsJSONObject.put("max_ad_time_secs", maxAdExperienceTime)
        val ceSettings = CreativeExperienceSettingsParser.parse(ceSettingsJSONObject, false)

        val closeAfterSecs = CreativeExperiencesFormulae.getCloseAfterSecs(
            isVast,
            isEndCard,
            endCardType,
            videoDurationSecs,
            ceSettings
        )

        Assert.assertEquals(maxAdExperienceTime, closeAfterSecs)
    }

    @Test
    fun `getCloseAfterSecs for VAST with NONE end card and maxAdExperienceTime greater than videoDurationSecs should return videoDurationSecs`() {
        val isVast = true
        val isEndCard = false
        val endCardType: EndCardType = EndCardType.NONE
        val videoDurationSecs = 15
        val maxAdExperienceTime = videoDurationSecs + 1

        val ceSettingsJSONObject = JSONObject(CE_SETTINGS_SKELETON_STRING)

        ceSettingsJSONObject.put("max_ad_time_secs", maxAdExperienceTime)
        val ceSettings = CreativeExperienceSettingsParser.parse(ceSettingsJSONObject, false)

        val closeAfterSecs = CreativeExperiencesFormulae.getCloseAfterSecs(
            isVast,
            isEndCard,
            endCardType,
            videoDurationSecs,
            ceSettings
        )

        Assert.assertEquals(videoDurationSecs, closeAfterSecs)
    }

    @Test
    fun `getCloseAfterSecs for VAST with STATIC end card and maxAdExperienceTime less than (videoDurationSecs + static end card dur) should return maxAdExperienceTime`() {
        val isVast = true
        val isEndCard = false
        val endCardType = EndCardType.STATIC
        val videoDurationSecs = 15
        val expectedStaticEndCardDuration = 1
        val maxAdExperienceTime = (videoDurationSecs + expectedStaticEndCardDuration) - 1

        val ceSettingsJSONObject = JSONObject(CE_SETTINGS_SKELETON_STRING)

        // modify the config before parsing
        ceSettingsJSONObject.put("max_ad_time_secs", maxAdExperienceTime)
        val ecDursJSONObject = ceSettingsJSONObject.getJSONObject("ec_durs_secs")
        ecDursJSONObject.put("static", expectedStaticEndCardDuration)
        ceSettingsJSONObject.put("ec_durs_secs", ecDursJSONObject)

        val ceSettings = CreativeExperienceSettingsParser.parse(ceSettingsJSONObject, false)

        val closeAfterSecs = CreativeExperiencesFormulae.getCloseAfterSecs(
            isVast,
            isEndCard,
            endCardType,
            videoDurationSecs,
            ceSettings
        )

        Assert.assertEquals(maxAdExperienceTime, closeAfterSecs)
    }

    @Test
    fun `getCloseAfterSecs for VAST with STATIC end card and maxAdExperienceTime greater than (videoDurationSecs + static end card dur) should return (videoDurationSecs + expectedStaticEndCardDuration)`() {
        val isVast = true
        val isEndCard = false
        val endCardType = EndCardType.STATIC
        val videoDurationSecs = 15
        val expectedStaticEndCardDuration = 1
        val maxAdExperienceTime = (videoDurationSecs + expectedStaticEndCardDuration) + 1

        val ceSettingsJSONObject = JSONObject(CE_SETTINGS_SKELETON_STRING)

        // modify the config before parsing
        ceSettingsJSONObject.put("max_ad_time_secs", maxAdExperienceTime)
        val ecDursJSONObject = ceSettingsJSONObject.getJSONObject("ec_durs_secs")
        ecDursJSONObject.put("static", expectedStaticEndCardDuration)
        ceSettingsJSONObject.put("ec_durs_secs", ecDursJSONObject)

        val ceSettings = CreativeExperienceSettingsParser.parse(ceSettingsJSONObject, false)

        val closeAfterSecs = CreativeExperiencesFormulae.getCloseAfterSecs(
            isVast,
            isEndCard,
            endCardType,
            videoDurationSecs,
            ceSettings
        )

        Assert.assertEquals((videoDurationSecs + expectedStaticEndCardDuration), closeAfterSecs)
    }

    @Test
    fun `getCloseAfterSecs for VAST with INTERACTIVE end card and maxAdExperienceTime less than (videoDurationSecs + interactive end card dur) should return maxAdExperienceTime`() {
        val isVast = true
        val isEndCard = false
        val endCardType = EndCardType.INTERACTIVE
        val videoDurationSecs = 15
        val expectedInteractiveEndCardDuration = 1
        val maxAdExperienceTime = (videoDurationSecs + expectedInteractiveEndCardDuration) - 1

        val ceSettingsJSONObject = JSONObject(CE_SETTINGS_SKELETON_STRING)

        // modify the config before parsing
        ceSettingsJSONObject.put("max_ad_time_secs", maxAdExperienceTime)
        val ecDursJSONObject = ceSettingsJSONObject.getJSONObject("ec_durs_secs")
        ecDursJSONObject.put("interactive", expectedInteractiveEndCardDuration)
        ceSettingsJSONObject.put("ec_durs_secs", ecDursJSONObject)

        val ceSettings = CreativeExperienceSettingsParser.parse(ceSettingsJSONObject, false)

        val closeAfterSecs = CreativeExperiencesFormulae.getCloseAfterSecs(
            isVast,
            isEndCard,
            endCardType,
            videoDurationSecs,
            ceSettings
        )

        Assert.assertEquals(maxAdExperienceTime, closeAfterSecs)
    }

    @Test
    fun `getCloseAfterSecs for VAST with INTERACTIVE end card and maxAdExperienceTime greater than (videoDurationSecs + interactive end card dur) should return (videoDurationSecs + expectedInteractiveEndCardDuration)`() {
        val isVast = true
        val isEndCard = false
        val endCardType = EndCardType.INTERACTIVE
        val videoDurationSecs = 15
        val expectedInteractiveEndCardDuration = 1
        val maxAdExperienceTime = (videoDurationSecs + expectedInteractiveEndCardDuration) + 1

        val ceSettingsJSONObject = JSONObject(CE_SETTINGS_SKELETON_STRING)

        // modify the config before parsing
        ceSettingsJSONObject.put("max_ad_time_secs", maxAdExperienceTime)
        val ecDursJSONObject = ceSettingsJSONObject.getJSONObject("ec_durs_secs")
        ecDursJSONObject.put("interactive", expectedInteractiveEndCardDuration)
        ceSettingsJSONObject.put("ec_durs_secs", ecDursJSONObject)

        val ceSettings = CreativeExperienceSettingsParser.parse(ceSettingsJSONObject, false)

        val closeAfterSecs = CreativeExperiencesFormulae.getCloseAfterSecs(
            isVast,
            isEndCard,
            endCardType,
            videoDurationSecs,
            ceSettings
        )

        Assert.assertEquals((videoDurationSecs + expectedInteractiveEndCardDuration), closeAfterSecs)
    }

    @Test
    fun `getCloseAfterSecs for non-VAST with null end card should return maxAdExperienceTime`() {
        val isVast = false
        val isEndCard = false
        val endCardType: EndCardType? = null
        val videoDurationSecs = 51
        val maxAdExperienceTime = 50

        val ceSettingsJSONObject = JSONObject(CE_SETTINGS_SKELETON_STRING)

        // modify the config before parsing
        ceSettingsJSONObject.put("max_ad_time_secs", maxAdExperienceTime)

        val ceSettings = CreativeExperienceSettingsParser.parse(ceSettingsJSONObject, false)

        val closeAfterSecs = CreativeExperiencesFormulae.getCloseAfterSecs(
            isVast,
            isEndCard,
            endCardType,
            videoDurationSecs,
            ceSettings
        )

        Assert.assertEquals(maxAdExperienceTime, closeAfterSecs)
    }

    @Test
    fun `getCloseAfterSecs for non-VAST with NONE end card should return maxAdExperienceTime`() {
        val isVast = false
        val isEndCard = false
        val endCardType = EndCardType.NONE
        val videoDurationSecs = 51
        val expectedStaticEndCardDuration = 1
        val maxAdExperienceTime = 50

        val ceSettingsJSONObject = JSONObject(CE_SETTINGS_SKELETON_STRING)

        // modify the config before parsing
        ceSettingsJSONObject.put("max_ad_time_secs", maxAdExperienceTime)
        val ecDursJSONObject = ceSettingsJSONObject.getJSONObject("ec_durs_secs")
        ecDursJSONObject.put("static", expectedStaticEndCardDuration)
        ceSettingsJSONObject.put("ec_durs_secs", ecDursJSONObject)

        val ceSettings = CreativeExperienceSettingsParser.parse(ceSettingsJSONObject, false)

        val closeAfterSecs = CreativeExperiencesFormulae.getCloseAfterSecs(
            isVast,
            isEndCard,
            endCardType,
            videoDurationSecs,
            ceSettings
        )

        Assert.assertEquals(maxAdExperienceTime, closeAfterSecs)
    }

    @Test
    fun `getCloseAfterSecs for non-VAST with STATIC end card should return maxAdExperienceTime`() {
        val isVast = false
        val isEndCard = false
        val endCardType = EndCardType.STATIC
        val videoDurationSecs = 51
        val expectedStaticEndCardDuration = 1
        val maxAdExperienceTime = 50

        val ceSettingsJSONObject = JSONObject(CE_SETTINGS_SKELETON_STRING)

        // modify the config before parsing
        ceSettingsJSONObject.put("max_ad_time_secs", maxAdExperienceTime)
        val ecDursJSONObject = ceSettingsJSONObject.getJSONObject("ec_durs_secs")
        ecDursJSONObject.put("static", expectedStaticEndCardDuration)
        ceSettingsJSONObject.put("ec_durs_secs", ecDursJSONObject)

        val ceSettings = CreativeExperienceSettingsParser.parse(ceSettingsJSONObject, false)

        val closeAfterSecs = CreativeExperiencesFormulae.getCloseAfterSecs(
            isVast,
            isEndCard,
            endCardType,
            videoDurationSecs,
            ceSettings
        )

        Assert.assertEquals(maxAdExperienceTime, closeAfterSecs)
    }

    @Test
    fun `getCloseAfterSecs for non-VAST with INTERACTIVE end card should return maxAdExperienceTime`() {
        val isVast = false
        val isEndCard = false
        val endCardType = EndCardType.INTERACTIVE
        val videoDurationSecs = 51
        val expectedInteractiveEndCardDuration = 1
        val maxAdExperienceTime = 50

        val ceSettingsJSONObject = JSONObject(CE_SETTINGS_SKELETON_STRING)

        // modify the config before parsing
        ceSettingsJSONObject.put("max_ad_time_secs", maxAdExperienceTime)
        val ecDursJSONObject = ceSettingsJSONObject.getJSONObject("ec_durs_secs")
        ecDursJSONObject.put("interactive", expectedInteractiveEndCardDuration)
        ceSettingsJSONObject.put("ec_durs_secs", ecDursJSONObject)

        val ceSettings = CreativeExperienceSettingsParser.parse(ceSettingsJSONObject, false)

        val closeAfterSecs = CreativeExperiencesFormulae.getCloseAfterSecs(
            isVast,
            isEndCard,
            endCardType,
            videoDurationSecs,
            ceSettings
        )

        Assert.assertEquals(maxAdExperienceTime, closeAfterSecs)
    }

    //endregion getCloseAfterSecs

    //region getTimeUntilNextActionSecs

    @Test
    fun `getTimeUntilNextActionSecs for VAST main ad with videoDurationSecs greater than the skip threshold's skipMinSecs and greater than the skip threshold's skipAfterSecs should return the threshold's skipAfterSecs`() {
        val isVast = true
        val isEndCard = false
        val endCardType = EndCardType.INTERACTIVE
        val videoDurationSecs = 16
        val expectedTimeUntilNextActionSecs = 5

        val ceSettingsJSONObject = JSONObject(CE_SETTINGS_SKELETON_STRING)

        // modify the config before parsing
        val skipThresholdJSONArray = ceSettingsJSONObject.getJSONArray("video_skip_thresholds_secs")
        val skipThreshold1 = JSONObject("""
            {
                "min": 15,
                "after": 5
            }
            """)
        skipThresholdJSONArray.put(skipThreshold1)
        ceSettingsJSONObject.put("max_ad_time_secs", skipThresholdJSONArray)

        val ceSettings = CreativeExperienceSettingsParser.parse(ceSettingsJSONObject, false)

        val timeUntilNextActionSecs = CreativeExperiencesFormulae.getTimeUntilNextActionSecs(
            isVast,
            isEndCard,
            endCardType,
            videoDurationSecs,
            ceSettings
        )

        Assert.assertEquals(expectedTimeUntilNextActionSecs, timeUntilNextActionSecs)
    }

    @Test
    fun `getTimeUntilNextActionSecs for VAST main ad with videoDurationSecs greater than the skip threshold's skipMinSecs but less than the skip threshold's skipAfterSecs should return videoDurationSecs`() {
        val isVast = true
        val isEndCard = false
        val endCardType = EndCardType.INTERACTIVE
        val videoDurationSecs = 16
        val expectedTimeUntilNextActionSecs = 16

        val ceSettingsJSONObject = JSONObject(CE_SETTINGS_SKELETON_STRING)

        // modify the config before parsing
        val skipThresholdJSONArray = ceSettingsJSONObject.getJSONArray("video_skip_thresholds_secs")
        val skipThreshold1 = JSONObject("""
            {
                "min": 15,
                "after": 17
            }
            """)
        skipThresholdJSONArray.put(skipThreshold1)
        ceSettingsJSONObject.put("max_ad_time_secs", skipThresholdJSONArray)

        val ceSettings = CreativeExperienceSettingsParser.parse(ceSettingsJSONObject, false)

        val timeUntilNextActionSecs = CreativeExperiencesFormulae.getTimeUntilNextActionSecs(
            isVast,
            isEndCard,
            endCardType,
            videoDurationSecs,
            ceSettings
        )

        Assert.assertEquals(expectedTimeUntilNextActionSecs, timeUntilNextActionSecs)
    }

    @Test
    fun `getTimeUntilNextActionSecs for VAST main ad with videoDurationSecs less than smallest skip threshold should return video videoDurationSecs`() {
        val isVast = true
        val isEndCard = false
        val endCardType = EndCardType.INTERACTIVE
        val videoDurationSecs = 4
        val expectedTimeUntilNextActionSecs = 4

        val ceSettingsJSONObject = JSONObject(CE_SETTINGS_SKELETON_STRING)

        // modify the config before parsing
        val skipThresholdJSONArray = ceSettingsJSONObject.getJSONArray("video_skip_thresholds_secs")
        val skipThreshold1 = JSONObject("""
            {
                "min": 15,
                "after": 5
            }
            """)
        val skipThreshold2 = JSONObject("""
            {
                "min": 30,
                "after": 20
            }
            """)
        skipThresholdJSONArray.put(skipThreshold1)
        skipThresholdJSONArray.put(skipThreshold2)
        ceSettingsJSONObject.put("max_ad_time_secs", skipThresholdJSONArray)

        val ceSettings = CreativeExperienceSettingsParser.parse(ceSettingsJSONObject, false)

        val timeUntilNextActionSecs = CreativeExperiencesFormulae.getTimeUntilNextActionSecs(
            isVast,
            isEndCard,
            endCardType,
            videoDurationSecs,
            ceSettings
        )

        Assert.assertEquals(expectedTimeUntilNextActionSecs, timeUntilNextActionSecs)
    }

    @Test
    fun `getTimeUntilNextActionSecs for VAST main ad with videoDurationSecs greater than smallest skip threshold and smaller than next largest skip threshold should return video smallest skip threshold's after value`() {
        val isVast = true
        val isEndCard = false
        val endCardType = EndCardType.INTERACTIVE
        val videoDurationSecs = 20
        val expectedTimeUntilNextActionSecs = 7

        val ceSettingsJSONObject = JSONObject(CE_SETTINGS_SKELETON_STRING)

        // modify the config before parsing
        val skipThresholdJSONArray = ceSettingsJSONObject.getJSONArray("video_skip_thresholds_secs")
        val skipThreshold1 = JSONObject("""
            {
                "min": 30,
                "after": 20
            }
            """)
        val skipThreshold2 = JSONObject("""
            {
                "min": 15,
                "after": $expectedTimeUntilNextActionSecs
            }
            """)
        skipThresholdJSONArray.put(skipThreshold1)
        skipThresholdJSONArray.put(skipThreshold2)
        ceSettingsJSONObject.put("max_ad_time_secs", skipThresholdJSONArray)

        val ceSettings = CreativeExperienceSettingsParser.parse(ceSettingsJSONObject, false)

        val timeUntilNextActionSecs = CreativeExperiencesFormulae.getTimeUntilNextActionSecs(
            isVast,
            isEndCard,
            endCardType,
            videoDurationSecs,
            ceSettings
        )

        Assert.assertEquals(expectedTimeUntilNextActionSecs, timeUntilNextActionSecs)
    }

    @Test
    fun `getTimeUntilNextActionSecs for VAST main ad with videoDurationSecs greater than largest skip threshold should return video largest skip threshold's after value`() {
        val isVast = true
        val isEndCard = false
        val endCardType = EndCardType.INTERACTIVE
        val videoDurationSecs = 35
        val expectedTimeUntilNextActionSecs = 20

        val ceSettingsJSONObject = JSONObject(CE_SETTINGS_SKELETON_STRING)

        // modify the config before parsing
        val skipThresholdJSONArray = ceSettingsJSONObject.getJSONArray("video_skip_thresholds_secs")
        val skipThreshold1 = JSONObject("""
            {
                "min": 15,
                "after": 5
            }
            """)
        val skipThreshold2 = JSONObject("""
            {
                "min": 30,
                "after": $expectedTimeUntilNextActionSecs
            }
            """)
        skipThresholdJSONArray.put(skipThreshold1)
        skipThresholdJSONArray.put(skipThreshold2)
        ceSettingsJSONObject.put("max_ad_time_secs", skipThresholdJSONArray)

        val ceSettings = CreativeExperienceSettingsParser.parse(ceSettingsJSONObject, false)

        val timeUntilNextActionSecs = CreativeExperiencesFormulae.getTimeUntilNextActionSecs(
            isVast,
            isEndCard,
            endCardType,
            videoDurationSecs,
            ceSettings
        )

        Assert.assertEquals(expectedTimeUntilNextActionSecs, timeUntilNextActionSecs)
    }

    @Test
    fun `getTimeUntilNextActionSecs for non-VAST null end card should return 0`() {
        val isVast = false
        val isEndCard = true
        val endCardType:EndCardType? = null
        val videoDurationSecs = 0
        val expectedTimeUntilNextActionSecs = 0

        val ceSettingsJSONObject = JSONObject(CE_SETTINGS_SKELETON_STRING)

        // modify the config before parsing
        val ecDursJSONObject = ceSettingsJSONObject.getJSONObject("ec_durs_secs")
        // Make sure other end card values are not 0
        ecDursJSONObject.put("min_static", expectedTimeUntilNextActionSecs + 1)
        ecDursJSONObject.put("min_interactive", expectedTimeUntilNextActionSecs + 1)
        ceSettingsJSONObject.put("ec_durs_secs", ecDursJSONObject)

        val ceSettings = CreativeExperienceSettingsParser.parse(ceSettingsJSONObject, false)

        val timeUntilNextActionSecs = CreativeExperiencesFormulae.getTimeUntilNextActionSecs(
            isVast,
            isEndCard,
            endCardType,
            videoDurationSecs,
            ceSettings
        )

        Assert.assertEquals(expectedTimeUntilNextActionSecs, timeUntilNextActionSecs)
    }

    @Test
    fun `getTimeUntilNextActionSecs for non-VAST NONE end card should return 0`() {
        val isVast = false
        val isEndCard = true
        val endCardType = EndCardType.NONE
        val videoDurationSecs = 0
        val expectedTimeUntilNextActionSecs = 0

        val ceSettingsJSONObject = JSONObject(CE_SETTINGS_SKELETON_STRING)

        // modify the config before parsing
        val ecDursJSONObject = ceSettingsJSONObject.getJSONObject("ec_durs_secs")
        // Make sure other end card values are not 0
        ecDursJSONObject.put("min_static", expectedTimeUntilNextActionSecs + 1)
        ecDursJSONObject.put("min_interactive", expectedTimeUntilNextActionSecs + 1)
        ceSettingsJSONObject.put("ec_durs_secs", ecDursJSONObject)

        val ceSettings = CreativeExperienceSettingsParser.parse(ceSettingsJSONObject, false)

        val timeUntilNextActionSecs = CreativeExperiencesFormulae.getTimeUntilNextActionSecs(
            isVast,
            isEndCard,
            endCardType,
            videoDurationSecs,
            ceSettings
        )

        Assert.assertEquals(expectedTimeUntilNextActionSecs, timeUntilNextActionSecs)
    }

    @Test
    fun `getTimeUntilNextActionSecs for non-VAST STATIC end card should return 0`() {
        val isVast = false
        val isEndCard = true
        val endCardType = EndCardType.STATIC
        val videoDurationSecs = 0
        val expectedTimeUntilNextActionSecs = 5

        val ceSettingsJSONObject = JSONObject(CE_SETTINGS_SKELETON_STRING)

        // modify the config before parsing
        val ecDursJSONObject = ceSettingsJSONObject.getJSONObject("ec_durs_secs")
        // Make sure the STATIC end card value is the expected value
        ecDursJSONObject.put("min_static", expectedTimeUntilNextActionSecs)
        // Make sure other end card value is not the expected value
        ecDursJSONObject.put("min_interactive", expectedTimeUntilNextActionSecs + 1)
        ceSettingsJSONObject.put("ec_durs_secs", ecDursJSONObject)

        val ceSettings = CreativeExperienceSettingsParser.parse(ceSettingsJSONObject, false)

        val timeUntilNextActionSecs = CreativeExperiencesFormulae.getTimeUntilNextActionSecs(
            isVast,
            isEndCard,
            endCardType,
            videoDurationSecs,
            ceSettings
        )

        Assert.assertEquals(expectedTimeUntilNextActionSecs, timeUntilNextActionSecs)
    }

    @Test
    fun `getTimeUntilNextActionSecs for non-VAST INTERACTIVE end card should return 0`() {
        val isVast = false
        val isEndCard = true
        val endCardType = EndCardType.INTERACTIVE
        val videoDurationSecs = 0
        val expectedTimeUntilNextActionSecs = 5

        val ceSettingsJSONObject = JSONObject(CE_SETTINGS_SKELETON_STRING)

        // modify the config before parsing
        val ecDursJSONObject = ceSettingsJSONObject.getJSONObject("ec_durs_secs")
        // Make sure other end card value is not the expected value
        ecDursJSONObject.put("min_static", expectedTimeUntilNextActionSecs + 1)
        // Make sure the INTERACTIVE end card value is the expected value
        ecDursJSONObject.put("min_interactive", expectedTimeUntilNextActionSecs)
        ceSettingsJSONObject.put("ec_durs_secs", ecDursJSONObject)

        val ceSettings = CreativeExperienceSettingsParser.parse(ceSettingsJSONObject, false)

        val timeUntilNextActionSecs = CreativeExperiencesFormulae.getTimeUntilNextActionSecs(
            isVast,
            isEndCard,
            endCardType,
            videoDurationSecs,
            ceSettings
        )

        Assert.assertEquals(expectedTimeUntilNextActionSecs, timeUntilNextActionSecs)
    }

    @Test
    fun `getTimeUntilNextActionSecs for non-VAST main ad should return main ad config's min_next_action_secs`() {
        val isVast = false
        val isEndCard = false
        val endCardType = EndCardType.INTERACTIVE
        val videoDurationSecs = 0
        val expectedTimeUntilNextActionSecs = 12

        val ceSettingsJSONObject = JSONObject(CE_SETTINGS_SKELETON_STRING)

        // modify the config before parsing
        val mainAdJSONObject = ceSettingsJSONObject.getJSONObject("main_ad")
        // Make sure other end card value is not the expected value
        mainAdJSONObject.put("min_next_action_secs", expectedTimeUntilNextActionSecs)
        ceSettingsJSONObject.put("main_ad", mainAdJSONObject)

        val ceSettings = CreativeExperienceSettingsParser.parse(ceSettingsJSONObject, false)

        val timeUntilNextActionSecs = CreativeExperiencesFormulae.getTimeUntilNextActionSecs(
            isVast,
            isEndCard,
            endCardType,
            videoDurationSecs,
            ceSettings
        )

        Assert.assertEquals(expectedTimeUntilNextActionSecs, timeUntilNextActionSecs)
    }

    //endregion getTimeUntilNextActionSecs

    //region getCountdownDuration

    @Test
    fun `getCountdownDuration for VAST main ad with null end card when closeAfterSecs is greater than timeUntilNextActionSecs should return closeAfterSecs`() {
        val isVast = true
        val isEndCard = false
        val endCardType = null
        val videoDurationSecs = 0
        val elapsedTimeInAdSecs = 0
        val mockedCloseAfterSecs = 10
        val mockedTimeUntilNextActionSecs = 9

        val ceSettingsJSONObject = JSONObject(CE_SETTINGS_SKELETON_STRING)
        val ceSettings = CreativeExperienceSettingsParser.parse(ceSettingsJSONObject, false)

        PowerMockito.stub<Int>(
            PowerMockito.method(
                CreativeExperiencesFormulae::class.java,
                "getCloseAfterSecs",
                Boolean::class.java,
                Boolean::class.java,
                EndCardType::class.java,
                Int::class.java,
                CreativeExperienceSettings::class.java
            )
        ).toReturn(mockedCloseAfterSecs)

        PowerMockito.stub<Int>(
            PowerMockito.method(
                CreativeExperiencesFormulae::class.java,
                "getTimeUntilNextActionSecs",
                Boolean::class.java,
                Boolean::class.java,
                EndCardType::class.java,
                Int::class.java,
                CreativeExperienceSettings::class.java
            )
        ).toReturn(mockedTimeUntilNextActionSecs)

        val countdownDuration = CreativeExperiencesFormulae.getCountdownDuration(
            isVast,
            isEndCard,
            endCardType,
            videoDurationSecs,
            elapsedTimeInAdSecs,
            ceSettings
        )

        Assert.assertEquals(mockedCloseAfterSecs, countdownDuration)
    }

    @Test
    fun `getCountdownDuration for VAST main ad with NONE end card when closeAfterSecs is greater than timeUntilNextActionSecs should return closeAfterSecs`() {
        val isVast = true
        val isEndCard = false
        val endCardType = EndCardType.NONE
        val videoDurationSecs = 0
        val elapsedTimeInAdSecs = 0
        val mockedCloseAfterSecs = 10
        val mockedTimeUntilNextActionSecs = 9

        val ceSettingsJSONObject = JSONObject(CE_SETTINGS_SKELETON_STRING)
        val ceSettings = CreativeExperienceSettingsParser.parse(ceSettingsJSONObject, false)

        PowerMockito.stub<Int>(
            PowerMockito.method(
                CreativeExperiencesFormulae::class.java,
                "getCloseAfterSecs",
                Boolean::class.java,
                Boolean::class.java,
                EndCardType::class.java,
                Int::class.java,
                CreativeExperienceSettings::class.java
            )
        ).toReturn(mockedCloseAfterSecs)

        PowerMockito.stub<Int>(
            PowerMockito.method(
                CreativeExperiencesFormulae::class.java,
                "getTimeUntilNextActionSecs",
                Boolean::class.java,
                Boolean::class.java,
                EndCardType::class.java,
                Int::class.java,
                CreativeExperienceSettings::class.java
            )
        ).toReturn(mockedTimeUntilNextActionSecs)

        val countdownDuration = CreativeExperiencesFormulae.getCountdownDuration(
            isVast,
            isEndCard,
            endCardType,
            videoDurationSecs,
            elapsedTimeInAdSecs,
            ceSettings
        )

        Assert.assertEquals(mockedCloseAfterSecs, countdownDuration)
    }

    @Test
    fun `getCountdownDuration for VAST main ad with null end card when closeAfterSecs is less than timeUntilNextActionSecs should return timeUntilNextActionSecs`() {
        val isVast = true
        val isEndCard = false
        val endCardType = null
        val videoDurationSecs = 0
        val elapsedTimeInAdSecs = 0
        val mockedCloseAfterSecs = 9
        val mockedTimeUntilNextActionSecs = 10

        val ceSettingsJSONObject = JSONObject(CE_SETTINGS_SKELETON_STRING)
        val ceSettings = CreativeExperienceSettingsParser.parse(ceSettingsJSONObject, false)

        PowerMockito.stub<Int>(
            PowerMockito.method(
                CreativeExperiencesFormulae::class.java,
                "getCloseAfterSecs",
                Boolean::class.java,
                Boolean::class.java,
                EndCardType::class.java,
                Int::class.java,
                CreativeExperienceSettings::class.java
            )
        ).toReturn(mockedCloseAfterSecs)

        PowerMockito.stub<Int>(
            PowerMockito.method(
                CreativeExperiencesFormulae::class.java,
                "getTimeUntilNextActionSecs",
                Boolean::class.java,
                Boolean::class.java,
                EndCardType::class.java,
                Int::class.java,
                CreativeExperienceSettings::class.java
            )
        ).toReturn(mockedTimeUntilNextActionSecs)

        val countdownDuration = CreativeExperiencesFormulae.getCountdownDuration(
            isVast,
            isEndCard,
            endCardType,
            videoDurationSecs,
            elapsedTimeInAdSecs,
            ceSettings
        )

        Assert.assertEquals(mockedTimeUntilNextActionSecs, countdownDuration)
    }

    @Test
    fun `getCountdownDuration for VAST main ad with NONE end card when closeAfterSecs is less than timeUntilNextActionSecs should return timeUntilNextActionSecs`() {
        val isVast = true
        val isEndCard = false
        val endCardType = EndCardType.NONE
        val videoDurationSecs = 0
        val elapsedTimeInAdSecs = 0
        val mockedCloseAfterSecs = 9
        val mockedTimeUntilNextActionSecs = 10

        val ceSettingsJSONObject = JSONObject(CE_SETTINGS_SKELETON_STRING)
        val ceSettings = CreativeExperienceSettingsParser.parse(ceSettingsJSONObject, false)

        PowerMockito.stub<Int>(
            PowerMockito.method(
                CreativeExperiencesFormulae::class.java,
                "getCloseAfterSecs",
                Boolean::class.java,
                Boolean::class.java,
                EndCardType::class.java,
                Int::class.java,
                CreativeExperienceSettings::class.java
            )
        ).toReturn(mockedCloseAfterSecs)

        PowerMockito.stub<Int>(
            PowerMockito.method(
                CreativeExperiencesFormulae::class.java,
                "getTimeUntilNextActionSecs",
                Boolean::class.java,
                Boolean::class.java,
                EndCardType::class.java,
                Int::class.java,
                CreativeExperienceSettings::class.java
            )
        ).toReturn(mockedTimeUntilNextActionSecs)

        val countdownDuration = CreativeExperiencesFormulae.getCountdownDuration(
            isVast,
            isEndCard,
            endCardType,
            videoDurationSecs,
            elapsedTimeInAdSecs,
            ceSettings
        )

        Assert.assertEquals(mockedTimeUntilNextActionSecs, countdownDuration)
    }

    @Test
    fun `getCountdownDuration for VAST main ad with end card should return timeUntilNextActionSecs`() {
        val isVast = true
        val isEndCard = false
        val endCardType = EndCardType.INTERACTIVE
        val videoDurationSecs = 0
        val elapsedTimeInAdSecs = 0
        val mockedCloseAfterSecs = 10
        val mockedTimeUntilNextActionSecs = 9

        val ceSettingsJSONObject = JSONObject(CE_SETTINGS_SKELETON_STRING)
        val ceSettings = CreativeExperienceSettingsParser.parse(ceSettingsJSONObject, false)

        PowerMockito.stub<Int>(
            PowerMockito.method(
                CreativeExperiencesFormulae::class.java,
                "getCloseAfterSecs",
                Boolean::class.java,
                Boolean::class.java,
                EndCardType::class.java,
                Int::class.java,
                CreativeExperienceSettings::class.java
            )
        ).toReturn(mockedCloseAfterSecs)

        PowerMockito.stub<Int>(
            PowerMockito.method(
                CreativeExperiencesFormulae::class.java,
                "getTimeUntilNextActionSecs",
                Boolean::class.java,
                Boolean::class.java,
                EndCardType::class.java,
                Int::class.java,
                CreativeExperienceSettings::class.java
            )
        ).toReturn(mockedTimeUntilNextActionSecs)

        val countdownDuration = CreativeExperiencesFormulae.getCountdownDuration(
            isVast,
            isEndCard,
            endCardType,
            videoDurationSecs,
            elapsedTimeInAdSecs,
            ceSettings
        )

        Assert.assertEquals(mockedTimeUntilNextActionSecs, countdownDuration)
    }

    @Test
    fun `getCountdownDuration for non-VAST main ad when adTimeRemainingSecs is greater than timeUntilNextActionSecs is should return adTimeRemainingSecs`() {
        val isVast = false
        val isEndCard = false
        val endCardType = EndCardType.INTERACTIVE
        val videoDurationSecs = 0
        val elapsedTimeInAdSecs = 1
        val mockedCloseAfterSecs = 10
        val mockedAdTimeRemainingSecs = mockedCloseAfterSecs - elapsedTimeInAdSecs
        val mockedTimeUntilNextActionSecs = 8

        val ceSettingsJSONObject = JSONObject(CE_SETTINGS_SKELETON_STRING)
        val ceSettings = CreativeExperienceSettingsParser.parse(ceSettingsJSONObject, false)

        PowerMockito.stub<Int>(
            PowerMockito.method(
                CreativeExperiencesFormulae::class.java,
                "getCloseAfterSecs",
                Boolean::class.java,
                Boolean::class.java,
                EndCardType::class.java,
                Int::class.java,
                CreativeExperienceSettings::class.java
            )
        ).toReturn(mockedCloseAfterSecs)

        PowerMockito.stub<Int>(
            PowerMockito.method(
                CreativeExperiencesFormulae::class.java,
                "getTimeUntilNextActionSecs",
                Boolean::class.java,
                Boolean::class.java,
                EndCardType::class.java,
                Int::class.java,
                CreativeExperienceSettings::class.java
            )
        ).toReturn(mockedTimeUntilNextActionSecs)

        val countdownDuration = CreativeExperiencesFormulae.getCountdownDuration(
            isVast,
            isEndCard,
            endCardType,
            videoDurationSecs,
            elapsedTimeInAdSecs,
            ceSettings
        )

        Assert.assertEquals(mockedAdTimeRemainingSecs, countdownDuration)
    }

    @Test
    fun `getCountdownDuration for non-VAST main ad when adTimeRemainingSecs is less than timeUntilNextActionSecs is should return timeUntilNextActionSecs`() {
        val isVast = false
        val isEndCard = false
        val endCardType = EndCardType.INTERACTIVE
        val videoDurationSecs = 0
        val elapsedTimeInAdSecs = 3
        val mockedCloseAfterSecs = 10
        val mockedTimeUntilNextActionSecs = 8

        val ceSettingsJSONObject = JSONObject(CE_SETTINGS_SKELETON_STRING)
        val ceSettings = CreativeExperienceSettingsParser.parse(ceSettingsJSONObject, false)

        PowerMockito.stub<Int>(
            PowerMockito.method(
                CreativeExperiencesFormulae::class.java,
                "getCloseAfterSecs",
                Boolean::class.java,
                Boolean::class.java,
                EndCardType::class.java,
                Int::class.java,
                CreativeExperienceSettings::class.java
            )
        ).toReturn(mockedCloseAfterSecs)

        PowerMockito.stub<Int>(
            PowerMockito.method(
                CreativeExperiencesFormulae::class.java,
                "getTimeUntilNextActionSecs",
                Boolean::class.java,
                Boolean::class.java,
                EndCardType::class.java,
                Int::class.java,
                CreativeExperienceSettings::class.java
            )
        ).toReturn(mockedTimeUntilNextActionSecs)

        val countdownDuration = CreativeExperiencesFormulae.getCountdownDuration(
            isVast,
            isEndCard,
            endCardType,
            videoDurationSecs,
            elapsedTimeInAdSecs,
            ceSettings
        )

        Assert.assertEquals(mockedTimeUntilNextActionSecs, countdownDuration)
    }

    @Test
    fun `getCountdownDuration for null end card ad should return 0`() {
        val isVast = false
        val isEndCard = true
        val endCardType: EndCardType? = null
        val videoDurationSecs = 0
        val elapsedTimeInAdSecs = 3
        val mockedCloseAfterSecs = 10
        val mockedTimeUntilNextActionSecs = 8

        val ceSettingsJSONObject = JSONObject(CE_SETTINGS_SKELETON_STRING)
        val ceSettings = CreativeExperienceSettingsParser.parse(ceSettingsJSONObject, false)

        PowerMockito.stub<Int>(
            PowerMockito.method(
                CreativeExperiencesFormulae::class.java,
                "getCloseAfterSecs",
                Boolean::class.java,
                Boolean::class.java,
                EndCardType::class.java,
                Int::class.java,
                CreativeExperienceSettings::class.java
            )
        ).toReturn(mockedCloseAfterSecs)

        PowerMockito.stub<Int>(
            PowerMockito.method(
                CreativeExperiencesFormulae::class.java,
                "getTimeUntilNextActionSecs",
                Boolean::class.java,
                Boolean::class.java,
                EndCardType::class.java,
                Int::class.java,
                CreativeExperienceSettings::class.java
            )
        ).toReturn(mockedTimeUntilNextActionSecs)

        val countdownDuration = CreativeExperiencesFormulae.getCountdownDuration(
            isVast,
            isEndCard,
            endCardType,
            videoDurationSecs,
            elapsedTimeInAdSecs,
            ceSettings
        )

        Assert.assertEquals(0, countdownDuration)
    }

    @Test
    fun `getCountdownDuration for NONE end card ad should return 0`() {
        val isVast = false
        val isEndCard = true
        val endCardType = EndCardType.NONE
        val videoDurationSecs = 0
        val elapsedTimeInAdSecs = 3
        val mockedCloseAfterSecs = 10
        val mockedTimeUntilNextActionSecs = 8

        val ceSettingsJSONObject = JSONObject(CE_SETTINGS_SKELETON_STRING)
        val ceSettings = CreativeExperienceSettingsParser.parse(ceSettingsJSONObject, false)

        PowerMockito.stub<Int>(
            PowerMockito.method(
                CreativeExperiencesFormulae::class.java,
                "getCloseAfterSecs",
                Boolean::class.java,
                Boolean::class.java,
                EndCardType::class.java,
                Int::class.java,
                CreativeExperienceSettings::class.java
            )
        ).toReturn(mockedCloseAfterSecs)

        PowerMockito.stub<Int>(
            PowerMockito.method(
                CreativeExperiencesFormulae::class.java,
                "getTimeUntilNextActionSecs",
                Boolean::class.java,
                Boolean::class.java,
                EndCardType::class.java,
                Int::class.java,
                CreativeExperienceSettings::class.java
            )
        ).toReturn(mockedTimeUntilNextActionSecs)

        val countdownDuration = CreativeExperiencesFormulae.getCountdownDuration(
            isVast,
            isEndCard,
            endCardType,
            videoDurationSecs,
            elapsedTimeInAdSecs,
            ceSettings
        )

        Assert.assertEquals(0, countdownDuration)
    }

    @Test
    fun `getCountdownDuration for STATIC end card when adTimeRemainingSecs is greater than timeUntilNextActionSecs is should return adTimeRemainingSecs`() {
        val isVast = false
        val isEndCard = false
        val endCardType = EndCardType.INTERACTIVE
        val videoDurationSecs = 0
        val elapsedTimeInAdSecs = 1
        val mockedCloseAfterSecs = 10
        val mockedAdTimeRemainingSecs = mockedCloseAfterSecs - elapsedTimeInAdSecs
        val mockedTimeUntilNextActionSecs = 8

        val ceSettingsJSONObject = JSONObject(CE_SETTINGS_SKELETON_STRING)
        val ceSettings = CreativeExperienceSettingsParser.parse(ceSettingsJSONObject, false)

        PowerMockito.stub<Int>(
            PowerMockito.method(
                CreativeExperiencesFormulae::class.java,
                "getCloseAfterSecs",
                Boolean::class.java,
                Boolean::class.java,
                EndCardType::class.java,
                Int::class.java,
                CreativeExperienceSettings::class.java
            )
        ).toReturn(mockedCloseAfterSecs)

        PowerMockito.stub<Int>(
            PowerMockito.method(
                CreativeExperiencesFormulae::class.java,
                "getTimeUntilNextActionSecs",
                Boolean::class.java,
                Boolean::class.java,
                EndCardType::class.java,
                Int::class.java,
                CreativeExperienceSettings::class.java
            )
        ).toReturn(mockedTimeUntilNextActionSecs)

        val countdownDuration = CreativeExperiencesFormulae.getCountdownDuration(
            isVast,
            isEndCard,
            endCardType,
            videoDurationSecs,
            elapsedTimeInAdSecs,
            ceSettings
        )

        Assert.assertEquals(mockedAdTimeRemainingSecs, countdownDuration)
    }

    @Test
    fun `getCountdownDuration for STATIC end card when adTimeRemainingSecs is less than timeUntilNextActionSecs is should return timeUntilNextActionSecs`() {
        val isVast = false
        val isEndCard = false
        val endCardType = EndCardType.STATIC
        val videoDurationSecs = 0
        val elapsedTimeInAdSecs = 3
        val mockedCloseAfterSecs = 10
        val mockedTimeUntilNextActionSecs = 8

        val ceSettingsJSONObject = JSONObject(CE_SETTINGS_SKELETON_STRING)
        val ceSettings = CreativeExperienceSettingsParser.parse(ceSettingsJSONObject, false)

        PowerMockito.stub<Int>(
            PowerMockito.method(
                CreativeExperiencesFormulae::class.java,
                "getCloseAfterSecs",
                Boolean::class.java,
                Boolean::class.java,
                EndCardType::class.java,
                Int::class.java,
                CreativeExperienceSettings::class.java
            )
        ).toReturn(mockedCloseAfterSecs)

        PowerMockito.stub<Int>(
            PowerMockito.method(
                CreativeExperiencesFormulae::class.java,
                "getTimeUntilNextActionSecs",
                Boolean::class.java,
                Boolean::class.java,
                EndCardType::class.java,
                Int::class.java,
                CreativeExperienceSettings::class.java
            )
        ).toReturn(mockedTimeUntilNextActionSecs)

        val countdownDuration = CreativeExperiencesFormulae.getCountdownDuration(
            isVast,
            isEndCard,
            endCardType,
            videoDurationSecs,
            elapsedTimeInAdSecs,
            ceSettings
        )

        Assert.assertEquals(mockedTimeUntilNextActionSecs, countdownDuration)
    }

    @Test
    fun `getCountdownDuration for INTERACTIVE end card when adTimeRemainingSecs is greater than timeUntilNextActionSecs is should return adTimeRemainingSecs`() {
        val isVast = false
        val isEndCard = true
        val endCardType = EndCardType.INTERACTIVE
        val videoDurationSecs = 0
        val elapsedTimeInAdSecs = 1
        val mockedCloseAfterSecs = 10
        val mockedAdTimeRemainingSecs = mockedCloseAfterSecs - elapsedTimeInAdSecs
        val mockedTimeUntilNextActionSecs = 8

        val ceSettingsJSONObject = JSONObject(CE_SETTINGS_SKELETON_STRING)
        val ceSettings = CreativeExperienceSettingsParser.parse(ceSettingsJSONObject, false)

        PowerMockito.stub<Int>(
            PowerMockito.method(
                CreativeExperiencesFormulae::class.java,
                "getCloseAfterSecs",
                Boolean::class.java,
                Boolean::class.java,
                EndCardType::class.java,
                Int::class.java,
                CreativeExperienceSettings::class.java
            )
        ).toReturn(mockedCloseAfterSecs)

        PowerMockito.stub<Int>(
            PowerMockito.method(
                CreativeExperiencesFormulae::class.java,
                "getTimeUntilNextActionSecs",
                Boolean::class.java,
                Boolean::class.java,
                EndCardType::class.java,
                Int::class.java,
                CreativeExperienceSettings::class.java
            )
        ).toReturn(mockedTimeUntilNextActionSecs)

        val countdownDuration = CreativeExperiencesFormulae.getCountdownDuration(
            isVast,
            isEndCard,
            endCardType,
            videoDurationSecs,
            elapsedTimeInAdSecs,
            ceSettings
        )

        Assert.assertEquals(mockedAdTimeRemainingSecs, countdownDuration)
    }

    @Test
    fun `getCountdownDuration for INTERACTIVE end card when adTimeRemainingSecs is less than timeUntilNextActionSecs is should return timeUntilNextActionSecs`() {
        val isVast = false
        val isEndCard = true
        val endCardType = EndCardType.INTERACTIVE
        val videoDurationSecs = 0
        val elapsedTimeInAdSecs = 3
        val mockedCloseAfterSecs = 10
        val mockedTimeUntilNextActionSecs = 8

        val ceSettingsJSONObject = JSONObject(CE_SETTINGS_SKELETON_STRING)
        val ceSettings = CreativeExperienceSettingsParser.parse(ceSettingsJSONObject, false)

        PowerMockito.stub<Int>(
            PowerMockito.method(
                CreativeExperiencesFormulae::class.java,
                "getCloseAfterSecs",
                Boolean::class.java,
                Boolean::class.java,
                EndCardType::class.java,
                Int::class.java,
                CreativeExperienceSettings::class.java
            )
        ).toReturn(mockedCloseAfterSecs)

        PowerMockito.stub<Int>(
            PowerMockito.method(
                CreativeExperiencesFormulae::class.java,
                "getTimeUntilNextActionSecs",
                Boolean::class.java,
                Boolean::class.java,
                EndCardType::class.java,
                Int::class.java,
                CreativeExperienceSettings::class.java
            )
        ).toReturn(mockedTimeUntilNextActionSecs)

        val countdownDuration = CreativeExperiencesFormulae.getCountdownDuration(
            isVast,
            isEndCard,
            endCardType,
            videoDurationSecs,
            elapsedTimeInAdSecs,
            ceSettings
        )

        Assert.assertEquals(mockedTimeUntilNextActionSecs, countdownDuration)
    }

    //endregion getCountdownDuration

    //endregion Base unit tests
}
