// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.simpleadsdemo

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button

import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.caller.notes.R

import com.mopub.mobileads.MoPubErrorCode
import com.mopub.mobileads.MoPubInterstitial

class InterstitialDetailFragment : Fragment(),
    MoPubInterstitial.InterstitialAdListener {
    private var moPubInterstitial: MoPubInterstitial? = null
    private var showButton: Button? = null
    private var callbacksAdapter: CallbacksAdapter? = null
    private lateinit var viewHolder: DetailFragmentViewHolder

    private enum class InterstitialCallbacks(private val callbackName: String) {
        LOADED("onAdLoaded"),
        FAILED("onAdFailed"),
        SHOWN("onAdShown"),
        CLICKED("onAdClicked"),
        DISMISSED("onAdDismissed");

        override fun toString(): String {
            return callbackName
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val adConfiguration: MoPubSampleAdUnit = MoPubSampleAdUnit.fromBundle(arguments)
        val view = inflater.inflate(
            R.layout.interstitial_detail_fragment,
            container,
            false
        )
        viewHolder = DetailFragmentViewHolder.fromView(view)
        arguments?.let {
            viewHolder.keywordsField.setText(
                it.getString(MoPubListFragment.KEYWORDS_KEY, "")
            )
            viewHolder.userDataKeywordsField.setText(
                it.getString(MoPubListFragment.USER_DATA_KEYWORDS_KEY, "")
            )
        }

        Utils.hideSoftKeyboard(viewHolder.userDataKeywordsField)
        val adUnitId = adConfiguration.adUnitId
        viewHolder.descriptionView.text = adConfiguration.description
        viewHolder.adUnitIdView.text = adUnitId
        viewHolder.loadButton.setOnClickListener {
            showButton?.isEnabled = false
            if (moPubInterstitial == null) {
                activity?.let {
                    moPubInterstitial = MoPubInterstitial(it, adUnitId)
                }
                moPubInterstitial?.interstitialAdListener = this@InterstitialDetailFragment
            }
            val keywords = viewHolder.keywordsField.text.toString()
            val userDataKeywords =
                viewHolder.userDataKeywordsField.text.toString()
            moPubInterstitial?.setKeywords(keywords)
            moPubInterstitial?.setUserDataKeywords(userDataKeywords)
            callbacksAdapter?.generateCallbackList(InterstitialCallbacks::class.java)
            viewHolder.shareButton?.isEnabled = false
            moPubInterstitial?.load()
        }
        showButton = viewHolder.showButton?.apply {
            isEnabled = false
            setOnClickListener { moPubInterstitial?.show() }
        }
        viewHolder.shareButton?.setOnClickListener { onShareClicked() }
        val callbacksView: RecyclerView = view.findViewById(R.id.callbacks_recycler_view)
        context?.let { context ->
            callbacksView.layoutManager = LinearLayoutManager(context)
            callbacksAdapter = CallbacksAdapter(context).apply {
                generateCallbackList(InterstitialCallbacks::class.java)
            }.also {
                callbacksView.adapter = it
            }
        }
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        moPubInterstitial?.destroy()
        moPubInterstitial = null
    }

    // InterstitialAdListener implementation
    override fun onInterstitialLoaded(interstitial: MoPubInterstitial) {
        showButton?.isEnabled = true
        callbacksAdapter?.notifyCallbackCalled(InterstitialCallbacks.LOADED.toString())
            ?: Utils.logToast(activity, "Interstitial loaded.")

        if (moPubInterstitial == null || moPubInterstitial?.getAdUnitId().isNullOrEmpty()) {
            Utils.logToast(activity, "Ad unit is not initialized")
            return
        }

        val loadedAdUrl = LastAdRequestUrlManager.getAdRequestUrl(moPubInterstitial?.getAdUnitId())
        if (!loadedAdUrl.isNullOrEmpty()) {
            viewHolder.shareButton?.isEnabled = true
        }
    }

    override fun onInterstitialFailed(interstitial: MoPubInterstitial, errorCode: MoPubErrorCode) {
        showButton?.isEnabled = false
        callbacksAdapter?.notifyCallbackCalled(
            InterstitialCallbacks.FAILED.toString(),
            errorCode.toString()
        ) ?: Utils.logToast(activity, "Interstitial failed to load: $errorCode")

        if (moPubInterstitial == null || moPubInterstitial?.getAdUnitId().isNullOrEmpty()) {
            Utils.logToast(activity, "Ad unit is not initialized")
            return
        }

        val loadedAdUrl = LastAdRequestUrlManager.getAdRequestUrl(moPubInterstitial?.getAdUnitId())
        if (!loadedAdUrl.isNullOrEmpty()) {
            viewHolder.shareButton?.isEnabled = true
        }
    }

    override fun onInterstitialShown(interstitial: MoPubInterstitial) {
        showButton?.isEnabled = false
        callbacksAdapter?.notifyCallbackCalled(InterstitialCallbacks.SHOWN.toString())
            ?: Utils.logToast(activity, "Interstitial shown.")
    }

    override fun onInterstitialClicked(interstitial: MoPubInterstitial) {
        callbacksAdapter?.notifyCallbackCalled(InterstitialCallbacks.CLICKED.toString())
            ?: Utils.logToast(activity, "Interstitial clicked.")
    }

    override fun onInterstitialDismissed(interstitial: MoPubInterstitial) {
        callbacksAdapter?.notifyCallbackCalled(InterstitialCallbacks.DISMISSED.toString())
            ?: Utils.logToast(activity, "Interstitial dismissed.")
    }

    private fun onShareClicked() {
        if (moPubInterstitial == null || moPubInterstitial?.getAdUnitId().isNullOrEmpty()) {
            Utils.logToast(activity, "Ad unit is not initialized")
            return
        }

        val loadedAdUrl = LastAdRequestUrlManager.getAdRequestUrl(moPubInterstitial?.getAdUnitId())
        if (!loadedAdUrl.isNullOrEmpty()) {
            Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_TEXT, loadedAdUrl)
                type = "text/plain"
                startActivity(Intent.createChooser(this, "Share"))
            }
        }
    }
}
