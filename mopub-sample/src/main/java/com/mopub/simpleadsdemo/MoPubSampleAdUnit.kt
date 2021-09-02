// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.simpleadsdemo

import android.os.Bundle

import androidx.fragment.app.Fragment

import java.util.Comparator
import java.util.Locale

class MoPubSampleAdUnit private constructor(builder: Builder) :
    Comparable<MoPubSampleAdUnit> {
    // Note that entries are also sorted in this order
    enum class AdType(
        val displayName: String,
        val fragmentClass: Class<out Fragment>
    ) {
        BANNER("Banner", BannerDetailFragment::class.java),
        MEDIUM_RECTANGLE("Medium Rectangle", MediumRectangleDetailFragment::class.java),
        INTERSTITIAL("Interstitial", InterstitialDetailFragment::class.java),
        REWARDED_AD("Rewarded Ad", RewardedAdDetailFragment::class.java),
        LIST_VIEW("Native List View", NativeListViewFragment::class.java),
        RECYCLER_VIEW("Native Recycler View", NativeRecyclerViewFragment::class.java),
        CUSTOM_NATIVE("Native Gallery (Custom Stream)", NativeGalleryFragment::class.java),
        MANUAL_NATIVE("Native Manual", NativeManualFragment::class.java);

        companion object {
            fun fromFragmentClassName(fragmentClassName: String): AdType? {
                values().forEach {
                    if (it.fragmentClass.name == fragmentClassName) {
                        return it
                    }
                }
                return null
            }

            fun fromDeeplinkString(adType: String?): AdType? {
                return when (adType?.toLowerCase(Locale.US)) {
                    "banner" -> BANNER
                    "interstitial" -> INTERSTITIAL
                    "mediumrectangle" -> MEDIUM_RECTANGLE
                    "rewarded" -> REWARDED_AD
                    "native" -> LIST_VIEW
                    "nativetableplacer" -> RECYCLER_VIEW
                    "nativecollectionplacer" -> CUSTOM_NATIVE
                    else -> null
                }
            }
        }
    }

    internal class Builder(
        val adUnitId: String,
        val adType: AdType
    ) {
        var description: String? = null
        var keywords: String = ""
        var isUserDefined = false
        var id: Long = -1

        fun description(description: String?) = apply { this.description = description }

        fun keywords(keywords: String) = apply { this.keywords = keywords }

        fun isUserDefined(userDefined: Boolean) = apply { this.isUserDefined = userDefined }

        fun id(id: Long) = apply { this.id = id }

        fun build() = MoPubSampleAdUnit(this)
    }

    val adUnitId: String
    val mAdType: AdType
    val description: String?
    val keywords: String
    val isUserDefined: Boolean
    val id: Long
    val fragmentClass: Class<out Fragment>
        get() = mAdType.fragmentClass

    val fragmentClassName: String
        get() = mAdType.fragmentClass.name

    val headerName: String
        get() = mAdType.displayName

    fun toBundle(): Bundle {
        val bundle = Bundle()
        bundle.putLong(ID, id)
        bundle.putString(AD_UNIT_ID, adUnitId)
        bundle.putString(DESCRIPTION, description)
        bundle.putSerializable(AD_TYPE, mAdType)
        bundle.putString(KEYWORDS, keywords)
        bundle.putBoolean(IS_USER_DEFINED, isUserDefined)
        return bundle
    }

    override fun compareTo(other: MoPubSampleAdUnit): Int {
        return if (mAdType != other.mAdType) {
            mAdType.ordinal - other.mAdType.ordinal
        } else adUnitId.compareTo(other.adUnitId)
    }

    override fun hashCode(): Int {
        var result = 11
        result = 31 * result + mAdType.ordinal
        result = 31 * result + if (isUserDefined) 1 else 0
        result = 31 * result + description.hashCode()
        result = 31 * result + keywords.hashCode()
        result = 31 * result + adUnitId.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) {
            return false
        }
        if (this === other) {
            return true
        }
        if (other !is MoPubSampleAdUnit) {
            return false
        }
        return other.mAdType == mAdType &&
                other.isUserDefined == isUserDefined &&
                other.description == description &&
                other.keywords == keywords &&
                other.adUnitId == adUnitId
    }

    override fun toString(): String {
        return description ?: ""
    }

    companion object {
        private const val AD_UNIT_ID = "adUnitId"
        const val DESCRIPTION = "description"
        private const val AD_TYPE = "adType"
        private const val KEYWORDS = "keywords"
        private const val IS_USER_DEFINED = "isCustom"
        private const val ID = "id"
        val COMPARATOR = Comparator<MoPubSampleAdUnit> { a, b ->
            if (a.mAdType != b.mAdType) {
                a.mAdType.ordinal - b.mAdType.ordinal
            } else {
                if (a.isUserDefined || b.isUserDefined) {
                    if (a.isUserDefined && b.isUserDefined) {
                        a.description?.compareTo(b.description ?: "") ?: 0
                    } else if (!a.isUserDefined) {
                        1
                    } else {
                        -1
                    }
                } else {
                    a.description?.compareTo(b.description ?: "") ?: 0
                }
            }
        }

        fun fromBundle(bundle: Bundle?): MoPubSampleAdUnit {
            val id = bundle!!.getLong(ID, -1L)
            val adUnitId = bundle.getString(AD_UNIT_ID)
            val adType =
                bundle.getSerializable(AD_TYPE) as AdType?
            val description =
                bundle.getString(DESCRIPTION)
            val keywords = bundle.getString(KEYWORDS, "")
            val isUserDefined =
                bundle.getBoolean(IS_USER_DEFINED, false)
            val builder =
                Builder(adUnitId!!, adType!!)
            builder.description(description)
            builder.keywords(keywords)
            builder.id(id)
            builder.isUserDefined(isUserDefined)
            return builder.build()
        }
    }

    init {
        adUnitId = builder.adUnitId
        mAdType = builder.adType
        description = builder.description
        keywords = builder.keywords
        isUserDefined = builder.isUserDefined
        id = builder.id
    }
}
