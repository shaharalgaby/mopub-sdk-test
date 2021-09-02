// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.simpleadsdemo

data class CallbackDataItem internal constructor(
    /**
     * The name of the callback
     */
    val callbackName: String
) {

    /**
     * Optional additional data to show
     */
    var additionalData: String? = null

    /**
     * Whether or not this callback has been called
     */
    var called = false
}
