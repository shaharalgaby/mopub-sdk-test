// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.simpleadsdemo

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner

import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.caller.notes.R

import com.mopub.mobileads.MoPubErrorCode
import com.mopub.mobileads.MoPubView
import com.mopub.mobileads.MoPubView.MoPubAdSize

/**
 * A base class for creating banner style ads with various height and width dimensions.
 *
 *
 * A subclass simply needs to specify the height and width of the ad in pixels, and this class will
 * inflate a layout containing a programmatically rescaled [MoPubView] that will be used to
 * display the ad.
 */
abstract class AbstractBannerDetailFragment : Fragment(),
    MoPubView.BannerAdListener {
    private lateinit var moPubView: MoPubView
    private lateinit var moPubSampleAdUnit: MoPubSampleAdUnit
    private lateinit var viewHolder: DetailFragmentViewHolder
    private var callbacksAdapter: CallbacksAdapter? = null
    abstract val adSize: MoPubAdSize
    private lateinit var moPubAdSizeSettings: MoPubAdSizeSettings

    private enum class AdSizeOption(
        // getLayoutParamsValue returns the height or width value equivalent from ViewGroup.LayoutParams
        val layoutParamsValue: Int
    ) {
        EXACT(0),
        MATCH_PARENT(ViewGroup.LayoutParams.MATCH_PARENT), // -1
        WRAP_CONTENT(ViewGroup.LayoutParams.WRAP_CONTENT); // -2

        companion object {
            fun fromLayoutParamsValue(layoutParamsValue: Int): AdSizeOption {
                return when (layoutParamsValue) {
                    ViewGroup.LayoutParams.MATCH_PARENT -> MATCH_PARENT
                    ViewGroup.LayoutParams.WRAP_CONTENT -> WRAP_CONTENT
                    else -> EXACT
                }
            }
        }
    }

    private enum class BannerCallbacks(private val callbackName: String) {
        LOADED("onBannerLoaded"),
        FAILED("onBannerFailed"),
        CLICKED("onBannerClicked"),
        EXPANDED("onBannerExpanded"),
        COLLAPSED("onBannerCollapsed");

        override fun toString(): String {
            return callbackName
        }
    }

    class MoPubAdSizeSettings internal constructor(
        var adSize: MoPubAdSize,
        var width: Int,
        var height: Int
    ) {
        override fun toString(): String {
            return String.format(
                DETAIL_STRING,
                adSizeString,
                getLayoutParamsString(width),
                getLayoutParamsString(height)
            )
        }

        private val adSizeString: String?
            get() = adSize.toString()

        private fun getLayoutParamsString(dimension: Int): String {
            return when (dimension) {
                ViewGroup.LayoutParams.MATCH_PARENT -> "MATCH_PARENT"
                ViewGroup.LayoutParams.WRAP_CONTENT -> "WRAP_CONTENT"
                else -> "$dimension"
            }
        }

        companion object {
            private const val DETAIL_STRING = "MoPub Ad Size:\nadSize=%s\nw=%s\nh=%s"
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val view = inflater.inflate(
            R.layout.banner_detail_fragment,
            container,
            false
        )
        viewHolder = DetailFragmentViewHolder.fromView(view)
        moPubSampleAdUnit = MoPubSampleAdUnit.fromBundle(arguments)
        moPubView = view.findViewById<MoPubView>(R.id.banner_mopubview).also {
            val layoutParams = it.layoutParams as LinearLayout.LayoutParams
            it.layoutParams = layoutParams
            it.adSize = adSize
            moPubAdSizeSettings = MoPubAdSizeSettings(
                adSize,
                layoutParams.width,
                layoutParams.height
            )
            it.bannerAdListener = this
        }

        viewHolder.adSizeInfoView?.text = moPubAdSizeSettings.toString()
        viewHolder.keywordsField.setText(
            arguments?.getString(
                MoPubListFragment.KEYWORDS_KEY,
                ""
            ) ?: ""
        )
        viewHolder.userDataKeywordsField.setText(
            arguments?.getString(
                MoPubListFragment.USER_DATA_KEYWORDS_KEY,
                ""
            ) ?: ""
        )
        Utils.hideSoftKeyboard(viewHolder.keywordsField)
        val adUnitId = moPubSampleAdUnit.adUnitId
        viewHolder.descriptionView.text = moPubSampleAdUnit.description
        viewHolder.adUnitIdView.text = adUnitId
        viewHolder.loadButton.setOnClickListener {
            val keywords = viewHolder.keywordsField.text.toString()
            val userDataKeywords =
                viewHolder.userDataKeywordsField.text.toString()
            setupMoPubView(adUnitId, keywords, userDataKeywords)
            viewHolder.shareButton?.isEnabled = false
            moPubView.loadAd()
        }
        view.findViewById<RecyclerView>(R.id.callbacks_recycler_view).let { callbacksView ->
            context?.let { ctx ->
                callbacksView.layoutManager = LinearLayoutManager(ctx)
                callbacksAdapter = CallbacksAdapter(ctx).also {
                    it.generateCallbackList(BannerCallbacks::class.java)
                    callbacksView.adapter = it
                }
            }
        }
        viewHolder.changeAdSizeButton?.setOnClickListener { onAdSizeClicked() }
        viewHolder.shareButton?.setOnClickListener { onShareClicked() }
        setupMoPubView(adUnitId, "", "")
        moPubView.loadAd()

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        moPubView.destroy()
    }

    private fun setupMoPubView(
        adUnitId: String,
        keywords: String,
        userDataKeywords: String
    ) {
        moPubView.setAdUnitId(adUnitId)
        moPubView.setKeywords(keywords)
        moPubView.setUserDataKeywords(userDataKeywords)
        callbacksAdapter?.generateCallbackList(BannerCallbacks::class.java)
    }

    private val name: String
        get() = moPubSampleAdUnit.headerName

    // BannerAdListener
    override fun onBannerLoaded(banner: MoPubView) {
        callbacksAdapter?.notifyCallbackCalled(BannerCallbacks.LOADED.toString())
            ?: Utils.logToast(activity, "$name loaded.")

        if (!::moPubSampleAdUnit.isInitialized) {
            Utils.logToast(activity, "Ad unit is not initialized")
            return
        }

        val loadedAdUrl = LastAdRequestUrlManager.getAdRequestUrl(moPubSampleAdUnit.adUnitId)
        if (!loadedAdUrl.isNullOrEmpty()) {
            viewHolder.shareButton?.isEnabled = true
        }
    }

    override fun onBannerFailed(banner: MoPubView, errorCode: MoPubErrorCode) {
        errorCode.toString().let {
            callbacksAdapter?.notifyCallbackCalled(BannerCallbacks.FAILED.toString(), it)
                ?: Utils.logToast(activity, "$name failed to load: $it")
        }

        if (!::moPubSampleAdUnit.isInitialized) {
            Utils.logToast(activity, "Ad unit is not initialized")
            return
        }

        val loadedAdUrl = LastAdRequestUrlManager.getAdRequestUrl(moPubSampleAdUnit.adUnitId)
        if (!loadedAdUrl.isNullOrEmpty()) {
            viewHolder.shareButton?.isEnabled = true
        }
    }

    override fun onBannerClicked(banner: MoPubView) {
        callbacksAdapter?.notifyCallbackCalled(BannerCallbacks.CLICKED.toString())
            ?: Utils.logToast(activity, "$name clicked.")
    }

    override fun onBannerExpanded(banner: MoPubView) {
        callbacksAdapter?.notifyCallbackCalled(BannerCallbacks.EXPANDED.toString())
            ?: Utils.logToast(activity, "$name expanded.")
    }

    override fun onBannerCollapsed(banner: MoPubView) {
        callbacksAdapter?.notifyCallbackCalled(BannerCallbacks.COLLAPSED.toString())
            ?: Utils.logToast(activity, "$name collapsed.")
    }

    private fun onAdSizeClicked() {
        activity?.let { activity ->
            MoPubAdSizeFragment.newInstance(moPubAdSizeSettings).apply {
                setTargetFragment(this@AbstractBannerDetailFragment, 0)
                show(activity.supportFragmentManager, "adSize")
            }
        }
    }

    private fun onShareClicked() {
        if (!::moPubSampleAdUnit.isInitialized) {
            Utils.logToast(activity, "Ad unit is not initialized")
            return
        }

        val loadedAdUrl = LastAdRequestUrlManager.getAdRequestUrl(moPubSampleAdUnit.adUnitId)
        if (!loadedAdUrl.isNullOrEmpty()) {
            Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_TEXT, loadedAdUrl)
                type = "text/plain"
                startActivity(Intent.createChooser(this, "Share"))
            }
        }
    }

    private fun onAdSizeSettingsChanged(moPubAdSizeSettings: MoPubAdSizeSettings) {
        this.moPubAdSizeSettings = moPubAdSizeSettings
        val density = resources.displayMetrics.density
        moPubView.layoutParams = moPubView.layoutParams.apply {
            this.height =
                (this@AbstractBannerDetailFragment.moPubAdSizeSettings.height * density).toInt()
            this.width =
                (this@AbstractBannerDetailFragment.moPubAdSizeSettings.width * density).toInt()
        }
        moPubView.adSize = this.moPubAdSizeSettings.adSize
        viewHolder.adSizeInfoView?.text = this.moPubAdSizeSettings.toString()
    }

    class MoPubAdSizeFragment : DialogFragment() {
        private lateinit var adSizeSpinner: Spinner
        private lateinit var adWidthSpinner: Spinner
        private lateinit var adHeightSpinner: Spinner
        private lateinit var adWidthEditText: EditText
        private lateinit var adHeightEditText: EditText
        private var adSizeSettings: MoPubAdSizeSettings? = null

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val dialog = AlertDialog.Builder(activity)
                .setTitle(R.string.ad_size_dialog_title)
                .setPositiveButton(
                    R.string.ad_size_dialog_save_button
                ) { _, _ ->
                    adSizeSettings?.let {
                        (targetFragment as AbstractBannerDetailFragment?)?.onAdSizeSettingsChanged(
                            it
                        )
                    }
                    dismiss()
                }
                .setNegativeButton(
                    android.R.string.cancel
                ) { _, _ -> dismiss() }
                .setCancelable(true)
                .create()

            // Inflate and add our custom layout to the dialog.
            val view = dialog.layoutInflater.inflate(R.layout.ad_size_dialog, null)
            adSizeSpinner = view.findViewById(R.id.ad_size_spinner)
            adWidthSpinner = view.findViewById(R.id.ad_size_width_spinner)
            adHeightSpinner = view.findViewById(R.id.ad_size_height_spinner)
            adWidthEditText = view.findViewById(R.id.ad_size_width_edit_text)
            adHeightEditText = view.findViewById(R.id.ad_size_height_edit_text)
            activity?.let { fragmentActivity ->
                ArrayAdapter(
                    fragmentActivity,
                    android.R.layout.simple_spinner_dropdown_item,
                    android.R.id.text1,
                    AdSizeOption.values()
                ).let {
                    adWidthSpinner.adapter = it
                    adHeightSpinner.adapter = it
                }
            }

            adWidthEditText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    charSequence: CharSequence,
                    i: Int,
                    i1: Int,
                    i2: Int
                ) {
                }

                override fun onTextChanged(
                    charSequence: CharSequence,
                    i: Int,
                    i1: Int,
                    i2: Int
                ) {
                }

                override fun afterTextChanged(editable: Editable) {
                    try {
                        adSizeSettings?.width = editable.toString().toInt()
                    } catch (e: Exception) {
                        // Couldn't parse. Likely from a backspace or illegal character.
                    }
                }
            })
            adHeightEditText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    charSequence: CharSequence,
                    i: Int,
                    i1: Int,
                    i2: Int
                ) {
                }

                override fun onTextChanged(
                    charSequence: CharSequence,
                    i: Int,
                    i1: Int,
                    i2: Int
                ) {
                }

                override fun afterTextChanged(editable: Editable) {
                    try {
                        adSizeSettings?.height = editable.toString().toInt()
                    } catch (e: Exception) {
                        // Couldn't parse. Likely from a backspace or illegal character.
                    }
                }
            })
            val adSizeSelectionListener: OnItemSelectedListener = object : OnItemSelectedListener {
                override fun onItemSelected(
                    adapterView: AdapterView<*>,
                    view: View,
                    i: Int,
                    l: Long
                ) {
                    adSizeSettings?.adSize = adapterView.selectedItem as MoPubAdSize
                    updateViews()
                }

                override fun onNothingSelected(adapterView: AdapterView<*>?) {
                    // STUB
                }
            }
            val adWidthSelectionListener: OnItemSelectedListener = object : OnItemSelectedListener {
                override fun onItemSelected(
                    adapterView: AdapterView<*>?,
                    view: View,
                    i: Int,
                    l: Long
                ) {
                    val selected = AdSizeOption.values()[i]
                    adSizeSettings?.let {
                        if (AdSizeOption.EXACT != selected) { // "MATCH_PARENT" or "WRAP_CONTENT"
                            it.width = selected.layoutParamsValue
                        } else if (it.width < 0) {
                            it.width = 0
                        }
                    }
                    updateViews()
                }

                override fun onNothingSelected(adapterView: AdapterView<*>?) {
                    // STUB
                }
            }
            val adHeightSelectionListener: OnItemSelectedListener =
                object : OnItemSelectedListener {
                    override fun onItemSelected(
                        adapterView: AdapterView<*>?,
                        view: View,
                        i: Int,
                        l: Long
                    ) {
                        val selected = AdSizeOption.values()[i]
                        adSizeSettings?.let {
                            if (AdSizeOption.EXACT != selected) { // "MATCH_PARENT" or "WRAP_CONTENT"
                                it.height = selected.layoutParamsValue
                            } else if (it.height < 0) {
                                it.height = 0
                            }
                        }
                        updateViews()
                    }

                    override fun onNothingSelected(adapterView: AdapterView<*>?) {
                        // STUB
                    }
                }
            activity?.let {
                adSizeSpinner.adapter = ArrayAdapter(
                    it,
                    android.R.layout.simple_spinner_dropdown_item,
                    android.R.id.text1,
                    MoPubAdSize.values()
                )
            }

            adSizeSpinner.onItemSelectedListener = adSizeSelectionListener
            adWidthSpinner.onItemSelectedListener = adWidthSelectionListener
            adHeightSpinner.onItemSelectedListener = adHeightSelectionListener
            updateViews()
            dialog.setView(view)
            return dialog
        }

        private fun updateViews() {
            // MoPubAdSize
            adSizeSettings?.adSize?.ordinal?.let {
                adSizeSpinner.setSelection(it)
            }

            // View Width
            adSizeSettings?.width?.let {
                adWidthSpinner.setSelection(AdSizeOption.fromLayoutParamsValue(it).ordinal)
            }
            if (adSizeSettings?.width == ViewGroup.LayoutParams.MATCH_PARENT || adSizeSettings?.width == ViewGroup.LayoutParams.WRAP_CONTENT) {
                adWidthEditText.visibility = View.GONE
            } else {
                adWidthEditText.visibility = View.VISIBLE
                adWidthEditText.setText(adSizeSettings?.width?.toString() ?: "")
            }

            // View Height
            adSizeSettings?.height?.let {
                adHeightSpinner.setSelection(AdSizeOption.fromLayoutParamsValue(it).ordinal)
            }
            if (adSizeSettings?.height == ViewGroup.LayoutParams.MATCH_PARENT || adSizeSettings?.height == ViewGroup.LayoutParams.WRAP_CONTENT) {
                adHeightEditText.visibility = View.GONE
            } else {
                adHeightEditText.visibility = View.VISIBLE
                adHeightEditText.setText(adSizeSettings?.height?.toString() ?: "")
            }
        }

        companion object {
            fun newInstance(adSizeSettings: MoPubAdSizeSettings): MoPubAdSizeFragment {
                val fragment = MoPubAdSizeFragment()
                fragment.adSizeSettings = adSizeSettings
                return fragment
            }
        }
    }
}
