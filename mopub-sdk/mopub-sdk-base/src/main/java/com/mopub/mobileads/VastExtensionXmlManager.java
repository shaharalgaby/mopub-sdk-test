// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;

import com.mopub.common.Preconditions;
import com.mopub.mobileads.util.XmlUtils;

import org.w3c.dom.Node;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This XML manager handles Extension nodes.
 */
public class VastExtensionXmlManager {
    // Elements
    public static final String VIDEO_VIEWABILITY_TRACKER = "MoPubViewabilityTracker";

    // Attributes
    public static final String TYPE = "type";
    public static final String ID = "id";

    final Node mExtensionNode;

    public VastExtensionXmlManager(@NonNull Node extensionNode) {
        Preconditions.checkNotNull(extensionNode);

        this.mExtensionNode = extensionNode;
    }

    /**
     * If there is an Extension node with a MoPubViewabilityTracker element, return its data object.
     *
     * @return The {@link VideoViewabilityTracker} parsed from the given node or null if missing or
     * invalid.
     */
    @Nullable
    VideoViewabilityTracker getVideoViewabilityTracker() {
        Node videoViewabilityTrackerNode =
                XmlUtils.getFirstMatchingChildNode(mExtensionNode, VIDEO_VIEWABILITY_TRACKER);
        if (videoViewabilityTrackerNode == null) {
            return null;
        }

        VideoViewabilityTrackerXmlManager videoViewabilityTrackerXmlManager =
                new VideoViewabilityTrackerXmlManager(videoViewabilityTrackerNode);
        Integer viewablePlaytime = videoViewabilityTrackerXmlManager.getViewablePlaytimeMS();
        Integer percentViewable = videoViewabilityTrackerXmlManager.getPercentViewable();
        String videoViewabilityTrackerUrl =
                videoViewabilityTrackerXmlManager.getVideoViewabilityTrackerUrl();

        if (viewablePlaytime == null || percentViewable == null
                || TextUtils.isEmpty(videoViewabilityTrackerUrl)) {
            return null;
        }

        return new VideoViewabilityTracker.Builder(videoViewabilityTrackerUrl,
                viewablePlaytime, percentViewable).build();

 
    }

    /**
     * If the node has a "type" attribute, return its value.
     *
     * @return A String with the value of the "type" attribute or null if missing.
     */
    @Nullable
    String getType() {
        return XmlUtils.getAttributeValue(mExtensionNode, TYPE);
    }
}
