// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.content.Context;
import android.graphics.Point;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.view.Display;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.MoPubHttpUrlConnection;
import com.mopub.common.Preconditions;
import com.mopub.common.ViewabilityVendor;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.Dips;
import com.mopub.common.util.Streams;
import com.mopub.common.util.Strings;
import com.mopub.network.Networking;

import org.w3c.dom.Node;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static com.mopub.common.logging.MoPubLog.SdkLogEvent.ERROR_WITH_THROWABLE;
import static com.mopub.network.TrackingRequest.makeVastTrackingHttpRequest;


/**
 * AsyncTask that reads in VAST xml and resolves redirects. This returns a
 * fully formed {@link VastVideoConfig} so that the video can be
 * displayed with the settings and trackers set in the configuration.
 */
public class VastXmlManagerAggregator extends AsyncTask<String, Void, VastVideoConfig> {

    private static final String MOPUB = "MoPub";

    public static final String ADS_BY_AD_SLOT_ID = "adsBy";

    /**
     * Listener for when the xml parsing is done.
     */
    interface VastXmlManagerAggregatorListener {
        /**
         * When all the wrappers have resolved and aggregation is done, this passes in
         * a video configuration or null if one is not found.
         * @param vastVideoConfig The video configuration found or null if
         *                               no video was found.
         */
        void onAggregationComplete(final @Nullable VastVideoConfig vastVideoConfig);
    }

    /**
     * Flag for companion ad orientation during xml parsing.
     */
    enum CompanionOrientation {
        LANDSCAPE,
        PORTRAIT
    }

    // More than reasonable number of nested VAST urls to follow
    static final int MAX_TIMES_TO_FOLLOW_VAST_REDIRECT = 10;
    private static final String MIME_TYPE_MP4 = "video/mp4";
    private static final String MIME_TYPE_3GPP = "video/3gpp";
    private static final List<String> VIDEO_MIME_TYPES =
            Arrays.asList(MIME_TYPE_MP4, MIME_TYPE_3GPP);
    private static final int MINIMUM_COMPANION_AD_WIDTH = 300;
    private static final int MINIMUM_COMPANION_AD_HEIGHT = 250;
    private static final int BITRATE_THRESHOLD_HIGH = 1500;
    private static final int BITRATE_THRESHOLD_LOW = 700;

    @NonNull private final WeakReference<VastXmlManagerAggregatorListener> mVastXmlManagerAggregatorListener;
    private final double mScreenAspectRatio;
    @NonNull private final Context mContext;
    private final int mScreenWidthDp;

    /**
     * Number of times this has followed a redirect. This value is only
     * accessed and set on the background thread.
     */
    private int mTimesFollowedVastRedirect;

    VastXmlManagerAggregator(@NonNull final VastXmlManagerAggregatorListener vastXmlManagerAggregatorListener,
                             final double screenAspectRatio,
                             final int screenWidthDp,
                             @NonNull final Context context) {
        super();

        Preconditions.checkNotNull(vastXmlManagerAggregatorListener);
        Preconditions.checkNotNull(context);
        mVastXmlManagerAggregatorListener =
                new WeakReference<VastXmlManagerAggregatorListener>(vastXmlManagerAggregatorListener);
        mScreenAspectRatio = screenAspectRatio;
        mScreenWidthDp = screenWidthDp;
        mContext = context.getApplicationContext();
    }

    @Override
    protected void onPreExecute() {
        // This is to set the WebView user agent in case it was not already set by some other
        // element (such as the request queue).
        Networking.getUserAgent(mContext);
    }

    @Override
    protected VastVideoConfig doInBackground(@Nullable String... strings) {
        if (strings == null || strings.length == 0 || strings[0] == null) {
            return null;
        }

        try {
            final String vastXml = strings[0];
            return evaluateVastXmlManager(vastXml, new ArrayList<VastTracker>());
        } catch (Exception e) {
            MoPubLog.log(ERROR_WITH_THROWABLE, "Unable to generate VastVideoConfig.", e);
            return null;
        }
    }

    @Override
    protected void onPostExecute(final @Nullable VastVideoConfig vastVideoConfig) {
        final VastXmlManagerAggregatorListener listener = mVastXmlManagerAggregatorListener.get();
        if (listener != null) {
            listener.onAggregationComplete(vastVideoConfig);
        }
    }

