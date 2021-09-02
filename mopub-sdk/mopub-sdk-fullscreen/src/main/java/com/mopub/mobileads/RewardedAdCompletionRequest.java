// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.network.MoPubNetworkResponse;
import com.mopub.network.MoPubRequest;
import com.mopub.network.MoPubRequestUtils;
import com.mopub.network.MoPubResponse;
import com.mopub.network.MoPubRetryPolicy;

import java.util.Map;

/**
 * The actual class making the rewarded ad completion request. Since we actually only care about the
 * status code of the request, that's the only thing that is delivered.
 */
public class RewardedAdCompletionRequest extends MoPubRequest<Integer> {

    public interface RewardedAdCompletionRequestListener extends MoPubResponse.Listener<Integer> {
    }

    @NonNull
    final RewardedAdCompletionRequestListener mListener;

    public RewardedAdCompletionRequest(@NonNull final Context context,
                                       @NonNull final String url,
                                       @NonNull final MoPubRetryPolicy retryPolicy,
                                       @NonNull final RewardedAdCompletionRequestListener listener) {
        super(context,
                url,
                MoPubRequestUtils.truncateQueryParamsIfPost(url),
                MoPubRequestUtils.chooseMethod(url),
                listener);
        setShouldCache(false);
        setRetryPolicy(retryPolicy);
        mListener = listener;
    }

    @Nullable
    @Override
    protected Map<String, String> getParams() {
        if (!MoPubRequestUtils.isMoPubRequest(getUrl())) {
            return null;
        }
        return super.getParams();
    }

    @NonNull
    @Override
    protected String getBodyContentType() {
        if (MoPubRequestUtils.isMoPubRequest(getUrl())) {
            return JSON_CONTENT_TYPE;
        }
        return super.getBodyContentType();
    }

    @Override
    protected MoPubResponse<Integer> parseNetworkResponse(final MoPubNetworkResponse networkResponse) {
        return MoPubResponse.success(networkResponse.getStatusCode(), networkResponse);
    }

    @Override
    protected void deliverResponse(@NonNull final Integer response) {
        mListener.onResponse(response);
    }
}
