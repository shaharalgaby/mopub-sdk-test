// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.mopub.common.CloseableLayout;
import com.mopub.common.CreativeOrientation;
import com.mopub.common.FullAdType;
import com.mopub.common.Preconditions;
import com.mopub.common.UrlAction;
import com.mopub.common.UrlHandler;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.AsyncTasks;
import com.mopub.common.util.DeviceUtils;
import com.mopub.common.util.Dips;
import com.mopub.common.util.Intents;
import com.mopub.mobileads.factories.HtmlControllerFactory;
import com.mopub.mobileads.base.R;
import com.mopub.mobileads.resource.DrawableConstants;
import com.mopub.mraid.MraidController;
import com.mopub.mraid.PlacementType;
import com.mopub.mraid.WebViewDebugListener;
import com.mopub.network.MoPubImageLoader;
import com.mopub.network.MoPubNetworkError;
import com.mopub.network.Networking;
import com.mopub.network.TrackingRequest;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.EnumSet;

import static com.mopub.common.IntentActions.ACTION_FULLSCREEN_CLICK;
import static com.mopub.common.IntentActions.ACTION_FULLSCREEN_DISMISS;
import static com.mopub.common.IntentActions.ACTION_FULLSCREEN_FAIL;
import static com.mopub.common.IntentActions.ACTION_REWARDED_AD_COMPLETE;
import static com.mopub.common.MoPubBrowser.MOPUB_BROWSER_REQUEST_CODE;
import static com.mopub.common.logging.MoPubLog.SdkLogEvent.CUSTOM;
import static com.mopub.common.util.JavaScriptWebViewCallbacks.WEB_VIEW_DID_APPEAR;
import static com.mopub.common.util.JavaScriptWebViewCallbacks.WEB_VIEW_DID_CLOSE;
import static com.mopub.mobileads.AdData.MILLIS_IN_SECOND;
import static com.mopub.mobileads.BaseBroadcastReceiver.broadcastAction;

public class FullscreenAdController implements BaseVideoViewController.BaseVideoViewControllerListener {

    static final String IMAGE_KEY = "image";
    @VisibleForTesting
    static final String CLICK_DESTINATION_KEY = "clk";
    @VisibleForTesting
    static final String WIDTH_KEY = "w";
    @VisibleForTesting
    static final String HEIGHT_KEY = "h";
    private final static EnumSet<UrlAction> SUPPORTED_URL_ACTIONS = EnumSet.of(
            UrlAction.IGNORE_ABOUT_SCHEME,
            UrlAction.HANDLE_PHONE_SCHEME,
            UrlAction.OPEN_APP_MARKET,
            UrlAction.OPEN_NATIVE_BROWSER,
            UrlAction.OPEN_IN_APP_BROWSER,
            UrlAction.HANDLE_SHARE_TWEET,
            UrlAction.FOLLOW_DEEP_LINK_WITH_FALLBACK,
            UrlAction.FOLLOW_DEEP_LINK);

    @NonNull
    private final Activity mActivity;
    @Nullable
    private BaseVideoViewController mVideoViewController;
    @NonNull
    private final MoPubWebViewController mMoPubWebViewController;
    @NonNull
    private final AdData mAdData;
    @NonNull
    private ControllerState mState = ControllerState.MRAID;
    @Nullable
    private WebViewDebugListener mDebugListener;
    @Nullable
    private CloseableLayout mCloseableLayout;
    @Nullable
    private RadialCountdownWidget mRadialCountdownWidget;
    @Nullable
    private CloseButtonCountdownRunnable mCountdownRunnable;
    @Nullable
    private VastCompanionAdConfig mSelectedVastCompanionAdConfig;
    @Nullable
    private ImageView mImageView;
    @Nullable
    private VideoCtaButtonWidget mVideoCtaButtonWidget;
    @Nullable
    private VastVideoBlurLastVideoFrameTask mBlurLastVideoFrameTask;
    @Nullable
    private String mImageClickDestinationUrl;
    private int mCurrentElapsedTimeMillis;
    private int mCountdownTimeMillis;
    private boolean mShowCloseButtonEventFired;
    private boolean mIsCalibrationDone;
    private boolean mRewardedCompletionFired;
    private int mVideoTimeElapsed;
    private boolean mOnVideoFinishCalled;
    private int mVideoDurationMillis;
    private int mShowCountdownTimerDelayMillis = 0;
    private boolean mShowCountdownTimer = true;