    @Override
    protected void onCancelled() {
        final VastXmlManagerAggregatorListener listener = mVastXmlManagerAggregatorListener.get();
        if (listener != null) {
            listener.onAggregationComplete(null);
        }
    }

    /**
     * Recursively traverses the VAST xml documents and finds the first Linear creative containing a
     * valid media file. For each Ad element in a document, the method will first try to find a
     * valid Linear creative in the InLine element. If it does not find one, it will then try to
     * resolve the Wrapper element which should redirect to more VAST xml documents with more InLine
     * elements.
     * <p/>
     * The list of error trackers are propagated through each wrapper redirect. If at the end of the
     * wrapper chain, there's no ad, then the error trackers for the entire wrapper chain are fired.
     * If a valid video is found, the error trackers are stored in the video configuration for
     * non-xml errors.
     *
     * @param vastXml           The xml that this class parses
     * @param errorTrackers     This is the current list of error tracker URLs to hit if something
     *                          goes wrong.
     * @return {@link VastVideoConfig} with all available fields set or null if the xml is
     * invalid or null.
     */
    @VisibleForTesting
    @Nullable
    VastVideoConfig evaluateVastXmlManager(@NonNull final String vastXml,
            @NonNull final List<VastTracker> errorTrackers) {
        Preconditions.checkNotNull(vastXml, "vastXml cannot be null");
        Preconditions.checkNotNull(errorTrackers, "errorTrackers cannot be null");

        final VastXmlManager xmlManager = new VastXmlManager();
        try {
            xmlManager.parseVastXml(vastXml);
        } catch (Exception e) {
            MoPubLog.log(ERROR_WITH_THROWABLE, "Failed to parse VAST XML", e);
            makeVastTrackingHttpRequest(errorTrackers, VastErrorCode.XML_PARSING_ERROR, null,
                    null, mContext);
            return null;
        }

        final List<VastAdXmlManager> vastAdXmlManagers = xmlManager.getAdXmlManagers();

        // If there are no ads, fire the error trackers
        if (fireErrorTrackerIfNoAds(vastAdXmlManagers, xmlManager, mContext)) {
            return null;
        }

        for (VastAdXmlManager vastAdXmlManager : vastAdXmlManagers) {
            if (!isValidSequenceNumber(vastAdXmlManager.getSequence())) {
                continue;
            }

            // InLine evaluation
            final VastInLineXmlManager vastInLineXmlManager =
                    vastAdXmlManager.getInLineXmlManager();
            if (vastInLineXmlManager != null) {
                final VastVideoConfig vastVideoConfig = evaluateInLineXmlManager(
                        vastInLineXmlManager, errorTrackers);
                // If the vastVideoConfig is non null, it means we found a valid media file
                if (vastVideoConfig != null) {
                    populateMoPubCustomElements(xmlManager, vastVideoConfig);
                    return vastVideoConfig;
                }
            }

            // Wrapper evaluation
            final VastWrapperXmlManager vastWrapperXmlManager
                    = vastAdXmlManager.getWrapperXmlManager();
            if (vastWrapperXmlManager != null) {
                final List<VastTracker> wrapperErrorTrackers = new ArrayList<VastTracker>(errorTrackers);
                wrapperErrorTrackers.addAll(vastWrapperXmlManager.getErrorTrackers());
                final String vastRedirectXml = evaluateWrapperRedirect(vastWrapperXmlManager,
                        wrapperErrorTrackers);
                if (vastRedirectXml == null) {
                    continue;
                }

                final VastVideoConfig vastVideoConfig = evaluateVastXmlManager(
                        vastRedirectXml,
                        wrapperErrorTrackers);
                // If we don't find a valid video creative somewhere down this wrapper chain,
                // look at the next Ad element
                // NOTE: Wrapper elements will never contain media files according to the VAST
                // 3.0 spec
                if (vastVideoConfig == null) {
                    continue;
                }

                // If we have a vastVideoConfig it means that we found a valid media file
                // in one of Wrapper redirects. Therefore, aggregate all trackers in the wrapper
                vastVideoConfig.addImpressionTrackers(
                        vastWrapperXmlManager.getImpressionTrackers());
                final List<VastLinearXmlManager> linearXmlManagers =
                        vastWrapperXmlManager.getLinearXmlManagers();
                for (VastLinearXmlManager linearXmlManager : linearXmlManagers) {
                    populateLinearTrackersAndIcon(linearXmlManager, vastVideoConfig);
                }
                populateVideoViewabilityTracker(vastWrapperXmlManager, vastVideoConfig);
                populateViewabilityMetadata(vastWrapperXmlManager, vastVideoConfig);
                populateAdVerificationsOmid(vastWrapperXmlManager.mNode, vastVideoConfig);

                final List<VastCompanionAdXmlManager> companionAdXmlManagers =
                        vastWrapperXmlManager.getCompanionAdXmlManagers();
                // Only populate a companion ad if we don't already have one from one of the
                // redirects
                if (!vastVideoConfig.hasCompanionAd()) {
                    vastVideoConfig.addVastCompanionAdConfigs(
                            getAllCompanionAds(companionAdXmlManagers));
                } else {
                    // Otherwise append the companion trackers if it doesn't have resources
                    final Set<VastCompanionAdConfig> companionAds = vastVideoConfig.getVastCompanionAdConfigs();
                    for (VastCompanionAdConfig vastCompanionAdConfig : companionAds) {
                        for (final VastCompanionAdXmlManager companionAdXmlManager : companionAdXmlManagers) {
                            if (!companionAdXmlManager.hasResources()) {
                                vastCompanionAdConfig.addClickTrackers(
                                        companionAdXmlManager.getClickTrackers());
                                vastCompanionAdConfig.addCreativeViewTrackers(
                                        companionAdXmlManager.getCompanionCreativeViewTrackers());
                            }
                        }
                    }
                }

                populateMoPubCustomElements(xmlManager, vastVideoConfig);

                return vastVideoConfig;
            }
        }

        return null;
    }

