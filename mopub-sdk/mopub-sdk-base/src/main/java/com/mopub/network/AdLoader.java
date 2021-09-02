// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.network;

import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.AdFormat;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.MoPubError;

import java.lang.ref.WeakReference;

import static com.mopub.common.logging.MoPubLog.AdLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdLogEvent.REQUESTED;
import static com.mopub.common.logging.MoPubLog.AdLogEvent.RESPONSE_RECEIVED;

/**
 * AdLoader implements several simple functions: communicate with the networking library to download multiple ads in one
 * HTTP call, implement client side waterfall logic, and asynchronously return {@link AdResponse} objects.
 * This class is immutable and fully supports multithreading.
 */
public class AdLoader {
    // to be implemented by external listener
    public interface Listener extends MoPubResponse.Listener<AdResponse> {}

    private final MultiAdRequest.Listener mAdListener;
    private final WeakReference<Context> mContext;
    private final Listener mOriginalListener;

    @NonNull
    private MultiAdRequest mMultiAdRequest;
    @Nullable
    protected MultiAdResponse mMultiAdResponse;
    @NonNull
    private final Object lock = new Object(); // multithreading
    @Nullable
    protected AdResponse mLastDeliveredResponse = null;
    @Nullable
    private ContentDownloadAnalytics mDownloadTracker;

    private volatile boolean mRunning;
    private volatile boolean mFailed;
    private boolean mContentDownloaded;

    @NonNull
    private Handler mHandler;

    /**
     * @param url      initial URL to download ads from ads.mopub.com
     * @param adFormat banner, interstitial, etc.
     * @param adUnitId ad unit id will be sent to the server
     * @param context  required by {@link Networking} class
     * @param listener callback to return results
     */
    public AdLoader(@NonNull final String url,
                    @NonNull final AdFormat adFormat,
                    @Nullable final String adUnitId,
                    @NonNull final Context context,
                    @NonNull final Listener listener) {
        Preconditions.checkArgument(!TextUtils.isEmpty(url));
        Preconditions.checkNotNull(adFormat);
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(listener);

        mContext = new WeakReference<>(context);
        mOriginalListener = listener;

        mHandler = new Handler();

        mAdListener = new MultiAdRequest.Listener() {
            @Override
            public void onErrorResponse(@NonNull final MoPubNetworkError networkError) {
                MoPubLog.log(RESPONSE_RECEIVED, networkError.getMessage());

                mFailed = true;
                mRunning = false;
                deliverError(networkError);
            }

            @Override
            public void onResponse(@NonNull final MultiAdResponse response) {
                synchronized (lock) {
                    mRunning = false;
                    mMultiAdResponse = response;
                    if (mMultiAdResponse.hasNext()) {
                        deliverResponse(mMultiAdResponse.next());
                    }
                }
            }
        };

        mRunning = false;
        mFailed = false;
        mMultiAdRequest = new MultiAdRequest(url,
                adFormat,
                adUnitId,
                context,
                mAdListener
        );
    }

    /**
     * @return true if more ads available locally or on the server, otherwise false
     */
    public boolean hasMoreAds() {
        if (mFailed) {
            return false;
        }

        if (mContentDownloaded) {
            return false;
        }

        MultiAdResponse response = mMultiAdResponse;
        return response == null || response.hasNext() || !response.isWaterfallFinished();
    }

