// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.simpleadsdemo

import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.caller.notes.R

/**
 * ViewHolder data object that parses and stores named child Views for sample app DetailFragments,
 * e.g. [InterstitialDetailFragment].
 */
internal class DetailFragmentViewHolder private constructor(
    val descriptionView: TextView,
    val adUnitIdView: TextView,
    val loadButton: Button,
    val showButton: Button?,
    val changeAdSizeButton: Button?,
    val shareButton: Button?,
    val keywordsField: EditText,
    val userDataKeywordsField: EditText,
    val customDataField: EditText?,
    val adSizeInfoView: TextView?
) {

    companion object {
        fun fromView(view: View): DetailFragmentViewHolder {
            val descriptionView =
                view.findViewById<TextView>(R.id.description)
            val adUnitIdView =
                view.findViewById<TextView>(R.id.ad_unit_id)
            val loadButton =
                view.findViewById<Button>(R.id.load_button)
            val showButton =
                view.findViewById<Button>(R.id.show_button)
            val changeAdSizeButton =
                view.findViewById<Button>(R.id.ad_size_button)
            val shareButton =
                view.findViewById<Button>(R.id.share_ad_request_button)
            val keywordsField =
                view.findViewById<EditText>(R.id.keywords_field)
            val userDataKeywordsField =
                view.findViewById<EditText>(R.id.user_data_keywords_field)
            val customDataField =
                view.findViewById<EditText>(R.id.custom_data_field)
            val adSizeInfoView =
                view.findViewById<TextView>(R.id.ad_size_info)
            return DetailFragmentViewHolder(
                descriptionView,
                adUnitIdView,
                loadButton,
                showButton,
                changeAdSizeButton,
                shareButton,
                keywordsField,
                userDataKeywordsField,
                customDataField,
                adSizeInfoView
            )
        }
    }
}