    /**
     * Parses and evaluates an InLine element looking for a valid media file. InLine elements are
     * evaluated in order and the first valid media file found is used. If a media file is
     * found, a {@link VastVideoConfig} is created and trackers are aggregated. If a
     * valid companion ad is found, it is also added to the configuration.
     *
     * @param vastInLineXmlManager used to extract the media file, clickthrough link, trackers, and
     *                         companion ad
     * @param errorTrackers The error trackers from previous wrappers
     * @return a {@link VastVideoConfig} or null if a valid media file was not found
     */
    @Nullable
    private VastVideoConfig evaluateInLineXmlManager(
            @NonNull final VastInLineXmlManager vastInLineXmlManager,
            @NonNull final List<VastTracker> errorTrackers) {
        Preconditions.checkNotNull(vastInLineXmlManager);
        Preconditions.checkNotNull(errorTrackers);

        final List<VastLinearXmlManager> linearXmlManagers
                = vastInLineXmlManager.getLinearXmlManagers();

        for (VastLinearXmlManager linearXmlManager : linearXmlManagers) {
            String bestMediaFileUrl = getBestMediaFileUrl(linearXmlManager.getMediaXmlManagers());
            if (bestMediaFileUrl != null) {
                // Create vast video configuration and populate initial trackers
                final VastVideoConfig vastVideoConfig = new VastVideoConfig();
                vastVideoConfig.addImpressionTrackers(vastInLineXmlManager.getImpressionTrackers());
                populateLinearTrackersAndIcon(linearXmlManager, vastVideoConfig);

                // Linear nodes will only have a click through url and network media file when they
                // are under an InLine element. They will not have these assets when they are under
                // a Wrapper element.
                vastVideoConfig.setClickThroughUrl(linearXmlManager.getClickThroughUrl());
                vastVideoConfig.setNetworkMediaFileUrl(bestMediaFileUrl);

                final List<VastCompanionAdXmlManager> companionAdXmlManagers =
                        vastInLineXmlManager.getCompanionAdXmlManagers();

                vastVideoConfig.addVastCompanionAdConfigs(getAllCompanionAds(companionAdXmlManagers));
                errorTrackers.addAll(vastInLineXmlManager.getErrorTrackers());
                vastVideoConfig.addErrorTrackers(errorTrackers);
                populateVideoViewabilityTracker(vastInLineXmlManager, vastVideoConfig);
                populateViewabilityMetadata(vastInLineXmlManager, vastVideoConfig);
                populateAdVerificationsOmid(vastInLineXmlManager.mNode, vastVideoConfig);

                return vastVideoConfig;
            }
        }

        return null;
    }

