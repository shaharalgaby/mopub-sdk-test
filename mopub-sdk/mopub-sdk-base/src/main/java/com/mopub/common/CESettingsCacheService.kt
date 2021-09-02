// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common

import android.content.Context
import androidx.annotation.AnyThread
import androidx.annotation.WorkerThread

import com.mopub.common.logging.MoPubLog
import com.mopub.common.logging.MoPubLog.SdkLogEvent.CUSTOM
import com.mopub.mobileads.CreativeExperienceSettings

import kotlinx.coroutines.SupervisorJob

object CESettingsCacheService : CacheService("mopub-ce-cache") {

    private val supervisorJob = SupervisorJob()

    /**
     * Interface for CE Settings cache related callbacks
     */
    interface CESettingsCacheListener {
        @JvmDefault
        fun onSettingsReceived(settings: CreativeExperienceSettings?) {
        }

        @JvmDefault
        fun onHashReceived(hash: String) {
        }
    }

    /**
     * Get the CE Settings hash for an ad unit from the CE Settings cache and pass the hash
     * retrieved to the provided listener.
     *
     * @param adUnitId the ad unit id for which to fetch the CE Settings hash for.
     * @param listener the listener to pass the retrieved hash to.
     * @param context to initialize the CE Settings cache.
     */
    @JvmStatic
    @AnyThread
    fun getCESettingsHash(
        adUnitId: String,
        listener: CESettingsCacheListener,
        context: Context?
    ) {
        if (context == null) {
            MoPubLog.log(CUSTOM, "Context cannot be null.")
            listener.onHashReceived("0")
            return
        }

        val cacheListener = object : DiskLruCacheListener {
            override fun onGetComplete(key: String, content: ByteArray?) {
                if (key != adUnitId) {
                    return
                }
                val settingsFromCache = CreativeExperienceSettings.fromByteArray(content)
                listener.onHashReceived(settingsFromCache?.hash ?: "0")
            }
        }

        getFromDiskCacheAsync(
            adUnitId,
            cacheListener,
            supervisorJob,
            context
        )
    }

    /**
     * Get CE Settings for an ad unit from the CE Settings cache and pass the settings retrieved to
     * the provided listener.
     *
     * @param adUnitId the ad unit id for which to fetch CE Settings for.
     * @param listener the listener to pass the retrieved settings to.
     * @param context to initialize the CE Settings cache.
     */
    @JvmStatic
    @AnyThread
    fun getCESettings(
        adUnitId: String,
        listener: CESettingsCacheListener,
        context: Context?
    ) {
        if (context == null) {
            MoPubLog.log(CUSTOM, "Context cannot be null.")
            listener.onSettingsReceived(null)
            return
        }

        val cacheListener = object : DiskLruCacheListener {
            override fun onGetComplete(key: String, content: ByteArray?) {
                if (key != adUnitId) {
                    return
                }

                val settingsFromCache = CreativeExperienceSettings.fromByteArray(content)
                listener.onSettingsReceived(settingsFromCache)
            }
        }

        getFromDiskCacheAsync(
            adUnitId,
            cacheListener,
            supervisorJob,
            context
        )
    }

    /**
     * Fire and forget call to cache CE Settings for an ad unit.
     *
     * @param adUnitId the ad unit id for which to cache CE Settings for (key).
     * @param ceSettings the creative experience settings to cache (value).
     * @param context to initialize the CE Settings cache.
     */
    @JvmStatic
    @AnyThread
    fun putCESettings(
        adUnitId: String,
        ceSettings: CreativeExperienceSettings,
        context: Context?
    ) {
        if (context == null) {
            MoPubLog.log(CUSTOM, "Context cannot be null.")
            return
        }

        putToDiskCacheAsync(
            adUnitId,
            ceSettings.toByteArray(),
            null,
            supervisorJob,
            context
        )
    }

    // Testing
    @JvmStatic
    @WorkerThread
    @VisibleForTesting
    fun clearCESettingsCache() = clearAndNullCache()
}
