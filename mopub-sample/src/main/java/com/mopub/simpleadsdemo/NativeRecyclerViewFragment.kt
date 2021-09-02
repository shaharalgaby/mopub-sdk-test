// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.simpleadsdemo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView

import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.caller.notes.R

import com.mopub.nativeads.*
import com.mopub.nativeads.FacebookAdRenderer.FacebookViewBinder
import com.mopub.nativeads.MoPubNativeAdPositioning.MoPubServerPositioning
import com.mopub.nativeads.RequestParameters.NativeAdAsset

import java.util.EnumSet
import java.util.Locale

class NativeRecyclerViewFragment : Fragment() {
    private var recyclerAdapter: MoPubRecyclerAdapter? = null
    private lateinit var adConfiguration: MoPubSampleAdUnit
    private var requestParameters: RequestParameters? = null

    private enum class LayoutType {
        LINEAR, GRID
    }

    private var layoutType: LayoutType? = null
    private var recyclerView: RecyclerView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        adConfiguration = MoPubSampleAdUnit.fromBundle(arguments)
        val adUnitId = adConfiguration.adUnitId
        val view = inflater.inflate(
            R.layout.recycler_view_fragment,
            container,
            false
        )
        recyclerView = view.findViewById<View>(R.id.native_recycler_view) as RecyclerView
        val viewHolder: DetailFragmentViewHolder = DetailFragmentViewHolder.fromView(view)
        val switchButton = view.findViewById<View>(R.id.switch_button) as Button
        switchButton.setOnClickListener { toggleRecyclerLayout() }
        viewHolder.loadButton.setOnClickListener {
            updateRequestParameters(viewHolder)
            recyclerAdapter?.refreshAds(adUnitId, requestParameters)

        }
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

        val originalAdapter: RecyclerView.Adapter<*> = DemoRecyclerAdapter()
        recyclerAdapter =
            MoPubRecyclerAdapter(activity!!, originalAdapter, MoPubServerPositioning()).apply {
                // The first renderer that can handle a particular native ad gets used.
                // We are prioritizing network renderers.
                registerAdRenderer(mintegralAdRenderer)
                registerAdRenderer(referenceRenderer)
                registerAdRenderer(pangleAdRenderer)
                registerAdRenderer(googlePlayServicesAdRenderer)
                registerAdRenderer(facebookAdRenderer)
                registerAdRenderer(moPubStaticNativeAdRenderer)
            }.also {
                recyclerView?.adapter = it
                recyclerView?.layoutManager = LinearLayoutManager(activity)
                layoutType = LayoutType.LINEAR
                it.loadAds(adUnitId, requestParameters)
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

    private fun toggleRecyclerLayout() {
        if (layoutType == LayoutType.LINEAR) {
            layoutType = LayoutType.GRID
            recyclerView?.layoutManager = GridLayoutManager(
                activity,
                2
            )
        } else {
            layoutType = LayoutType.LINEAR
            recyclerView?.layoutManager = LinearLayoutManager(
                activity
            )
        }
    }

    override fun onDestroyView() {
        // You must call this or the ad adapter may cause a memory leak.
        recyclerAdapter?.destroy()
        super.onDestroyView()
    }

    private class DemoRecyclerAdapter :
        RecyclerView.Adapter<DemoViewHolder>() {
        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): DemoViewHolder {
            val itemView = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_1, parent, false)
            return DemoViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: DemoViewHolder, position: Int) {
            holder.textView.text = String.format(Locale.US, "Content Item #$position")
        }

        override fun getItemId(position: Int) = position.toLong()

        override fun getItemCount() = ITEM_COUNT

        companion object {
            private const val ITEM_COUNT = 150
        }
    }

    /**
     * A view holder for R.layout.simple_list_item_1
     */
    private class DemoViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(android.R.id.text1)
    }
}
