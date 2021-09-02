// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

import android.util.Pair;
import android.view.View;
import android.webkit.WebView;

import com.mopub.common.logging.MoPubLog;

import java.util.HashSet;
import java.util.Set;

import static com.mopub.common.logging.MoPubLog.AdLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.SdkLogEvent.CUSTOM_WITH_THROWABLE;

/**
 * Encapsulates all third-party viewability session measurements.
 */
public class ExternalViewabilitySessionManager {
    /**
     * @deprecated as of 5.14.0. Use {@link MoPub#disableViewability()}
     */
    @Deprecated
    public enum ViewabilityVendor {
        AVID, MOAT, ALL
    }

    @Nullable
    private ViewabilityTracker mViewabilityTracker = null;

    @NonNull
    final Set<Pair<View, ViewabilityObstruction>> mObstructions = new HashSet<>();

    private ExternalViewabilitySessionManager() {
    }

    @NonNull
    public static ExternalViewabilitySessionManager create() {
        if (sCreator == null) {
            return new ExternalViewabilitySessionManager();
        } else {
            return sCreator.create();
        }
    }

    /**
     * Create ViewabilityTracker to track viewability of a WebView
     *
     * @param webView to track viewability on
     */
    @UiThread
    public void createWebViewSession(@NonNull final WebView webView) {
        Preconditions.checkUiThread();
        Preconditions.checkNotNull(webView);

        if (mViewabilityTracker != null) {
            return;
        }

        try {
            mViewabilityTracker = ViewabilityTracker.createWebViewTracker(webView);
        } catch (Exception ex) {
            MoPubLog.log(CUSTOM_WITH_THROWABLE, "createWebViewTracker failed", ex);
        }
    }

    /**
     * Create ViewabilityTracker to track viewability of native ads
     *
     * @param adView             to track viewability on
     * @param viewabilityVendors list of third party viewability vendors
     */
    @UiThread
    public void createNativeSession(@NonNull final View adView,
                                    @NonNull final Set<com.mopub.common.ViewabilityVendor> viewabilityVendors) {
        Preconditions.checkUiThread();
        Preconditions.checkNotNull(adView);
        Preconditions.checkNotNull(viewabilityVendors);

        if (mViewabilityTracker != null) {
            return;
        }

        try {
            mViewabilityTracker = ViewabilityTracker.createNativeTracker(adView, viewabilityVendors);
        } catch (Exception ex) {
            MoPubLog.log(CUSTOM_WITH_THROWABLE, "createNativeTracker failed", ex);
        }
    }

    /**
     * Create ViewabilityTracker to track viewability of VAST video
     *
     * @param videoView          to track viewability on
     * @param viewabilityVendors list of third party viewability vendors
     */
    @UiThread
    public void createVideoSession(@NonNull final View videoView,
                                   @NonNull final Set<com.mopub.common.ViewabilityVendor> viewabilityVendors) {
        Preconditions.checkUiThread();
        Preconditions.checkNotNull(videoView);
        Preconditions.checkNotNull(viewabilityVendors);

        if (mViewabilityTracker != null) {
            return;
        }

        try {
            mViewabilityTracker = ViewabilityTrackerVideo.createVastVideoTracker(videoView, viewabilityVendors);
        } catch (Exception ex) {
            MoPubLog.log(CUSTOM_WITH_THROWABLE, "createVastVideoTracker failed", ex);
        }
    }

    @UiThread
    public void startSession() {
        Preconditions.checkUiThread();

        try {
            if (mViewabilityTracker != null) {
                registerFriendlyObstruction(null, null);// yes, it is intentional
                mViewabilityTracker.startTracking();
            }
        } catch (Exception ex) {
            MoPubLog.log(CUSTOM_WITH_THROWABLE, "startSession()", ex);
        }
    }

    @UiThread
    public boolean isTracking() {
        Preconditions.checkUiThread();

        if (mViewabilityTracker == null) {
            return false;
        }

        return mViewabilityTracker.isTracking();
    }

