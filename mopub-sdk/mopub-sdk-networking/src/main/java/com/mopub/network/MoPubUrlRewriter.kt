// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.network

/**
 * Interface that all Url Rewriters need to implement
 */
interface MoPubUrlRewriter {
    fun rewriteUrl(url: String) : String {
        return url
    }
}
