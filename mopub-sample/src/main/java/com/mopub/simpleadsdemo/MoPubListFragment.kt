// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.simpleadsdemo

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*

import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.ListFragment
import com.caller.notes.R

import com.mopub.common.MoPub
import com.mopub.common.logging.MoPubLog
import com.mopub.common.logging.MoPubLog.SdkLogEvent
import com.mopub.common.privacy.ConsentStatus

internal interface TrashCanClickListener {
    fun onTrashCanClicked(adUnit: MoPubSampleAdUnit)
}

class MoPubListFragment : ListFragment(), TrashCanClickListener {
    private var adapter: MoPubSampleListAdapter? = null
    private var adUnitDataSource: AdUnitDataSource? = null
    private var searchBar: EditText? = null
    private var searchBarClearButton: View? = null

    fun addAdUnitViaDeeplink(deeplinkData: Uri?) {
        if (deeplinkData == null) {
            return
        }
        val adUnitId = deeplinkData.getQueryParameter(AD_UNIT_ID_KEY)?.also {
            try {
                Utils.validateAdUnitId(it)
            } catch (e: IllegalArgumentException) {
                Utils.logToast(
                    context,
                    "Ignoring invalid ad unit: $it"
                )
                return@addAdUnitViaDeeplink
            }
        } ?: return

        val format = deeplinkData.getQueryParameter(FORMAT_KEY)
        val adType: MoPubSampleAdUnit.AdType? = MoPubSampleAdUnit.AdType.fromDeeplinkString(format)
        val keywords = deeplinkData.getQueryParameter(KEYWORDS_KEY)
        if (adType == null) {
            Utils.logToast(
                context,
                "Ignoring invalid ad format: $format"
            )
            return
        }
        val name = deeplinkData.getQueryParameter(NAME_KEY)
        val adUnit = MoPubSampleAdUnit.Builder(adUnitId, adType)
            .description(name ?: "")
            .keywords(keywords ?: "")
            .build()
        addAdUnit(adUnit)?.let {
            enterAdFragment(it, keywords, deeplinkData.getQueryParameter(USER_DATA_KEYWORDS_KEY))
        } ?: MoPubLog.log(SdkLogEvent.ERROR, "Failed to create ad unit $adUnit")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(
            R.layout.ad_unit_list_fragment,
            container,
            false
        )
        val button = view.findViewById<View>(R.id.add_ad_unit_button) as Button
        val versionCodeView = view.findViewById<View>(R.id.version_code) as TextView
        versionCodeView.text = getString(R.string.sdk_version, MoPub.SDK_VERSION)
        button.setOnClickListener { onAddClicked() }

        searchBar = view.findViewById(R.id.search_bar_et)
        searchBarClearButton = view.findViewById(R.id.search_bar_clear_button)
        searchBarClearButton?.setOnClickListener {
            searchBar?.text?.clear()
        }
        searchBar?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(text: Editable) {
                val adapter = adapter
                adapter?.filter?.filter(text.toString())
            }
        })
        return view
    }

    override fun onListItemClick(
        listView: ListView,
        view: View,
        position: Int,
        id: Long
    ) {
        super.onListItemClick(listView, view, position, id)
        adapter?.getItem(position)?.let { adConfiguration ->
            enterAdFragment(adConfiguration, adConfiguration.keywords, null)
        }
    }

    private fun enterAdFragment(
        adConfiguration: MoPubSampleAdUnit,
        keywords: String?, userDataKeywords: String?
    ) {
        val activity: FragmentActivity = activity ?: return
        val fragmentTransaction = activity.supportFragmentManager.beginTransaction()
        val fragmentClass = adConfiguration.fragmentClass
        val fragment: Fragment = try {
            fragmentClass.newInstance()
        } catch (e: java.lang.InstantiationException) {
            MoPubLog.log(
                SdkLogEvent.ERROR_WITH_THROWABLE,
                "Error creating fragment for class $fragmentClass",
                e
            )
            return
        } catch (e: IllegalAccessException) {
            MoPubLog.log(
                SdkLogEvent.ERROR_WITH_THROWABLE,
                "Error creating fragment for class $fragmentClass",
                e
            )
            return
        }
        val bundle = adConfiguration.toBundle()
        if (!TextUtils.isEmpty(keywords)) {
            bundle.putString(KEYWORDS_KEY, keywords)
        }
        if (!TextUtils.isEmpty(userDataKeywords)) {
            bundle.putString(USER_DATA_KEYWORDS_KEY, userDataKeywords)
        }
        fragment.arguments = bundle
        fragmentManager?.apply {
            if (backStackEntryCount > 0) {
                popBackStack()
            }
        }

        fragmentTransaction
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onTrashCanClicked(adUnit: MoPubSampleAdUnit) {
        val deleteConfirmation: DialogFragment = DeleteDialogFragment.newInstance(adUnit)
        deleteConfirmation.setTargetFragment(this, 0)
        activity?.let {
            deleteConfirmation.show(it.supportFragmentManager, "delete")
        }
    }

    private fun onAddClicked() {
        val dialogFragment = AddDialogFragment.newInstance()
        dialogFragment.setTargetFragment(this, 0)
        activity?.let {
            dialogFragment.show(it.supportFragmentManager, "add")
        }
    }

    override fun onResume() {
        super.onResume()
        syncDbAdapter()
        Utils.hideSoftKeyboard(listView)
    }

    private fun syncDbAdapter() {
        if (adapter == null) {
            activity?.let {
                adUnitDataSource = AdUnitDataSource(it)
                adapter = MoPubSampleListAdapter(
                    it, this,
                    adUnitDataSource?.allAdUnits as ArrayList<MoPubSampleAdUnit>
                )
            }

            listAdapter = adapter
        }
        searchBar?.let {
            adapter?.filter?.filter(it.text.toString())
        }
        adapter?.sort(MoPubSampleAdUnit.COMPARATOR)
    }

    fun addAdUnit(moPubSampleAdUnit: MoPubSampleAdUnit): MoPubSampleAdUnit? {
        val createdAdUnit = adUnitDataSource?.createSampleAdUnit(moPubSampleAdUnit) ?: return null
        adapter?.let {
            for (i in 0 until it.count) {
                val currentAdUnit = it.getItem(i)
                if (moPubSampleAdUnit.adUnitId == currentAdUnit.adUnitId &&
                    moPubSampleAdUnit.fragmentClassName == currentAdUnit.fragmentClassName &&
                    currentAdUnit.isUserDefined
                ) {
                    it.remove(currentAdUnit)
                    Utils.logToast(
                        context,
                        moPubSampleAdUnit.adUnitId + " replaced."
                    )
                    break
                }
            }
            it.add(createdAdUnit)
        }

        syncDbAdapter()
        return createdAdUnit
    }

    fun deleteAdUnit(moPubSampleAdUnit: MoPubSampleAdUnit) {
        adUnitDataSource?.deleteSampleAdUnit(moPubSampleAdUnit)
        adapter?.remove(moPubSampleAdUnit)
        syncDbAdapter()
    }

    /**
     * Call this function to grant or revoke user consent
     * @param consentGranted - true to grant consent, false to revoke
     * @return - true successfully completed operation, false failed for some reason
     */
    fun onChangeConsent(consentGranted: Boolean): Boolean {
        val personalInfoManager = MoPub.getPersonalInformationManager()
        val view = view
        if (personalInfoManager == null || view == null) {
            MoPubLog.log(
                SdkLogEvent.CUSTOM,
                getString(R.string.pim_is_not_available)
            )
            return false
        }
        val text = view.findViewById<EditText>(R.id.status_change_notification)
        text.visibility = View.VISIBLE
        if (consentGranted) {
            personalInfoManager.grantConsent()
            text.setText(R.string.consent_whitelisted)
        } else {
            if (personalInfoManager.personalInfoConsentStatus == ConsentStatus.DNT) {
                text.setText(R.string.donottrack_text)
                return false
            }
            personalInfoManager.revokeConsent()
            text.setText(R.string.consent_denied)
        }
        return true
    }

    class DeleteDialogFragment : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            return arguments?.let {
                AlertDialog.Builder(activity)
                    .setTitle("Delete Ad Unit " + it.getString(MoPubSampleAdUnit.DESCRIPTION) + "?")
                    .setPositiveButton("Delete") { _, _ ->
                        (targetFragment as MoPubListFragment?)?.deleteAdUnit(
                            MoPubSampleAdUnit.fromBundle(it)
                        )
                    }
                    .setNegativeButton("Cancel") { _, _ -> dismiss() }
                    .setCancelable(true)
                    .create()
            } ?: AlertDialog.Builder(activity).setTitle("Unable To Delete Ad Unit")
                .setPositiveButton(R.string.ok) { _, _ -> }.setCancelable(true).create()
        }

        companion object {
            fun newInstance(adUnit: MoPubSampleAdUnit): DeleteDialogFragment {
                return DeleteDialogFragment().apply {
                    arguments = adUnit.toBundle()
                }
            }
        }
    }

    class AddDialogFragment : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val dialog = AlertDialog.Builder(activity)
                .setTitle("Add a custom Ad Unit")
                .setPositiveButton(
                    "Save ad unit",
                    DialogInterface.OnClickListener { dialogInterface, _ ->
                        val dialog = dialogInterface as AlertDialog
                        val adUnitIdField =
                            dialog.findViewById<View>(R.id.add_ad_unit_id) as EditText
                        val adTypeSpinner =
                            dialog.findViewById<View>(R.id.add_ad_unit_type) as Spinner
                        val descriptionField =
                            dialog.findViewById<View>(R.id.add_ad_unit_description) as EditText
                        val keywordsField =
                            dialog.findViewById<View>(R.id.add_ad_unit_keywords) as EditText

                        // Verify data:
                        try {
                            Utils.validateAdUnitId(
                                adUnitIdField.text.toString()
                            )
                        } catch (e: IllegalArgumentException) {
                            // Input is not valid.
                            val toast =
                                Toast.makeText(activity, "Ad Unit ID invalid", Toast.LENGTH_SHORT)
                            toast.show()
                            return@OnClickListener
                        }

                        // Create ad unit and save it in the database:
                        val adUnitId = adUnitIdField.text.toString()
                        val adType =
                            adTypes[adTypeSpinner.selectedItemPosition]
                        val description = descriptionField.text.toString()
                        val keywords = keywordsField.text.toString()
                        val sampleAdUnit =
                            MoPubSampleAdUnit.Builder(adUnitId, adType)
                                .description(description)
                                .keywords(keywords)
                                .isUserDefined(true)
                                .build()
                        (targetFragment as MoPubListFragment).addAdUnit(sampleAdUnit)
                        dismiss()
                    })
                .setNegativeButton("Cancel") { _, _ -> dismiss() }
                .setCancelable(true)
                .create()

            // Inflate and add our custom layout to the dialog.
            val view = dialog.layoutInflater.inflate(R.layout.ad_config_dialog, null)
            val spinner = view.findViewById<View>(R.id.add_ad_unit_type) as Spinner
            val adTypeStrings: MutableList<String?> = ArrayList(adTypes.size)
            for (adType in adTypes) {
                adTypeStrings.add(adType.displayName)
            }
            activity?.let {
                spinner.adapter = ArrayAdapter(
                    it,
                    android.R.layout.simple_spinner_dropdown_item, android.R.id.text1, adTypeStrings
                )
            }
            dialog.setView(view)
            return dialog
        }

        companion object {
            fun newInstance(): AddDialogFragment {
                return AddDialogFragment()
            }
        }
    }

    companion object {
        private const val AD_UNIT_ID_KEY = "adUnitId"
        private const val FORMAT_KEY = "format"
        const val KEYWORDS_KEY = "keywords"
        const val USER_DATA_KEYWORDS_KEY = "user_data_keywords"
        private const val NAME_KEY = "name"
        private val adTypes =
            MoPubSampleAdUnit.AdType.values()
    }
}
