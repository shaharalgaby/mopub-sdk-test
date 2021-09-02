// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.simpleadsdemo

import android.content.Context
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast

internal object Utils {
    private const val LOGTAG = "MoPub Sample App"

    fun validateAdUnitId(adUnitId: String?) {
        requireNotNull(adUnitId) { "Invalid Ad Unit ID: null ad unit." }
        require(adUnitId.isNotEmpty()) { "Invalid Ad Unit Id: empty ad unit." }
    }

    fun hideSoftKeyboard(view: View) {
        val inputMethodManager = view.context
            .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    fun isAlphaNumeric(input: String): Boolean {
        return input.matches("^[a-zA-Z0-9-_]*$".toRegex())
    }

    fun logToast(context: Context?, message: String?) {
        Log.d(LOGTAG, "$message")
        context?.applicationContext?.let {
            Toast.makeText(it, message, Toast.LENGTH_SHORT).show()
        }
    }
}
