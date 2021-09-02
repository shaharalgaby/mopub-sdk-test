// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import androidx.annotation.NonNull;

import static com.mopub.mobileads.MoPubView.BannerAdListener;

public class DefaultBannerAdListener implements BannerAdListener {
    @Override public void onBannerLoaded(@NonNull MoPubView banner) { }
    @Override public void onBannerFailed(MoPubView banner, MoPubErrorCode errorCode) { }
    @Override public void onBannerClicked(MoPubView banner) { }
    @Override public void onBannerExpanded(MoPubView banner) { }
    @Override public void onBannerCollapsed(MoPubView banner) { }
}