    private void populateVideoViewabilityTracker(
            @NonNull final VastBaseInLineWrapperXmlManager vastInLineXmlManager,
            @NonNull VastVideoConfig vastVideoConfig) {
        Preconditions.checkNotNull(vastInLineXmlManager);
        Preconditions.checkNotNull(vastVideoConfig);

        if (vastVideoConfig.getVideoViewabilityTracker() != null) {
            return;
        }

        final VastExtensionParentXmlManager vastExtensionParentXmlManager =
                vastInLineXmlManager.getVastExtensionParentXmlManager();
        if (vastExtensionParentXmlManager != null) {
            final List<VastExtensionXmlManager> vastExtensionXmlManagers =
                    vastExtensionParentXmlManager.getVastExtensionXmlManagers();
            for (VastExtensionXmlManager vastExtensionXmlManager : vastExtensionXmlManagers) {
                if (MOPUB.equals(vastExtensionXmlManager.getType())) {
                    vastVideoConfig.setVideoViewabilityTracker(vastExtensionXmlManager
                            .getVideoViewabilityTracker());
                    break;
                }
            }
        }
    }

    private void populateViewabilityMetadata(
            @NonNull final VastBaseInLineWrapperXmlManager vastInLineXmlManager,
            @NonNull VastVideoConfig vastVideoConfig) {
        final VastExtensionParentXmlManager vastExtensionParentXmlManager =
                vastInLineXmlManager.getVastExtensionParentXmlManager();
        if (vastExtensionParentXmlManager != null) {
            final List<VastExtensionXmlManager> vastExtensionXmlManagers =
                    vastExtensionParentXmlManager.getVastExtensionXmlManagers();
            for (VastExtensionXmlManager vastExtensionXmlManager : vastExtensionXmlManagers) {
                if (vastExtensionXmlManager != null) {
                    populateAdVerificationsOmid(vastExtensionXmlManager.mExtensionNode, vastVideoConfig);
                }
            }
        }
    }

    private void populateAdVerificationsOmid(@NonNull final Node adVerificationsParent,
                                             @NonNull VastVideoConfig vastVideoConfig) {
        final VastAdVerificationsParser adVerificationNodes = new VastAdVerificationsParser(adVerificationsParent);
        final Set<ViewabilityVendor> vendors = adVerificationNodes.getViewabilityVendors();
        vastVideoConfig.addViewabilityVendors(vendors);
    }

    /**
     * Retrieves the Wrapper's redirect uri and follows it to return the next VAST xml String.
     *
     * @param vastWrapperXmlManager used to get the redirect uri
     * @param wrapperErrorTrackers  Error trackers to hit if something goes wrong
     * @return the next VAST xml String or {@code null} if it could not be resolved
     */
    @Nullable
    private String evaluateWrapperRedirect(@NonNull VastWrapperXmlManager vastWrapperXmlManager,
            @NonNull List<VastTracker> wrapperErrorTrackers) {
        final String vastAdTagUri = vastWrapperXmlManager.getVastAdTagURI();
        if (vastAdTagUri == null) {
            return null;
        }

        String vastRedirectXml = null;
        try {
            vastRedirectXml = followVastRedirect(vastAdTagUri);
        } catch (Exception e) {
            MoPubLog.log(ERROR_WITH_THROWABLE, "Failed to follow VAST redirect", e);
            if (!wrapperErrorTrackers.isEmpty()) {
                makeVastTrackingHttpRequest(wrapperErrorTrackers, VastErrorCode.WRAPPER_TIMEOUT,
                                null, null, mContext);
            }
        }

        return vastRedirectXml;
    }

