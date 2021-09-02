// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.network;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;

import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.VastErrorCode;
import com.mopub.mobileads.VastMacroHelper;
import com.mopub.mobileads.VastTracker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.mopub.common.logging.MoPubLog.SdkLogEvent.CUSTOM;

public class TrackingRequest extends MoPubRequest<String> {

    // Retrying may cause duplicate impressions
    private static final int ZERO_RETRIES = 0;

    public interface Listener extends MoPubResponse.Listener<String> {}

    @Nullable private final Listener mListener;

    private TrackingRequest(
            @NonNull final Context context,
            @NonNull final String url,
            @Nullable final Listener listener) {
        super(context,
                url,
                MoPubRequestUtils.truncateQueryParamsIfPost(url),
                MoPubRequestUtils.chooseMethod(url),
                listener);
        mListener = listener;
        setShouldCache(false);
        setRetryPolicy(new MoPubRetryPolicy(
                MoPubRetryPolicy.DEFAULT_TIMEOUT_MS,
                ZERO_RETRIES,
                MoPubRetryPolicy.DEFAULT_BACKOFF_MULT));
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
    protected MoPubResponse<String> parseNetworkResponse(final MoPubNetworkResponse networkResponse) {
        if (networkResponse.getStatusCode() != 200) {
            return MoPubResponse.error(
                    new MoPubNetworkError.Builder("Failed to log tracking request. Response code: "
                            + networkResponse.getStatusCode() + " for url: " + getUrl())
                            .reason(MoPubNetworkError.Reason.TRACKING_FAILURE)
                            .build());
        }
        return MoPubResponse.success(Integer.toString(networkResponse.getStatusCode()), networkResponse);
    }

    @Override
    protected void deliverResponse(@NonNull final String data) {
        if (mListener != null) {
            mListener.onResponse(getUrl());
        }
    }

    ///////////////////////////////////////////////////////////////
    // Static helper methods that can be used as utilities:
    //////////////////////////////////////////////////////////////

    public static void makeVastTrackingHttpRequest(
            @NonNull final List<VastTracker> vastTrackers,
            @Nullable final VastErrorCode vastErrorCode,
            @Nullable final Integer contentPlayHead,
            @Nullable final String assetUri,
            @Nullable final Context context) {
        Preconditions.checkNotNull(vastTrackers);

        List<String> trackers = new ArrayList<String>(vastTrackers.size());
        for (VastTracker vastTracker : vastTrackers) {
            if (vastTracker == null) {
                continue;
            }
            if (vastTracker.isTracked() && !vastTracker.isRepeatable()) {
                continue;
            }
            trackers.add(vastTracker.getContent());
            vastTracker.setTracked();
        }

        makeTrackingHttpRequest(
                new VastMacroHelper(trackers)
                        .withErrorCode(vastErrorCode)
                        .withContentPlayHead(contentPlayHead)
                        .withAssetUri(assetUri)
                        .getUris(),
                context
        );
    }

    public static void makeTrackingHttpRequest(@Nullable final Iterable<String> urls,
                                               @Nullable final Context context,
                                               @Nullable final Listener listener) {
        if (urls == null || context == null) {
            return;
        }

        final MoPubRequestQueue requestQueue = Networking.getRequestQueue(context);
        for (final String url : urls) {
            if (TextUtils.isEmpty(url)) {
                continue;
            }

            final TrackingRequest.Listener internalListener = new TrackingRequest.Listener() {
                @Override
                public void onResponse(@NonNull String url) {
                    MoPubLog.log(CUSTOM, "Successfully hit tracking endpoint: " + url);
                    if (listener != null) {
                        listener.onResponse(url);
                    }
                }

                @Override
                public void onErrorResponse(@NonNull final MoPubNetworkError networkError) {
                    MoPubLog.log(CUSTOM, "Failed to hit tracking endpoint: " + url);
                    if (listener != null) {
                        listener.onErrorResponse(networkError);
                    }
                }
            };
            final TrackingRequest trackingRequest = new TrackingRequest(context, url, internalListener);
            requestQueue.add(trackingRequest);
        }
    }

    public static void makeTrackingHttpRequest(@Nullable final String url,
                                               @Nullable final Context context) {
        makeTrackingHttpRequest(url, context, null);
    }

    public static void makeTrackingHttpRequest(@Nullable final String url,
                                               @Nullable final Context context,
                                               @Nullable Listener listener) {
        if (!TextUtils.isEmpty(url)) {
            makeTrackingHttpRequest(Arrays.asList(url), context, listener);
        }
    }

    public static void makeTrackingHttpRequest(@Nullable final Iterable<String> urls,
                                               @Nullable final Context context) {
        makeTrackingHttpRequest(urls, context, null);
    }
}