    @VisibleForTesting
    enum ControllerState {
        VIDEO,
        MRAID,
        HTML,
        IMAGE
    }

    public FullscreenAdController(@NonNull final Activity activity,
                                  @Nullable final Bundle savedInstanceState,
                                  @NonNull final Intent intent,
                                  @NonNull final AdData adData) {
        mActivity = activity;
        mAdData = adData;

        final WebViewCacheService.Config config = WebViewCacheService.popWebViewConfig(adData.getBroadcastIdentifier());
        if (config != null && config.getController() != null) {
            mMoPubWebViewController = config.getController();
        } else if ("html".equals(adData.getAdType())) {
            mMoPubWebViewController = HtmlControllerFactory.create(activity,
                    adData.getDspCreativeId());
        } else {
            // If we hit this, then we assume this is MRAID since it isn't HTML
            mMoPubWebViewController = new MraidController(activity,
                    adData.getDspCreativeId(),
                    PlacementType.INTERSTITIAL);
        }

        final String htmlData = adData.getAdPayload();
        if (TextUtils.isEmpty(htmlData)) {
            MoPubLog.log(CUSTOM, "MoPubFullscreenActivity received an empty HTML body. Finishing the activity.");
            activity.finish();
            return;
        }

        mMoPubWebViewController.setDebugListener(mDebugListener);
        mMoPubWebViewController.setMoPubWebViewListener(new BaseHtmlWebView.BaseWebViewListener() {
            @Override
            public void onLoaded(View view) {
                if (ControllerState.HTML.equals(mState) || ControllerState.MRAID.equals(mState)) {
                    mMoPubWebViewController.loadJavascript(WEB_VIEW_DID_APPEAR.getJavascript());
                }
            }

            @Override
            public void onFailedToLoad(MoPubErrorCode errorCode) {
                /* NO-OP. Loading has already completed if we're here */
            }

            @Override
            public void onFailed() {
                MoPubLog.log(CUSTOM, "FullscreenAdController failed to load. Finishing MoPubFullscreenActivity.");
                broadcastAction(activity, adData.getBroadcastIdentifier(), ACTION_FULLSCREEN_FAIL);
                activity.finish();
            }

            @Override
            public void onRenderProcessGone(@NonNull final MoPubErrorCode errorCode) {
                MoPubLog.log(CUSTOM, "Finishing the activity due to a render process gone problem: " + errorCode);
                broadcastAction(activity, adData.getBroadcastIdentifier(), ACTION_FULLSCREEN_FAIL);
                activity.finish();
            }

            @Override
            public void onClicked() {
                onAdClicked(activity, adData);
            }

            public void onClose() {
                broadcastAction(activity, adData.getBroadcastIdentifier(), ACTION_FULLSCREEN_DISMISS);
                mMoPubWebViewController.loadJavascript(WEB_VIEW_DID_CLOSE.getJavascript());
                activity.finish();
            }

            @Override
            public void onExpand() {
                // No-op. The interstitial is always expanded.
            }

            @Override
            public void onResize(final boolean toOriginalSize) {
                // No-op. The interstitial is always expanded.
            }
        });

        mCloseableLayout = new CloseableLayout(activity, null);

        mShowCountdownTimer = mAdData.getCreativeExperienceSettings().getMainAdConfig()
                .getShowCountdownTimer();

        if (FullAdType.VAST.equals(mAdData.getFullAdType())) {
            mVideoViewController = createVideoViewController(activity, savedInstanceState, intent, adData.getBroadcastIdentifier());
            mState = ControllerState.VIDEO;
            mVideoViewController.onCreate();
            return;
        } else if (FullAdType.JSON.equals(mAdData.getFullAdType())) {
            mState = ControllerState.IMAGE;
            final JSONObject imageData;
            final String imageUrl;
            final int imageWidth, imageHeight;
            try {
                imageData = new JSONObject(mAdData.getAdPayload());
                imageUrl = imageData.getString(IMAGE_KEY);
                imageWidth = imageData.getInt(WIDTH_KEY);
                imageHeight = imageData.getInt(HEIGHT_KEY);
                mImageClickDestinationUrl = imageData.optString(CLICK_DESTINATION_KEY);
            } catch (JSONException e) {
                MoPubLog.log(CUSTOM, "Unable to load image into fullscreen container.");
                broadcastAction(activity, adData.getBroadcastIdentifier(), ACTION_FULLSCREEN_FAIL);
                mActivity.finish();
                return;
            }
            mImageView = new ImageView(mActivity);
            Networking.getImageLoader(mActivity).fetch(imageUrl, new MoPubImageLoader.ImageListener() {
                @Override
                public void onResponse(@NonNull MoPubImageLoader.ImageContainer imageContainer, boolean isImmediate) {
                    Bitmap bitmap = imageContainer.getBitmap();
                    if (mImageView != null && bitmap != null) {
                        mImageView.setAdjustViewBounds(true);
                        mImageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                        // Scale up the image as if the device had a DPI of 160
                        bitmap.setDensity(DisplayMetrics.DENSITY_MEDIUM);
                        mImageView.setImageBitmap(bitmap);
                    } else {
                        MoPubLog.log(CUSTOM, String.format("%s returned null bitmap", imageUrl));
                    }
                }

                @Override
                public void onErrorResponse(@NonNull MoPubNetworkError networkError) {
                    MoPubLog.log(CUSTOM, String.format("Failed to retrieve image at %s", imageUrl));
                }
            }, imageWidth, imageHeight, ImageView.ScaleType.CENTER_INSIDE);

            final FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            layoutParams.gravity = Gravity.CENTER;
            mImageView.setLayoutParams(layoutParams);
            mCloseableLayout.addView(mImageView);
            mCloseableLayout.setOnCloseListener(() -> {
                destroy();
                mActivity.finish();
            });
            mActivity.setContentView(mCloseableLayout);

            // Images should be clickable immediately
            if (mImageView != null) {
                mImageView.setOnClickListener(view -> onAdClicked(mActivity, mAdData));
            }
        } else {
            if (config == null || config.getController() == null) {
                mMoPubWebViewController.fillContent(htmlData,
                        adData.getViewabilityVendors(),
                        webView -> { });
            }

            if ("html".equals(mAdData.getAdType())) {
                mState = ControllerState.HTML;
            } else {
                mState = ControllerState.MRAID;
            }

            mCloseableLayout.setOnCloseListener(() -> {
                destroy();
                mActivity.finish();
            });
            mCloseableLayout.addView(mMoPubWebViewController.getAdContainer(),
                    new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

            mActivity.setContentView(mCloseableLayout);
            mMoPubWebViewController.onShow(mActivity);
        }

        if (ControllerState.HTML.equals(mState) || ControllerState.IMAGE.equals(mState)) {
            // Default to device orientation
            CreativeOrientation requestedOrientation = CreativeOrientation.DEVICE;
            if (adData.getOrientation() != null) {
                requestedOrientation = adData.getOrientation();
            }
            DeviceUtils.lockOrientation(activity, requestedOrientation);
        }

        // Calculate the show close button delay for all non-VAST ads
        mCountdownTimeMillis = CreativeExperiencesFormulae.getCountdownDuration(
                false, // isVast
                false, // isEndCard
                null, // endCardType
                0, // videoDurationSecs
                0, // elapsedTimeInAdSecs
                adData.getCreativeExperienceSettings()
        ) * MILLIS_IN_SECOND;

        if (mCountdownTimeMillis > 0) {
            mShowCountdownTimerDelayMillis = adData.getCreativeExperienceSettings().getMainAdConfig()
                    .getCountdownTimerDelaySecs() * MILLIS_IN_SECOND;

            if (!mShowCountdownTimer || mShowCountdownTimerDelayMillis >= mCountdownTimeMillis) {
                // Countdown timer is never shown
                mShowCountdownTimerDelayMillis = mCountdownTimeMillis;
                mShowCountdownTimer = false;
            }

            mCloseableLayout.setCloseAlwaysInteractable(false);
            mCloseableLayout.setCloseVisible(false);

            addRadialCountdownWidget(activity);
            if (mRadialCountdownWidget != null) {
                mRadialCountdownWidget.calibrate(mCountdownTimeMillis);
                mIsCalibrationDone = true;
                final Handler mainHandler = new Handler(Looper.getMainLooper());
                mCountdownRunnable = new CloseButtonCountdownRunnable(this, mainHandler);
                return;
            }
        }

        // Show the close button immediately if mCountdownTimeMillis == 0 or
        // mRadialCountdownWidget is null
        showCloseButton();
    }

    @VisibleForTesting
    BaseVideoViewController createVideoViewController(Activity activity, Bundle savedInstanceState, Intent intent, Long broadcastIdentifier) throws IllegalStateException {
        return new VastVideoViewController(activity, intent.getExtras(), savedInstanceState, broadcastIdentifier, this);
    }

    // Start BaseVideoViewControllerListener implementation

    @Override
    public void onSetContentView(View view) {
        mActivity.setContentView(view);
    }

    @Override
    public void onSetRequestedOrientation(int requestedOrientation) {
        mActivity.setRequestedOrientation(requestedOrientation);
    }

    @Override
    public void onVideoFinish(final int timeElapsed) {
        if (mCloseableLayout == null || mSelectedVastCompanionAdConfig == null) {
            destroy();
            mActivity.finish();
            return;
        }

        if (mOnVideoFinishCalled) {
            return;
        }
        mOnVideoFinishCalled = true;

        mVideoTimeElapsed = timeElapsed;

        if (mVideoViewController != null) {
            mVideoViewController.onPause();
            mVideoViewController.onDestroy();
            mVideoViewController = null;
        }

        mCloseableLayout.removeAllViews();
        mCloseableLayout.setOnCloseListener(() -> {
            destroy();
            mActivity.finish();
        });
        final VastResource vastResource = mSelectedVastCompanionAdConfig.getVastResource();
        if (VastResource.Type.STATIC_RESOURCE.equals(vastResource.getType()) &&
                VastResource.CreativeType.IMAGE.equals(vastResource.getCreativeType()) ||
                VastResource.Type.BLURRED_LAST_FRAME.equals(vastResource.getType())) {
            mState = ControllerState.IMAGE;
            if (mImageView == null) {
                MoPubLog.log(CUSTOM, "Companion image null. Skipping.");
                destroy();
                mActivity.finish();
                return;
            }
            final RelativeLayout relativeLayout = new RelativeLayout(mActivity);
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.MATCH_PARENT);
            mImageView.setLayoutParams(layoutParams);
            // remove mImageView first to prevent IllegalStateException when added to relativeLayout
            final ViewGroup imageViewParent = (ViewGroup) mImageView.getParent();
            if (imageViewParent != null) {
                imageViewParent.removeView(mImageView);
            }
            relativeLayout.addView(mImageView);
            if (mVideoCtaButtonWidget != null) {
                // remove mVideoCtaButtonWidget first to prevent IllegalStateException when added to relativeLayout
                final ViewGroup videoCtaButtonWidgetParent = (ViewGroup) mVideoCtaButtonWidget.getParent();
                if (videoCtaButtonWidgetParent != null) {
                    videoCtaButtonWidgetParent.removeView(mVideoCtaButtonWidget);
                }
            }
            addVideoCtaButtonToLayout(mActivity,
                    !VastResource.Type.BLURRED_LAST_FRAME.equals(vastResource.getType()));
            mCloseableLayout.addView(relativeLayout);
        } else {
            mState = ControllerState.MRAID;
            mCloseableLayout.addView(mMoPubWebViewController.getAdContainer(),
                    new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        }

        mActivity.setContentView(mCloseableLayout);
        mMoPubWebViewController.onShow(mActivity);

        // End card timer
        mCountdownTimeMillis = CreativeExperiencesFormulae.getCountdownDuration(
                false, // isVast
                true, // isEndCard
                EndCardType.fromVastResourceType(vastResource.getType()), // endCardType
                mVideoDurationMillis / MILLIS_IN_SECOND, // videoDurationSecs
                timeElapsed / MILLIS_IN_SECOND, // elapsedTimeInAdSecs
                mAdData.getCreativeExperienceSettings()
        ) * MILLIS_IN_SECOND;

        CreativeExperienceAdConfig endCardAdConfig = mAdData.getCreativeExperienceSettings()
                .getEndCardConfig();
        mShowCountdownTimer = endCardAdConfig.getShowCountdownTimer();

        if (mCountdownTimeMillis > 0) {
            mShowCountdownTimerDelayMillis = endCardAdConfig.getCountdownTimerDelaySecs() * MILLIS_IN_SECOND;

            if (!mShowCountdownTimer || mShowCountdownTimerDelayMillis >= mCountdownTimeMillis) {
                // Countdown timer is never shown
                mShowCountdownTimerDelayMillis = mCountdownTimeMillis;
                mShowCountdownTimer = false;
            }

            mCloseableLayout.setCloseAlwaysInteractable(false);
            mCloseableLayout.setCloseVisible(false);

            addRadialCountdownWidget(mActivity);
            if (mRadialCountdownWidget != null) {
                // start a new timer for the end card
                mRadialCountdownWidget.calibrate(mCountdownTimeMillis);
                mRadialCountdownWidget.updateCountdownProgress(mCountdownTimeMillis, 0);
                mIsCalibrationDone = true;
                final Handler mainHandler = new Handler(Looper.getMainLooper());
                mCountdownRunnable = new CloseButtonCountdownRunnable(this, mainHandler);
                mCountdownRunnable.mCurrentElapsedTimeMillis = 0;
                startRunnables();

                mSelectedVastCompanionAdConfig.handleImpression(mActivity, timeElapsed);
                return;
            }
        }

        // Show the close button immediately if mCountdownTimeMillis == 0 or
        // mRadialCountdownWidget is null
        mCloseableLayout.setCloseAlwaysInteractable(true);
        showCloseButton();

        mSelectedVastCompanionAdConfig.handleImpression(mActivity, timeElapsed);
    }

    @Override
    public void onStartActivityForResult(Class<? extends Activity> clazz, int requestCode, Bundle extras) {
        if (clazz == null) {
            return;
        }

        final Intent intent = Intents.getStartActivityIntent(mActivity, clazz, extras);

        try {
            mActivity.startActivityForResult(intent, requestCode);
        } catch (ActivityNotFoundException e) {
            MoPubLog.log(CUSTOM, "Activity " + clazz.getName() + " not found. Did you declare it in your AndroidManifest.xml?");
        }
    }

    @Override
    public void onCompanionAdReady(@Nullable final VastCompanionAdConfig selectedVastCompanionAdConfig,
                                   final int videoDurationMs) {
        if (mCloseableLayout == null) {
            MoPubLog.log(CUSTOM, "CloseableLayout is null. This should not happen.");
        }

        mVideoDurationMillis = videoDurationMs;

        mSelectedVastCompanionAdConfig = selectedVastCompanionAdConfig;
        if (mSelectedVastCompanionAdConfig == null) {
            return;
        }
        final VastResource vastResource = mSelectedVastCompanionAdConfig.getVastResource();
        final String htmlResourceValue = vastResource.getHtmlResourceValue();
        if (TextUtils.isEmpty(htmlResourceValue)) {
            return;
        }
        if (VastResource.Type.STATIC_RESOURCE.equals(vastResource.getType()) &&
                VastResource.CreativeType.IMAGE.equals(vastResource.getCreativeType())) {
            mImageView = new ImageView(mActivity);
            Networking.getImageLoader(mActivity).fetch(vastResource.getResource(), new MoPubImageLoader.ImageListener() {
                @Override
                public void onResponse(@NotNull MoPubImageLoader.ImageContainer imageContainer, boolean isImmediate) {
                    Bitmap bitmap = imageContainer.getBitmap();
                    if (mImageView != null && bitmap != null) {
                        mImageView.setAdjustViewBounds(true);
                        mImageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                        // Scale up the image as if the device had a DPI of 160
                        bitmap.setDensity(DisplayMetrics.DENSITY_MEDIUM);
                        mImageView.setImageBitmap(bitmap);
                    } else {
                        MoPubLog.log(CUSTOM, String.format("%s returned null bitmap", vastResource.getResource()));
                    }
                }

                @Override
                public void onErrorResponse(@NotNull MoPubNetworkError networkError) {
                    MoPubLog.log(CUSTOM, String.format("Failed to retrieve image at %s",
                            vastResource.getResource()));
                }
            }, mSelectedVastCompanionAdConfig.getWidth(), mSelectedVastCompanionAdConfig.getHeight(), ImageView.ScaleType.CENTER_INSIDE);
            mImageView.setOnClickListener(view -> onAdClicked(mActivity, mAdData));
        } else if (VastResource.Type.BLURRED_LAST_FRAME.equals(vastResource.getType())) {
            mImageView = new ImageView(mActivity);
            mImageView.setOnClickListener(view -> onAdClicked(mActivity, mAdData));
            mBlurLastVideoFrameTask = new VastVideoBlurLastVideoFrameTask(
                    new MediaMetadataRetriever(),
                    mImageView,
                    videoDurationMs);
            AsyncTasks.safeExecuteOnExecutor(mBlurLastVideoFrameTask, vastResource.getResource());
        } else {
            mMoPubWebViewController.fillContent(htmlResourceValue, null, null);
        }
    }

    // End BaseVideoViewControllerListener implementation

    public void pause() {
        if (mVideoViewController != null) {
            mVideoViewController.onPause();
        }
        if (ControllerState.HTML.equals(mState) || ControllerState.MRAID.equals(mState)) {
            mMoPubWebViewController.pause(false);
        }
        stopRunnables();
    }

    public void resume() {
        if (mVideoViewController != null) {
            mVideoViewController.onResume();
        }
        if (ControllerState.HTML.equals(mState) || ControllerState.MRAID.equals(mState)) {
            mMoPubWebViewController.resume();
        }
        startRunnables();
    }

    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if (mVideoViewController != null) {
            mVideoViewController.onActivityResult(requestCode, resultCode, data);
        }
    }