    /**
     * This method aggregates all trackers found in the linearXmlManager and adds them to the
     * {@link VastVideoConfig}. This method also populates the skip offset and icon if they
     * have not already been populated in one of the wrapper redirects.
     *
     * @param linearXmlManager used to retrieve trackers, and assets
     * @param vastVideoConfig modified in this method to store trackers and assets
     */
    private void populateLinearTrackersAndIcon(@NonNull final VastLinearXmlManager linearXmlManager,
            @NonNull final VastVideoConfig vastVideoConfig) {
        Preconditions.checkNotNull(linearXmlManager, "linearXmlManager cannot be null");
        Preconditions.checkNotNull(vastVideoConfig, "vastVideoConfig cannot be null");

        vastVideoConfig.addAbsoluteTrackers(linearXmlManager.getAbsoluteProgressTrackers());
        vastVideoConfig.addFractionalTrackers(
                linearXmlManager.getFractionalProgressTrackers());
        vastVideoConfig.addPauseTrackers(linearXmlManager.getPauseTrackers());
        vastVideoConfig.addResumeTrackers(linearXmlManager.getResumeTrackers());
        vastVideoConfig.addCompleteTrackers(linearXmlManager.getVideoCompleteTrackers());
        vastVideoConfig.addCloseTrackers(linearXmlManager.getVideoCloseTrackers());
        vastVideoConfig.addSkipTrackers(linearXmlManager.getVideoSkipTrackers());
        vastVideoConfig.addClickTrackers(linearXmlManager.getClickTrackers());

        // Only set the icon if we haven't set it already in one of the redirects
        if (vastVideoConfig.getVastIconConfig() == null) {
            vastVideoConfig.setVastIconConfig(getBestIcon(linearXmlManager.getIconXmlManagers()));
        }
    }

    /**
     * Parses all custom MoPub specific custom extensions and impression trackers
     * and populates them in the {@link VastVideoConfig}. These extensions are not part
     * of the Vast 3.0 spec and are appended to the root of the xml document.
     *
     * @param xmlManager used to retrieve the custom extensions and impression trackers
     * @param vastVideoConfig modified in this method to store custom extensions and
     *                               impression trackers
     */
    private void populateMoPubCustomElements(@NonNull final VastXmlManager xmlManager,
            @NonNull final VastVideoConfig vastVideoConfig) {
        Preconditions.checkNotNull(xmlManager, "xmlManager cannot be null");
        Preconditions.checkNotNull(vastVideoConfig, "vastVideoConfig cannot be null");

        vastVideoConfig.addImpressionTrackers(xmlManager.getMoPubImpressionTrackers());

        if (vastVideoConfig.getCustomCtaText() == null) {
            vastVideoConfig.setCustomCtaText(xmlManager.getCustomCtaText());
        }
        if (vastVideoConfig.getCustomSkipText() == null) {
            vastVideoConfig.setCustomSkipText(xmlManager.getCustomSkipText());
        }
        if (vastVideoConfig.getCustomCloseIconUrl() == null) {
            vastVideoConfig.setCustomCloseIconUrl(xmlManager.getCustomCloseIconUrl());
        }
    }

    /**
     * Fires the available error tracker if the sole element in this vast document is an Error
     * element. In the VAST 3.0 spec in section 2.4.2.4, the No Ad Response can be represented by a
     * VAST document with only the Error element and no Ad elements. Returns whether or not the
     * error tracker was fired.
     *
     * @param vastAdXmlManagers The List of AdXmlManagers to determine if there are any ads
     *                          available
     * @param xmlManager        The current VastXmlManager that's used to get the new error tracker
     * @param context           Used to send an http request
     * @return {@code true} if the error tracker was fired, {@code false} if the error tracker was
     * not fired.
     */
    private boolean fireErrorTrackerIfNoAds(
            @NonNull final List<VastAdXmlManager> vastAdXmlManagers,
            @NonNull final VastXmlManager xmlManager, @NonNull Context context) {
        // When there is no <Ad> tag and when there is an error tracker
        if (vastAdXmlManagers.isEmpty() && xmlManager.getErrorTracker() != null ) {
            // Only use NO_ADS_VAST_RESPONSE if we've followed one or more wrappers
            makeVastTrackingHttpRequest(
                    Collections.singletonList(xmlManager.getErrorTracker()),
                    mTimesFollowedVastRedirect > 0
                            ? VastErrorCode.NO_ADS_VAST_RESPONSE
                            : VastErrorCode.UNDEFINED_ERROR,
                    null, null, context);
            return true;
        }
        return false;
    }

