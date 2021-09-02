// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.os.Bundle
import androidx.annotation.VisibleForTesting
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.mopub.common.Constants
import com.mopub.common.MoPubBrowser
import com.mopub.common.UrlAction
import com.mopub.common.UrlHandler
import com.mopub.common.UrlHandler.ResultActions
import com.mopub.common.logging.MoPubLog
import com.mopub.common.logging.MoPubLog.SdkLogEvent.CUSTOM
import com.mopub.common.util.Intents
import com.mopub.network.TrackingRequest.makeVastTrackingHttpRequest
import java.io.Serializable
import kotlin.math.abs

@VisibleForTesting
open class VastCompanionAdConfig(
    @Expose @SerializedName(Constants.VAST_WIDTH)
    val width: Int,
    @Expose @SerializedName(Constants.VAST_HEIGHT)
    val height: Int,
    @Expose @SerializedName(Constants.VAST_RESOURCE)
    val vastResource: VastResource,
    @Expose @SerializedName(Constants.VAST_URL_CLICKTHROUGH)
    val clickThroughUrl: String?,
    @Expose @SerializedName(Constants.VAST_TRACKERS_CLICK)
    val clickTrackers: MutableList<VastTracker>,
    @Expose @SerializedName(Constants.VAST_TRACKERS_IMPRESSION)
    val creativeViewTrackers: MutableList<VastTracker>,
    @Expose @SerializedName(Constants.VAST_CUSTOM_TEXT_CTA)
    val customCtaText: String?
) : Serializable {

    companion object {
        private const val serialVersionUID: Long = 3L
    }

    /**
     * Add click trackers.
     *
     * @param clickTrackers List of URLs to hit
     */
    fun addClickTrackers(clickTrackers: Collection<VastTracker>) {
        this.clickTrackers.addAll(clickTrackers)
    }

    /**
     * Add creativeView trackers that are supposed to be fired when the companion ad is visible.
     *
     * @param creativeViewTrackers List of URLs to hit when this companion is viewed
     */
    fun addCreativeViewTrackers(creativeViewTrackers: Collection<VastTracker>) {
        this.creativeViewTrackers.addAll(creativeViewTrackers)
    }

    fun handleImpression(
        context: Context,
        contentPlayHead: Int
    ) {
        makeVastTrackingHttpRequest(
            creativeViewTrackers,
            null,
            contentPlayHead,
            null,
            context
        )
    }

    fun formatScore(): Double {
        return when (vastResource.type) {
            VastResource.Type.STATIC_RESOURCE ->
                if (VastResource.CreativeType.JAVASCRIPT.equals(vastResource.creativeType)) {
                    1.0
                } else if (VastResource.CreativeType.IMAGE.equals(vastResource.creativeType)) {
                    0.8
                } else {
                    0.0
                }
            VastResource.Type.HTML_RESOURCE -> 1.2
            VastResource.Type.IFRAME_RESOURCE -> 1.0
            VastResource.Type.BLURRED_LAST_FRAME -> 0.0
        }
    }

    open fun calculateScore(containerWidth: Int, containerHeight: Int): Double {
        if (containerHeight == 0 || height == 0) {
            return 0.0
        }

        val aspectRatioScore = abs(containerWidth.toDouble() / containerHeight - width.toDouble() / height)
        val widthScore = abs((containerWidth.toDouble() - width) / containerWidth)
        val fitScore = aspectRatioScore + widthScore
        return formatScore() / (1 + fitScore)
    }


    open fun handleClick(
        context: Context,
        requestCode: Int,
        webViewClickThroughUrl: String?,
        dspCreativeId: String?
    ) {
        require(context is Activity) { "context must be an activity" }

        vastResource.getCorrectClickThroughUrl(clickThroughUrl, webViewClickThroughUrl)
            ?.takeIf { url -> url.isNotEmpty() }
            ?.let { url ->
                UrlHandler.Builder()
                    .withSupportedUrlActions(
                        UrlAction.IGNORE_ABOUT_SCHEME,
                        UrlAction.OPEN_APP_MARKET,
                        UrlAction.OPEN_NATIVE_BROWSER,
                        UrlAction.OPEN_IN_APP_BROWSER,
                        UrlAction.HANDLE_SHARE_TWEET,
                        UrlAction.FOLLOW_DEEP_LINK_WITH_FALLBACK,
                        UrlAction.FOLLOW_DEEP_LINK
                    )
                    .withResultActions(object : ResultActions {
                        override fun urlHandlingSucceeded(url: String, urlAction: UrlAction) {
                            if (urlAction == UrlAction.OPEN_IN_APP_BROWSER) {
                                val bundle = Bundle()
                                bundle.run {
                                    putString(MoPubBrowser.DESTINATION_URL_KEY, url)
                                    if (!dspCreativeId.isNullOrEmpty()) {
                                        putString(MoPubBrowser.DSP_CREATIVE_ID, dspCreativeId)
                                    }
                                }

                                val clazz = MoPubBrowser::class.java
                                val intent = Intents.getStartActivityIntent(context, clazz, bundle)
                                try {
                                    context.startActivityForResult(intent, requestCode)
                                } catch (anfe: ActivityNotFoundException) {
                                    MoPubLog.log(
                                        CUSTOM,
                                        "Activity " + clazz.getName() + " not found. Did you " +
                                                "declare it in your AndroidManifest.xml?"
                                    )
                                }
                            }
                        }

                        override fun urlHandlingFailed(
                            url: String,
                            lastFailedUrlAction: UrlAction
                        ) {
                        }
                    })
                    .withDspCreativeId(dspCreativeId)
                    .withoutMoPubBrowser()
                    .build()
                    .handleUrl(context, url)
            }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VastCompanionAdConfig) return false

        if (width != other.width) return false
        if (height != other.height) return false
        if (vastResource != other.vastResource) return false
        if (clickThroughUrl != other.clickThroughUrl) return false
        if (clickTrackers != other.clickTrackers) return false
        if (creativeViewTrackers != other.creativeViewTrackers) return false
        if (customCtaText != other.customCtaText) return false

        return true
    }

    override fun hashCode(): Int {
        var result = width
        result = 31 * result + height
        result = 31 * result + vastResource.hashCode()
        result = 31 * result + (clickThroughUrl?.hashCode() ?: 0)
        result = 31 * result + clickTrackers.hashCode()
        result = 31 * result + creativeViewTrackers.hashCode()
        result = 31 * result + (customCtaText?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "VastCompanionAdConfig(width=$width, height=$height, vastResource=$vastResource, " +
                "clickThroughUrl=$clickThroughUrl, clickTrackers=$clickTrackers, " +
                "creativeViewTrackers=$creativeViewTrackers, customCtaText=$customCtaText)"
    }
}
