// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.simpleadsdemo

import android.content.Context

import com.mopub.common.logging.MoPubLog
import com.mopub.common.logging.MoPubLog.SdkLogEvent
import com.mopub.mobileads.MoPubErrorCode

/**
 * Used to intercept logs so that we can view logs at a lower level
 * than Verbose (ie. Level.FINEST). This will show a toast when we
 * receive a matching error from the mopub sdk.
 */
object LoggingUtils {
    private var enabled = false

    /**
     * Makes it so that this app can intercept Level.FINEST log messages.
     * This is not thread safe.
     *
     * @param context Needs a context to send toasts.
     */
    fun enableCanaryLogging(context: Context) {
        if (enabled) {
            return
        }
        MoPubLog.addLogger({ className, methodName, identifier, message ->
            if (MoPubErrorCode.WARMUP.toString() == message) {
                Utils.logToast(
                    context,
                    MoPubErrorCode.WARMUP.toString()
                )
            }
            // Toasts the no connection message if a native ad failed due to no internet
            if (MoPubErrorCode.NO_CONNECTION.toString() == message) {
                Utils.logToast(
                    context,
                    MoPubErrorCode.NO_CONNECTION.toString()
                )
            }
        }, MoPubLog.LogLevel.INFO)

        // Share ad url logger
        MoPubLog.addLogger({ className, methodName, identifier, message ->
            message?.let { mes ->
                if(LastAdRequestUrlManager.isAdRequestLogMessage(mes)) {
                    LastAdRequestUrlManager.updateAdRequestUrl(mes)
                }
            }
        }, MoPubLog.LogLevel.DEBUG)

        MoPubLog.log(SdkLogEvent.CUSTOM, "Setting up MoPubLog")
        enabled = true
    }
}
