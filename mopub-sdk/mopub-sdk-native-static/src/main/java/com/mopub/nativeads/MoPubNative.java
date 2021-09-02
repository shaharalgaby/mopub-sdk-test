// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.nativeads;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.AdFormat;
import com.mopub.common.Constants;
import com.mopub.common.Preconditions;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.DeviceUtils;
import com.mopub.common.util.ManifestUtils;
import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.network.AdLoader;
import com.mopub.network.AdResponse;
import com.mopub.network.MoPubNetworkError;
import com.mopub.network.MoPubNetworkResponse;
import com.mopub.network.MoPubRequest;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.TreeMap;

import static com.mopub.common.logging.MoPubLog.AdLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdLogEvent.CUSTOM_WITH_THROWABLE;
import static com.mopub.common.logging.MoPubLog.AdLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdLogEvent.LOAD_SUCCESS;
import static com.mopub.nativeads.CustomEventNative.CustomEventNativeListener;
import static com.mopub.nativeads.NativeErrorCode.CONNECTION_ERROR;
import static com.mopub.nativeads.NativeErrorCode.EMPTY_AD_RESPONSE;
import static com.mopub.nativeads.NativeErrorCode.INVALID_REQUEST_URL;
import static com.mopub.nativeads.NativeErrorCode.INVALID_RESPONSE;
import static com.mopub.nativeads.NativeErrorCode.NATIVE_RENDERER_CONFIGURATION_ERROR;
import static com.mopub.nativeads.NativeErrorCode.SERVER_ERROR_RESPONSE_CODE;
import static com.mopub.nativeads.NativeErrorCode.TOO_MANY_REQUESTS;
import static com.mopub.nativeads.NativeErrorCode.UNSPECIFIED;

public class MoPubNative {

    public interface MoPubNativeNetworkListener {
        void onNativeLoad(final NativeAd nativeAd);
        void onNativeFail(final NativeErrorCode errorCode);
    }

    static final MoPubNativeNetworkListener EMPTY_NETWORK_LISTENER =
            new MoPubNativeNetworkListener() {
        @Override
        public void onNativeLoad(@NonNull final NativeAd nativeAd) {
            // If this listener is invoked, it means that MoPubNative instance has been destroyed
            // so destroy any leftover incoming NativeAds
            nativeAd.destroy();
        }
        @Override
        public void onNativeFail(final NativeErrorCode errorCode) {
        }
    };

    // Highly recommended to be an Activity since 3rd party networks need it
    @NonNull private final WeakReference<Context> mContext;
    @NonNull private final String mAdUnitId;
    @NonNull private MoPubNativeNetworkListener mMoPubNativeNetworkListener;

    // For small sets TreeMap, takes up less memory than HashMap
    @NonNull private Map<String, Object> mLocalExtras = new TreeMap<String, Object>();
    @Nullable private AdLoader mAdLoader;
    @Nullable private CustomEventNativeAdapter mNativeAdapter;
    @NonNull private final AdLoader.Listener moPubResponseListener;
    @Nullable private MoPubRequest mNativeRequest;
    @NonNull AdRendererRegistry mAdRendererRegistry;
    @Nullable
    private NativeAd mNativeAd;

    public MoPubNative(@NonNull final Context context,
            @NonNull final String adUnitId,
            @NonNull final MoPubNativeNetworkListener moPubNativeNetworkListener) {
        this(context, adUnitId, new AdRendererRegistry(), moPubNativeNetworkListener);
    }

