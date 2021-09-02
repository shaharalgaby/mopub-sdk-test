// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.mopub.common.Constants
import com.mopub.common.Mockable
import java.io.Serializable

@Mockable
class VastResource(
    @Expose @SerializedName(Constants.VAST_RESOURCE)
    val resource: String,
    @Expose @SerializedName(Constants.VAST_TYPE)
    val type: Type,
    @Expose @SerializedName(Constants.VAST_CREATIVE_TYPE)
    val creativeType: CreativeType,
    @Expose @SerializedName(Constants.VAST_WIDTH)
    val width: Int,
    @Expose @SerializedName(Constants.VAST_HEIGHT)
    val height: Int
) : Serializable {

    /**
     * The type of resource ordered according to priority.
     */
    enum class Type {
        HTML_RESOURCE,
        STATIC_RESOURCE,
        IFRAME_RESOURCE,
        BLURRED_LAST_FRAME
    }

    /**
     * The type of the static resource. Only static resources will have values other than NONE.
     */
    enum class CreativeType {
        NONE,
        IMAGE,
        JAVASCRIPT;
    }

    /**
     * Initializes a WebView used to display the resource.
     *
     * @param webView the resource's WebView.
     */
    fun initializeWebView(webView: VastWebView) {
        webView.run {
            getHtmlResourceValue()?.let { loadData(it) }
        }
    }

    /**
     * Gets the HTML necessary to show the creative type or the url where it exists.
     */
    fun getHtmlResourceValue(): String? {
        return when {
            type == Type.HTML_RESOURCE -> resource
            type == Type.IFRAME_RESOURCE -> "<iframe frameborder=\"0\" scrolling=\"no\" " +
                    "marginheight=\"0\" marginwidth=\"0\" style=\"border: 0px; margin: 0px;\"" +
                    " width=\"${this@VastResource.width}\"" +
                    " height=\"${this@VastResource.height}\"" +
                    " src=\"$resource\"></iframe>"
            type == Type.STATIC_RESOURCE && creativeType == CreativeType.IMAGE -> "<html>" +
                    "<head></head><body style=\"margin:0;padding:0\"><img src=\"$resource\"" +
                    " width=\"100%\" style=\"max-width:100%;max-height:100%;\" />" +
                    "</body></html>"
            type == Type.STATIC_RESOURCE && creativeType == CreativeType.JAVASCRIPT ->
                "<script src=\"$resource\"></script>"
            type == Type.BLURRED_LAST_FRAME -> resource
            else -> null
        }
    }

    /**
     * Selects the correct click through url based on the type of resource.
     *
     * @param vastClickThroughUrl    The click through url as specified in the vast document. This
     *                               is used with static images.
     * @param webViewClickThroughUrl The click through url when pertaining to Javascript, HTML,
     *                               IFrames that originated from a WebView.
     * @return String representing the correct click through for the resource type which may be
     * {@code null} if the correct click through url was not specified or {@code null}.
     */
    fun getCorrectClickThroughUrl(
        vastClickThroughUrl: String?,
        webViewClickThroughUrl: String?
    ): String? {
        return when {
            type == Type.HTML_RESOURCE || type == Type.IFRAME_RESOURCE -> webViewClickThroughUrl
            type == Type.STATIC_RESOURCE && creativeType == CreativeType.IMAGE -> vastClickThroughUrl
            type == Type.STATIC_RESOURCE && creativeType == CreativeType.JAVASCRIPT -> webViewClickThroughUrl
            type == Type.BLURRED_LAST_FRAME -> vastClickThroughUrl
            else -> null
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VastResource) return false

        if (resource != other.resource) return false
        if (type != other.type) return false
        if (creativeType != other.creativeType) return false
        if (width != other.width) return false
        if (height != other.height) return false

        return true
    }

    override fun hashCode(): Int {
        var result = resource.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + creativeType.hashCode()
        result = 31 * result + width
        result = 31 * result + height
        return result
    }

    override fun toString(): String {
        return "VastResource(resource='$resource', type=$type, creativeType=$creativeType, width=$width, height=$height)"
    }

    companion object {
        private const val serialVersionUID: Long = 1L

        private val VALID_IMAGE_TYPES = listOf(
            "image/jpeg", "image/png", "image/bmp", "image/gif", "image/jpg"
        )

        private val VALID_APPLICATION_TYPES = listOf(
            "application/x-javascript"
        )

        /**
         * Helper method that tries to create a {@link VastResource} by accessing all resource types on
         * the {@link VastResourceXmlManager} in order of priority defined by the {@link Type} enum.
         *
         * @param resourceXmlManager the manager used to populate the {@link VastResource}
         * @param width              the expected width of the resource. This only affects IFrames.
         * @param height             the expected height of the resource. This only affects IFrames.
         * @return the newly created VastResource
         */
        @JvmStatic
        fun fromVastResourceXmlManager(
            resourceXmlManager: VastResourceXmlManager,
            width: Int,
            height: Int
        ): VastResource? {
            return Type.values().mapNotNull {
                fromVastResourceXmlManager(
                    resourceXmlManager,
                    it,
                    width,
                    height
                )
            }.firstOrNull()
        }

        /**
         * Tries to create a {@link VastResource} by accessing a specific resource {@link Type} on the
         * {@link VastResourceXmlManager}.
         *
         * @param resourceXmlManager the manager used to populate the {@link VastResource}
         * @param type the resource {@link Type} to try to access
         * @param width the expected width of the resource. This only affects IFrames.
         * @param height the expected height of the resource. This only affects IFrames.
         * @return the newly created VastResource
         */
        @JvmStatic
        fun fromVastResourceXmlManager(
            resourceXmlManager: VastResourceXmlManager,
            type: Type,
            width: Int,
            height: Int
        ): VastResource? {
            val staticResourceType = resourceXmlManager.getStaticResourceType()

            var resource: String? = null
            var creativeType = CreativeType.NONE

            when (type) {
                Type.STATIC_RESOURCE -> {
                    resource = resourceXmlManager.getStaticResource().takeIf {
                        staticResourceType in VALID_IMAGE_TYPES || staticResourceType in VALID_APPLICATION_TYPES
                    }
                    creativeType = CreativeType.IMAGE.takeIf {
                        staticResourceType in VALID_IMAGE_TYPES
                    } ?: CreativeType.JAVASCRIPT
                }
                Type.HTML_RESOURCE -> resource = resourceXmlManager.getHTMLResource()
                Type.IFRAME_RESOURCE -> resource = resourceXmlManager.getIFrameResource()
            }

            return resource?.let {
                VastResource(it, type, creativeType, width, height)
            }
        }
    }

}