    public void destroy() {
        mMoPubWebViewController.destroy();
        if (mVideoViewController != null) {
            mVideoViewController.onDestroy();
            mVideoViewController = null;
        }
        stopRunnables();
        if (mBlurLastVideoFrameTask != null) {
            mBlurLastVideoFrameTask.cancel(true);
        }
        broadcastAction(mActivity, mAdData.getBroadcastIdentifier(), ACTION_FULLSCREEN_DISMISS);
    }

    boolean backButtonEnabled() {
        if (ControllerState.VIDEO.equals(mState) && mVideoViewController != null) {
            return mVideoViewController.backButtonEnabled();
        } else if (ControllerState.MRAID.equals(mState) || ControllerState.IMAGE.equals(mState)) {
            return mShowCloseButtonEventFired;
        }
        return true;
    }

    private boolean isCloseable() {
        return !mShowCloseButtonEventFired
                && mCurrentElapsedTimeMillis >= mCountdownTimeMillis;
    }

    @VisibleForTesting
    void showCloseButton() {
        mShowCloseButtonEventFired = true;

        if (mRadialCountdownWidget != null) {
            mRadialCountdownWidget.setVisibility(View.GONE);
        }
        if (mCloseableLayout != null) {
            mCloseableLayout.setCloseVisible(true);
        }

        if (!mRewardedCompletionFired && mAdData.isRewarded()) {
            broadcastAction(mActivity, mAdData.getBroadcastIdentifier(), ACTION_REWARDED_AD_COMPLETE);
            mRewardedCompletionFired = true;
        }
    }

