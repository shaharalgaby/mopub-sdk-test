// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.network

import com.mopub.common.BrowserAgentManager.BrowserAgent
import com.mopub.common.BrowserAgentManager.getBrowserAgent
import com.mopub.common.BrowserAgentManager.isBrowserAgentOverriddenByClient
import com.mopub.common.BrowserAgentManager.resetBrowserAgent
import com.mopub.common.BrowserAgentManager.setBrowserAgent
import com.mopub.common.BrowserAgentManager.setBrowserAgentFromAdServer
import com.mopub.common.test.support.SdkTestRunner

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(SdkTestRunner::class)
class BrowserAgentManagerTest {

    @Before
    fun setup() {
        resetBrowserAgent()
    }

    @Test
    fun setBrowserAgent_withDefaultValue_shouldNotChangeBrowserAgent_shouldSetOverriddenFlag() {
        setBrowserAgent(BrowserAgent.IN_APP)
        assertEquals(BrowserAgent.IN_APP, getBrowserAgent())
        assertTrue(isBrowserAgentOverriddenByClient)
    }

    @Test
    fun setBrowserAgent_withNonDefaultValue_shouldChangeBrowserAgent_shouldSetOverriddenFlag() {
        setBrowserAgent(BrowserAgent.NATIVE)
        assertEquals(BrowserAgent.NATIVE, getBrowserAgent())
        assertTrue(isBrowserAgentOverriddenByClient)
    }

    @Test
    fun setBrowserAgentFromAdServer_whenNotAlreadyOverriddenByClient_shouldSetBrowserAgentFromAdServer() {
        setBrowserAgentFromAdServer(BrowserAgent.NATIVE)
        assertEquals(BrowserAgent.NATIVE, getBrowserAgent())
        assertFalse(isBrowserAgentOverriddenByClient)
    }

    @Test
    fun setBrowserAgentFromAdServer_whenAlreadyOverriddenByClient_shouldNotChangeBrowserAgent() {
        setBrowserAgent(BrowserAgent.NATIVE)
        setBrowserAgentFromAdServer(BrowserAgent.IN_APP)
        assertEquals(BrowserAgent.NATIVE, getBrowserAgent())
        assertTrue(isBrowserAgentOverriddenByClient)
    }
}
