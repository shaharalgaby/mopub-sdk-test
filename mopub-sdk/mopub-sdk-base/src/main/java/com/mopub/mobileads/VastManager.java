// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.content.Context;
import android.text.TextUtils;
import android.view.Display;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.Preconditions;
import com.mopub.common.VideoCacheService;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.AsyncTasks;
import com.mopub.mobileads.VideoDownloader.VideoDownloaderListener;

import static com.mopub.common.logging.MoPubLog.SdkLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.SdkLogEvent.ERROR_WITH_THROWABLE;

/**
 * Given a VAST xml document, this class manages the lifecycle of parsing and finding a video and
 * possibly companion ad. It provides the API for clients to prepare a
 * {@link VastVideoConfig}.
 */
public class VastManager implements VastXmlManagerAggregator.VastXmlManagerAggregatorListener {


    /**
     * Users of this class should subscribe to this listener to get updates
     * when a video is found or when no video is available.
     */
    public interface VastManagerListener {
        /**
         * Called when a video is found or if the VAST document is invalid. Passes in {@code null}
         * when the VAST document is invalid.
         *
         * @param vastVideoConfig A configuration that can be used for displaying a VAST
         *                        video or {@code null} if the VAST document is invalid.
         */
        void onVastVideoConfigurationPrepared(@Nullable final VastVideoConfig vastVideoConfig);
    }

    @Nullable
    private VastManagerListener mVastManagerListener;
    @Nullable
    private VastXmlManagerAggregator mVastXmlManagerAggregator;
    @Nullable
    private String mDspCreativeId;
    private double mScreenAspectRatio;
    private int mScreenWidthDp;

    private final boolean mShouldPreCacheVideo;

    public VastManager(@NonNull final Context context, boolean shouldPreCacheVideo) {
        initializeScreenDimensions(context);
        mShouldPreCacheVideo = shouldPreCacheVideo;
    }

    /**
     * Creates and starts an async task that parses the VAST xml document.
     *
     * @param vastXml             The initial VAST xml document
     * @param vastManagerListener Notified when a video configuration has been found or when
     *                            the VAST document is invalid
     */
    public void prepareVastVideoConfiguration(@Nullable final String vastXml,
                                              @NonNull final VastManagerListener vastManagerListener,
                                              @Nullable String dspCreativeId,
                                              @NonNull final Context context) {
        Preconditions.checkNotNull(vastManagerListener, "vastManagerListener cannot be null");
        Preconditions.checkNotNull(context, "context cannot be null");

        if (mVastXmlManagerAggregator == null) {
            mVastManagerListener = vastManagerListener;
            mVastXmlManagerAggregator = new VastXmlManagerAggregator(this,
                    mScreenAspectRatio,
                    mScreenWidthDp,
                    context.getApplicationContext());
            mDspCreativeId = dspCreativeId;

            try {
                AsyncTasks.safeExecuteOnExecutor(mVastXmlManagerAggregator, vastXml);
            } catch (Exception e) {
                MoPubLog.log(ERROR_WITH_THROWABLE, "Failed to aggregate vast xml", e);
                mVastManagerListener.onVastVideoConfigurationPrepared(null);
            }
        }
    }

    /**
     * Stops the VAST aggregator from continuing to follow wrapper redirects.
     */
    public void cancel() {
        if (mVastXmlManagerAggregator != null) {
            mVastXmlManagerAggregator.cancel(true);
            mVastXmlManagerAggregator = null;
        }
    }

    @Override
    public void onAggregationComplete(@Nullable final VastVideoConfig vastVideoConfig) {
        if (mVastManagerListener == null) {
            throw new IllegalStateException(
                    "mVastManagerListener cannot be null here. Did you call " +
                            "prepareVastVideoConfiguration()?");
        }

        if (vastVideoConfig == null) {
            mVastManagerListener.onVastVideoConfigurationPrepared(null);
            return;
        }

        if (!TextUtils.isEmpty(mDspCreativeId)) {
            vastVideoConfig.setDspCreativeId(mDspCreativeId);
        }

        // Return immediately if we already have a cached video or if video precache is not required.
        if (!mShouldPreCacheVideo || updateDiskMediaFileUrl(vastVideoConfig)) {
            mVastManagerListener.onVastVideoConfigurationPrepared(vastVideoConfig);
            return;
        }

        final VideoDownloaderListener videoDownloaderListener = new VideoDownloaderListener() {
            @Override
            public void onComplete(boolean success) {
                if (success && updateDiskMediaFileUrl(vastVideoConfig)) {
                    mVastManagerListener.onVastVideoConfigurationPrepared(vastVideoConfig);
                } else {
                    MoPubLog.log(CUSTOM, "Failed to download VAST video.");
                    mVastManagerListener.onVastVideoConfigurationPrepared(null);
                }
            }
        };

        VideoDownloader.cache(vastVideoConfig.getNetworkMediaFileUrl(), videoDownloaderListener);
    }

    /**
     * This method takes the media file http url and checks to see if we have the media file downloaded
     * and cached in the Disk LRU cache. If it is cached, then the {@link VastVideoConfig} is
     * updated with the media file's url on disk.
     *
     * @param vastVideoConfig used to store the media file's disk url and web url
     * @return true if the media file was already cached locally, otherwise false
     */
    private boolean updateDiskMediaFileUrl(
            @NonNull final VastVideoConfig vastVideoConfig) {
        Preconditions.checkNotNull(vastVideoConfig, "vastVideoConfig cannot be null");

        final String networkMediaFileUrl = vastVideoConfig.getNetworkMediaFileUrl();
        if (VideoCacheService.containsKey(networkMediaFileUrl)) {
            final String filePathDiskCache = VideoCacheService.getFilePath(networkMediaFileUrl);
            vastVideoConfig.setDiskMediaFileUrl(filePathDiskCache);
            return true;
        }
        return false;
    }

    private void initializeScreenDimensions(@NonNull final Context context) {
        Preconditions.checkNotNull(context, "context cannot be null");
        final Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        final int screenWidth = display.getWidth();
        final int screenHeight = display.getHeight();
        // Use the screen density to convert x and y (in pixels) to DP. Also, check the density to
        // make sure that this is a valid density and that this is not going to divide by 0.
        float density = context.getResources().getDisplayMetrics().density;
        if (density <= 0) {
            density = 1;
        }

        mScreenAspectRatio = (double) screenWidth / screenHeight;
        mScreenWidthDp = (int) (screenWidth / density);
    }

    @VisibleForTesting
    @Deprecated
    int getScreenWidthDp() {
        return mScreenWidthDp;
    }

    @VisibleForTesting
    @Deprecated
    double getScreenAspectRatio() {
        return mScreenAspectRatio;
    }
}
