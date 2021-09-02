// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.nativeads;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.VisibleForTesting;
import com.mopub.network.MoPubNetworkError;
import com.mopub.network.MoPubNetworkResponse;
import com.mopub.network.MoPubNetworkUtils;
import com.mopub.network.MoPubRequest;
import com.mopub.network.MoPubRequestUtils;
import com.mopub.network.MoPubResponse;
import com.mopub.network.MoPubUrlRewriter;
import com.mopub.network.Networking;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.util.Map;

import static com.mopub.nativeads.MoPubNativeAdPositioning.MoPubClientPositioning;

public class PositioningRequest extends MoPubRequest<MoPubClientPositioning> {
    private static final String FIXED_KEY = "fixed";
    private static final String SECTION_KEY = "section";
    private static final String POSITION_KEY = "position";
    private static final String REPEATING_KEY = "repeating";
    private static final String INTERVAL_KEY = "interval";

    // Max value to avoid bad integer math calculations. This is 2 ^ 16.
    private static final int MAX_VALUE = 1 << 16;
    private MoPubResponse.Listener<MoPubClientPositioning> mListener;

    public PositioningRequest(@NonNull final Context context,
            final String url,
            final MoPubResponse.Listener<MoPubClientPositioning> listener) {

        super(context,
                url,
                MoPubRequestUtils.truncateQueryParamsIfPost(url),
                MoPubRequestUtils.chooseMethod(url),
                listener);

        mListener = listener;
    }

    @Override
    protected void deliverResponse(@NonNull final MoPubClientPositioning response) {
        mListener.onResponse(response);
    }

    @Override
    protected MoPubResponse<MoPubClientPositioning> parseNetworkResponse(final MoPubNetworkResponse response) {
        if (response == null) {
            return MoPubResponse.error(new MoPubNetworkError.Builder("Empty network response").build());
        }

        if (response.getStatusCode() != HttpURLConnection.HTTP_OK) {
            return MoPubResponse.error(new MoPubNetworkError.Builder().networkResponse(response).build());
        }

        if (response.getData() == null || response.getData().length == 0) {
            return MoPubResponse.error(new MoPubNetworkError
                    .Builder("Empty positioning response", new JSONException("Empty response"))
                    .build());
        }

        try {
            String jsonString = new String(response.getData(),
                    MoPubNetworkUtils.parseCharsetFromContentType(response.getHeaders()));

            return MoPubResponse.success(parseJson(jsonString), response);
        } catch (UnsupportedEncodingException e) {
            return MoPubResponse.error(new MoPubNetworkError.Builder("Couldn't parse JSON from Charset", e)
                    .build());
        } catch (JSONException e) {
            return MoPubResponse.error(new MoPubNetworkError.Builder("JSON Parsing Error", e).build());
        } catch (MoPubNetworkError e) {
            return MoPubResponse.error(e);
        }
    }

    @NonNull
    @VisibleForTesting
    MoPubClientPositioning parseJson(@NonNull String jsonString) throws  JSONException, MoPubNetworkError {
        JSONObject jsonObject = new JSONObject(jsonString);

        // If the server returns an error explicitly, throw an exception with the message.
        final String error = jsonObject.optString("error");
        if (!TextUtils.isEmpty(error)) {
            if (error.equalsIgnoreCase("WARMING_UP")) {
                throw new MoPubNetworkError.Builder().reason(MoPubNetworkError.Reason.WARMING_UP).build();
            }
            throw new JSONException(error);
        }

        // Parse fixed and repeating rules.
        JSONArray fixed = jsonObject.optJSONArray(FIXED_KEY);
        JSONObject repeating = jsonObject.optJSONObject(REPEATING_KEY);
        if (fixed == null && repeating == null) {
            throw new JSONException("Must contain fixed or repeating positions");
        }

        MoPubClientPositioning positioning = new MoPubClientPositioning();
        if (fixed != null) {
            parseFixedJson(fixed, positioning);
        }
        if (repeating != null) {
            parseRepeatingJson(repeating, positioning);
        }
        return positioning;
    }

    private void parseFixedJson(@NonNull final JSONArray fixed,
                                @NonNull final MoPubClientPositioning positioning) throws JSONException {
        for (int i = 0; i < fixed.length(); ++i) {
            JSONObject positionObject = fixed.getJSONObject(i);
            int section = positionObject.optInt(SECTION_KEY, 0);
            if (section < 0) {
                throw new JSONException("Invalid section " + section + " in JSON response");
            }
            if (section > 0) {
                // Ignore sections > 0.
                continue;
            }
            int position = positionObject.getInt(POSITION_KEY);
            if (position < 0 || position > MAX_VALUE) {
                throw new JSONException("Invalid position " + position + " in JSON response");
            }
            positioning.addFixedPosition(position);
        }
    }

    private void parseRepeatingJson(@NonNull final JSONObject repeatingObject,
                                    @NonNull final MoPubClientPositioning positioning) throws JSONException {
        int interval = repeatingObject.getInt(INTERVAL_KEY);
        if (interval < 2 || interval > MAX_VALUE) {
            throw new JSONException("Invalid interval " + interval + " in JSON response");
        }
        positioning.enableRepeatingPositions(interval);
    }

    @Nullable
    @Override
    protected Map<String, String> getParams() {
        MoPubUrlRewriter rewriter = Networking.getUrlRewriter();
        if (!MoPubRequestUtils.isMoPubRequest(getUrl()) || rewriter == null) {
            return null;
        }

        return MoPubNetworkUtils.convertQueryToMap(rewriter.rewriteUrl(getOriginalUrl()));
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
    public byte[] getBody() {
        final String body = !MoPubRequestUtils.isMoPubRequest(getUrl()) ? null
                : MoPubNetworkUtils.generateBodyFromParams(getParams());
        if (body == null) {
            return null;
        }
        return body.getBytes();
    }
}
