// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.simpleadsdemo

import android.content.ContentValues
import android.content.Context
import android.database.Cursor

import com.mopub.common.logging.MoPubLog
import com.mopub.common.logging.MoPubLog.SdkLogEvent
import com.mopub.simpleadsdemo.SampleAppAdUnits.Defaults.getAdUnits

import java.util.ArrayList
import java.util.HashSet

internal class AdUnitDataSource(context: Context) {
    private val context: Context = context.applicationContext
    private val databaseHelper: MoPubSQLiteHelper = MoPubSQLiteHelper(context)
    private val allColumns = arrayOf(
        MoPubSQLiteHelper.COLUMN_ID,
        MoPubSQLiteHelper.COLUMN_AD_UNIT_ID,
        MoPubSQLiteHelper.COLUMN_DESCRIPTION,
        MoPubSQLiteHelper.COLUMN_USER_GENERATED,
        MoPubSQLiteHelper.COLUMN_AD_TYPE,
        MoPubSQLiteHelper.COLUMN_KEYWORDS
    )

    private fun createDefaultSampleAdUnit(sampleAdUnit: MoPubSampleAdUnit): MoPubSampleAdUnit? {
        return createSampleAdUnit(sampleAdUnit, false)
    }

    fun createSampleAdUnit(sampleAdUnit: MoPubSampleAdUnit): MoPubSampleAdUnit? {
        return createSampleAdUnit(sampleAdUnit, true)
    }

    private fun createSampleAdUnit(
        sampleAdUnit: MoPubSampleAdUnit,
        isUserGenerated: Boolean
    ): MoPubSampleAdUnit? {
        deleteAllAdUnitsWithAdUnitIdAndAdTypeAndKeywords(
            sampleAdUnit.adUnitId,
            sampleAdUnit.fragmentClassName,
            sampleAdUnit.keywords,
            isUserGenerated
        )
        val values = ContentValues().apply {
            val userGenerated = if (isUserGenerated) 1 else 0
            put(MoPubSQLiteHelper.COLUMN_AD_UNIT_ID, sampleAdUnit.adUnitId)
            put(MoPubSQLiteHelper.COLUMN_DESCRIPTION, sampleAdUnit.description)
            put(MoPubSQLiteHelper.COLUMN_USER_GENERATED, userGenerated)
            put(MoPubSQLiteHelper.COLUMN_AD_TYPE, sampleAdUnit.fragmentClassName)
            put(MoPubSQLiteHelper.COLUMN_KEYWORDS, sampleAdUnit.keywords)
        }
        val database = databaseHelper.writableDatabase
        val insertId =
            database.insert(MoPubSQLiteHelper.TABLE_AD_CONFIGURATIONS, null, values)
        val cursor = database.query(
            MoPubSQLiteHelper.TABLE_AD_CONFIGURATIONS, allColumns,
            MoPubSQLiteHelper.COLUMN_ID + " = " + insertId, null, null, null, null
        )
        cursor.moveToFirst()
        val newAdConfiguration = cursorToAdConfiguration(cursor)
        cursor.close()
        database.close()
        if (newAdConfiguration != null) {
            MoPubLog.log(
                SdkLogEvent.CUSTOM,
                "Ad configuration added with id: ${newAdConfiguration.id}"
            )
        }
        return newAdConfiguration
    }

    fun deleteSampleAdUnit(adConfiguration: MoPubSampleAdUnit) {
        val id = adConfiguration.id
        databaseHelper.writableDatabase.let {
            it.delete(
                MoPubSQLiteHelper.TABLE_AD_CONFIGURATIONS,
                MoPubSQLiteHelper.COLUMN_ID + " = " + id,
                null
            )
            MoPubLog.log(SdkLogEvent.CUSTOM, "Ad Configuration deleted with id: $id")
            it.close()
        }
    }

    private fun deleteAllAdUnitsWithAdUnitIdAndAdTypeAndKeywords(
        adUnitId: String,
        adType: String,
        keywords: String,
        isUserGenerated: Boolean
    ) {
        val userGenerated = if (isUserGenerated) "1" else "0"
        val database = databaseHelper.writableDatabase
        val numDeletedRows = database.delete(
            MoPubSQLiteHelper.TABLE_AD_CONFIGURATIONS,
            MoPubSQLiteHelper.COLUMN_AD_UNIT_ID + " = '" + adUnitId
                    + "' AND " + MoPubSQLiteHelper.COLUMN_USER_GENERATED + " = " + userGenerated
                    + " AND " + MoPubSQLiteHelper.COLUMN_AD_TYPE + " = '" + adType + "'"
                    + " AND " + MoPubSQLiteHelper.COLUMN_KEYWORDS + " = '" + keywords + "'",
            null
        )
        MoPubLog.log(
            SdkLogEvent.CUSTOM, "$numDeletedRows rows deleted with adUnitId: $adUnitId"
        )
        database.close()
    }

    val allAdUnits: List<MoPubSampleAdUnit>
        get() {
            val adConfigurations: MutableList<MoPubSampleAdUnit> = ArrayList()
            val database = databaseHelper.readableDatabase
            val cursor = database.query(
                MoPubSQLiteHelper.TABLE_AD_CONFIGURATIONS,
                allColumns, null, null, null, null, null
            )
            cursor.moveToFirst()
            while (!cursor.isAfterLast) {
                cursorToAdConfiguration(cursor)?.let {
                    adConfigurations.add(it)
                }
                cursor.moveToNext()
            }
            cursor.close()
            database.close()
            return adConfigurations
        }

    private fun populateDefaultSampleAdUnits() {
        val allAdUnits = HashSet(allAdUnits)
        getAdUnits(context).forEach { defaultAdUnit ->
            if (!allAdUnits.contains(defaultAdUnit)) {
                createDefaultSampleAdUnit(defaultAdUnit)
            }
        }
    }

    private fun cursorToAdConfiguration(cursor: Cursor): MoPubSampleAdUnit? {
        val id = cursor.getLong(0)
        val adUnitId = cursor.getString(1)
        val description = cursor.getString(2)
        val userGenerated = cursor.getInt(3)
        val adType: MoPubSampleAdUnit.AdType? =
            MoPubSampleAdUnit.AdType.fromFragmentClassName(cursor.getString(4))
        val keywords = cursor.getString(5)
        return if (adType == null) {
            null
        } else MoPubSampleAdUnit.Builder(adUnitId, adType)
            .description(description)
            .isUserDefined(userGenerated == 1)
            .keywords(keywords)
            .id(id)
            .build()
    }

    init {
        populateDefaultSampleAdUnits()
    }
}
