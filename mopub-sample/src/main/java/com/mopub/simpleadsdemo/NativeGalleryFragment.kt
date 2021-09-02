// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.simpleadsdemo

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.caller.notes.R

import com.mopub.nativeads.*
import com.mopub.nativeads.RequestParameters.NativeAdAsset

import java.util.EnumSet

class NativeGalleryFragment : Fragment(),
    MoPubNativeAdLoadedListener {
    private lateinit var adConfiguration: MoPubSampleAdUnit
    private var viewPager: ViewPager? = null
    private var pagerAdapter: CustomPagerAdapter? = null
    var adPlacer: MoPubStreamAdPlacer? = null
        private set
    private var requestParameters: RequestParameters? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        adConfiguration = MoPubSampleAdUnit.fromBundle(arguments)
        val view = inflater.inflate(
            R.layout.native_gallery_fragment,
            container,
            false
        )
        val activity = activity ?: return view

        val views: DetailFragmentViewHolder = DetailFragmentViewHolder.fromView(view)
        views.loadButton.setOnClickListener {
            updateRequestParameters(views)
            adPlacer?.loadAds(adConfiguration.adUnitId, requestParameters)
        }
        val adUnitId = adConfiguration.adUnitId
        views.descriptionView.text = adConfiguration.description ?: ""
        views.adUnitIdView.text = adUnitId
        arguments?.let {
            views.keywordsField.setText(
                it.getString(
                    MoPubListFragment.KEYWORDS_KEY,
                    ""
                )
            )
            views.userDataKeywordsField.setText(
                it.getString(
                    MoPubListFragment.USER_DATA_KEYWORDS_KEY,
                    ""
                )
            )
        }

        viewPager = view.findViewById<View>(R.id.gallery_pager) as ViewPager
        updateRequestParameters(views)

        // Set up a renderer for a static native ad.
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
            FacebookAdRenderer.FacebookViewBinder.Builder(R.layout.native_ad_fan_list_item)
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

        // This ad placer is used to automatically insert ads into the ViewPager.
        adPlacer = MoPubStreamAdPlacer(activity).apply {
            // The first renderer that can handle a particular native ad gets used.
            // We are prioritizing network renderers.
            registerAdRenderer(mintegralAdRenderer)
            registerAdRenderer(referenceRenderer)
            registerAdRenderer(pangleAdRenderer)
            registerAdRenderer(googlePlayServicesAdRenderer)
            registerAdRenderer(facebookAdRenderer)
            registerAdRenderer(moPubStaticNativeAdRenderer)
        }.also {
            it.setAdLoadedListener(this)
            pagerAdapter = CustomPagerAdapter(childFragmentManager, it)
            viewPager?.adapter = pagerAdapter
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
        adPlacer?.destroy()
        super.onDestroyView()
    }

    override fun onResume() {
        // MoPub recommends reloading ads when the user returns to a view.
        adPlacer?.loadAds(adConfiguration.adUnitId, requestParameters)

        super.onResume()
    }

    override fun onAdLoaded(position: Int) {
        viewPager?.invalidate()
        pagerAdapter?.notifyDataSetChanged()
    }

    override fun onAdRemoved(position: Int) {
        viewPager?.invalidate()
        pagerAdapter?.notifyDataSetChanged()
    }

    private class CustomPagerAdapter(
        fragmentManager: FragmentManager,
        private val streamAdPlacer: MoPubStreamAdPlacer
    ) : FragmentStatePagerAdapter(fragmentManager) {
        override fun getItemPosition(item: Any): Int {
            return if (item is AdFragment) {
                item.adPosition
            } else PagerAdapter.POSITION_NONE
            // This forces all items to be recreated when invalidate() is called on the ViewPager.
        }

        override fun getItem(i: Int): Fragment {
            streamAdPlacer.placeAdsInRange(i - 5, i + 5)
            return if (streamAdPlacer.isAd(i)) {
                AdFragment.newInstance(i)
            } else ContentFragment.newInstance(streamAdPlacer.getOriginalPosition(i))
        }

        override fun getCount(): Int {
            return streamAdPlacer.getAdjustedCount(ITEM_COUNT)
        }

        override fun getPageTitle(position: Int): CharSequence {
            return if (streamAdPlacer.isAd(position)) {
                "Advertisement"
            } else "Content Item ${streamAdPlacer.getOriginalPosition(position)}"
        }

        companion object {
            private const val ITEM_COUNT = 30
        }

        init {
            streamAdPlacer.setItemCount(ITEM_COUNT)
        }
    }

    class ContentFragment : Fragment() {
        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            // Inflate the view.
            val rootView = inflater.inflate(
                R.layout.native_gallery_content,
                container,
                false
            )
            val contentNumber = arguments?.getInt(ARG_SECTION_NUMBER) ?: -1
            val textView =
                rootView.findViewById<View>(R.id.native_gallery_content_text) as TextView
            textView.text = "Content Item $contentNumber"
            return rootView
        }

        companion object {
            /**
             * The fragment argument representing the section number for this
             * fragment.
             */
            private const val ARG_SECTION_NUMBER = "section_number"
            fun newInstance(sectionNumber: Int): ContentFragment {
                val fragment = ContentFragment()
                val args = Bundle()
                args.putInt(ARG_SECTION_NUMBER, sectionNumber)
                fragment.arguments = args
                return fragment
            }
        }
    }

    class AdFragment : Fragment() {
        private var adPlacer: MoPubStreamAdPlacer? = null
        var adPosition = -2 // POSITION_NONE
        override fun onAttach(activity: Activity) {
            adPlacer = (parentFragment as NativeGalleryFragment).adPlacer
            super.onAttach(activity)
        }

        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            adPlacer?.let {
                val position = arguments?.getInt(ARG_AD_POSITION) ?: return@onCreateView null
                it.placeAdsInRange(position - 5, position + 5)
                return it.getAdView(position, null, container)
            }
            return null
        }

        companion object {
            private const val ARG_AD_POSITION = "ad_position"
            fun newInstance(adPosition: Int): AdFragment {
                val fragment = AdFragment()
                val bundle = Bundle()
                bundle.putInt(ARG_AD_POSITION, adPosition)
                fragment.arguments = bundle
                fragment.adPosition = adPosition
                return fragment
            }
        }
    }
}
