// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads

import com.mopub.common.logging.MoPubLog

import java.io.*

/**
 * This class stores creative experience settings for an ad unit.
 *
 * @property hash the SHA-256 hash of these settings.
 * @property maxAdExperienceTimeSecs the maximum time in seconds a user will be locked into the ad
 * experience.
 * @property vastSkipThresholds a list of [VastSkipThreshold].
 * @property endCardDurations an [EndCardDurations] object.
 * @property mainAdConfig a [CreativeExperienceAdConfig] object that stores settings for the main ad
 * in the ad experience.
 * @property endCardConfig a [CreativeExperienceAdConfig] object that stores settings for the end
 * card in the ad experience.
 */
data class CreativeExperienceSettings(
    var hash: String = "0",
    val maxAdExperienceTimeSecs: Int,
    val vastSkipThresholds: List<VastSkipThreshold>,
    val endCardDurations: EndCardDurations,
    val mainAdConfig: CreativeExperienceAdConfig,
    val endCardConfig: CreativeExperienceAdConfig,
) : Serializable {

    companion object {
        /**
         * Default Values
         */
        private const val DEFAULT_MAX_AD_EXPERIENCE_TIME_REWARDED_SECS = 30
        private const val DEFAULT_MAX_AD_EXPERIENCE_TIME_NON_REWARDED_SECS = 0

        @JvmStatic
        fun getDefaultSettings(isRewarded: Boolean) =
            CreativeExperienceSettings(
                maxAdExperienceTimeSecs = if (isRewarded)
                    DEFAULT_MAX_AD_EXPERIENCE_TIME_REWARDED_SECS
                else DEFAULT_MAX_AD_EXPERIENCE_TIME_NON_REWARDED_SECS,
                vastSkipThresholds = listOf(
                    VastSkipThreshold.getDefaultVastSkipThreshold(isRewarded)
                ),
                endCardDurations = EndCardDurations.getDefaultEndCardDurations(isRewarded),
                mainAdConfig = CreativeExperienceAdConfig.getDefaultCEAdConfig(
                    isRewarded,
                    isMainAd = true
                ),
                endCardConfig = CreativeExperienceAdConfig.getDefaultCEAdConfig(
                    isRewarded,
                    isMainAd = false
                )
            )

        @JvmStatic
        fun getDefaultMaxAdExperienceTimeSecs(isRewarded: Boolean) =
            if (isRewarded)
                DEFAULT_MAX_AD_EXPERIENCE_TIME_REWARDED_SECS
            else DEFAULT_MAX_AD_EXPERIENCE_TIME_NON_REWARDED_SECS

        @JvmStatic
        fun fromByteArray(byteArray: ByteArray?): CreativeExperienceSettings? {
            if (byteArray == null) {
                return null
            }
            try {
                val byteArrayInputStream = ByteArrayInputStream(byteArray)
                val objectInput = ObjectInputStream(byteArrayInputStream)
                val result = objectInput.readObject() as CreativeExperienceSettings
                objectInput.close()
                byteArrayInputStream.close()
                return result
            } catch (e: IOException) {
                MoPubLog.log(
                    MoPubLog.SdkLogEvent.CUSTOM,
                    "Unable to parse creative experience settings from byte array.",
                    e
                )
            } catch (e: ClassCastException) {
                MoPubLog.log(
                    MoPubLog.SdkLogEvent.CUSTOM,
                    "Unable to cast byte array to CreativeExperienceSettings.",
                    e
                )
            }
            return null
        }
    }

    fun toByteArray(): ByteArray? {
        try {
            val byteArrayOutputStream = ByteArrayOutputStream()
            val objectOutputStream = ObjectOutputStream(byteArrayOutputStream)
            objectOutputStream.writeObject(this@CreativeExperienceSettings)
            objectOutputStream.flush()
            val result = byteArrayOutputStream.toByteArray()
            byteArrayOutputStream.close()
            objectOutputStream.close()
            return result
        } catch (e: IOException) {
            MoPubLog.log(
                MoPubLog.SdkLogEvent.CUSTOM,
                "Unable to convert creative experience settings to byte array.",
                e
            )
        }
        return null
    }

    override fun toString() = "CreativeExperienceSettings(" +
            "hash=$hash, " +
            "maxAdExperienceTimeSecs=$maxAdExperienceTimeSecs, " +
            "vastSkipThresholds=$vastSkipThresholds, " +
            "endCardDurations=$endCardDurations, " +
            "mainAd=$mainAdConfig, " +
            "endCard=$endCardConfig)"
}
