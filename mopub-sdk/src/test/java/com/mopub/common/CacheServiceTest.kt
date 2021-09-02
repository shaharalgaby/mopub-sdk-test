// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common

import android.app.Activity

import com.mopub.common.CacheService.DiskLruCacheListener
import com.mopub.common.test.support.SdkTestRunner

import junit.framework.Assert.*

import kotlinx.coroutines.*
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

import org.fest.assertions.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Matchers
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PowerMockIgnore
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.rule.PowerMockRule
import org.robolectric.Robolectric

import java.io.File
import java.io.InputStream
import java.util.concurrent.Semaphore

@RunWith(SdkTestRunner::class)
@PowerMockIgnore("org.mockito.*", "org.robolectric.*", "android.*", "org.json.*")
@PrepareForTest(CacheService::class)
class CacheServiceTest {
    private lateinit var context: Activity
    private lateinit var key1: String
    private lateinit var data1: String
    private lateinit var asyncGetKey: String
    private var asyncGetValue: ByteArray? = null
    private var asyncPutSuccess: Boolean? = null
    private lateinit var diskCacheListener: DiskLruCacheListener
    private lateinit var semaphore: Semaphore
    private var supervisorJob = SupervisorJob()

    @ObsoleteCoroutinesApi
    private val mainThreadSurrogate = newSingleThreadContext("UI thread")

    @get:Rule
    var rule = PowerMockRule()

    @ExperimentalCoroutinesApi
    @ObsoleteCoroutinesApi
    @Before
    @Throws(Exception::class)
    fun setUp() {
        context = Robolectric.buildActivity(Activity::class.java).create().get()
        key1 = "https://www.mopub.com/"
        data1 = "image_data_1"
        testCacheService = object : CacheService(cacheName) {}
        semaphore = Semaphore(0)
        diskCacheListener = Mockito.mock(DiskLruCacheListener::class.java)

        Mockito.doAnswer { invocationOnMock ->
            val args = invocationOnMock.arguments
            this.asyncGetKey = args[0] as String
            this.asyncGetValue = args[1] as? ByteArray
            semaphore.release()
            null
        }.`when`(diskCacheListener).onGetComplete(
            Matchers.anyString(),
            Matchers.any(ByteArray::class.java)
        )

        Mockito.doAnswer { invocationOnMock ->
            this.asyncPutSuccess = invocationOnMock.arguments[0] as Boolean
            semaphore.release()
            null
        }.`when`(diskCacheListener).onPutComplete(
            Matchers.anyBoolean()
        )

        Dispatchers.setMain(mainThreadSurrogate)
    }

    @ObsoleteCoroutinesApi
    @ExperimentalCoroutinesApi
    @After
    fun tearDown() {
        testCacheService.clearAndNullCache()

        Dispatchers.resetMain() // reset main dispatcher to the original Main dispatcher
        mainThreadSurrogate.close()
    }

    @Test
    fun initializeDiskCache_withNullCacheDirectory_shouldNotCreateCache_shouldReturnFalse() {
        val mockContext = Mockito.mock(Activity::class.java)
        `when`(mockContext.cacheDir).thenReturn(null)

        assertFalse(testCacheService.initializeDiskCache(mockContext))

        assertNull(testCacheService.diskLruCache)
    }

    @Test
    fun initializeDiskCache_withNullContext__shouldNotCreateCache_shouldReturnFalse() {
        assertFalse(testCacheService.initializeDiskCache(null))

        assertNull(testCacheService.diskLruCache)
    }

    @Test
    fun initializeDiskCache_withValidContext_shouldCreateCache_shouldReturnTrue() {
        assertTrue(testCacheService.initializeDiskCache(context))

        assertNotNull(testCacheService.diskLruCache)
    }

    @Test
    fun initialize_withValidContext_shouldCreateNewCachesIdempotently() {
        assertNull(testCacheService.diskLruCache)

        testCacheService.initialize(context)

        val diskLruCache = testCacheService.diskLruCache
        assertNotNull(diskLruCache)

        testCacheService.initialize(context)

        assertEquals(diskLruCache, testCacheService.diskLruCache)
    }

    @Test
    fun getDiskCacheDirectory_withNullContext_shouldReturnNull() {
        val file = testCacheService.getDiskCacheDirectory(null)

        assertEquals(null, file)
    }