    private void updateCountdown(int currentElapsedTimeMillis) {
        mCurrentElapsedTimeMillis = currentElapsedTimeMillis;
        if (mIsCalibrationDone && mRadialCountdownWidget != null) {
            mRadialCountdownWidget.updateCountdownProgress(mCountdownTimeMillis,
                    mCurrentElapsedTimeMillis);

            // Make the countdown timer visible if the show countdown timer delay has passed
            if (!mShowCloseButtonEventFired && mShowCountdownTimer
                    && mRadialCountdownWidget.getVisibility() != View.VISIBLE
                    && currentElapsedTimeMillis >= mShowCountdownTimerDelayMillis) {
                mRadialCountdownWidget.setVisibility(View.VISIBLE);
            }
        }
    }

    private void startRunnables() {
        if (mCountdownRunnable != null) {
            mCountdownRunnable.startRepeating(AdData.COUNTDOWN_UPDATE_INTERVAL_MILLIS);
        }
    }

    private void stopRunnables() {
        if (mCountdownRunnable != null) {
            mCountdownRunnable.stop();
        }
    }

    private void addRadialCountdownWidget(@NonNull final Context context) {
        Preconditions.checkNotNull(context);
        
        if (mCloseableLayout == null) {
            return;
        }

        mRadialCountdownWidget = LayoutInflater.from(context)
                .inflate(R.layout.radial_countdown_layout, mCloseableLayout, true)
                .findViewById(R.id.mopub_fullscreen_radial_countdown);
    }

