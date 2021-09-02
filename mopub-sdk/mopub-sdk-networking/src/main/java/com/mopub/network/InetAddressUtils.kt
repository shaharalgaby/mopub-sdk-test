// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.network

import com.mopub.common.VisibleForTesting
import java.net.InetAddress
import java.net.UnknownHostException

/**
 * This class exists to wrap InetAddress static calls since java.net classes cannot be mocked
 */
object InetAddressUtils {
    private var mockInetAddress: InetAddress? = null

    @JvmStatic
    @Throws(UnknownHostException::class)
    fun getInetAddressByName(host: String?): InetAddress = mockInetAddress ?: InetAddress.getByName(host)

    @JvmStatic
    @Deprecated("")
    @VisibleForTesting
    fun setMockInetAddress(inetAddress: InetAddress?) {
        mockInetAddress = inetAddress
    }
}