    @Test
    fun getDiskCacheDirectory_whenCacheDirIsNull_shouldReturnNull() {
        val mockContext = Mockito.mock(Activity::class.java)
        `when`(mockContext.cacheDir).thenReturn(null)

        assertNull(testCacheService.getDiskCacheDirectory(mockContext))
    }

    @Test
    fun getDiskCacheDirectory_withValidContext_shouldReturnValidCacheDirectory() {
        val file = testCacheService.getDiskCacheDirectory(context)

        assertEquals(
            "${context.cacheDir}/mopub-mock-cache",
            file?.absolutePath
        )
    }

    @Test
    fun createValidDiskLruCacheKey_withNullKey_shouldReturnEmptyString() {
        testCacheService.initialize(context)

        assertEquals("", testCacheService.createValidDiskCacheKey(null))
    }

    @Test
    fun containsKeyDiskCache_whenDiskCacheIsNull_shouldReturnFalse() {
        assertNull(testCacheService.diskLruCache)

        assertFalse(testCacheService.containsKeyDiskCache(key1))
    }

    @Test
    fun containsKeyDiskCache_withKeyNotInCache_shouldReturnFalse() {
        testCacheService.initialize(context)
        testCacheService.putToDiskCache(key1, data1.toByteArray())

        assertFalse(testCacheService.containsKeyDiskCache("key2"))
    }

    @Test
    fun containsKeyDiskCache_withKeyInCache_shouldReturnTrue() {
        testCacheService.initialize(context)
        testCacheService.putToDiskCache(key1, data1.toByteArray())

        assertTrue(testCacheService.containsKeyDiskCache(key1))
    }

    @Test
    fun getFilePathDiskCache_whenDiskCacheIsNull_returnsNull() {
        assertNull(testCacheService.diskLruCache)

        assertNull(testCacheService.getFilePathDiskCache(key1))
    }

    @Test
    fun getFilePathDiskCache_withNullKey_returnsNull() {
        testCacheService.initialize(context)

        assertNull(testCacheService.getFilePathDiskCache(null))
    }

    @Test
    fun getFilePathDiskCache_withValidKey_returnsValidFilePath() {
        testCacheService.initialize(context)

        val filePath = testCacheService.getFilePathDiskCache(key1)

        val expectedPath =
            "${testCacheService.diskLruCache?.directory}${File.separator}" +
                    "${testCacheService.createValidDiskCacheKey(key1)}.0"
        assertEquals(expectedPath, filePath)
    }

    @Test
    fun putToDiskCache_whenDiskCacheIsNull_shouldNotPut_shouldReturnFalse() {
        // null produces an empty string key which is invalid for disk lru cache
        testCacheService.initialize(context)

        val putSuccess = testCacheService.putToDiskCache(null, data1.toByteArray())

        assertFalse(putSuccess)
        assertCachesAreEmpty()
    }

    @Test
    fun putToDiskCache_withNullContent_shouldNotPut_shouldReturnFalse() {
        testCacheService.initialize(context)

        val inStream: InputStream? = null
        val putSuccess = testCacheService.putToDiskCache(key1, inStream)

        assertCachesAreEmpty()
        assertFalse(putSuccess)
    }

    @Test
    fun putToDiskCache_withEmptyKey_shouldNotPut_shouldReturnFalse() {
        testCacheService.initialize(context)

        val putSuccess = testCacheService.putToDiskCache("", data1.toByteArray())

        assertFalse(putSuccess)
    }

    @Test
    fun putToDiskCache_withNullKey_shouldNotPut_shouldReturnFalse() {
        testCacheService.initialize(context)

        val putSuccess = testCacheService.putToDiskCache(null, data1.toByteArray())

        assertFalse(putSuccess)
        assertCachesAreEmpty()
    }

    @Test
    fun putToDiskCache_withValidKeyAndValue_shouldPut_shouldReturnTrue() {
        testCacheService.initialize(context)

        val putSuccess = testCacheService.putToDiskCache(key1, data1.toByteArray())

        assertTrue(putSuccess)
        val getValue = testCacheService.getFromDiskCache(key1)
        assertTrue(data1.toByteArray().contentEquals(getValue))
    }