    @VisibleForTesting
    public MoPubNative(@NonNull final Context context,
            @NonNull final String adUnitId,
            @NonNull AdRendererRegistry adRendererRegistry,
            @NonNull final MoPubNativeNetworkListener moPubNativeNetworkListener) {
        Preconditions.checkNotNull(context, "context may not be null.");
        Preconditions.checkNotNull(adUnitId, "AdUnitId may not be null.");
        Preconditions.checkNotNull(adRendererRegistry, "AdRendererRegistry may not be null.");
        Preconditions.checkNotNull(moPubNativeNetworkListener, "MoPubNativeNetworkListener may not be null.");

        ManifestUtils.checkNativeActivitiesDeclared(context);

        mContext = new WeakReference<Context>(context);
        mAdUnitId = adUnitId;
        mMoPubNativeNetworkListener = moPubNativeNetworkListener;
        mAdRendererRegistry = adRendererRegistry;
        moPubResponseListener = new AdLoader.Listener() {
            @Override
            public void onResponse(@NonNull final AdResponse response) {
                onAdLoad(response);
            }

            @Override
            public void onErrorResponse(@NonNull final MoPubNetworkError networkError) {
                onAdError(networkError);
            }
        };
    }

    /**
     * Registers an ad renderer for rendering a specific native ad format.
     * Note that if multiple ad renderers support a specific native ad format, the first
     * one registered will be used.
     */
    public void registerAdRenderer(@NonNull MoPubAdRenderer moPubAdRenderer) {
        if (!Preconditions.NoThrow.checkNotNull(moPubAdRenderer, "Can't register a null adRenderer")) {
            return;
        }

        mAdRendererRegistry.registerAdRenderer(moPubAdRenderer);
    }

    public void destroy() {
        mContext.clear();
        if (mNativeRequest != null) {
            mNativeRequest.cancel();
            mNativeRequest = null;
        }
        mAdLoader = null;

        if (mNativeAd != null) {
            mNativeAd.destroy();
            mNativeAd = null;
        }
        mMoPubNativeNetworkListener = EMPTY_NETWORK_LISTENER;
    }

    public void setLocalExtras(@Nullable final Map<String, Object> localExtras) {
        if (localExtras == null) {
            mLocalExtras = new TreeMap<String, Object>();
        } else {
            mLocalExtras = new TreeMap<String, Object>(localExtras);
        }
    }

    public void makeRequest() {
        makeRequest((RequestParameters)null);
    }

    public void makeRequest(@Nullable final RequestParameters requestParameters) {
        makeRequest(requestParameters, null);
    }

    public void makeRequest(@Nullable final RequestParameters requestParameters,
            @Nullable Integer sequenceNumber) {
        final Context context = getContextOrDestroy();
        if (context == null) {
            return;
        }

        if (!DeviceUtils.isNetworkAvailable(context)) {
            mMoPubNativeNetworkListener.onNativeFail(CONNECTION_ERROR);
            return;
        }

        loadNativeAd(requestParameters, sequenceNumber);
    }

    private void loadNativeAd(
            @Nullable final RequestParameters requestParameters,
            @Nullable final Integer sequenceNumber) {
        final Context context = getContextOrDestroy();
        if (context == null) {
            return;
        }

        MoPubLog.log(LOAD_ATTEMPTED);

        final NativeUrlGenerator generator = new NativeUrlGenerator(context)
                .withAdUnitId(mAdUnitId)
                .withRequest(requestParameters);

        if (sequenceNumber != null) {
            generator.withSequenceNumber(sequenceNumber);
        }

        final String endpointUrl = generator.generateUrlString(Constants.HOST);

        requestNativeAd(endpointUrl, null);
    }

    void requestNativeAd(@Nullable final String endpointUrl, @Nullable final NativeErrorCode errorCode) {
        final Context context = getContextOrDestroy();
        if (context == null) {
            return;
        }

        if (mAdLoader == null || !mAdLoader.hasMoreAds()) {
            if (TextUtils.isEmpty(endpointUrl)) {
                mMoPubNativeNetworkListener.onNativeFail(errorCode == null ? INVALID_REQUEST_URL : errorCode);
                return;
            } else {
                mAdLoader = new AdLoader(endpointUrl, AdFormat.NATIVE, mAdUnitId, context, moPubResponseListener);
            }
        }
        mNativeRequest = mAdLoader.loadNextAd(errorCode);
    }

