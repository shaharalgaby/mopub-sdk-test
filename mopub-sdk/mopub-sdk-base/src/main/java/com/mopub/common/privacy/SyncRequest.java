// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common.privacy;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.network.MoPubNetworkError;
import com.mopub.network.MoPubNetworkResponse;
import com.mopub.network.MoPubRequest;
import com.mopub.network.MoPubRequestUtils;
import com.mopub.network.MoPubResponse;
import com.mopub.network.MoPubRetryPolicy;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

public class SyncRequest extends MoPubRequest<SyncResponse> {

    public interface Listener extends MoPubResponse.Listener<SyncResponse> {}

    @Nullable private Listener mListener;

    public SyncRequest(@NonNull final Context context,
            @NonNull final String url,
            @Nullable final Listener listener) {
        super(context,
                url,
                MoPubRequestUtils.truncateQueryParamsIfPost(url),
                MoPubRequestUtils.chooseMethod(url),
                listener);

        mListener = listener;

        MoPubRetryPolicy retryPolicy = new MoPubRetryPolicy(
                MoPubRetryPolicy.DEFAULT_TIMEOUT_MS,
                0,
                MoPubRetryPolicy.DEFAULT_BACKOFF_MULT);
        setRetryPolicy(retryPolicy);
        setShouldCache(false);
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
    protected MoPubResponse<SyncResponse> parseNetworkResponse(final MoPubNetworkResponse networkResponse) {
        final SyncResponse.Builder builder = new SyncResponse.Builder();
        final String responseBody = parseStringBody(networkResponse);

        try {
            final JSONObject jsonBody = new JSONObject(responseBody);
            builder.setIsGdprRegion(jsonBody.getString(PrivacyKey.IS_GDPR_REGION.getKey()))
                    .setForceExplicitNo(jsonBody.optString(PrivacyKey.FORCE_EXPLICIT_NO.getKey()))
                    .setInvalidateConsent(
                            jsonBody.optString(PrivacyKey.INVALIDATE_CONSENT.getKey()))
                    .setReacquireConsent(jsonBody.optString(PrivacyKey.REACQUIRE_CONSENT.getKey()))
                    .setIsWhitelisted(jsonBody.getString(PrivacyKey.IS_WHITELISTED.getKey()))
                    .setForceGdprApplies(jsonBody.optString(PrivacyKey.FORCE_GDPR_APPLIES.getKey()))
                    .setCurrentVendorListVersion(
                            jsonBody.getString(PrivacyKey.CURRENT_VENDOR_LIST_VERSION.getKey()))
                    .setCurrentVendorListLink(
                            jsonBody.getString(PrivacyKey.CURRENT_VENDOR_LIST_LINK.getKey()))
                    .setCurrentPrivacyPolicyLink(
                            jsonBody.getString(PrivacyKey.CURRENT_PRIVACY_POLICY_LINK.getKey()))
                    .setCurrentPrivacyPolicyVersion(
                            jsonBody.getString(PrivacyKey.CURRENT_PRIVACY_POLICY_VERSION.getKey()))
                    .setCurrentVendorListIabFormat(
                            jsonBody.optString(PrivacyKey.CURRENT_VENDOR_LIST_IAB_FORMAT.getKey()))
                    .setCurrentVendorListIabHash(
                            jsonBody.getString(PrivacyKey.CURRENT_VENDOR_LIST_IAB_HASH.getKey()))
                    .setCallAgainAfterSecs(
                            jsonBody.optString(PrivacyKey.CALL_AGAIN_AFTER_SECS.getKey()))
                    .setExtras(jsonBody.optString(PrivacyKey.EXTRAS.getKey()))
                    .setConsentChangeReason(
                            jsonBody.optString(PrivacyKey.CONSENT_CHANGE_REASON.getKey()));
        } catch (JSONException e) {
            return MoPubResponse.error(
                    new MoPubNetworkError.Builder("Unable to parse sync request network response.")
                            .reason(MoPubNetworkError.Reason.BAD_BODY)
                            .build()
            );
        }

        return MoPubResponse.success(builder.build(), networkResponse);
    }

    @Override
    protected void deliverResponse(@NonNull final SyncResponse syncResponse) {
        if (mListener != null) {
            mListener.onResponse(syncResponse);
        }
    }
}
