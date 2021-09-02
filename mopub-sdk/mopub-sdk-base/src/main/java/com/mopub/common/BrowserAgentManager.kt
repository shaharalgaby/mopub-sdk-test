// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common

import com.mopub.common.logging.MoPubLog
import com.mopub.common.logging.MoPubLog.SdkLogEvent

object BrowserAgentManager {
    /**
     * Browser agent to handle URIs
     */
    enum class BrowserAgent {
        /**
         * MoPub's in-app browser
         */
        IN_APP,

        /**
         * Default browser application on device
         */
        NATIVE;

        companion object {
            /**
             * Maps header value from MoPub's AdServer to browser agent:
             * 0 is MoPub's in-app browser (IN_APP), and 1 is device's default browser (NATIVE).
             * For null or all other undefined values, returns default browser agent IN_APP.
             * @param browserAgent Integer header value from MoPub's AdServer.
             * @return IN_APP for 0, NATIVE for 1, and IN_APP for null or all other undefined values.
             */
            @JvmStatic
            fun fromHeader(browserAgent: Int?): BrowserAgent {
                return when(browserAgent) {
                    1 -> NATIVE
                    else -> IN_APP
                }
            }
        }
    }

    @Volatile
    private var browserAgent: BrowserAgent = BrowserAgent.IN_APP

    @JvmStatic
    @VisibleForTesting
    @Volatile
    var isBrowserAgentOverriddenByClient = false
        private set

    @JvmStatic
    fun getBrowserAgent() = browserAgent

    @JvmStatic
    @VisibleForTesting
    fun setBrowserAgent(browserAgent: BrowserAgent) {
        this.browserAgent = browserAgent
        isBrowserAgentOverriddenByClient = true
    }

    @JvmStatic
    fun setBrowserAgentFromAdServer(adServerBrowserAgent: BrowserAgent) {
        if (isBrowserAgentOverriddenByClient) {
            MoPubLog.log(SdkLogEvent.CUSTOM, "Browser agent already overridden by client with value $browserAgent")
        } else {
            browserAgent = adServerBrowserAgent
        }
    }

    @Deprecated("")
    @JvmStatic
    @VisibleForTesting
    fun resetBrowserAgent() {
        browserAgent = BrowserAgent.IN_APP
        isBrowserAgentOverriddenByClient = false
    }
}
