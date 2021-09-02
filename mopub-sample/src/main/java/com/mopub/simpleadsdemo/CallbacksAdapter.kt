// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.simpleadsdemo

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.caller.notes.R

import java.util.ArrayList

class CallbacksAdapter internal constructor(context: Context) :
    RecyclerView.Adapter<CallbacksAdapter.ViewHolder>() {
    private val backgroundLight: Int = ContextCompat.getColor(context, R.color.listLight)
    private val backgroundDark: Int = ContextCompat.getColor(context, R.color.listDark)
    private val callbacks: MutableList<CallbackDataItem> = ArrayList()
    override fun onCreateViewHolder(
        parent: ViewGroup,
        i: Int
    ): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.callback_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        viewHolder: ViewHolder,
        i: Int
    ) {
        val callback: CallbackDataItem = try {
            callbacks[i]
        } catch (e: IndexOutOfBoundsException) {
            Log.e(
                TAG,
                "Index out of bounds exception when binding CallbacksAdapter.",
                e
            )
            return
        }
        viewHolder.callbackNameTextView.text = callback.callbackName
        val additionalData = callback.additionalData
        if (!additionalData.isNullOrEmpty()) {
            viewHolder.additionalDataTextView.text = additionalData
        } else {
            viewHolder.additionalDataTextView.text = ""
        }
        if (callback.called) {
            viewHolder.checkMarkImageView.visibility = View.VISIBLE
            viewHolder.callbackNameTextView.isEnabled = true
        } else {
            viewHolder.checkMarkImageView.visibility = View.INVISIBLE
            viewHolder.callbackNameTextView.isEnabled = false
        }
        val color = if (i and 1 == 0) backgroundDark else backgroundLight
        viewHolder.itemView.setBackgroundColor(color)
    }

    override fun getItemCount(): Int {
        return callbacks.size
    }

    fun generateCallbackList(callbacksEnumClass: Class<out Enum<*>>) {
        callbacks.clear()
        callbacksEnumClass.enumConstants?.forEach {
            callbacks.add(CallbackDataItem(it.toString()))
        }
        notifyDataSetChanged()
    }

    @JvmOverloads
    fun notifyCallbackCalled(
        methodName: String,
        additionalData: String? = null
    ) {
        for (item in callbacks) {
            if (item.callbackName == methodName) {
                item.called = true
                if (!additionalData.isNullOrEmpty()) {
                    item.additionalData = additionalData
                }
                notifyDataSetChanged()
                break
            }
        }
    }

    class ViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        val callbackNameTextView: TextView = itemView.findViewById(R.id.callback_name_tv)
        val additionalDataTextView: TextView = itemView.findViewById(R.id.additional_data_tv)
        val checkMarkImageView: ImageView = itemView.findViewById(R.id.checkmark_iv)
        val parentView: View = itemView
    }

    companion object {
        private val TAG = CallbacksAdapter::class.java.name
    }
}
