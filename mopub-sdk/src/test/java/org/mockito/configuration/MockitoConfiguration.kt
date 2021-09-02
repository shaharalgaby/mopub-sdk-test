// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package org.mockito.configuration

/**
 * Mockito Configuration that disables the Objenesis cache
 */
class MockitoConfiguration : DefaultMockitoConfiguration() {

    override fun enableClassCache() = false

}