    @Test
    fun putToDiskCacheAsync_shouldPut_shouldCallOnPutCompleteTrue() {
        testCacheService.putToDiskCacheAsync(
            key1,
            data1.toByteArray(),
            diskCacheListener,
            supervisorJob,
            context
        )
        semaphore.acquire()

        assertTrue(data1.toByteArray().contentEquals(testCacheService.getFromDiskCache(key1)))
        // assertEquals instead of assertTrue because asyncPutSuccess is a Boolean?
        assertEquals(true, asyncPutSuccess)
    }

    @Test
    fun putToDiskCacheAsync_withNullContent_shouldNotPut_shouldCallOnPutCompleteFalse() {
        testCacheService.putToDiskCacheAsync(
            key1,
            null,
            diskCacheListener,
            supervisorJob,
            context
        )
        semaphore.acquire()

        assertCachesAreEmpty()
        // assertEquals instead of assertFalse because asyncPutSuccess is a Boolean?
        assertEquals(false, asyncPutSuccess)
    }

    @Test
    fun putToDiskCacheAsync_whenCacheInitializationFails_shouldNotPut_shouldCallOnPutCompleteFalse() {
        PowerMockito.stub<Boolean>(
            PowerMockito.method(
                CacheService::class.java,
                "initializeDiskCache"
            )
        ).toReturn(false)

        testCacheService.putToDiskCacheAsync(
            key1,
            data1.toByteArray(),
            diskCacheListener,
            supervisorJob,
            context
        )
        semaphore.acquire()

        assertNull(testCacheService.diskLruCache)
        // assertEquals instead of assertFalse because asyncPutSuccess is a Boolean?
        assertEquals(false, asyncPutSuccess)
    }

    @Test
    @Throws(Exception::class)
    fun getFromDiskCache_whenCacheIsNull_shouldReturnNull() {
        assertNull(testCacheService.diskLruCache)

        assertNull(testCacheService.getFromDiskCache(key1))
    }

    @Test
    @Throws(Exception::class)
    fun getFromDiskCache_withNullKey_shouldReturnNull() {
        testCacheService.initialize(context)

        assertNull(testCacheService.getFromDiskCache(null))
    }

    @Test
    @Throws(Exception::class)
    fun getFromDiskCache_withEmptyKey_shouldReturnNull() {
        testCacheService.initialize(context)

        assertNull(testCacheService.getFromDiskCache(""))
    }

    @Test
    fun getFromDiskCache_whenPopulated_shouldReturnCachedValue() {
        testCacheService.initialize(context)
        testCacheService.putToDiskCache(key1, data1.toByteArray())

        val getValue = testCacheService.getFromDiskCache(key1)
        assertTrue(
            data1.toByteArray().contentEquals(getValue)
        )
    }

    @Test
    fun getFromDiskCacheAsync_whenCacheIsEmpty_shouldCallOnGetCompleteWithNullValue() {
        testCacheService.getFromDiskCacheAsync(
            key1,
            diskCacheListener,
            supervisorJob,
            context
        )
        semaphore.acquire()

        assertEquals(key1, asyncGetKey)
        assertNull(asyncGetValue)
    }

    @Test
    fun getFromDiskCacheAsync_whenPopulated_shouldCallOnGetCompleteWithValue() {
        testCacheService.initializeDiskCache(context)
        testCacheService.putToDiskCache(key1, data1.toByteArray())

        testCacheService.getFromDiskCacheAsync(
            key1,
            diskCacheListener,
            supervisorJob,
            context
        )
        semaphore.acquire()

        assertEquals(key1, asyncGetKey)
        assertTrue(data1.toByteArray().contentEquals(asyncGetValue))
    }

    @Test
    fun getFromDiskCacheAsync_whenCacheInitializationFails_shouldNotGet_shouldCallOnGetCompleteWithNullValue() {
        PowerMockito.stub<Boolean>(
            PowerMockito.method(
                CacheService::class.java,
                "initializeDiskCache"
            )
        ).toReturn(false)

        testCacheService.getFromDiskCacheAsync(
            key1,
            diskCacheListener,
            supervisorJob,
            context
        )
        semaphore.acquire()

        assertEquals(key1, asyncGetKey)
        assertNull(asyncGetValue)
    }

    companion object {
        const val cacheName = "mopub-mock-cache"
        private lateinit var testCacheService: CacheService

        fun assertCachesAreEmpty() {
            assertThat<DiskLruCache>(testCacheService.diskLruCache).isNotNull
            assertThat(testCacheService.diskLruCache?.size()).isEqualTo(0)
        }
    }
}