    @UiThread
    public void trackImpression() {
        Preconditions.checkUiThread();

        try {
            if (mViewabilityTracker != null) {
                mViewabilityTracker.trackImpression();
            }
        } catch (Exception ex) {
            MoPubLog.log(CUSTOM_WITH_THROWABLE, "trackImpression()", ex);
        }
    }

    public boolean hasImpressionOccurred() {
        if (mViewabilityTracker != null) {
            return mViewabilityTracker.hasImpressionOccurred();
        }
        return false;
    }

    public void registerTrackedView(@NonNull final View adView) {
        if (mViewabilityTracker != null) {
            mViewabilityTracker.registerTrackedView(adView);
        }
    }

    /**
     * Unregisters and disables all viewability tracking for the given WebView.
     */
    @UiThread
    public void endSession() {
        Preconditions.checkUiThread();

        try {
            if (mViewabilityTracker != null) {
                mViewabilityTracker.stopTracking();
            }
        } catch (Exception ex) {
            MoPubLog.log(CUSTOM_WITH_THROWABLE, "stopTracking failed", ex);
        }
    }

    /**
     * Prevents friendly obstructions from affecting viewability scores.
     *
     * @param view View in the same Window and a higher z-index as the video playing.
     */
    @UiThread
    public void registerFriendlyObstruction(@Nullable final View view, @Nullable final ViewabilityObstruction purpose) {
        Preconditions.checkUiThread();

        final ViewabilityTracker tracker = mViewabilityTracker;
        try {
            if (tracker == null) {
                if (view != null && purpose != null) {
                    mObstructions.add(new Pair<>(view, purpose));
                }
            } else {
                if (view != null && purpose != null) {
                    tracker.registerFriendlyObstruction(view, purpose);
                }
                if (mObstructions.size() > 0) {
                    tracker.registerFriendlyObstructions(mObstructions);
                    mObstructions.clear();
                }
            }
        } catch (Exception ex) {
            MoPubLog.log(CUSTOM, ex.getLocalizedMessage());
        }
    }

    public void registerVideoObstruction(@Nullable final View view, @Nullable final ViewabilityObstruction purpose) {
        registerFriendlyObstruction(view, purpose);
    }

    @UiThread
    public void onVideoPrepared(final long durationMills) {
        Preconditions.checkUiThread();

        try {
            if (mViewabilityTracker != null) {
                mViewabilityTracker.videoPrepared(durationMills / 1000f);
            }
        } catch (Exception ex) {
            MoPubLog.log(CUSTOM_WITH_THROWABLE, "videoPrepared failed", ex);
        }
    }

    /**
     * Notify pertinent video lifecycle events (e.g. MediaPlayer onPrepared, first quartile fired).
     *
     * @param event          Corresponding {@link VideoEvent}.
     * @param playheadMillis Current video playhead, in milliseconds.
     */
    @UiThread
    public void recordVideoEvent(@NonNull final VideoEvent event,
                                 final int playheadMillis) {
        Preconditions.checkUiThread();
        Preconditions.checkNotNull(event);

        try {
            if (mViewabilityTracker != null) {
                mViewabilityTracker.trackVideo(event);
            }
        } catch (Exception ex) {
            MoPubLog.log(CUSTOM_WITH_THROWABLE, "trackVideo failed", ex);
        }
    }

    //region unit tests helper
    @VisibleForTesting
    void setMockViewabilityTracker(@Nullable final ViewabilityTracker tracker) {
        mViewabilityTracker = tracker;
    }

    // for unit testing
    @Nullable
    private static ExternalViewabilityManagerFactory sCreator;

    @VisibleForTesting
    public interface ExternalViewabilityManagerFactory {
        @NonNull
        ExternalViewabilitySessionManager create();
    }

    @VisibleForTesting
    public static void setCreator(@Nullable final ExternalViewabilityManagerFactory factory) {
        sCreator = factory;
    }
    //endregion
}