    @VisibleForTesting
    @Nullable
    String getBestMediaFileUrl(@NonNull final List<VastMediaXmlManager> managers) {
        Preconditions.checkNotNull(managers, "managers cannot be null");
        final List<VastMediaXmlManager> mediaXmlManagers = new ArrayList<VastMediaXmlManager>(managers);
        double bestMediaFitness = Double.NEGATIVE_INFINITY;
        String bestMediaFileUrl = null;

        final Iterator<VastMediaXmlManager> xmlManagerIterator = mediaXmlManagers.iterator();
        while (xmlManagerIterator.hasNext()) {
            final VastMediaXmlManager mediaXmlManager = xmlManagerIterator.next();

            final String mediaType = mediaXmlManager.getType();
            final String mediaUrl = mediaXmlManager.getMediaUrl();
            if (!VIDEO_MIME_TYPES.contains(mediaType) || mediaUrl == null) {
                xmlManagerIterator.remove();
                continue;
            }

            final Integer mediaWidth = mediaXmlManager.getWidth();
            final Integer mediaHeight = mediaXmlManager.getHeight();
            final Integer mediaBitrate = mediaXmlManager.getBitrate();
            if (mediaWidth == null || mediaWidth <= 0 || mediaHeight == null || mediaHeight <= 0) {
                continue;
            }

            final double mediaFitness = calculateFitness(mediaWidth,
                    mediaHeight,
                    mediaBitrate,
                    mediaType);
            if (mediaFitness > bestMediaFitness) {
                bestMediaFitness = mediaFitness;
                bestMediaFileUrl = mediaUrl;
            }
        }

        return bestMediaFileUrl;
    }

    @VisibleForTesting
    @NonNull
    Set<VastCompanionAdConfig> getAllCompanionAds(
            @NonNull final List<VastCompanionAdXmlManager> managers) {
        Preconditions.checkNotNull(managers, "managers cannot be null");

        final Set<VastCompanionAdConfig> vastCompanionAdConfigs = new HashSet<>();
        final List<VastCompanionAdXmlManager> companionXmlManagers = new ArrayList<>(managers);

        for (VastResource.Type type : VastResource.Type.values()) {
            for (VastCompanionAdXmlManager companionXmlManager : companionXmlManagers) {
                final Integer width = companionXmlManager.getWidth();
                final Integer height = companionXmlManager.getHeight();
                if (width == null || width < MINIMUM_COMPANION_AD_WIDTH ||
                        height == null || height < MINIMUM_COMPANION_AD_HEIGHT) {
                    continue;
                }

                Point vastScaledDimensions = getScaledDimensions(width, height, type);
                VastResource vastResource = VastResource.fromVastResourceXmlManager(
                        companionXmlManager.getResourceXmlManager(), type,
                        vastScaledDimensions.x, vastScaledDimensions.y);
                if (vastResource == null) {
                    continue;
                }

                VastCompanionAdConfig vastCompanionAdConfig = new VastCompanionAdConfig(
                        vastScaledDimensions.x,
                        vastScaledDimensions.y,
                        vastResource,
                        companionXmlManager.getClickThroughUrl(),
                        companionXmlManager.getClickTrackers(),
                        companionXmlManager.getCompanionCreativeViewTrackers(),
                        null
                );
                vastCompanionAdConfigs.add(vastCompanionAdConfig);
            }
        }

       return vastCompanionAdConfigs;
    }

