// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import androidx.annotation.NonNull;

import org.w3c.dom.Node;

/**
 * This XML manager handles InLine nodes. An InLine node can contain impression trackers,
 * video ads, and companion ads.
 */
class VastInLineXmlManager extends VastBaseInLineWrapperXmlManager {

    VastInLineXmlManager(@NonNull final Node inLineNode) {
        super(inLineNode);
    }
}
