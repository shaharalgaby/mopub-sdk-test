// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.Preconditions;
import com.mopub.mobileads.util.XmlUtils;

import org.w3c.dom.Node;

/**
 * This XML manager handles the actual video.
 */
class VastMediaXmlManager {

    // Attribute names
    private static final String WIDTH = "width";
    private static final String HEIGHT = "height";
    private static final String DELIVERY = "delivery";
    private static final String VIDEO_TYPE  = "type";
    private static final String BITRATE = "bitrate";
    private static final String BITRATE_MIN = "minBitrate";
    private static final String BITRATE_MAX = "maxBitrate";

    @NonNull private final Node mMediaNode;

    VastMediaXmlManager(@NonNull final Node mediaNode) {
        Preconditions.checkNotNull(mediaNode, "mediaNode cannot be null");
        mMediaNode = mediaNode;
    }

    /**
     * 'progressive' for progressive download (e.g. HTTP) or 'streaming' for streaming protocols
     * or {@code null} if not specified. MoPub expects to download the video. This is a required
     * attribute.
     *
     * @return String of delivery type or {@code null}
     */
    @Nullable
    String getDelivery() {
        return XmlUtils.getAttributeValue(mMediaNode, DELIVERY);
    }

    /**
     * Expected width of the video in pixels or {@code null} if not specified. This is a
     * required attribute.
     *
     * @return Integer width of video or {@code null}
     */
    @Nullable
    Integer getWidth() {
        return XmlUtils.getAttributeValueAsInt(mMediaNode, WIDTH);
    }

    /**
     * Expected height of the video in pixels or {@code null} if not specified. This is a
     * required attribute.
     *
     * @return Integer height of video or {@code null}
     */
    @Nullable
    Integer getHeight() {
        return XmlUtils.getAttributeValueAsInt(mMediaNode, HEIGHT);
    }

    /**
     * The MIME file type of the video or {@code null} if not specified. This is a required
     * attribute. (e.g. 'video/x-flv' or 'video/mp4').
     *
     * @return The String type or {@code null}
     */
    @Nullable
    String getType() {
        return XmlUtils.getAttributeValue(mMediaNode, VIDEO_TYPE);
    }

    /**
     * The URL of the video or {@code null} if not specified.
     *
     * @return String url of video or {@code null}
     */
    @Nullable
    String getMediaUrl() {
        return XmlUtils.getNodeValue(mMediaNode);
    }

    /**
     * The Bitrate of the video or {@code null} if not specified.
     *
     * @return Integer representation of the video in kbps or {@code null}
     */
    @Nullable
    Integer getBitrate() {
        // the "bitrate" attribute is the average across the entire video:
        final Integer bitrate = XmlUtils.getAttributeValueAsInt(mMediaNode, BITRATE);

        if (bitrate != null) {
            return bitrate;
        }

        // If an average bitrate isn't provided:
        final Integer minBitrate = XmlUtils.getAttributeValueAsInt(mMediaNode, BITRATE_MIN);
        final Integer maxBitrate = XmlUtils.getAttributeValueAsInt(mMediaNode, BITRATE_MAX);

        // Use the min and max to calculate the average, if both are non-null:
        if (minBitrate != null && maxBitrate != null) {
            return (minBitrate + maxBitrate) / 2;
        }

        // If only minBitrate is non-null:
        if (minBitrate != null) {
            return minBitrate;
        }

        // Return maxBitrate since at this point we would return null anyway if it's null:
        return maxBitrate;

    }
}
