// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.simpleadsdemo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView

import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.caller.notes.R

import java.util.ArrayList

/**
 * A simple [Fragment] subclass.
 * Use the [ImpressionsInfoFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ImpressionsInfoFragment : Fragment() {
    private lateinit var adapter: ImpressionsInfoAdapter
    private lateinit var fragmentView: View

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val impressionsList: ArrayList<String?>? = arguments?.getStringArrayList(ARG_IMPRESSIONS_LIST)

        // Inflate the layout for this fragment
        fragmentView = inflater.inflate(
            R.layout.fragment_impressions_info,
            container,
            false
        )
        val recyclerView: RecyclerView = fragmentView.findViewById(R.id.impressions_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = ImpressionsInfoAdapter(context!!, impressionsList)
        recyclerView.adapter = adapter
        recyclerView.addItemDecoration(
            DividerItemDecoration(
                recyclerView.context,
                DividerItemDecoration.VERTICAL
            )
        )
        recyclerView.descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
        if (adapter.stringList.size > 0) {
            fragmentView.findViewById<View>(R.id.text_no_impressions).visibility = View.GONE
        }
        val closeButton = fragmentView.findViewById<Button>(R.id.close_btn)
        closeButton.setOnClickListener {
            activity?.onBackPressed()
        }
        return fragmentView
    }

    fun onClear() {
        fragmentView.findViewById<View>(R.id.text_no_impressions).visibility = View.VISIBLE
        adapter.stringList.clear()
        adapter.notifyDataSetChanged()
    }

    /**
     * RecyclerView adapter for the impression data list
     */
    internal class ImpressionsInfoAdapter(
        context: Context,
        list: MutableList<String?>?
    ) : RecyclerView.Adapter<ImpressionsInfoAdapter.ImpressionsViewHolder>() {
        private val mBackgroundLight: Int = ContextCompat.getColor(context, R.color.listLight)
        private val mBackgroundDark: Int = ContextCompat.getColor(context, R.color.listDark)
        var stringList: MutableList<String?> = list ?: ArrayList()
        override fun onCreateViewHolder(
            viewGroup: ViewGroup,
            i: Int
        ): ImpressionsViewHolder {
            val itemView = LayoutInflater.from(viewGroup.context)
                .inflate(android.R.layout.simple_list_item_1, viewGroup, false)
            return ImpressionsViewHolder(itemView)
        }

        override fun onBindViewHolder(
            viewHolder: ImpressionsViewHolder,
            i: Int
        ) {
            val color = if (i and 1 == 0) mBackgroundDark else mBackgroundLight
            viewHolder.bindViewHolder(stringList[i], color)
        }

        override fun getItemCount(): Int {
            return stringList.size
        }

        /**
         * ViewHolder
         */
        internal class ImpressionsViewHolder(itemView: View) :
            RecyclerView.ViewHolder(itemView) {
            private val infoTextView: TextView
            fun bindViewHolder(text: String?, color: Int) {
                itemView.setBackgroundColor(color)
                infoTextView.text = text
            }

            private fun shareImpressionData(textView: TextView) {
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra(Intent.EXTRA_SUBJECT, "impression data")
                    putExtra(Intent.EXTRA_TEXT, textView.text.toString())
                    textView.context.startActivity(
                        Intent.createChooser(
                            this,
                            textView.resources.getString(R.string.share_impression)
                        )
                    )
                }
            }

            init {
                itemView.tag = this
                infoTextView = itemView.findViewById(android.R.id.text1)
                infoTextView.setOnLongClickListener {
                    shareImpressionData(infoTextView)
                    true
                }
            }
        }
    }

    companion object {
        private const val ARG_IMPRESSIONS_LIST = "list_of_impressions"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameter.
         *
         * @param impressions - list of impressions.
         * @return A new instance of fragment ImpressionsInfoFragment.
         */
        fun newInstance(impressions: ArrayList<String>): ImpressionsInfoFragment {
            val fragment = ImpressionsInfoFragment()
            val args = Bundle()
            args.putStringArrayList(
                ARG_IMPRESSIONS_LIST,
                impressions
            )
            fragment.arguments = args
            return fragment
        }
    }
}
