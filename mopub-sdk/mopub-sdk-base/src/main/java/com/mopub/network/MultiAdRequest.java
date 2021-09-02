// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.network;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.AdFormat;
import com.mopub.common.MoPub;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.privacy.PersonalInfoManager;

import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdLogEvent.CUSTOM;

/**
 * {@link MoPubRequest} class helper to support ad requests specifics
 */
public class MultiAdRequest extends MoPubRequest<MultiAdResponse> {

    @NonNull
    public final Listener mListener;
    @NonNull
    final AdFormat mAdFormat;
    @Nullable
    final String mAdUnitId;
    @NonNull
    private final Context mContext;

    private int hashCode = 0;

    public interface Listener extends MoPubResponse.Listener<MultiAdResponse> {}

    MultiAdRequest(@NonNull final String url,
                   @NonNull final AdFormat adFormat,
                   @Nullable final String adUnitId,
                   @NonNull final Context context,
                   @NonNull final Listener listener) {
        super(context,
                clearUrlIfSdkNotInitialized(url),
                MoPubRequestUtils.truncateQueryParamsIfPost(url),
                MoPubRequestUtils.chooseMethod(url),
                listener);
        Preconditions.checkNotNull(url);
        Preconditions.checkNotNull(adFormat);
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(listener);

        mAdUnitId = adUnitId;
        mListener = listener;
        mAdFormat = adFormat;
        mContext = context.getApplicationContext();

        setShouldCache(false);

        final PersonalInfoManager personalInfoManager = MoPub.getPersonalInformationManager();
        if (personalInfoManager != null) {
            personalInfoManager.requestSync(false);
        }
    }

    /**
     * For 5.2.0 and onwards, disable load when the sdk is not initialized.
     *
     * @param url The original url
     * @return The original url if the sdk is initialized. Otherwise, returns an empty url.
     */
    @NonNull
    private static String clearUrlIfSdkNotInitialized(@NonNull final String url) {
        if (MoPub.getPersonalInformationManager() == null || !MoPub.isSdkInitialized()) {
            MoPubLog.log(CUSTOM, "Make sure to call MoPub#initializeSdk before loading an ad.");
            return "";
        }
        return url;
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

    /**
     * Callback from Volley to parse network response
     * @param networkResponse data to be parsed
     * @return valid response or null in case of error
     */
    @Nullable
    @Override
    protected MoPubResponse<MultiAdResponse> parseNetworkResponse(final MoPubNetworkResponse networkResponse) {
        MultiAdResponse multiAdResponse;
        try {
            multiAdResponse = new MultiAdResponse(mContext, networkResponse, mAdFormat, mAdUnitId);
        } catch (Exception ex) {
            if (ex instanceof MoPubNetworkError) {
                return MoPubResponse.error((MoPubNetworkError) ex);
            }
            // JSON Exception
            return MoPubResponse.error(new MoPubNetworkError.Builder(null, ex)
                    .reason(MoPubNetworkError.Reason.UNSPECIFIED)
                    .build());
        }

        return MoPubResponse.success(multiAdResponse, networkResponse);
    }

    /**
     * Callback to deliver successful response to listener
     * @param multiAdResponse valid object {@link MultiAdResponse}
     */
    @Override
    protected void deliverResponse(@NonNull final MultiAdResponse multiAdResponse) {
        if (!isCanceled()) {
            mListener.onResponse(multiAdResponse);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;

        if (!(obj instanceof MultiAdRequest)) {
            return false;
        }

        MultiAdRequest other = (MultiAdRequest) obj;
        int res = 0;
        if (mAdUnitId != null) {
            res = other.mAdUnitId == null ? 1 : mAdUnitId.compareTo(other.mAdUnitId);
        } else if (other.mAdUnitId != null) {
            res = -1;
        }

        return res == 0
                && mAdFormat == other.mAdFormat
                && getUrl().compareTo(other.getUrl()) == 0;
    }

    @Override
    public int hashCode() {
        if (hashCode == 0) {
            int result = mAdUnitId == null ? 29 : mAdUnitId.hashCode();
            result = 31 * result + mAdFormat.hashCode();
            result = 31 * result + getOriginalUrl().hashCode();
            hashCode = result;
        }
        return hashCode;
    }
}
