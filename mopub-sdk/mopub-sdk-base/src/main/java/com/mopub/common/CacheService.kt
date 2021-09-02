// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common

import android.content.Context
import androidx.annotation.AnyThread
import androidx.annotation.WorkerThread

import com.mopub.common.logging.MoPubLog
import com.mopub.common.logging.MoPubLog.SdkLogEvent
import com.mopub.common.util.DeviceUtils
import com.mopub.common.util.Streams
import com.mopub.common.util.Utils

import kotlinx.coroutines.*

import java.io.*

abstract class CacheService(private val uniqueCacheName: String) {

    companion object {
        private const val APP_VERSION = 1
        private const val DISK_CACHE_INDEX = 0
        private const val VALUE_COUNT = 1  // Number of values per cache entry. Must be positive.
    }

    @Volatile
    @VisibleForTesting
    var diskLruCache: DiskLruCache? = null
        protected set

    /**
     * Interface for async get and put operation listeners
     */
    interface DiskLruCacheListener {
        @JvmDefault
        fun onGetComplete(key: String, content: ByteArray?) {
        }

        @JvmDefault
        fun onPutComplete(success: Boolean) {
        }
    }

    @WorkerThread
    fun initializeDiskCache(context: Context?): Boolean {
        if (context == null) {
            return false
        }
        // Double-checked locking to initialize.
        diskLruCache ?: synchronized(CacheService::class) {
            diskLruCache ?: run {
                val cacheDirectory = getDiskCacheDirectory(context) ?: return false
                val diskCacheSizeBytes = DeviceUtils.diskCacheSizeBytes(cacheDirectory)
                try {
                    diskLruCache = DiskLruCache.open(
                        cacheDirectory,
                        APP_VERSION,
                        VALUE_COUNT,
                        diskCacheSizeBytes
                    )
                } catch (e: IOException) {
                    MoPubLog.log(SdkLogEvent.CUSTOM, "Unable to create DiskLruCache", e)
                    return false
                }
            }
        }
        return true
    }

    @WorkerThread
    fun initialize(context: Context?) {
        initializeDiskCache(context)
    }

    @AnyThread
    fun getDiskCacheDirectory(context: Context?): File? {
        val cacheDir = context?.cacheDir ?: return null
        val cachePath = cacheDir.path
        return File("$cachePath${File.separator}$uniqueCacheName")
    }

    @AnyThread
    fun createValidDiskCacheKey(key: String?): String = Utils.sha1(key)

    @WorkerThread
    fun containsKeyDiskCache(key: String?): Boolean {
        return diskLruCache?.let {
            try {
                it.get(createValidDiskCacheKey(key))
            } catch (e: IOException) {
                null
            }
        } != null
    }

    @AnyThread
    fun getFilePathDiskCache(key: String?): String? {
        if (key == null) {
            return null
        }
        return diskLruCache?.let {
            // This violates encapsulation but there is no convenience method to get a filename from
            // DiskLruCache. Filename was derived from private class method Entry#getCleanFile
            // in DiskLruCache.java
            "${it.directory}${File.separator}${createValidDiskCacheKey(key)}.$DISK_CACHE_INDEX"
        }
    }

    @WorkerThread
    fun putToDiskCache(key: String?, content: ByteArray?): Boolean {
        if (key == null || content == null) {
            return false
        }
        return putToDiskCache(key, ByteArrayInputStream(content))
    }

    @WorkerThread
    fun putToDiskCache(key: String?, content: InputStream?): Boolean {
        if (diskLruCache == null || key.isNullOrEmpty() || content == null) {
            return false
        }
        var editor: DiskLruCache.Editor? = null
        try {
            editor = diskLruCache?.edit(createValidDiskCacheKey(key)) ?:
                    // another edit is in progress
                    return false
            BufferedOutputStream(editor.newOutputStream(DISK_CACHE_INDEX)).apply {
                Streams.copyContent(content, this)
                flush()
                close()
            }
            diskLruCache?.flush()
            editor.commit()
        } catch (e: IOException) {
            MoPubLog.log(SdkLogEvent.CUSTOM, "Unable to put to DiskLruCache", e)
            try {
                editor?.abort()
            } catch (ignore: IOException) {
                // ignore
            }
            return false
        }
        return true
    }

    @AnyThread
    fun putToDiskCacheAsync(
        key: String,
        content: ByteArray?,
        listener: DiskLruCacheListener?,
        supervisorJob: CompletableJob,
        context: Context
    ) {
        CoroutineScope(supervisorJob + Dispatchers.IO).launch(
            CoroutineExceptionHandler { coroutineContext, e ->
                coroutineContext.cancel()
                MoPubLog.log(SdkLogEvent.CUSTOM, "Exception in putToDiskCacheAsync", e)
                listener?.onPutComplete(false)
            })
        {
            if (!initializeDiskCache(context)) {
                // Failed to initialize cache
                withContext(supervisorJob + Dispatchers.Main) {
                    MoPubLog.log(SdkLogEvent.CUSTOM, "Failed to initialize cache.")
                    listener?.onPutComplete(false)
                }
                return@launch
            }

            val success = putToDiskCache(key, content)
            withContext(supervisorJob + Dispatchers.Main) {
                listener?.onPutComplete(success)
            }
        }
    }

    @WorkerThread
    fun getFromDiskCache(key: String?): ByteArray? {
        if (diskLruCache == null || key.isNullOrEmpty()) {
            return null
        }
        var bytes: ByteArray? = null
        var snapshot: DiskLruCache.Snapshot? = null
        try {
            snapshot = diskLruCache?.get(createValidDiskCacheKey(key)) ?: return null
            val inStream = snapshot.getInputStream(DISK_CACHE_INDEX)
            inStream?.let {
                bytes = ByteArray(snapshot.getLength(0).toInt())
                val buffIn = BufferedInputStream(it)
                try {
                    Streams.readStream(buffIn, bytes)
                } finally {
                    Streams.closeStream(buffIn)
                    it.close()
                }
            }
        } catch (e: IOException) {
            MoPubLog.log(SdkLogEvent.CUSTOM, "Unable to get from DiskLruCache", e)
        } finally {
            snapshot?.close()
        }
        return bytes
    }

    @AnyThread
    fun getFromDiskCacheAsync(
        key: String,
        listener: DiskLruCacheListener,
        supervisorJob: CompletableJob,
        context: Context
    ) {
        CoroutineScope(supervisorJob + Dispatchers.IO).launch(
            CoroutineExceptionHandler { coroutineContext, e ->
                coroutineContext.cancel()
                MoPubLog.log(SdkLogEvent.CUSTOM, "Exception in getFromDiskCacheAsync", e)
                listener.onGetComplete(key, null)
            })
        {
            if (!initializeDiskCache(context)) {
                // Failed to initialize cache
                withContext(supervisorJob + Dispatchers.Main) {
                    MoPubLog.log(SdkLogEvent.CUSTOM, "Failed to initialize cache.")
                    listener.onGetComplete(key, null)
                }
                return@launch
            }

            val result = getFromDiskCache(key)
            withContext(supervisorJob + Dispatchers.Main) {
                listener.onGetComplete(key, result)
            }
        }
    }

    @WorkerThread
    @VisibleForTesting
    fun clearAndNullCache() {
        if (diskLruCache != null) {
            diskLruCache = try {
                diskLruCache?.delete()
                null
            } catch (ignore: IOException) {
                null
            }
        }
    }
}
