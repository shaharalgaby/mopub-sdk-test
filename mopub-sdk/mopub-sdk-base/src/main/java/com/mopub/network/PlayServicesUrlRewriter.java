// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.network;

import android.net.Uri;

import androidx.annotation.NonNull;

import com.mopub.common.ClientMetadata;
import com.mopub.common.MoPub;
import com.mopub.common.privacy.AdvertisingId;
import com.mopub.common.privacy.MoPubIdentifier;

import static com.mopub.common.Constants.TAS_AUTHORIZED;
import static com.mopub.common.Constants.TAS_DENIED;

/**
 * Url Rewriter that replaces MoPub templates for Google Advertising ID, Do Not Track settings, MoPub ID, and Tracking
 * Authorization Status when a request is queued for dispatch
 */
public class PlayServicesUrlRewriter implements MoPubUrlRewriter {
    public static final String IFA_TEMPLATE = "mp_tmpl_advertising_id";
    public static final String DO_NOT_TRACK_TEMPLATE = "mp_tmpl_do_not_track";
    public static final String MOPUB_ID_TEMPLATE = "mp_tmpl_mopub_id";
    public static final String TAS_TEMPLATE = "mp_tmpl_tas";

    public PlayServicesUrlRewriter() {
    }

    @Override
    public String rewriteUrl(@NonNull final String url) {
        ClientMetadata clientMetadata = ClientMetadata.getInstance();
        if (clientMetadata == null) {
            return url;
        }
        MoPubIdentifier identifier = clientMetadata.getMoPubIdentifier();
        AdvertisingId info = identifier.getAdvertisingInfo();
        String toReturn = url.replace(DO_NOT_TRACK_TEMPLATE, info.isDoNotTrack() ? "1" : "0");
        toReturn = toReturn.replace(TAS_TEMPLATE, info.isDoNotTrack() ? TAS_DENIED : TAS_AUTHORIZED);

        if (MoPub.canCollectPersonalInformation() && !info.isDoNotTrack()) {
            toReturn = toReturn.replace(IFA_TEMPLATE, Uri.encode(info.getIdentifier(true)));
        } else {
            final String ifaFullTemplate = "&ifa=" + IFA_TEMPLATE;
            toReturn = toReturn.replace(ifaFullTemplate, "");
        }

        toReturn = toReturn.replace(MOPUB_ID_TEMPLATE,  Uri.encode(info.getIdentifier(false)));
        return toReturn;
    }
}