    /**
     * The waterfall logic is mostly implemented inside loadNextAd.
     * The ad is downloaded asynchronously. The ad might come from internal cache
     * or downloaded from the server. Make sure to call {@link #hasMoreAds()} before
     * calling loadNextAd.
     *
     * @param errorCode creative download error or null for the first call
     * @return The returned object Request<?> can be used to cancel asynchronous operation.
     */
    @Nullable
    public MoPubRequest<?> loadNextAd(@Nullable final MoPubError errorCode) {
        if (mRunning) {
            return mMultiAdRequest;
        }

        if (mFailed) {
            // call back using handler to make sure it is always async.
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    deliverError(new MoPubNetworkError.Builder()
                            .reason(MoPubNetworkError.Reason.UNSPECIFIED)
                            .build());
                }
            });
            return null;
        }

        synchronized (lock) {

            // not running and not failed: start it for the first time
            if (mMultiAdResponse == null) {
                if (RequestRateTracker.getInstance().isBlockedByRateLimit(mMultiAdRequest.mAdUnitId)) {
                    // report no fill
                    MoPubLog.log(MoPubLog.SdkLogEvent.CUSTOM, mMultiAdRequest.mAdUnitId + " is blocked by request rate limiting.");
                    mFailed = true;
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            deliverError(new MoPubNetworkError.Builder()
                                    .reason(MoPubNetworkError.Reason.TOO_MANY_REQUESTS)
                                    .build());
                        }
                    });
                    return null;
                } else {
                    return fetchAd(mMultiAdRequest, mContext.get()); // first request
                }
            }

            // report creative download error to the server
            if (null != errorCode) {
                creativeDownloadFailed(errorCode);
            }

            // in the middle of waterfall, check if preloaded items available
            if (mMultiAdResponse.hasNext()) {
                // logic to return next preloaded AdResponse item
                final AdResponse adResponse = mMultiAdResponse.next();
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        deliverResponse(adResponse);
                    }
                });
                return mMultiAdRequest;
            }

            // logic to request more waterfall ads from server
            if (!mMultiAdResponse.isWaterfallFinished()) {
                // create new request with failURL
                mMultiAdRequest = new MultiAdRequest(mMultiAdResponse.getFailURL(),
                        mMultiAdRequest.mAdFormat,
                        mMultiAdRequest.mAdUnitId,
                        mContext.get(),
                        mAdListener
                );
                return fetchAd(mMultiAdRequest, mContext.get());
            }
        } // end synchronized(lock)

        // end of waterfall, there is nothing to download
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                deliverError(new MoPubNetworkError.Builder()
                        .reason(MoPubNetworkError.Reason.NO_FILL)
                        .build());
            }
        });

        return null;
    }

    /**
     * Call this function to notify server that creative content successfully downloaded
     */
    public void creativeDownloadSuccess() {
        mContentDownloaded = true;

        if (null == mDownloadTracker) {
            MoPubLog.log(CUSTOM, "Response analytics should not be null here");
            return;
        }

        Context context = mContext.get();
        if (null == context || null == mLastDeliveredResponse) {
            MoPubLog.log(CUSTOM, "Cannot send 'x-after-load-url' analytics.");
            return;
        }

        mDownloadTracker.reportAfterLoad(context, null);
        mDownloadTracker.reportAfterLoadSuccess(context);
    }

    private void creativeDownloadFailed(@Nullable final MoPubError errorCode) {
        if (null == errorCode) {
            MoPubLog.log(CUSTOM, "Must provide error code to report creative download error");
            return;
        }

        Context context = mContext.get();
        if (null == context || null == mLastDeliveredResponse) {
            MoPubLog.log(CUSTOM, "Cannot send creative mFailed analytics.");
            return;
        }

        if (mDownloadTracker != null) {
            mDownloadTracker.reportAfterLoad(context, errorCode);
            mDownloadTracker.reportAfterLoadFail(context, errorCode);
        }
    }

    /**
     * Submits request to the networking library
     *
     * @param request will be sent to the networking library
     * @param context required by networking library
     * @return generic object Request to be used for cancel() if necessary
     */
    @Nullable
    private MoPubRequest<?> fetchAd(@NonNull final MultiAdRequest request,
                               @Nullable final Context context) {
        Preconditions.checkNotNull(request);

        if (context == null) {
            return null;
        }

        String bodyString = "<no body>";
        if (MoPubRequestUtils.isMoPubRequest(request.getUrl()) && request.getBody() != null) {
            bodyString = new String(request.getBody());
        }

        MoPubLog.log(REQUESTED, request.getOriginalUrl(), bodyString);

        mRunning = true;
        MoPubRequestQueue requestQueue = Networking.getRequestQueue(context);
        mMultiAdRequest = request;
        requestQueue.add(request);
        return request;
    }

    /**
     * Helper function to make callback
     *
     * @param networkError error to be delivered
     */
    private void deliverError(@NonNull final MoPubNetworkError networkError) {
        mLastDeliveredResponse = null;
        if (mOriginalListener != null) {
            if (networkError.getReason() != null) {
                mOriginalListener.onErrorResponse(networkError);
            } else {
                mOriginalListener.onErrorResponse(
                        new MoPubNetworkError.Builder(networkError.getMessage(), networkError.getCause())
                                .reason(MoPubNetworkError.Reason.UNSPECIFIED)
                                .build());
            }
        }
    }

    /**
     * Helper function to make 'success' callback
     *
     * @param adResponse valid {@link AdResponse} object
     */
    private void deliverResponse(@NonNull final AdResponse adResponse) {
        Preconditions.checkNotNull(adResponse);

        Context context = mContext.get();
        mDownloadTracker = new ContentDownloadAnalytics(adResponse);
        mDownloadTracker.reportBeforeLoad(context);

        if (mOriginalListener != null) {
            mLastDeliveredResponse = adResponse;
            mOriginalListener.onResponse(adResponse);
        }
    }

    public boolean isRunning() {
        return mRunning;
    }

    public boolean isFailed() {
        return mFailed;
    }
}