    /**
     * Given a width and height for a resource, if the dimensions are larger than the screen size
     * then scale them down to fit in the screen. This maintains the aspect ratio if the resource is
     * not an HTMLResource. Since HTML can freely fill any space, the maximum size of an
     * HTMLResource is the screen size. Scaling takes into account the default Android WebView
     * padding.
     *
     * @param widthDp     width of the resource in dips
     * @param heightDp    height of the resource in dips
     * @param type        The type of the resource. HTMLResource uses special scaling.
     * @return the new scaled dimensions that honor the aspect ratio
     */
    @VisibleForTesting
    @NonNull
    Point getScaledDimensions(int widthDp, int heightDp, final VastResource.Type type) {
        final Point defaultPoint = new Point(widthDp, heightDp);
        final Display display = ((WindowManager) mContext.getSystemService(
                Context.WINDOW_SERVICE)).getDefaultDisplay();
        int screenWidthPx = display.getWidth();
        int screenHeightPx = display.getHeight();

        int widthPx = Dips.dipsToIntPixels(widthDp, mContext);
        int heightPx = Dips.dipsToIntPixels(heightDp, mContext);

        // Return if the width and height already fit in the screen
        if (widthPx <= (screenWidthPx - VastVideoViewController.WEBVIEW_PADDING) &&
                heightPx <= (screenHeightPx - VastVideoViewController.WEBVIEW_PADDING)) {
            return defaultPoint;
        }

        final Point point = new Point();
        if (VastResource.Type.HTML_RESOURCE == type) {
            point.x = Math.min(screenWidthPx, widthPx);
            point.y = Math.min(screenHeightPx, heightPx);
        } else {
            float widthRatio = (float) widthPx / screenWidthPx;
            float heightRatio = (float) heightPx / screenHeightPx;

            if (widthRatio >= heightRatio) {
                point.x = screenWidthPx;
                point.y = (int) (heightPx / widthRatio);
            } else {
                point.x = (int) (widthPx / heightRatio);
                point.y = screenHeightPx;
            }
        }

        point.x -= VastVideoViewController.WEBVIEW_PADDING;
        point.y -= VastVideoViewController.WEBVIEW_PADDING;

        if (point.x < 0 || point.y < 0) {
            return defaultPoint;
        }

        point.x = Dips.pixelsToIntDips(point.x, mContext);
        point.y = Dips.pixelsToIntDips(point.y, mContext);

        return point;
    }

    @VisibleForTesting
    @Nullable
    VastIconConfig getBestIcon(@NonNull final List<VastIconXmlManager> managers) {
        Preconditions.checkNotNull(managers, "managers cannot be null");
        final List<VastIconXmlManager> iconXmlManagers = new ArrayList<VastIconXmlManager>(managers);

        // Look for the best icon in order of prioritized resource types
        for (VastResource.Type type : VastResource.Type.values()) {
            final Iterator<VastIconXmlManager> xmlManagerIterator = iconXmlManagers.iterator();
            while (xmlManagerIterator.hasNext()) {
                final VastIconXmlManager iconXmlManager = xmlManagerIterator.next();

                final Integer width = iconXmlManager.getWidth();
                final Integer height = iconXmlManager.getHeight();

                // Icons can be a max of 300 x 300 dp
                if (width == null || width <= 0 || width > 300
                        || height == null || height <= 0 || height > 300) {
                    continue;
                }

                VastResource vastResource = VastResource.fromVastResourceXmlManager(
                        iconXmlManager.getResourceXmlManager(), type, width, height);

                if (vastResource == null) {
                    continue;
                }

                return new VastIconConfig(
                        iconXmlManager.getWidth(),
                        iconXmlManager.getHeight(),
                        iconXmlManager.getOffsetMS(),
                        iconXmlManager.getDurationMS(),
                        vastResource,
                        iconXmlManager.getClickTrackingUris(),
                        iconXmlManager.getClickThroughUri(),
                        iconXmlManager.getViewTrackingUris());
            }
        }

        return null;
    }

    /**
     * Calculates the fitness of the media file or companion using the aspect ratio, width, and
     * bitrate and of the device. The closer to 0 the score, the better.
     *
     * @param widthDp  the width of the media file or companion ad
     * @param heightDp the height of the media file or companion ad
     * @param bitrate  the bitrate of the media file - null if none provided or needed
     * @param format   the MIME format fo the media file - null if none provided or needed
     * @return the overall fitness score. The closer to 0, the better.
     */
    private double calculateFitness(final int widthDp,
                                    final int heightDp,
                                    @Nullable final Integer bitrate,
                                    @Nullable final String format) {

        final double screenFitness = calculateScreenFitnessFactor(widthDp, heightDp);
        final double bitrateFitness = calculateBitrateFitnessFactor(bitrate);
        final double formatFitness = calculateFormatFitnessFactor(format);

        return formatFitness * (1.0 / (1.0 + screenFitness + bitrateFitness));
    }

