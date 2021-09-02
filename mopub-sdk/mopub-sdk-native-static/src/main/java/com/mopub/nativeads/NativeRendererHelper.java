// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.nativeads;

import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.UrlAction;
import com.mopub.common.UrlHandler;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.Drawables;
import com.mopub.mobileads.native_static.R;

import java.util.IllegalFormatException;
import java.util.Map;

import static com.mopub.common.logging.MoPubLog.SdkLogEvent.CUSTOM;

/**
 * A set of helper methods for Native Ad Rendering
 */
public class NativeRendererHelper {
    public static void addTextView(@Nullable final TextView textView,
            @Nullable final String contents) {
        if (textView == null) {
            MoPubLog.log(CUSTOM, "Attempted to add text (" + contents + ") to null TextView.");
            return;
        }

        // Clear previous value
        textView.setText(null);

        if (contents == null) {
            MoPubLog.log(CUSTOM, "Attempted to set TextView contents to null.");
        } else {
            textView.setText(contents);
        }
    }

    /**
     * Fills in the Privacy Information Icon with the specified image url and attaches a click
     * listener for the clickthrough url.
     *
     * @param privacyInformationIconImageView   The image view of where the icon is supposed to be
     *                                          placed. If this is null, this method does nothing.
     * @param privacyInformationImageUrl        The image url. If this is null, the default MoPub
     *                                          icon is used.
     * @param privacyInformationClickthroughUrl The clickthrough url for the privacy information
     *                                          icon. If this is null, the icon will be cleared.
     */
    public static void addPrivacyInformationIcon(final ImageView privacyInformationIconImageView,
            final String privacyInformationImageUrl,
            final String privacyInformationClickthroughUrl) {
        if (privacyInformationIconImageView == null) {
            return;
        }
        if (privacyInformationClickthroughUrl == null) {
            privacyInformationIconImageView.setImageDrawable(null);
            privacyInformationIconImageView.setOnClickListener(null);
            privacyInformationIconImageView.setVisibility(View.INVISIBLE);
            return;
        }

        final Context context = privacyInformationIconImageView.getContext();
        if (context == null) {
            return;
        }

        if (privacyInformationImageUrl == null) {
            privacyInformationIconImageView.setImageDrawable(
                    Drawables.NATIVE_PRIVACY_INFORMATION_ICON.createDrawable(context));
        } else {
            NativeImageHelper.loadImageView(privacyInformationImageUrl,
                    privacyInformationIconImageView);
        }

        privacyInformationIconImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                new UrlHandler.Builder()
                        .withSupportedUrlActions(
                                UrlAction.IGNORE_ABOUT_SCHEME,
                                UrlAction.OPEN_NATIVE_BROWSER,
                                UrlAction.OPEN_IN_APP_BROWSER,
                                UrlAction.HANDLE_SHARE_TWEET,
                                UrlAction.FOLLOW_DEEP_LINK_WITH_FALLBACK,
                                UrlAction.FOLLOW_DEEP_LINK)
                        .build().handleUrl(context, privacyInformationClickthroughUrl);
            }
        });
        privacyInformationIconImageView.setVisibility(View.VISIBLE);
    }

    public static void addCtaButton(@Nullable final TextView ctaTextView,
            @Nullable final View rootView, @Nullable final String contents) {
        addTextView(ctaTextView, contents);

        if (ctaTextView == null || rootView == null) {
            return;
        }

        // Defer click to rootView's onClickListener, which should also fire click and
        // impression trackers as needed.
        ctaTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                rootView.performClick();
            }
        });
    }

    public static void addSponsoredView(@Nullable final String sponsoredString,
            @Nullable final TextView sponsoredTextView) {
        if (sponsoredTextView == null) {
            return;
        }

        if (TextUtils.isEmpty(sponsoredString)) {
            sponsoredTextView.setVisibility(View.INVISIBLE);
            return;
        }

        String sponsoredByFormattedString = sponsoredString;
        try {
            sponsoredByFormattedString = sponsoredTextView.getContext()
                    .getString(R.string.com_mopub_nativeads_sponsored_by, sponsoredString);
        } catch (IllegalFormatException e) {
            MoPubLog.log(CUSTOM, "Unable to format sponsored by String.");
        } catch (Resources.NotFoundException e) {
            MoPubLog.log(CUSTOM, "Unable to format sponsored by String.");
        }
        if (!sponsoredByFormattedString.contains(sponsoredString)) {
            MoPubLog.log(CUSTOM, "The formatted sponsored String does not include the sponsor. " +
                    "Please include %s in the com_mopub_nativeads_sponsored_by translation.");
        }
        addTextView(sponsoredTextView, sponsoredByFormattedString);
        sponsoredTextView.setVisibility(View.VISIBLE);
    }

    public static void updateExtras(@Nullable final View mainView,
            @NonNull final Map<String, Integer> extrasIds,
            @NonNull final Map<String, Object> extras) {
        if (mainView == null) {
            MoPubLog.log(CUSTOM, "Attempted to bind extras on a null main view.");
            return;
        }

        for (final String key : extrasIds.keySet()) {
            final int resourceId = extrasIds.get(key);
            final View view = mainView.findViewById(resourceId);
            final Object content = extras.get(key);

            if (view instanceof ImageView) {
                // Clear previous image
                ((ImageView) view).setImageDrawable(null);
                final Object object = extras.get(key);
                if (object != null && object instanceof String) {
                    NativeImageHelper.loadImageView((String) object, (ImageView) view);
                }
            } else if (view instanceof TextView) {
                // Clear previous text value
                ((TextView) view).setText(null);
                if (content instanceof String) {
                    NativeRendererHelper.addTextView((TextView) view, (String) content);
                }
            } else {
                MoPubLog.log(CUSTOM, "View bound to " + key + " should be an instance of TextView or ImageView.");
            }
        }
    }
}