    private void addVideoCtaButtonToLayout(@NonNull final Context context,
                                           final boolean hasCompanionAd) {
        Preconditions.checkNotNull(context);

        if (TextUtils.isEmpty(mSelectedVastCompanionAdConfig.getClickThroughUrl())
                || mCloseableLayout == null) {
            return;
        }

        mVideoCtaButtonWidget = LayoutInflater.from(context)
                .inflate(R.layout.video_cta_button_layout, mCloseableLayout, true)
                .findViewById(R.id.mopub_fullscreen_video_cta_button);

        mVideoCtaButtonWidget.setHasCompanionAd(hasCompanionAd);
        mVideoCtaButtonWidget.setHasClickthroughUrl(true);
        final String customCtaText = mSelectedVastCompanionAdConfig.getCustomCtaText();
        if (!TextUtils.isEmpty(customCtaText)) {
            mVideoCtaButtonWidget.updateCtaText(customCtaText);
        }
        mVideoCtaButtonWidget.notifyVideoComplete();
        mVideoCtaButtonWidget.setOnClickListener(view -> onAdClicked(mActivity, mAdData));
    }

    void onAdClicked(@NonNull final Activity activity, @NonNull final AdData adData) {
        if (mSelectedVastCompanionAdConfig != null &&
                !TextUtils.isEmpty(mSelectedVastCompanionAdConfig.getClickThroughUrl()) &&
                ControllerState.IMAGE.equals(mState)) {
            broadcastAction(activity, adData.getBroadcastIdentifier(), ACTION_FULLSCREEN_CLICK);
            TrackingRequest.makeVastTrackingHttpRequest(
                    mSelectedVastCompanionAdConfig.getClickTrackers(),
                    null,
                    mVideoTimeElapsed,
                    null,
                    activity
            );
            mSelectedVastCompanionAdConfig.handleClick(
                    activity,
                    MOPUB_BROWSER_REQUEST_CODE,
                    null,
                    adData.getDspCreativeId()
            );
        } else if (mSelectedVastCompanionAdConfig != null && ControllerState.MRAID.equals(mState)) {
            broadcastAction(activity, adData.getBroadcastIdentifier(), ACTION_FULLSCREEN_CLICK);
            TrackingRequest.makeVastTrackingHttpRequest(
                    mSelectedVastCompanionAdConfig.getClickTrackers(),
                    null,
                    mVideoTimeElapsed,
                    null,
                    activity
            );
        } else if (mSelectedVastCompanionAdConfig == null &&
                ControllerState.IMAGE.equals(mState) &&
                mImageClickDestinationUrl != null &&
                !TextUtils.isEmpty(mImageClickDestinationUrl)) {
            broadcastAction(activity, adData.getBroadcastIdentifier(), ACTION_FULLSCREEN_CLICK);
            new UrlHandler.Builder()
                    .withDspCreativeId(mAdData.getDspCreativeId())
                    .withSupportedUrlActions(SUPPORTED_URL_ACTIONS)
                    .build().handleUrl(mActivity, mImageClickDestinationUrl);
        } else if (mSelectedVastCompanionAdConfig == null &&
                (ControllerState.MRAID.equals(mState) || ControllerState.HTML.equals(mState))) {
            broadcastAction(activity, adData.getBroadcastIdentifier(), ACTION_FULLSCREEN_CLICK);
        }
    }

