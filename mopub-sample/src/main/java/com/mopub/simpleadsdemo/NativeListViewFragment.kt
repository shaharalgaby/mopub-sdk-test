// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.simpleadsdemo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView

import androidx.fragment.app.Fragment
import com.caller.notes.R

import com.mopub.nativeads.*
import com.mopub.nativeads.FacebookAdRenderer.FacebookViewBinder
import com.mopub.nativeads.MoPubNativeAdPositioning.MoPubServerPositioning
import com.mopub.nativeads.RequestParameters.NativeAdAsset

import java.util.EnumSet

class NativeListViewFragment : Fragment() {
    private var adAdapter: MoPubAdAdapter? = null
    private lateinit var adConfiguration: MoPubSampleAdUnit
    private var requestParameters: RequestParameters? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        adConfiguration = MoPubSampleAdUnit.fromBundle(arguments)
        val view = inflater.inflate(
            R.layout.native_list_view_fragment,
            container,
            false
        )
        val listView = view.findViewById<View>(R.id.native_list_view) as ListView
        val views: DetailFragmentViewHolder = DetailFragmentViewHolder.fromView(view)
        views.loadButton.setOnClickListener {
            updateRequestParameters(views)
            adAdapter?.loadAds(adConfiguration.adUnitId, requestParameters)
        }
        val adUnitId = adConfiguration.adUnitId
        views.descriptionView.text = adConfiguration.description ?: ""
        views.adUnitIdView.text = adUnitId
        arguments?.let {
            views.keywordsField.setText(it.getString(MoPubListFragment.KEYWORDS_KEY, ""))
            views.userDataKeywordsField.setText(
                it.getString(
                    MoPubListFragment.USER_DATA_KEYWORDS_KEY,
                    ""
                )
            )
        }

        val adapter = ArrayAdapter<String>(activity!!, android.R.layout.simple_list_item_1)
        for (i in 0..99) {
            adapter.add("Item $i")
        }

        // Set up a renderer that knows how to put ad data in your custom native view.
        val staticAdRender = MoPubStaticNativeAdRenderer(
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

        // Set up a renderer for AdMob ads.
        val googlePlayServicesAdRenderer =
            GooglePlayServicesAdRenderer(
                GooglePlayServicesViewBinder.Builder(R.layout.admob_video_ad_list_item)
                    .titleId(R.id.native_title)
                    .textId(R.id.native_text)
                    .mediaLayoutId(R.id.native_media_layout)
                    .iconImageId(R.id.native_icon_image)
                    .callToActionId(R.id.native_cta)
                    .privacyInformationIconImageId(R.id.native_privacy_information_icon_image)
                    .build()
            )


        // Set up a renderer for Pangle ads.
        val pangleAdRenderer = PangleAdRenderer(
            PangleAdViewBinder.Builder(R.layout.native_ad_pangle_list_item)
                .callToActionId(R.id.native_cta)
                .decriptionTextId(R.id.native_text)
                .iconImageId(R.id.native_icon_image)
                .titleId(R.id.native_title)
                .mediaViewIdId(R.id.native_main_image)
                .build()
        )

        // Set up a renderer for Reference adapters.
        val referenceRenderer = ReferenceNativeAdRenderer(
            ReferenceNativeAdRenderer.ReferenceViewBinder.Builder(R.layout.native_ad_list_item)
                .titleId(R.id.native_title)
                .textId(R.id.native_text)
                .mainImageId(R.id.native_main_image)
                .iconImageId(R.id.native_icon_image)
                .callToActionId(R.id.native_cta)
                .adChoicesRelativeLayoutId(R.id.native_privacy_information_icon_layout)
                .build()
        )

        // Set up a renderer for Mintegral ads
        val mintegralAdRenderer = MintegralAdRenderer(
            MintegralAdRenderer.ViewBinder.Builder(R.layout.native_ad_list_item)
                .titleId(R.id.native_title)
                .textId(R.id.native_text)
                .mainImageId(R.id.native_main_image)
                .iconImageId(R.id.native_icon_image)
                .callToActionId(R.id.native_cta)
                .build()
        )

        // Create an ad adapter that gets its positioning information from the MoPub Ad Server.
        // This adapter will be used in place of the original adapter for the ListView.
        adAdapter = MoPubAdAdapter(activity!!, adapter, MoPubServerPositioning()).apply {
            // Register the renderers with the MoPubAdAdapter and then set the adapter on the ListView.
            // The first renderer that can handle a particular native ad gets used.
            // We are prioritizing network renderers.
            registerAdRenderer(mintegralAdRenderer)
            registerAdRenderer(referenceRenderer)
            registerAdRenderer(pangleAdRenderer)
            registerAdRenderer(googlePlayServicesAdRenderer)
            registerAdRenderer(facebookAdRenderer)
            registerAdRenderer(staticAdRender)
        }.also {
            listView.adapter = it
            updateRequestParameters(views)
            it.loadAds(adConfiguration.adUnitId, requestParameters)
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

    override fun onDestroyView() {
        // You must call this or the ad adapter may cause a memory leak.
        adAdapter?.destroy()
        super.onDestroyView()
    }
}
