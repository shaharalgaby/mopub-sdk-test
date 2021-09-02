// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads

enum class EndCardType {
    INTERACTIVE, // HTML and MRAID
    STATIC, // image types such as IFrame, javascript, and image urls
    NONE; // blurred last frame (VAST)

    companion object {
        @JvmStatic
        fun fromVastResourceType(vastResourceType: VastResource.Type?): EndCardType? {
            return when (vastResourceType) {
                VastResource.Type.HTML_RESOURCE -> INTERACTIVE
                VastResource.Type.STATIC_RESOURCE -> STATIC
                VastResource.Type.IFRAME_RESOURCE -> STATIC
                VastResource.Type.BLURRED_LAST_FRAME -> NONE
                else -> null
            }
        }
    }
}
