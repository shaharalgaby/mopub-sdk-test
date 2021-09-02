// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common;

import androidx.annotation.NonNull;

import com.iab.omid.library.mopub.adsession.FriendlyObstructionPurpose;

/**
 * OM SDK requirement to identify the purpose of every object partially covering tracked Ad.
 */
public enum ViewabilityObstruction {
    VIDEO_CONTROLS(FriendlyObstructionPurpose.VIDEO_CONTROLS),
    CLOSE_BUTTON(FriendlyObstructionPurpose.CLOSE_AD),
    CTA_BUTTON(FriendlyObstructionPurpose.OTHER),
    SKIP_BUTTON(FriendlyObstructionPurpose.OTHER),
    INDUSTRY_ICON(FriendlyObstructionPurpose.OTHER),
    COUNTDOWN_TIMER(FriendlyObstructionPurpose.OTHER),
    OVERLAY(FriendlyObstructionPurpose.OTHER),
    BLUR(FriendlyObstructionPurpose.OTHER),
    PROGRESS_BAR(FriendlyObstructionPurpose.OTHER),
    NOT_VISIBLE(FriendlyObstructionPurpose.NOT_VISIBLE),
    OTHER(FriendlyObstructionPurpose.OTHER);

    @NonNull
    FriendlyObstructionPurpose value;

    ViewabilityObstruction(@NonNull final FriendlyObstructionPurpose purpose) {
        this.value = purpose;
    }
}
