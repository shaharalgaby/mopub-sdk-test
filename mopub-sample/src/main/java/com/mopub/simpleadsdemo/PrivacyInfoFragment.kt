// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.simpleadsdemo

import android.app.Activity
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView

import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.caller.notes.R

import com.mopub.common.ClientMetadata
import com.mopub.common.MoPub
import com.mopub.common.privacy.ConsentStatus

import java.util.ArrayList

class PrivacyInfoFragment : Fragment() {
    class PrivacyItem internal constructor(
        title: String?,
        value: String?,
        description: String?
    ) {
        val title: String = title ?: ""
        val description: String = description ?: ""
        val value: String = value ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(
            R.layout.privacy_info_fragment,
            container,
            false
        )
        val privacySettings = readPrivacySettings()
        if (privacySettings.isNotEmpty()) {
            val recyclerView: RecyclerView = view.findViewById(R.id.privacy_recycler_view)
            recyclerView.layoutManager = LinearLayoutManager(context)
            recyclerView.adapter = PrivacyAdapter(privacySettings)
            recyclerView.addItemDecoration(
                DividerItemDecoration(
                    recyclerView.context,
                    DividerItemDecoration.VERTICAL
                )
            )
        }
        val closeButton = view.findViewById<Button>(R.id.privacy_close_btn)
        closeButton.setOnClickListener {
            val activity: Activity? = activity
            activity?.onBackPressed()
        }
        return view
    }

    internal inner class PrivacyAdapter(private var privacyInfo: List<PrivacyItem>) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun onCreateViewHolder(
            viewGroup: ViewGroup,
            viewType: Int
        ): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(viewGroup.context)
            val viewHolder: RecyclerView.ViewHolder
            if (viewType == TYPE_PRIVACY_INFO) {
                val itemView = inflater.inflate(
                    R.layout.privacy_info_item,
                    viewGroup,
                    false
                )
                viewHolder =
                    ViewHolder(itemView)
                itemView.tag = viewHolder
            } else {
                val itemView = inflater.inflate(
                    R.layout.privacy_info_divider,
                    viewGroup,
                    false
                )
                viewHolder = DividerViewHolder(itemView)
                itemView.tag = viewHolder
            }
            return viewHolder
        }

        override fun onBindViewHolder(
            viewHolder: RecyclerView.ViewHolder,
            i: Int
        ) {
            val item = privacyInfo[i]
            if (isContentItem(item)) {
                val holder = viewHolder as ViewHolder
                holder.titleTextView.text = item.title
                holder.descTextView.text = item.description
                holder.valueTextView.text = item.value
            } else {
                val holder = viewHolder as DividerViewHolder
                holder.dividerTextView.text = item.description
            }
        }

        override fun getItemCount(): Int {
            return privacyInfo.size
        }

        override fun getItemViewType(position: Int): Int {
            val item = privacyInfo[position]
            return if (isContentItem(item)) TYPE_PRIVACY_INFO else TYPE_DIVIDER
        }

        // ViewHolder
        internal inner class ViewHolder(itemView: View) :
            RecyclerView.ViewHolder(itemView) {
            val titleTextView: TextView = itemView.findViewById(R.id.privacy_title_view)
            val descTextView: TextView = itemView.findViewById(R.id.privacy_desc_view)
            val valueTextView: TextView = itemView.findViewById(R.id.privacy_value_view)
        }

        // divider
        internal inner class DividerViewHolder(itemView: View) :
            RecyclerView.ViewHolder(itemView) {
            val dividerTextView: TextView = itemView.findViewById(R.id.text_divider)
        }
    }

    private fun readPrivacySettings(): List<PrivacyItem> {
        val manager = MoPub.getPersonalInformationManager()
        val context = context
        if (manager == null || context == null) {
            return ArrayList()
        }
        val consentData = manager.consentData
        val status = manager.personalInfoConsentStatus
        val gdprApplies = manager.gdprApplies()
        val advertisingId =
            ClientMetadata.getInstance(context).moPubIdentifier.advertisingInfo
        val gdprAppliesString = if (gdprApplies == null || gdprApplies) "true" else "false"
        val checkmarkUnicode = "\u2705"
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        )
        val list = ArrayList<PrivacyItem>()
        list.add(PrivacyItem("", "", "Identifiers"))
        list.add(
            PrivacyItem(
                "MoPub ID", String.format("\n%s", advertisingId.getIdentifier(false)),
                if (manager.canCollectPersonalInformation()) "" else checkmarkUnicode
            )
        )
        list.add(
            PrivacyItem(
                "Advertising ID", String.format("\n%s", advertisingId.getIdentifier(true)),
                if (manager.canCollectPersonalInformation()) checkmarkUnicode else ""
            )
        )
        list.add(PrivacyItem("Android ID", androidId, ""))
        list.add(PrivacyItem("", "", "Allowable Data Collection"))
        list.add(PrivacyItem("Is GDPR applicable?", gdprAppliesString, ""))
        list.add(PrivacyItem("Consent Status", status.value, ""))
        list.add(
            PrivacyItem(
                "Can Collect PII",
                if (manager.canCollectPersonalInformation()) "true" else "false",
                ""
            )
        )
        list.add(
            PrivacyItem(
                "Should Show Consent Dialog",
                if (manager.shouldShowConsentDialog()) "true" else "false",
                ""
            )
        )
        list.add(
            PrivacyItem(
                "Is Whitelisted",
                if (status == ConsentStatus.POTENTIAL_WHITELIST) "true" else "false",
                ""
            )
        )
        list.add(
            PrivacyItem(
                "DNT Enabled",
                String.format("\n%s", advertisingId.isDoNotTrack),
                ""
            )
        )
        list.add(PrivacyItem("", "", "Current Versions"))
        list.add(PrivacyItem("Current Vendor List Url", "", consentData.currentVendorListLink))
        list.add(
            PrivacyItem(
                "Current Vendor List Version",
                consentData.currentVendorListVersion,
                ""
            )
        )
        list.add(
            PrivacyItem(
                "Current Privacy Policy Url",
                "",
                consentData.currentPrivacyPolicyLink
            )
        )
        list.add(
            PrivacyItem(
                "Current Privacy Policy Version",
                consentData.currentPrivacyPolicyVersion,
                ""
            )
        )
        list.add(
            PrivacyItem(
                "Current IAB Vendor List Format",
                consentData.currentVendorListIabFormat,
                ""
            )
        )
        list.add(PrivacyItem("", "", "Consented Versions"))
        list.add(
            PrivacyItem(
                "Consented Vendor List Version",
                consentData.consentedVendorListVersion,
                ""
            )
        )
        list.add(
            PrivacyItem(
                "Consented Privacy Policy Version",
                consentData.consentedPrivacyPolicyVersion,
                ""
            )
        )
        list.add(
            PrivacyItem(
                "Consented IAB Vendor List Version",
                consentData.consentedVendorListIabFormat,
                ""
            )
        )
        return list
    }

    companion object {
        private const val TYPE_PRIVACY_INFO = 0
        private const val TYPE_DIVIDER = 1
        private fun isContentItem(item: PrivacyItem?): Boolean {
            if (item == null) {
                return true
            }
            return item.title.isNotEmpty() || item.value.isNotEmpty()
        }
    }
}
