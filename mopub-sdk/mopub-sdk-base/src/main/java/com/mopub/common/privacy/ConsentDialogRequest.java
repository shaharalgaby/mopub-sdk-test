// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common.privacy;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;

import com.mopub.network.MoPubNetworkError;
import com.mopub.network.MoPubNetworkResponse;
import com.mopub.network.MoPubRequest;
import com.mopub.network.MoPubRequestUtils;
import com.mopub.network.MoPubResponse;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

class ConsentDialogRequest extends MoPubRequest<ConsentDialogResponse> {
    private static final String HTML_KEY = "dialog_html";

    public interface Listener extends MoPubResponse.Listener<ConsentDialogResponse> {}

    @Nullable
    private Listener mListener;

    ConsentDialogRequest(@NonNull Context context, @NonNull String url, @Nullable Listener listener) {
        super(context,
                url,
                MoPubRequestUtils.truncateQueryParamsIfPost(url),
                MoPubRequestUtils.chooseMethod(url),
                listener);

        mListener = listener;

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
    protected MoPubResponse<ConsentDialogResponse> parseNetworkResponse(final MoPubNetworkResponse networkResponse) {
        final String responseBody = parseStringBody(networkResponse);

        ConsentDialogResponse response;
        try {
            final JSONObject jsonBody = new JSONObject(responseBody);
            String html = jsonBody.getString(HTML_KEY);
            if (TextUtils.isEmpty(html)) {
                throw new JSONException("Empty HTML body");
            }
            response = new ConsentDialogResponse(html);
        } catch (JSONException e) {
            return MoPubResponse.error(
                    new MoPubNetworkError.Builder("Unable to parse consent dialog request network response.")
                            .reason(MoPubNetworkError.Reason.BAD_BODY)
                            .build()
            );
        }

        return MoPubResponse.success(response, networkResponse);
    }

    @Override
    protected void deliverResponse(@NonNull final ConsentDialogResponse consentDialogResponse) {
        if (mListener != null) {
            mListener.onResponse(consentDialogResponse);
        }
    }
}
