// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.simpleadsdemo

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout

import androidx.fragment.app.Fragment
import com.caller.notes.R

import com.mopub.nativeads.*
import com.mopub.nativeads.FacebookAdRenderer.FacebookViewBinder
import com.mopub.nativeads.MoPubNative.MoPubNativeNetworkListener
import com.mopub.nativeads.NativeAd.MoPubNativeEventListener
import com.mopub.nativeads.RequestParameters.NativeAdAsset

import java.util.EnumSet

class NativeManualFragment : Fragment() {
    private lateinit var adConfiguration: MoPubSampleAdUnit
    private var moPubNative: MoPubNative? = null
    private var adContainer: LinearLayout? = null
    private var requestParameters: RequestParameters? = null
    private lateinit var viewHolder: DetailFragmentViewHolder

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val view = inflater.inflate(
            R.layout.native_manual_fragment,
            container,
            false
        )
        viewHolder = DetailFragmentViewHolder.fromView(view)
        adConfiguration = MoPubSampleAdUnit.fromBundle(arguments)
        adContainer = view.findViewById(R.id.parent_view)
        viewHolder.loadButton.setOnClickListener {
            updateRequestParameters(viewHolder)
            adContainer?.removeAllViews()

            viewHolder.shareButton?.isEnabled = false
            moPubNative?.makeRequest(requestParameters) ?: Utils.logToast(
                activity,
                "$name failed to load. MoPubNative instance is null."
            )
        }
        viewHolder.shareButton?.setOnClickListener { onShareClicked() }
        val adUnitId = "6b6b852aeccd49229b7922363a33a69c"

        viewHolder.descriptionView.text = adConfiguration.description
        viewHolder.adUnitIdView.text = adUnitId
        arguments?.let {
            viewHolder.keywordsField.setText(it.getString(MoPubListFragment.KEYWORDS_KEY, ""))
            viewHolder.userDataKeywordsField.setText(
                it.getString(
                    MoPubListFragment.USER_DATA_KEYWORDS_KEY,
                    ""
                )
            )
        }

        updateRequestParameters(viewHolder)
        moPubNative = MoPubNative(context!!, "6b6b852aeccd49229b7922363a33a69c", object : MoPubNativeNetworkListener {
            override fun onNativeLoad(nativeAd: NativeAd) {
                val ctx = context ?: return
                val moPubNativeEventListener: MoPubNativeEventListener =
                    object : MoPubNativeEventListener {
                        override fun onImpression(view: View?) {
                            // The ad has registered an impression. You may call any app logic that
                            // depends on having the ad view shown.
                            Utils.logToast(
                                activity,
                                "$name impressed."
                            )
                        }

                        override fun onClick(view: View?) {
                            Utils.logToast(
                                activity,
                                "$name clicked."
                            )
                        }
                    }

                // In a manual integration, any interval that is at least 2 is acceptable
                val adapterHelper = AdapterHelper(ctx, 0, 2)
                val adView: View = adapterHelper.getAdView(
                    null,
                    null,
                    nativeAd,
                    ViewBinder.Builder(0).build()
                )
                nativeAd.setMoPubNativeEventListener(moPubNativeEventListener)
                adContainer?.addView(adView) ?: Utils.logToast(
                    activity,
                    "$name failed to show. Ad container is null."
                )

                if (!::adConfiguration.isInitialized) {
                    Utils.logToast(activity, "Ad unit is not initialized")
                    return
                }

                val loadedAdUrl = LastAdRequestUrlManager.getAdRequestUrl(adConfiguration.adUnitId)
                if (!loadedAdUrl.isNullOrEmpty()) {
                    viewHolder.shareButton?.isEnabled = true
                }
            }

            override fun onNativeFail(errorCode: NativeErrorCode) {
                if (!::adConfiguration.isInitialized) {
                    Utils.logToast(activity, "Ad unit is not initialized")
                    return
                }

                val loadedAdUrl = LastAdRequestUrlManager.getAdRequestUrl(adConfiguration.adUnitId)
                if (!loadedAdUrl.isNullOrEmpty()) {
                    viewHolder.shareButton?.isEnabled = true
                }

                Utils.logToast(activity, "$name failed to load: $errorCode")
            }
        })

        val moPubStaticNativeAdRenderer = MoPubStaticNativeAdRenderer(
            ViewBinder.Builder(R.layout.native_ad_list_item)
                .titleId(R.id.native_title)
                .textId(R.id.native_text)
                .mainImageId(R.id.native_main_image)
                .iconImageId(R.id.native_icon_image)
                .callToActionId(R.id.native_cta)
                .privacyInformationIconImageId(R.id.native_privacy_information_icon_image)
                .sponsoredTextId(R.id.native_sponsored_text_view)
                .build()
        )

        // Set up a renderer for Facebook video ads.
        val facebookAdRenderer = FacebookAdRenderer(
            FacebookViewBinder.Builder(R.layout.native_ad_fan_list_item)
                .titleId(R.id.native_title)
                .textId(R.id.native_text)
                .mediaViewId(R.id.native_media_view)
                .adIconViewId(R.id.native_icon)
                .callToActionId(R.id.native_cta)
                .adChoicesRelativeLayoutId(R.id.native_privacy_information_icon_layout)
                .build()
        )

        // The first renderer that can handle a particular native ad gets used.
        // We are prioritizing network renderers.
        moPubNative?.apply {
            registerAdRenderer(facebookAdRenderer)
            registerAdRenderer(moPubStaticNativeAdRenderer)
            makeRequest(requestParameters)
        }
        return view
    }

    private fun updateRequestParameters(views: DetailFragmentViewHolder) {
        val keywords = views.keywordsField.text.toString()
        val userDataKeywords = views.userDataKeywordsField.text.toString()

        // Setting desired assets on your request helps native ad networks and bidders
        // provide higher-quality ads.
        val desiredAssets = EnumSet.of(
            NativeAdAsset.TITLE,
            NativeAdAsset.TEXT,
            NativeAdAsset.ICON_IMAGE,
            NativeAdAsset.MAIN_IMAGE,
            NativeAdAsset.CALL_TO_ACTION_TEXT,
            NativeAdAsset.SPONSORED
        )
        requestParameters = RequestParameters.Builder()
            .keywords(keywords)
            .userDataKeywords(userDataKeywords)
            .desiredAssets(desiredAssets)
            .build()
    }

    private fun onShareClicked() {
        if (!::adConfiguration.isInitialized) {
            Utils.logToast(activity, "Ad unit is not initialized")
            return
        }

        val loadedAdUrl = LastAdRequestUrlManager.getAdRequestUrl(adConfiguration.adUnitId)
        if (!loadedAdUrl.isNullOrEmpty()) {
            Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_TEXT, loadedAdUrl)
                type = "text/plain"
                startActivity(Intent.createChooser(this, "Share"))
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        moPubNative?.destroy()
        moPubNative = null
        adContainer?.removeAllViews()
        adContainer = null
    }

    private val name: String
    get() = if (adConfiguration.headerName.isEmpty()) {
            MoPubSampleAdUnit.AdType.MANUAL_NATIVE.displayName
        } else adConfiguration.headerName
}