    static class CloseButtonCountdownRunnable extends RepeatingHandlerRunnable {
        @NonNull
        private final FullscreenAdController mController;
        private int mCurrentElapsedTimeMillis;

        private CloseButtonCountdownRunnable(@NonNull final FullscreenAdController controller,
                                             @NonNull final Handler handler) {
            super(handler);
            Preconditions.checkNotNull(handler);
            Preconditions.checkNotNull(controller);

            mController = controller;
        }

        @Override
        public void doWork() {
            mCurrentElapsedTimeMillis += mUpdateIntervalMillis;
            mController.updateCountdown(mCurrentElapsedTimeMillis);

            if (mController.isCloseable()) {
                mController.showCloseButton();
            }
        }

        @Deprecated
        @VisibleForTesting
        int getCurrentElapsedTimeMillis() {
            return mCurrentElapsedTimeMillis;
        }
    }

    @Deprecated
    @VisibleForTesting
    void setDebugListener(@Nullable final WebViewDebugListener debugListener) {
        mDebugListener = debugListener;
        mMoPubWebViewController.setDebugListener(mDebugListener);
    }

    @Deprecated
    @VisibleForTesting
    void setVideoTimeElapsed(final int videoTimeElapsed) {
        mVideoTimeElapsed = videoTimeElapsed;
    }