    /**
     * Calculates the fitness value of the media file's bitrate (by determining whether it is low,
     * medium, or high). The closer to 0 the score, the better. This is used by the overall fitness
     * function to choose an appropriate MediaFile.
     *
     * @param bitrate the bitrate of the media file - null if none provided or needed
     * @return the fitness factor based on the bitrate. The closer to 0, the better.
     */
    private double calculateBitrateFitnessFactor(@Nullable final Integer bitrate) {
        // Default bitrate to 0 if one was not provided for the MediaFile.
        final int usableBitrate = (bitrate == null || bitrate < 0) ? 0 : bitrate;

        if (BITRATE_THRESHOLD_LOW <= usableBitrate && usableBitrate <= BITRATE_THRESHOLD_HIGH) {
            return 0;
        } else {
            final double lowDistance = Math.abs(BITRATE_THRESHOLD_LOW - usableBitrate)
                    / (float) BITRATE_THRESHOLD_LOW;
            final double highDistance = Math.abs(BITRATE_THRESHOLD_HIGH - usableBitrate)
                    / (float) BITRATE_THRESHOLD_HIGH;
            return Math.min(lowDistance, highDistance);
        }
    }

    /**
     * Calculates the fitness of the media file or companion by comparing its aspect ratio and
     * width to those of the device. Scores cannot be negative and the closer the score is to 0, the
     * better. This is used by the overall fitness function to choose an appropriate MediaFile.
     *
     * @param widthDp  the width of the media file or companion ad
     * @param heightDp the height of the media file or companion ad
     * @return the fitness factor based on the screen size. The closer to 0, the better.
     */
    private double calculateScreenFitnessFactor(final int widthDp, final int heightDp) {
        // mScreenAspectRatio calculated as `(double) screenWidth / screenHeight`, so we'll do the
        // same here.
        final double mediaAspectRatio = (double) widthDp / heightDp;
        final double aspectRatioScore = Math.abs(mScreenAspectRatio - mediaAspectRatio);
        final double widthScore = Math.abs((mScreenWidthDp - widthDp) / mScreenWidthDp);

        return aspectRatioScore + widthScore;
    }

    /**
     * Calculates the fitness of the media file or companion based on the MIME type. This currently
     * gives preference to MP4 files.
     *
     * @param format the MIME format fo the media file - null if none provided or needed
     * @return the factor to be using in calculating the fitness score. Higher scores are better.
     */
    private double calculateFormatFitnessFactor(final String format) {
        final String safeFormat = (format == null) ? "" : format;

        switch (safeFormat) {
            case MIME_TYPE_MP4:
                return 1.5f;
            case MIME_TYPE_3GPP:
            default:
                return 1.0f;
        }
    }

    /**
     * Since MoPub does not support ad pods, do not accept any positive integers greater than 1.
     * MoPub will use the first ad in an ad pod (sequence = 1), but it will ignore all other ads in
     * the pod. If no sequence number, MoPub treats it like a stand-alone ad. If the sequence number
     * is nonsensical (e.g. negative, fails to parse as an integer), MoPub treats it like a
     * stand-alone ad.
     *
     * @param sequence The sequence number
     * @return True if this is a sequence number that MoPub would show an ad for, false if not.
     */
    static boolean isValidSequenceNumber(@Nullable final String sequence) {
        if (TextUtils.isEmpty(sequence)) {
            return true;
        }
        try {
            final int sequenceInt = Integer.parseInt(sequence);
            return sequenceInt < 2;
        } catch (NumberFormatException e) {
            // Since the sequence number is not a valid integer, go ahead and pretend there's no
            // sequence number and show this ad
            return true;
        }
    }

    @Nullable
    private String followVastRedirect(@NonNull final String redirectUrl) throws IOException {
        Preconditions.checkNotNull(redirectUrl);

        if (mTimesFollowedVastRedirect < MAX_TIMES_TO_FOLLOW_VAST_REDIRECT) {
            mTimesFollowedVastRedirect++;

            HttpURLConnection urlConnection = null;
            InputStream inputStream = null;
            try {
                urlConnection = MoPubHttpUrlConnection.getHttpUrlConnection(redirectUrl);
                inputStream = new BufferedInputStream(urlConnection.getInputStream());

                return Strings.fromStream(inputStream);
            } finally {
                Streams.closeStream(inputStream);
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
        }

        return null;
    }

    @VisibleForTesting
    @Deprecated
    void setTimesFollowedVastRedirect(final int timesFollowedVastRedirect) {
        mTimesFollowedVastRedirect = timesFollowedVastRedirect;
    }
}
