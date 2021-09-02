// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads

import android.os.Handler

/**
 * A runnable that is used to update a [VastVideoViewController]'s countdown display according
 * to rules contained in the [VastVideoViewController]
 */
open class VastVideoViewCountdownRunnable(
    private val videoViewController: VastVideoViewController,
    handler: Handler
) : RepeatingHandlerRunnable(handler) {
    override fun doWork() {
        videoViewController.updateCountdown()
    }
}