    @Deprecated
    @VisibleForTesting
    int getVideoTimeElapsed() {
        return mVideoTimeElapsed;
    }

    @Deprecated
    @VisibleForTesting
    void setOnVideoFinishCalled(final boolean onVideoFinishCalled) {
        mOnVideoFinishCalled = onVideoFinishCalled;
    }

    @Deprecated
    @VisibleForTesting
    boolean getOnVideoFinishCalled() {
        return mOnVideoFinishCalled;
    }

    @Deprecated
    @VisibleForTesting
    @Nullable
    MoPubWebViewController getMoPubWebViewController() {
        return mMoPubWebViewController;
    }

    @Deprecated
    @VisibleForTesting
    int getCountdownTimeMillis() {
        return mCountdownTimeMillis;
    }

    @Deprecated
    @VisibleForTesting
    @NonNull
    CloseableLayout getCloseableLayout() {
        return mCloseableLayout;
    }

    @Deprecated
    @VisibleForTesting
    void setCloseableLayout(@Nullable final CloseableLayout closeableLayout) {
        mCloseableLayout = closeableLayout;
    }

    @Deprecated
    @VisibleForTesting
    @Nullable
    CloseButtonCountdownRunnable getCountdownRunnable() {
        return mCountdownRunnable;
    }

    @Deprecated
    @VisibleForTesting
    @Nullable
    RadialCountdownWidget getRadialCountdownWidget() {
        return mRadialCountdownWidget;
    }

