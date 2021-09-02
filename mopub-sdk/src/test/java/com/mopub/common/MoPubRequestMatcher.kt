// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common

import com.mopub.network.MoPubRequest

import org.mockito.ArgumentMatcher

/**
 * A Mockito Request Matcher, used in tests to allow verifying that MoPubRequests match a given url.
 * <p>
 * "verify(mock).add(argThat(MoPubRequestMatcher.isUrl("testUrl")));"
 */
class MoPubRequestMatcher private constructor(
    private val url: String,
    private val matchType: MatchType
) : ArgumentMatcher<MoPubRequest<*>>() {

    internal enum class MatchType {
        EXACT, STARTS_WITH
    }

    companion object {
        @JvmStatic
        fun isUrl(url: String) = MoPubRequestMatcher(url, MatchType.EXACT)

        @JvmStatic
        fun isUrlStartingWith(url: String) = MoPubRequestMatcher(url, MatchType.STARTS_WITH)
    }

    override fun matches(that: Any?): Boolean {
        return when (matchType) {
            MatchType.STARTS_WITH -> that is MoPubRequest<*>
                    && (that.getUrl().startsWith(url))
            MatchType.EXACT -> that is MoPubRequest<*>
                    && (that.getUrl() == url)
        }
    }
}
