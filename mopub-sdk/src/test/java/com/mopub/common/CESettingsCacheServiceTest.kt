// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common

import android.app.Activity
import android.content.Context

import com.mopub.common.CESettingsCacheService.CESettingsCacheListener
import com.mopub.common.test.support.SdkTestRunner
import com.mopub.mobileads.CreativeExperienceSettings
import com.mopub.mobileads.CreativeExperienceSettingsParser
import com.mopub.mobileads.CreativeExperienceSettingsParserTest

import junit.framework.Assert.assertEquals

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import java.util.concurrent.Semaphore

@RunWith(SdkTestRunner::class)
class CESettingsCacheServiceTest {

    private val adUnitId = "adUnitId"
    private var cachedSettings : CreativeExperienceSettings? = null
    private var cachedHash : String? = null
    private lateinit var responseSettings : CreativeExperienceSettings
    private lateinit var context: Context
    private lateinit var semaphore: Semaphore

    @ObsoleteCoroutinesApi
    private val mainThreadSurrogate = newSingleThreadContext("UI thread")

    private val listener = object : CESettingsCacheListener {
        override fun onSettingsReceived(settings: CreativeExperienceSettings?) {
            cachedSettings = settings
            semaphore.release()
        }

        override fun onHashReceived(hash: String) {
            cachedHash = hash
            semaphore.release()
        }
    }

    @ExperimentalCoroutinesApi
    @ObsoleteCoroutinesApi
    @Before
    fun setUp() {
        context =  Robolectric.buildActivity(Activity::class.java).create().get()
        responseSettings = CreativeExperienceSettingsParser.parse(
            CreativeExperienceSettingsParserTest.ceSettingsJSONObject,
            false
        )
        semaphore = Semaphore(0)

        Dispatchers.setMain(mainThreadSurrogate)
    }

    @ExperimentalCoroutinesApi
    @ObsoleteCoroutinesApi
    @After
    fun tearDown() {
        CESettingsCacheService.clearCESettingsCache()
        cachedSettings = null
        cachedHash = null

        Dispatchers.resetMain() // reset main dispatcher to the original Main dispatcher
        mainThreadSurrogate.close()
    }

    @Test
    fun getCESettingsHash_withNullContext_shouldCallOnHashReceivedWithZeroHash() {
        CESettingsCacheService.getCESettingsHash(adUnitId, listener, null)

        assertEquals("0", cachedHash)
    }

    @Test
    fun getCESettingsHash_shouldCallOnHashReceivedWithCachedHash() {
        val ceSettingsBytes = CreativeExperienceSettingsParser.parse(
            CreativeExperienceSettingsParserTest.ceSettingsJSONObject,
            false
        ).toByteArray()
        CESettingsCacheService.initializeDiskCache(context)
        CESettingsCacheService.putToDiskCache(adUnitId, ceSettingsBytes)

        CESettingsCacheService.getCESettingsHash(adUnitId, listener, context)
        semaphore.acquire()

        assertEquals("12345", cachedHash)
    }

    @Test
    fun getCESettings_withNullContext_shouldCallOnSettingsReceivedNull() {
        // Set cachedSettings so we can verify the listener calls onSettingsReceived(null)
        cachedSettings = CreativeExperienceSettings.getDefaultSettings(false)

        CESettingsCacheService.getCESettings(
            adUnitId,
            listener,
            null
        )

        assertEquals(null, cachedSettings)
    }

    @Test
    fun getCESettings_shouldCallOnSettingsReceivedWithCachedSettings() {
        val ceSettingsBytes = responseSettings.toByteArray()
        CESettingsCacheService.initializeDiskCache(context)
        CESettingsCacheService.putToDiskCache(adUnitId, ceSettingsBytes)

        CESettingsCacheService.getCESettings(
            adUnitId,
            listener,
            context
        )
        semaphore.acquire()

        assertEquals(responseSettings, cachedSettings)
    }
}
