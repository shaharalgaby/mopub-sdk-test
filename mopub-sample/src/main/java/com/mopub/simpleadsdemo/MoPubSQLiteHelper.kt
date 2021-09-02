// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.simpleadsdemo

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

internal class MoPubSQLiteHelper(context: Context?) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    override fun onCreate(sqLiteDatabase: SQLiteDatabase) {
        sqLiteDatabase.execSQL(DATABASE_CREATE)
    }

    override fun onDowngrade(
        database: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {
        Log.w(
            MoPubSQLiteHelper::class.java.name,
            "Downgrading database from version $oldVersion to $newVersion which will " +
                    "destroy all old data"
        )
        recreateDb(database)
    }

    override fun onUpgrade(
        database: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {
        if (oldVersion == 3 && newVersion == 4) {
            addStringColumn(database, COLUMN_KEYWORDS, "")
        } else {
            Log.w(
                MoPubSQLiteHelper::class.java.name,
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data"
            )
            recreateDb(database)
        }
    }

    private fun recreateDb(database: SQLiteDatabase) {
        database.execSQL("DROP TABLE IF EXISTS $TABLE_AD_CONFIGURATIONS")
        onCreate(database)
    }

    private fun addStringColumn(
        database: SQLiteDatabase,
        columnName: String,
        defaultValue: String
    ) {
        database.execSQL(
            "alter table " + TABLE_AD_CONFIGURATIONS
                    + " add column " + columnName
                    + " text default \"" + defaultValue + "\""
        )
    }

    companion object {
        const val TABLE_AD_CONFIGURATIONS = "adConfigurations"
        const val COLUMN_ID = "_id"
        const val COLUMN_AD_UNIT_ID = "adUnitId"
        const val COLUMN_DESCRIPTION = "description"
        const val COLUMN_USER_GENERATED = "userGenerated"
        const val COLUMN_AD_TYPE = "adType"
        const val COLUMN_KEYWORDS = "keywords"
        private const val DATABASE_NAME = "savedConfigurations.db"
        private const val DATABASE_VERSION = 4
        private const val DATABASE_CREATE =
            ("create table " + TABLE_AD_CONFIGURATIONS
                    + " ("
                    + COLUMN_ID + " integer primary key autoincrement, "
                    + COLUMN_AD_UNIT_ID + " text not null, "
                    + COLUMN_DESCRIPTION + " text not null, "
                    + COLUMN_USER_GENERATED + " integer not null, "
                    + COLUMN_AD_TYPE + " text not null,"
                    + COLUMN_KEYWORDS + " text not null"
                    + ");")
    }
}
