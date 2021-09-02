// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.simpleadsdemo

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.ImageView
import android.widget.TextView
import com.caller.notes.R

import java.util.Locale

internal class MoPubSampleListAdapter(
    context: Context,
    private val listener: TrashCanClickListener,
    private val allAdUnits: ArrayList<MoPubSampleAdUnit>
) : ArrayAdapter<MoPubSampleAdUnit>(context, 0, allAdUnits) {

    internal class ViewHolder {
        var separator: TextView? = null
        var description: TextView? = null
        var adUnitId: TextView? = null
        var trashCan: ImageView? = null
    }

    private val layoutInflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private var filteredAdUnits = arrayListOf<MoPubSampleAdUnit>()
    private val adTypeStrings = MoPubSampleAdUnit.AdType.values().map {
        it.displayName.toLowerCase(Locale.US)
    }

    init {
        filteredAdUnits.addAll(allAdUnits)
    }

    override fun getCount() = filteredAdUnits.size

    override fun getItem(position: Int) = filteredAdUnits[position]

    override fun getItemId(position: Int) = filteredAdUnits[position].id

    override fun getView(
        position: Int,
        convertView: View?,
        parent: ViewGroup
    ): View {
        val view: View
        val viewHolder: ViewHolder

        if (convertView == null) {
            view = layoutInflater.inflate(
                R.layout.ad_configuration_list_item,
                parent,
                false
            )
            viewHolder = ViewHolder()
            viewHolder.separator =
                view.findViewById<View>(R.id.separator) as TextView
            viewHolder.description =
                view.findViewById<View>(R.id.banner_description) as TextView
            viewHolder.adUnitId =
                view.findViewById<View>(R.id.banner_ad_unit_id) as TextView
            viewHolder.trashCan =
                view.findViewById<View>(R.id.banner_delete) as ImageView
        } else {
            view = convertView
            viewHolder = view.tag as ViewHolder
        }
        view.tag = viewHolder
        val sampleAdUnit = getItem(position)
        viewHolder.description?.text = sampleAdUnit.description
        viewHolder.adUnitId?.text = (sampleAdUnit.adUnitId)

        if (isFirstInSection(position)) {
            viewHolder.separator?.visibility = View.VISIBLE
            viewHolder.separator?.text = (sampleAdUnit.headerName)
        } else {
            viewHolder.separator?.visibility = View.GONE
        }

        if (sampleAdUnit.isUserDefined) {
            viewHolder.trashCan?.visibility = View.VISIBLE
            viewHolder.trashCan?.setOnClickListener { listener.onTrashCanClicked(sampleAdUnit) }
        } else {
            viewHolder.trashCan?.visibility = View.INVISIBLE
            viewHolder.trashCan?.setOnClickListener(null)
        }

        return view
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun publishResults(charSequence: CharSequence?, filterResults: FilterResults) {
                filteredAdUnits = filterResults.values as ArrayList<MoPubSampleAdUnit>
                notifyDataSetChanged()
            }

            override fun performFiltering(charSequence: CharSequence?): FilterResults {
                val queryString = charSequence?.toString()?.toLowerCase(Locale.US)?.trim()
                val filterResults = FilterResults()

                if (queryString.isNullOrEmpty()) {
                    filterResults.values = allAdUnits
                    filterResults.count = allAdUnits.size
                } else {
                    // Avoid ConcurrentModificationException when using filter
                    val allAdUnitsCopy = ArrayList(allAdUnits)

                    val filterList : ArrayList<MoPubSampleAdUnit> = when {
                        // filter by section name/ad type
                        adTypeStrings.any { it.contains(queryString) } -> {
                            allAdUnitsCopy.filter {
                                it.mAdType.displayName.toLowerCase(Locale.US).contains(queryString)
                            } as ArrayList<MoPubSampleAdUnit>
                        }
                        // filter for custom ad units
                        "mine" == queryString -> {
                            allAdUnitsCopy.filter {
                                it.isUserDefined
                            } as ArrayList<MoPubSampleAdUnit>
                        }
                        else -> {
                            allAdUnitsCopy.filter {
                                it.description?.toLowerCase(Locale.US)?.contains(queryString) ?: false ||
                                        it.adUnitId.startsWith(queryString)
                            } as ArrayList<MoPubSampleAdUnit>
                        }
                    }
                    filterResults.values = filterList
                    filterResults.count = filterList.size
                }
                return filterResults
            }
        }
    }

    private fun isFirstInSection(position: Int) =
        if (position <= 0) {
            true
        } else {
            val previous = getItem(position - 1)
            val current = getItem(position)
            previous.headerName != current.headerName
        }
}
