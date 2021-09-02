// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common

import android.content.Context
import androidx.annotation.AnyThread
import androidx.annotation.WorkerThread

import java.io.InputStream

object VideoCacheService : CacheService("mopub-video-cache") {

    @JvmStatic
    @WorkerThread
    fun initializeCache(context: Context?) = initializeDiskCache(context)

    @JvmStatic
    @WorkerThread
    fun containsKey(key: String?) = containsKeyDiskCache(key)

    @JvmStatic
    @WorkerThread
    fun get(key: String?): ByteArray? = getFromDiskCache(key)

    @JvmStatic
    @AnyThread
    fun getFilePath(key: String?): String? = getFilePathDiskCache(key)

    @JvmStatic
    @WorkerThread
    fun put(key: String?, content: InputStream?): Boolean =
        putToDiskCache(key, content)

    @JvmStatic
    @WorkerThread
    fun put(key: String?, content: ByteArray?): Boolean =
        putToDiskCache(key, content)

    // Testing
    @JvmStatic
    @VisibleForTesting
    fun getVideoCache() = diskLruCache

    @JvmStatic
    @WorkerThread
    @VisibleForTesting
    fun clearAndNullVideoCache() = clearAndNullCache()
}