    @Deprecated
    @VisibleForTesting
    boolean isCalibrationDone() {
        return mIsCalibrationDone;
    }

    @Deprecated
    @VisibleForTesting
    boolean isRewarded() {
        return mRewardedCompletionFired;
    }

    @Deprecated
    @VisibleForTesting
    boolean isShowCloseButtonEventFired() {
        return mShowCloseButtonEventFired;
    }

    @Deprecated
    @VisibleForTesting
    @Nullable
    ImageView getImageView() {
        return mImageView;
    }

    @Deprecated
    @VisibleForTesting
    void setImageView(@Nullable ImageView imageView) {
        mImageView = imageView;
    }

    @Deprecated
    @VisibleForTesting
    void setBlurLastVideoFrameTask(@Nullable final VastVideoBlurLastVideoFrameTask blurLastVideoFrameTask) {
        mBlurLastVideoFrameTask = blurLastVideoFrameTask;
    }

    @Deprecated
    @VisibleForTesting
    @Nullable
    VastVideoBlurLastVideoFrameTask getBlurLastVideoFrameTask() {
        return mBlurLastVideoFrameTask;
    }

    @Deprecated
    @VisibleForTesting
    @Nullable
    String getImageClickDestinationUrl() {
        return mImageClickDestinationUrl;
    }

    @Deprecated
    @VisibleForTesting
    @Nullable
    VideoCtaButtonWidget getVideoCtaButtonWidget() {
        return mVideoCtaButtonWidget;
    }

    @Deprecated
    @VisibleForTesting
    @Nullable
    VastCompanionAdConfig getSelectedVastCompanionAdConfig() {
        return mSelectedVastCompanionAdConfig;
    }

    @Deprecated
    @VisibleForTesting
    void setSelectedVastCompanionAdConfig(@Nullable final VastCompanionAdConfig selectedVastCompanionAdConfig) {
        mSelectedVastCompanionAdConfig = selectedVastCompanionAdConfig;
    }

    @Deprecated
    @VisibleForTesting
    @Nullable
    BaseVideoViewController getVideoViewController() {
        return mVideoViewController;
    }

    @Deprecated
    @VisibleForTesting
    void setVideoViewController(@Nullable BaseVideoViewController videoViewController) {
        mVideoViewController = videoViewController;
    }

    @Deprecated
    @VisibleForTesting
    @NonNull
    ControllerState getState() {
        return mState;
    }

    @Deprecated
    @VisibleForTesting
    boolean getShowCountdownTimer() {
        return mShowCountdownTimer;
    }

    @Deprecated
    @VisibleForTesting
    int getShowCountdownTimerDelaysMillis() {
        return mShowCountdownTimerDelayMillis;
    }

    @Deprecated
    @VisibleForTesting
    int getCurrentTimeElapsedMillis() {
        return mCurrentElapsedTimeMillis;
    }
}
