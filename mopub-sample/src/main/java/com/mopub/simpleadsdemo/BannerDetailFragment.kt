// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.simpleadsdemo

import com.mopub.mobileads.MoPubView.MoPubAdSize

class BannerDetailFragment : AbstractBannerDetailFragment() {
    override val adSize: MoPubAdSize
        get() = MoPubAdSize.HEIGHT_250
}