    private void onAdLoad(@NonNull final AdResponse response) {
        final Context context = getContextOrDestroy();
        if (context == null) {
            return;
        }
        final CustomEventNativeListener customEventNativeListener =
                new CustomEventNativeListener() {
                    @Override
                    public void onNativeAdLoaded(@NonNull final BaseNativeAd nativeAd) {
                        MoPubLog.log(LOAD_SUCCESS);
                        mNativeAdapter = null;

                        final Context context = getContextOrDestroy();
                        if (context == null) {
                            return;
                        }

                        MoPubAdRenderer renderer = mAdRendererRegistry.getRendererForAd(nativeAd);
                        if (renderer == null) {
                            onNativeAdFailed(NATIVE_RENDERER_CONFIGURATION_ERROR);
                            return;
                        }

                        if(mAdLoader!=null) {
                            mAdLoader.creativeDownloadSuccess();
                        }

                        mNativeAd = new NativeAd(context,
                                response,
                                mAdUnitId,
                                nativeAd,
                                renderer);
                        mMoPubNativeNetworkListener.onNativeLoad(mNativeAd);
                    }

                    @Override
                    public void onNativeAdFailed(final NativeErrorCode errorCode) {
                        MoPubLog.log(LOAD_FAILED, errorCode.getIntCode(), errorCode.toString());
                        mNativeAdapter = null;
                        requestNativeAd("", errorCode);
                    }
                };

        if (mNativeAdapter != null) {
            MoPubLog.log(CUSTOM, "Native adapter is not null.");
            mNativeAdapter.stopLoading();
        }

        mNativeAdapter = new CustomEventNativeAdapter(customEventNativeListener);
        mNativeAdapter.loadNativeAd(
                context,
                mLocalExtras,
                response);
    }

    @VisibleForTesting
    void onAdError(@NonNull final MoPubNetworkError networkError) {
        MoPubLog.log(CUSTOM_WITH_THROWABLE, "Native ad request failed.", networkError);
        if (networkError.getReason() != null) {
            switch (networkError.getReason()) {
                case BAD_BODY:
                case BAD_HEADER_DATA:
                    mMoPubNativeNetworkListener.onNativeFail(INVALID_RESPONSE);
                    return;
                case WARMING_UP:
                    // Used for the sample app to signal a toast.
                    // This is not customer-facing except in the sample app.
                    MoPubLog.log(CUSTOM, MoPubErrorCode.WARMUP);
                case NO_FILL:
                    mMoPubNativeNetworkListener.onNativeFail(EMPTY_AD_RESPONSE);
                    return;
                case TOO_MANY_REQUESTS:
                    mMoPubNativeNetworkListener.onNativeFail(TOO_MANY_REQUESTS);
                    return;
                case NO_CONNECTION:
                    mMoPubNativeNetworkListener.onNativeFail(CONNECTION_ERROR);
                case UNSPECIFIED:
                default:
                    mMoPubNativeNetworkListener.onNativeFail(UNSPECIFIED);
            }
        } else {
            // Process our other status code errors.
            MoPubNetworkResponse response = networkError.getNetworkResponse();
            if (response != null && response.getStatusCode() >= 500 && response.getStatusCode() < 600) {
                mMoPubNativeNetworkListener.onNativeFail(SERVER_ERROR_RESPONSE_CODE);
            } else if (response == null && !DeviceUtils.isNetworkAvailable(mContext.get())) {
                MoPubLog.log(CUSTOM, MoPubErrorCode.NO_CONNECTION);
                mMoPubNativeNetworkListener.onNativeFail(CONNECTION_ERROR);
            } else {
                mMoPubNativeNetworkListener.onNativeFail(UNSPECIFIED);
            }
        }
    }

    @VisibleForTesting
    @Nullable
    Context getContextOrDestroy() {
        final Context context = mContext.get();
        if (context == null) {
            destroy();
            MoPubLog.log(CUSTOM, "Weak reference to Context in MoPubNative became null. This instance" +
                    " of MoPubNative is destroyed and No more requests will be processed.");
        }
        return context;
    }

    @VisibleForTesting
    @Deprecated
    @NonNull
    MoPubNativeNetworkListener getMoPubNativeNetworkListener() {
        return mMoPubNativeNetworkListener;
    }
}
