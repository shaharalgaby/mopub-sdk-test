// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.test.support

import com.mopub.common.Preconditions
import com.mopub.common.util.AsyncTasks

import org.junit.runners.model.InitializationError
import org.mockito.MockitoAnnotations
import org.robolectric.DefaultTestLifecycle
import org.robolectric.RobolectricTestRunner
import org.robolectric.TestLifecycle
import org.robolectric.android.util.concurrent.RoboExecutorService

class NetworkingTestRunner @Throws(InitializationError::class) constructor(testClass: Class<*>?) :
    RobolectricTestRunner(testClass) {

    override fun getTestLifecycleClass(): Class<out TestLifecycle<*>?> {
        return TestLifeCycleWithInjection::class.java
    }

    class TestLifeCycleWithInjection : DefaultTestLifecycle() {
        override fun prepareTest(test: Any) {
            // Precondition exceptions should not be thrown during tests so that we can test
            // for unexpected behavior even after failing a precondition.
            Preconditions.NoThrow.setStrictMode(false)

            MockitoAnnotations.initMocks(test)

            AsyncTasks.setExecutor(RoboExecutorService())
        }
    }
}
