// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.simpleadsdemo

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button

import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.caller.notes.R

import com.mopub.common.MoPubReward
import com.mopub.mobileads.MoPubErrorCode
import com.mopub.mobileads.MoPubRewardedAdListener
import com.mopub.mobileads.MoPubRewardedAdManager
import com.mopub.mobileads.MoPubRewardedAds
import com.mopub.mobileads.MoPubRewardedAds.getAvailableRewards
import com.mopub.mobileads.MoPubRewardedAds.loadRewardedAd
import com.mopub.mobileads.MoPubRewardedAds.setRewardedAdListener
import com.mopub.mobileads.MoPubRewardedAds.showRewardedAd

import java.util.HashMap
import java.util.Locale

class RewardedAdDetailFragment : Fragment(),
    MoPubRewardedAdListener {
    private var showButton: Button? = null
    private var adUnitId: String? = null
    private val moPubRewardsMap: MutableMap<String, MoPubReward> = HashMap()
    private var callbacksAdapter: CallbacksAdapter? = null
    private lateinit var viewHolder: DetailFragmentViewHolder

    private enum class RewardedCallbacks(private val callbackName: String) {
        LOADED("onRewardedAdLoadSuccess"),
        FAILED("onRewardedAdLoadFailed"),
        STARTED("onRewardedAdStarted"),
        SHOW_ERROR("onRewardedAdShowError"),
        CLICKED("onRewardedAdClicked"),
        CLOSED("onRewardedAdClosed"),
        COMPLETED("onRewardedAdCompleted");

        override fun toString() = callbackName
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
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
                it.getString(
                    MoPubListFragment.KEYWORDS_KEY,
                    ""
                )
            )
            viewHolder.userDataKeywordsField.setText(
                it.getString(
                    MoPubListFragment.USER_DATA_KEYWORDS_KEY,
                    ""
                )
            )
        }

        Utils.hideSoftKeyboard(viewHolder.keywordsField)
        Utils.hideSoftKeyboard(viewHolder.userDataKeywordsField)
        setRewardedAdListener(this)
        adUnitId = adConfiguration.adUnitId
        viewHolder.descriptionView.text = adConfiguration.description
        viewHolder.adUnitIdView.text = adUnitId ?: ""
        viewHolder.loadButton.setOnClickListener(View.OnClickListener {
            val adUnit = adUnitId ?: return@OnClickListener
            callbacksAdapter?.generateCallbackList(RewardedCallbacks::class.java)
            loadRewardedAd(
                adUnit,
                MoPubRewardedAdManager.RequestParameters(
                    viewHolder.keywordsField.text.toString(),
                    viewHolder.userDataKeywordsField.text.toString(),
                    null,
                    "sample_app_customer_id"
                )
            )
            viewHolder.shareButton?.isEnabled = false
            showButton?.isEnabled = false
        })
        showButton = viewHolder.showButton?.apply {
            isEnabled = false
            setOnClickListener {
                adUnitId?.let {
                    showRewardedAd(it, viewHolder.customDataField?.text.toString())
                }
            }
        }
        viewHolder.shareButton?.setOnClickListener { onShareClicked() }

        viewHolder.customDataField?.visibility = View.VISIBLE
        val callbacksView: RecyclerView = view.findViewById(R.id.callbacks_recycler_view)
        context?.let {
            callbacksView.layoutManager = LinearLayoutManager(it)
            callbacksView.adapter = CallbacksAdapter(it).apply {
                generateCallbackList(RewardedCallbacks::class.java)
                callbacksAdapter = this
            }
        }
        return view
    }

    override fun onDestroyView() {
        setRewardedAdListener(null)
        super.onDestroyView()
    }

    // MoPubRewardedAdListener implementation
    override fun onRewardedAdLoadSuccess(adUnitId: String) {
        if (adUnitId != this.adUnitId) {
            return
        }

        showButton?.isEnabled = true
        callbacksAdapter?.notifyCallbackCalled(RewardedCallbacks.LOADED.toString())
            ?: Utils.logToast(activity, "Rewarded ad loaded.")

        val loadedAdUrl = LastAdRequestUrlManager.getAdRequestUrl(adUnitId)
        if (!loadedAdUrl.isNullOrEmpty()) {
            viewHolder.shareButton?.isEnabled = true
        }

        val availableRewards = getAvailableRewards(adUnitId)

        // Pop up alert dialog for reward selection
        if (availableRewards.size > 0) {
            val selectRewardDialogFragment = SelectRewardDialogFragment.newInstance()

            // The user must select a reward from the dialog
            selectRewardDialogFragment.isCancelable = false

            // Reset rewards mapping and selected reward
            moPubRewardsMap.clear()

            // Initialize mapping between reward string and reward instance
            moPubRewardsMap.putAll(availableRewards.map {
                "${it.amount} ${it.label}" to it
            })
            selectRewardDialogFragment.loadRewards(
                moPubRewardsMap.keys.toTypedArray()
            )
            selectRewardDialogFragment.setTargetFragment(this, 0)
            activity?.let {
                selectRewardDialogFragment.show(it.supportFragmentManager, "selectReward")
            }
        }
    }

    override fun onRewardedAdLoadFailure(
        adUnitId: String,
        errorCode: MoPubErrorCode
    ) {
        if (adUnitId == this.adUnitId) {
            showButton?.isEnabled = false
            callbacksAdapter?.notifyCallbackCalled(
                RewardedCallbacks.FAILED.toString(),
                errorCode.toString()
            ) ?: Utils.logToast(
                activity,
                String.format(Locale.US, "Rewarded ad failed to load: $errorCode")
            )

            val loadedAdUrl = LastAdRequestUrlManager.getAdRequestUrl(adUnitId)
            if (!loadedAdUrl.isNullOrEmpty()) {
                viewHolder.shareButton?.isEnabled = true
            }
        }
    }

    override fun onRewardedAdStarted(adUnitId: String) {
        if (adUnitId == this.adUnitId) {
            showButton?.isEnabled = false
            callbacksAdapter?.notifyCallbackCalled(RewardedCallbacks.STARTED.toString())
                ?: Utils.logToast(activity, "Rewarded ad started.")
        }
    }

    override fun onRewardedAdShowError(
        adUnitId: String,
        errorCode: MoPubErrorCode
    ) {
        if (adUnitId == this.adUnitId) {
            showButton?.isEnabled = false
            callbacksAdapter?.notifyCallbackCalled(
                RewardedCallbacks.SHOW_ERROR.toString(),
                errorCode.toString()
            ) ?: Utils.logToast(
                activity,
                String.format(Locale.US, "Rewarded ad show error: %s", errorCode.toString())
            )
        }
    }

    override fun onRewardedAdClicked(adUnitId: String) {
        if (adUnitId == this.adUnitId) {
            callbacksAdapter?.notifyCallbackCalled(RewardedCallbacks.CLICKED.toString())
                ?: Utils.logToast(activity, "Rewarded ad clicked.")
        }
    }

    override fun onRewardedAdClosed(adUnitId: String) {
        if (adUnitId == this.adUnitId) {
            showButton?.isEnabled = false
            callbacksAdapter?.notifyCallbackCalled(RewardedCallbacks.CLOSED.toString())
                ?: Utils.logToast(activity, "Rewarded ad closed.")
        }
    }

    override fun onRewardedAdCompleted(
        adUnitIds: Set<String?>,
        reward: MoPubReward
    ) {
        if (adUnitIds.contains(adUnitId)) {
            val message = String.format(
                Locale.US,
                "Rewarded ad completed with reward  \"%d %s\"",
                reward.amount,
                reward.label
            )
            callbacksAdapter?.notifyCallbackCalled(RewardedCallbacks.COMPLETED.toString(), message)
                ?: Utils.logToast(activity, message)
        }
    }

    private fun onShareClicked() {
        if (adUnitId.isNullOrEmpty()) {
            Utils.logToast(activity, "Ad unit is null")
            return
        }

        val loadedAdUrl = LastAdRequestUrlManager.getAdRequestUrl(adUnitId)
        if (!loadedAdUrl.isNullOrEmpty()) {
            Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_TEXT, loadedAdUrl)
                type = "text/plain"
                startActivity(Intent.createChooser(this, "Share"))
            }
        }
    }

    fun selectReward(selectedRewardString: String) {
        val selectedReward = moPubRewardsMap[selectedRewardString]
        if (selectedReward == null) {
            Utils.logToast(
                activity,
                "Reward not part of the reward map. Cannot select reward."
            )
            return
        }
        adUnitId?.let {
            MoPubRewardedAds.selectReward(it, selectedReward)
        } ?: Utils.logToast(activity, "Ad unit is null. Cannot select reward.")
    }

    class SelectRewardDialogFragment : DialogFragment() {
        private var rewards: Array<String> = arrayOf("Reward List Empty!")
        private var selectedReward: String? = null
        fun loadRewards(rewards: Array<String>) {
            this.rewards = rewards
        }

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val dialog = AlertDialog.Builder(activity)
                .setTitle("Select a reward")
                .setSingleChoiceItems(rewards, -1) { _, which ->
                    selectedReward = rewards[which]
                }
                .setPositiveButton("Select", null)
                .create()

            // Overriding onShow() of dialog's OnShowListener() and onClick() of the Select button's
            // OnClickListener() to prevent the dialog from dismissing upon any button click without
            // selecting an item first.
            dialog.setOnShowListener { dialogInterface ->
                val selectButton =
                    (dialogInterface as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
                selectButton.setOnClickListener {
                    selectedReward?.let { reward ->
                        (targetFragment as RewardedAdDetailFragment?)?.let {
                            it.selectReward(reward)
                            dismiss()
                        }
                    }
                }
            }
            return dialog
        }

        companion object {
            fun newInstance(): SelectRewardDialogFragment {
                return SelectRewardDialogFragment()
            }
        }
    }
}
