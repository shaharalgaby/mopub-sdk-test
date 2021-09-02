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
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.caller.notes.R

import com.mopub.common.MoPub

class NetworksInfoFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(
            R.layout.networks_info_fragment,
            container,
            false
        )
        MoPub.getAdapterConfigurationInfo()?.let {
            if (it.isNotEmpty()) {
                val recyclerView: RecyclerView =
                    view.findViewById(R.id.networks_recycler_view)
                recyclerView.layoutManager = LinearLayoutManager(context)
                recyclerView.adapter = NetworksInfoAdapter(it)
                recyclerView.addItemDecoration(
                    DividerItemDecoration(
                        recyclerView.context,
                        DividerItemDecoration.VERTICAL
                    )
                )
                view.findViewById<View>(R.id.text_no_adapters).visibility = View.GONE
            }
        }

        view.findViewById<Button>(R.id.networks_close_btn).setOnClickListener {
            activity?.onBackPressed()
        }
        return view
    }

    internal inner class NetworksInfoAdapter(private var networksInfo: List<String>) :
        RecyclerView.Adapter<NetworksInfoAdapter.ViewHolder>() {
        override fun onCreateViewHolder(
            viewGroup: ViewGroup,
            i: Int
        ): ViewHolder {
            val itemView = LayoutInflater.from(viewGroup.context)
                .inflate(android.R.layout.simple_selectable_list_item, viewGroup, false)
            val viewHolder = ViewHolder(itemView)
            itemView.tag = viewHolder
            return viewHolder
        }

        override fun onBindViewHolder(
            viewHolder: ViewHolder,
            i: Int
        ) {
            viewHolder.nameTextView.text = networksInfo[i]
        }

        override fun getItemCount(): Int {
            return networksInfo.size
        }

        internal inner class ViewHolder(itemView: View) :
            RecyclerView.ViewHolder(itemView) {
            val nameTextView: TextView = itemView.findViewById(android.R.id.text1)
        }
    }
}
