// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.MotionEvent.ACTION_UP
import android.view.View
import android.view.View.*
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.RelativeLayout

import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.media.AudioAttributesCompat
import androidx.media2.common.SessionPlayer
import androidx.media2.common.SessionPlayer.*
import androidx.media2.common.UriMediaItem
import androidx.media2.player.MediaPlayer
import androidx.media2.player.PlaybackParams
import androidx.media2.widget.VideoView

import com.mopub.common.*
import com.mopub.common.MoPubBrowser.MOPUB_BROWSER_REQUEST_CODE
import com.mopub.common.logging.MoPubLog
import com.mopub.common.logging.MoPubLog.SdkLogEvent.CUSTOM
import com.mopub.common.logging.MoPubLog.SdkLogEvent.CUSTOM_WITH_THROWABLE
import com.mopub.common.util.Dips
import com.mopub.mobileads.AdData.Companion.MILLIS_IN_SECOND
import com.mopub.mobileads.base.R
import com.mopub.mobileads.factories.MediaPlayerFactory
import com.mopub.mobileads.factories.VideoViewFactory
import com.mopub.mobileads.resource.DrawableConstants.PrivacyInfoIcon.LEFT_MARGIN_DIPS
import com.mopub.mobileads.resource.DrawableConstants.PrivacyInfoIcon.TOP_MARGIN_DIPS
import com.mopub.network.TrackingRequest.makeVastTrackingHttpRequest

import java.util.*
import java.util.concurrent.ExecutorService

import kotlin.collections.HashSet

@Mockable
class VastVideoViewController(
    val activity: Activity,
    val extras: Bundle,
    val savedInstanceState: Bundle?,
    broadcastIdentifier: Long,
    baseListener: BaseVideoViewControllerListener
) : BaseVideoViewController(activity, broadcastIdentifier, baseListener) {

    companion object {
        const val VAST_VIDEO_CONFIG = "vast_video_config"
        const val CURRENT_POSITION = "current_position"
        const val RESUMED_VAST_CONFIG = "resumed_vast_config"
        const val WEBVIEW_PADDING = 16

        private const val VIDEO_PROGRESS_TIMER_CHECKER_DELAY: Long = 50
        private const val VIDEO_COUNTDOWN_UPDATE_INTERVAL: Long = 250
        private const val SEEKER_POSITION_NOT_INITIALIZED = -1
    }

    private val videoView: VideoView

    val mediaPlayer = MediaPlayerFactory.create(context)
    val playerCallback = PlayerCallback()

    private var seekerPositionOnPause = SEEKER_POSITION_NOT_INITIALIZED
    private var vastCompanionAdConfigs: MutableSet<VastCompanionAdConfig> = HashSet()
    @VisibleForTesting
    val vastVideoConfig: VastVideoConfig
    @VisibleForTesting
    val vastIconConfig: VastIconConfig?
    private val externalViewabilitySessionManager = ExternalViewabilitySessionManager.create()
    @VisibleForTesting
    lateinit var iconView: View

    private val progressCheckerRunnable: VastVideoViewProgressRunnable
    private val countdownRunnable: VastVideoViewCountdownRunnable
    @VisibleForTesting
    val clickThroughListener: OnTouchListener

    @VisibleForTesting
    lateinit var topGradientStripWidget: VastVideoGradientStripWidget
    @VisibleForTesting
    lateinit var bottomGradientStripWidget: VastVideoGradientStripWidget
    @VisibleForTesting
    lateinit var progressBarWidget: VastVideoProgressBarWidget
    @VisibleForTesting
    lateinit var radialCountdownWidget: RadialCountdownWidget
    @VisibleForTesting
    val ctaButtonWidget: VideoCtaButtonWidget
    @VisibleForTesting
    lateinit var closeButtonWidget: VastVideoCloseButtonWidget

    @VisibleForTesting
    var isComplete: Boolean = false
    @VisibleForTesting
    var shouldAllowClose: Boolean = false
    @VisibleForTesting
    var countdownTimeMillis = 0
    @VisibleForTesting
    var isCalibrationDone: Boolean = false
    @VisibleForTesting
    var isClosing: Boolean = false
    @VisibleForTesting
    var hasCompanionAd: Boolean = false
    @VisibleForTesting
    var showCountdownTimerDelayMillis: Int = 0
    @VisibleForTesting
    var showCountdownTimer: Boolean = true

    var videoError: Boolean = false
    val networkMediaFileUrl get() = vastVideoConfig.networkMediaFileUrl

    lateinit var creativeExperienceSettings: CreativeExperienceSettings

    init {
        val resumed =
            (savedInstanceState?.getSerializable(RESUMED_VAST_CONFIG) as? VastVideoConfig)

        val adData = extras.getParcelable<AdData>(DataKeys.AD_DATA_KEY)
        vastVideoConfig = resumed
            ?: requireNotNull(
                VastVideoConfig.fromVastVideoConfigString(
                    requireNotNull(
                        requireNotNull(adData) {
                            "AdData is invalid"
                        }.vastVideoConfigString
                    ) {
                        "VastVideoConfigByteArray is null"
                    })) {
                "VastVideoConfig is invalid"
            }

        creativeExperienceSettings = requireNotNull(adData) {
            "AdData is invalid"
        }.creativeExperienceSettings
        showCountdownTimer = creativeExperienceSettings.mainAdConfig.showCountdownTimer

        seekerPositionOnPause = resumed?.let {
            savedInstanceState?.getInt(CURRENT_POSITION, SEEKER_POSITION_NOT_INITIALIZED)
        } ?: SEEKER_POSITION_NOT_INITIALIZED

        requireNotNull(vastVideoConfig.diskMediaFileUrl) {
            "VastVideoConfig does not have a video disk path"
        }

        vastCompanionAdConfigs = vastVideoConfig.vastCompanionAdConfigs
        if (vastCompanionAdConfigs.isEmpty()) {
            vastVideoConfig.diskMediaFileUrl?.let {
                val vastResource = VastResource(
                    it,
                    VastResource.Type.BLURRED_LAST_FRAME,
                    VastResource.CreativeType.IMAGE,
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.MATCH_PARENT
                )
                vastCompanionAdConfigs.add(
                    VastCompanionAdConfig(
                        RelativeLayout.LayoutParams.MATCH_PARENT,
                        RelativeLayout.LayoutParams.MATCH_PARENT,
                        vastResource,
                        vastVideoConfig.clickThroughUrl,
                        vastVideoConfig.clickTrackers,
                        Collections.emptyList(),
                        vastVideoConfig.customCtaText
                    )
                )
            }
        } else {
            hasCompanionAd = true
        }
        vastIconConfig = vastVideoConfig.vastIconConfig


        clickThroughListener = OnTouchListener { _, event ->
            if (event.action == ACTION_UP &&
                !vastVideoConfig.clickThroughUrl.isNullOrEmpty()
            ) {
                externalViewabilitySessionManager.recordVideoEvent(
                    VideoEvent.AD_CLICK_THRU,
                    getCurrentPosition()
                )
                isClosing = isComplete
                broadcastAction(IntentActions.ACTION_FULLSCREEN_CLICK)
                vastVideoConfig.handleClickForResult(activity,
                    getDuration().takeIf { isComplete } ?: getCurrentPosition(),
                    MOPUB_BROWSER_REQUEST_CODE)
            }
            true
        }

        setLayout(activity.layoutInflater.inflate(R.layout.vast_layout, null) as RelativeLayout)

        // Video view
        videoView = createVideoView(activity, VISIBLE)
        videoView.requestFocus()

        externalViewabilitySessionManager.createVideoSession(
            videoView,
            vastVideoConfig.viewabilityVendors
        )

        // Top transparent gradient strip overlaying top of screen
        val hasCompanionAd = vastCompanionAdConfigs.isNotEmpty()

        topGradientStripWidget =
            (layout.findViewById(R.id.mopub_vast_top_gradient) as VastVideoGradientStripWidget)
                .also {
                    it.setGradientOrientation(GradientDrawable.Orientation.TOP_BOTTOM)
                    it.hasCompanionAd = hasCompanionAd
                    it.setVisibilityForCompanionAd(VISIBLE)
                    it.alwaysVisibleDuringVideo = true
                    externalViewabilitySessionManager.registerVideoObstruction(it,
                        ViewabilityObstruction.OVERLAY)
                    it.updateVisibility()
                }

        // Progress bar overlaying bottom of video view
        progressBarWidget =
            (layout.findViewById(R.id.mopub_vast_progress_bar) as VastVideoProgressBarWidget)
                .also {
                    it.visibility = INVISIBLE
                    externalViewabilitySessionManager.registerVideoObstruction(it, ViewabilityObstruction.PROGRESS_BAR)
                }

        // Bottom transparent gradient strip above progress bar
        bottomGradientStripWidget =
            (layout.findViewById(R.id.mopub_vast_bottom_gradient) as VastVideoGradientStripWidget)
                .also {
                    it.setGradientOrientation(GradientDrawable.Orientation.BOTTOM_TOP)
                    it.hasCompanionAd = hasCompanionAd
                    it.setVisibilityForCompanionAd(GONE)
                    it.alwaysVisibleDuringVideo = false
                    externalViewabilitySessionManager.registerVideoObstruction(it,
                        ViewabilityObstruction.OVERLAY)
                    it.updateVisibility()
                }

        // Radial countdown timer snapped to top-right corner of screen
        radialCountdownWidget =
            (layout.findViewById(R.id.mopub_vast_radial_countdown) as RadialCountdownWidget)
                .also {
                    externalViewabilitySessionManager.registerVideoObstruction(it, ViewabilityObstruction.COUNTDOWN_TIMER)
                    it.visibility = INVISIBLE
                    it.setOnTouchListener { _, _ ->
                        true
                    }
                    it.setOnClickListener{ }
                }

        ctaButtonWidget =
            (layout.findViewById(R.id.mopub_vast_cta_button) as VideoCtaButtonWidget)
                .also {
                    it.setHasCompanionAd(hasCompanionAd)
                    it.setHasClickthroughUrl(!vastVideoConfig.clickThroughUrl.isNullOrEmpty())
                    externalViewabilitySessionManager.registerVideoObstruction(it, ViewabilityObstruction.CTA_BUTTON)
                    vastVideoConfig.customCtaText?.let { ctaText ->
                        it.updateCtaText(ctaText)
                    }
                    it.setOnTouchListener(clickThroughListener)
                }

        closeButtonWidget =
            (layout.findViewById(R.id.mopub_vast_close_button) as VastVideoCloseButtonWidget)
                .also {
                    it.visibility = GONE
                    externalViewabilitySessionManager.registerVideoObstruction(it, ViewabilityObstruction.CLOSE_BUTTON)

                    it.setOnTouchListenerToContent(OnTouchListener { _, event ->
                        if (event.action != ACTION_UP) {
                            return@OnTouchListener true
                        }

                        isClosing = isComplete
                        handleExitTrackers()
                        Handler(Looper.getMainLooper()).post {
                            baseVideoViewControllerListener.onVideoFinish(getCurrentPosition())
                        }
                        return@OnTouchListener true
                    })
                    vastVideoConfig.customSkipText?.let { skipText ->
                        it.updateCloseButtonText(skipText)
                    }
                    vastVideoConfig.customCloseIconUrl?.let { closeIcon ->
                        it.updateCloseButtonIcon(closeIcon, context)
                    }
                }

        val mainHandler = Handler(Looper.getMainLooper())
        progressCheckerRunnable = VastVideoViewProgressRunnable(
            this,
            vastVideoConfig,
            mainHandler
        )
        countdownRunnable = VastVideoViewCountdownRunnable(this, mainHandler)
    }

    /**
     * Set the countdown time and countdown timer delay
     */
    private fun setCountdownTime(endCardType: EndCardType?) {
        val videoDurationMillis = getDuration()

        countdownTimeMillis = CreativeExperiencesFormulae.getCountdownDuration(
            isVast = true,
            isEndCard = false,
            endCardType,
            videoDurationSecs = videoDurationMillis / MILLIS_IN_SECOND,
            elapsedTimeInAdSecs = 0,
            creativeExperienceSettings
        ) * MILLIS_IN_SECOND

        if (countdownTimeMillis > 0) {
            showCountdownTimerDelayMillis =
                creativeExperienceSettings.mainAdConfig.countdownTimerDelaySecs * MILLIS_IN_SECOND

            if (!showCountdownTimer || showCountdownTimerDelayMillis >= countdownTimeMillis) {
                // Countdown timer is never shown
                showCountdownTimerDelayMillis = countdownTimeMillis
                showCountdownTimer = false
            }
        }
    }

    private fun createVideoView(context: Context, initialVisibility: Int): VideoView {
        val tempVideoView = VideoViewFactory.create(context, layout as RelativeLayout?)
        val executor = ContextCompat.getMainExecutor(context)

        val playbackParams = PlaybackParams.Builder()
            .setAudioFallbackMode(PlaybackParams.AUDIO_FALLBACK_MODE_DEFAULT)
            .setSpeed(1.0f)
            .build()
        mediaPlayer.playbackParams = playbackParams
        val audioAttrs = AudioAttributesCompat.Builder()
            .setUsage(AudioAttributesCompat.USAGE_MEDIA)
            .setContentType(AudioAttributesCompat.CONTENT_TYPE_MOVIE)
            .build()
        mediaPlayer.setAudioAttributes(audioAttrs)
        mediaPlayer.registerPlayerCallback(executor, playerCallback)
        tempVideoView.removeView(tempVideoView.mediaControlView)
        tempVideoView.setPlayer(mediaPlayer)
        tempVideoView.setOnTouchListener(clickThroughListener)

        mediaPlayer.run {
            setMediaItem(
                UriMediaItem.Builder(Uri.parse(vastVideoConfig.diskMediaFileUrl)).build()
            )
            prepare().addListener(
                Runnable {
                    // Called when media source is ready for playback
                    // The VideoView duration defaults to -1 when the video is not prepared or playing;
                    // Therefore set it here so that we have access to it at all times
                    externalViewabilitySessionManager.onVideoPrepared(duration)
                    mediaPlayer.playerVolume = 1.0f

                    // Countdown time calculations can only happen once the companion ad is selected
                    val selectedVastCompanionAd = selectVastCompanionAd()
                    setCountdownTime(
                        EndCardType.fromVastResourceType(selectedVastCompanionAd?.vastResource?.type)
                    )

                    progressBarWidget.calibrateAndMakeVisible(
                        duration.toInt(),
                        countdownTimeMillis
                    )
                    radialCountdownWidget.calibrate(countdownTimeMillis)
                    radialCountdownWidget.updateCountdownProgress(
                        countdownTimeMillis,
                        currentPosition.toInt()
                    )
                    isCalibrationDone = true

                    baseVideoViewControllerListener.onCompanionAdReady(
                        selectedVastCompanionAd,
                        duration.toInt()
                    )
                },
                executor
            )
        }

        return tempVideoView
    }

    private fun selectVastCompanionAd(): VastCompanionAdConfig? {
        val displayMetrics: DisplayMetrics = activity.resources.displayMetrics
        val widthPixels = displayMetrics.widthPixels
        val heightPixels = displayMetrics.heightPixels
        val widthDp = (widthPixels / displayMetrics.density).toInt()
        val heightDp = (heightPixels / displayMetrics.density).toInt()
        var bestCompanionAdConfig: VastCompanionAdConfig? = null
        for (vastCompanionAdConfig in vastCompanionAdConfigs) {
            if (bestCompanionAdConfig == null ||
                vastCompanionAdConfig.calculateScore(widthDp, heightDp) >
                bestCompanionAdConfig.calculateScore(widthDp, heightDp)
            ) {
                bestCompanionAdConfig = vastCompanionAdConfig
            }
        }
        return bestCompanionAdConfig
    }

    override fun onCreate() {
        super.onCreate()
        vastVideoConfig.handleImpression(context, getCurrentPosition())
    }

    override fun onResume() {
        if (!externalViewabilitySessionManager.isTracking()) {
            externalViewabilitySessionManager.startSession();
        }
        startRunnables()

        if (seekerPositionOnPause > 0) {
            mediaPlayer.seekTo(seekerPositionOnPause.toLong(), MediaPlayer.SEEK_CLOSEST)
        } else {
            if (!isComplete) {
                mediaPlayer.play()
            }
        }

        if (seekerPositionOnPause != SEEKER_POSITION_NOT_INITIALIZED && !isComplete) {
            vastVideoConfig.handleResume(context, seekerPositionOnPause)
        }
    }

    @SuppressLint("RestrictedApi", "VisibleForTests")
    override fun onPause() {
        stopRunnables()
        seekerPositionOnPause = getCurrentPosition()
        val pauseFuture = mediaPlayer.pause()

        // To address a bug where a video may resume after a transient audio loss, we close the
        // focus handler here in onPause() as we manage play and pause manually
        val pauseRunnable = Runnable {
            try {
                val audioFocusHandlerField =
                    MediaPlayer::class.java.getDeclaredField("mAudioFocusHandler")
                audioFocusHandlerField.isAccessible = true
                val audioFocusHandler = audioFocusHandlerField.get(mediaPlayer)

                val audioFocusHandlerCloseMethod =
                    audioFocusHandler.javaClass.getMethod("close")
                audioFocusHandlerCloseMethod.invoke(audioFocusHandler)
            } catch (e: Exception) {
                MoPubLog.log(
                    CUSTOM_WITH_THROWABLE,
                    "Unable to call close() on the AudioFocusHandler due to an exception.",
                    e
                )
            }
        }
        try {
            val executorField = MediaPlayer::class.java.getDeclaredField("mExecutor")
            executorField.isAccessible = true
            val executor = executorField.get(mediaPlayer) as ExecutorService
            pauseFuture.addListener(pauseRunnable, executor)
        } catch (e: Exception) {
            MoPubLog.log(
                CUSTOM_WITH_THROWABLE,
                "Unable to get the executor from mediaPlayer due to an exception.",
                e
            )
        }

        if (!isComplete) {
            vastVideoConfig.handlePause(context, seekerPositionOnPause)
        }
    }

    override fun onDestroy() {
        stopRunnables()
        externalViewabilitySessionManager.endSession()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(CURRENT_POSITION, seekerPositionOnPause)
        outState.putSerializable(RESUMED_VAST_CONFIG, vastVideoConfig)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        // Deliberate no-op
    }

    override fun getVideoView(): View {
        return videoView
    }

    override fun onBackPressed() {
        handleExitTrackers()
    }

    // Enable the device's back button when the video close button has been displayed
    override fun backButtonEnabled(): Boolean {
        return shouldAllowClose
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (isClosing
            && requestCode == MOPUB_BROWSER_REQUEST_CODE
            && resultCode == Activity.RESULT_OK
        ) {
            baseVideoViewControllerListener.onVideoFinish(getCurrentPosition())
        }
    }

    fun handleViewabilityQuartileEvent(enumValue: String) {
        VideoEvent.valueOf(enumValue)?.let {
            externalViewabilitySessionManager.recordVideoEvent(it, getCurrentPosition())
        }
    }

    fun getDuration(): Int {
        return mediaPlayer.duration.toInt()
    }

    fun getCurrentPosition(): Int {
        return mediaPlayer.currentPosition.toInt()
    }

    fun updateCountdown(forceCloseable: Boolean = false) {
        if (isCalibrationDone) {
            radialCountdownWidget.updateCountdownProgress(
                countdownTimeMillis,
                getCurrentPosition()
            )

            // Make the countdown timer visible if the show countdown timer delay has passed
            if (showCountdownTimer
                && !radialCountdownWidget.isVisible
                && getCurrentPosition() >= showCountdownTimerDelayMillis
            ) {
                radialCountdownWidget.visibility = VISIBLE
            }
        }

        if (forceCloseable
            || (isCalibrationDone && (getCurrentPosition() >= countdownTimeMillis))
        ) {
            radialCountdownWidget.visibility = GONE
            closeButtonWidget.visibility = VISIBLE
            shouldAllowClose = true
        }
    }

    fun updateProgressBar() {
        progressBarWidget.updateProgress(getCurrentPosition())
    }

    /**
     * Displays and impresses the icon if the current position of the video is greater than the
     * offset of the icon. Once the current position is greater than the offset plus duration, the
     * icon is then hidden again. We intentionally do not preload the icon.
     *
     * @param currentPosition the current position of the video in milliseconds.
     */
    fun handleIconDisplay(currentPosition: Int) {
        val offsetMs = vastIconConfig?.offsetMS ?: return
        if (currentPosition < offsetMs) {
            return
        }

        if (!::iconView.isInitialized) {
            iconView = vastIconConfig?.let { iconConfig ->
                VastWebView.createView(context, iconConfig.vastResource).also {
                    it.vastWebViewClickListener = VastWebView.VastWebViewClickListener {
                        makeVastTrackingHttpRequest(
                            iconConfig.clickTrackingUris,
                            null,
                            getCurrentPosition(),
                            networkMediaFileUrl,
                            context
                        )
                        vastIconConfig?.handleClick(context, null, vastVideoConfig.dspCreativeId)
                    }
                    it.webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            url: String
                        ): Boolean {
                            vastIconConfig?.handleClick(
                                context,
                                url,
                                vastVideoConfig.dspCreativeId
                            )
                            return true
                        }

                        @RequiresApi(Build.VERSION_CODES.O)
                        override fun onRenderProcessGone(
                            view: WebView?,
                            detail: RenderProcessGoneDetail?
                        ): Boolean {
                            MoPubLog.log(
                                CUSTOM,
                                "onRenderProcessGone() called from the IconView. Ignoring the icon."
                            )
                            vastVideoConfig.handleError(
                                context,
                                VastErrorCode.UNDEFINED_ERROR,
                                getCurrentPosition()
                            )
                            return true
                        }
                    }
                    val layoutParams = vastIconConfig?.let {
                        RelativeLayout.LayoutParams(
                            Dips.asIntPixels(iconConfig.width.toFloat(), context),
                            Dips.asIntPixels(iconConfig.height.toFloat(), context)
                        )
                    }
                    val leftMargin = Dips.dipsToIntPixels(LEFT_MARGIN_DIPS.toFloat(), context)
                    val topMargin = Dips.dipsToIntPixels(TOP_MARGIN_DIPS.toFloat(), context)
                    layoutParams?.setMargins(leftMargin, topMargin, 0, 0)

                    layout.addView(it, layoutParams)
                    externalViewabilitySessionManager.registerVideoObstruction(
                        it,
                        ViewabilityObstruction.INDUSTRY_ICON
                    )
                }
            } ?: View(context)
            iconView.visibility = VISIBLE
        }

        networkMediaFileUrl?.let {
            vastIconConfig?.handleImpression(context, currentPosition, it)
        }

        val durationMS = vastIconConfig?.durationMS ?: return

        if (currentPosition >= offsetMs + durationMS && ::iconView.isInitialized) {
            iconView.visibility = GONE
        }
    }

    fun handleExitTrackers() {
        val currentPosition: Int = getCurrentPosition()
        if (isComplete || currentPosition >= getDuration()) {
            vastVideoConfig.handleComplete(context, getDuration())
        } else {
            externalViewabilitySessionManager.recordVideoEvent(
                VideoEvent.AD_SKIPPED,
                currentPosition
            )
            vastVideoConfig.handleSkip(context, currentPosition)
        }

        vastVideoConfig.handleClose(context, getDuration())
    }

    private fun startRunnables() {
        progressCheckerRunnable.startRepeating(VIDEO_PROGRESS_TIMER_CHECKER_DELAY)
        countdownRunnable.startRepeating(VIDEO_COUNTDOWN_UPDATE_INTERVAL)
    }

    private fun stopRunnables() {
        progressCheckerRunnable.stop()
        countdownRunnable.stop()
    }

    inner class PlayerCallback : MediaPlayer.PlayerCallback() {
        var complete = false
        override fun onPlayerStateChanged(player: SessionPlayer, playerState: Int) {
            super.onPlayerStateChanged(player, playerState)

            when (playerState) {
                PLAYER_STATE_ERROR -> {
                    externalViewabilitySessionManager.recordVideoEvent(
                        VideoEvent.RECORD_AD_ERROR,
                        getCurrentPosition()
                    )
                    stopRunnables()
                    updateCountdown(true)
                    videoError(false)
                    videoError = true
                    vastVideoConfig.handleError(
                        context,
                        VastErrorCode.GENERAL_LINEAR_AD_ERROR, getCurrentPosition()
                    )
                }
                PLAYER_STATE_PAUSED -> {
                    if(externalViewabilitySessionManager.hasImpressionOccurred() ) {
                        externalViewabilitySessionManager.recordVideoEvent(
                            VideoEvent.AD_PAUSED,
                            getCurrentPosition()
                        )
                    }
                }
                PLAYER_STATE_PLAYING -> {
                    if (externalViewabilitySessionManager.hasImpressionOccurred()) {
                        externalViewabilitySessionManager.recordVideoEvent(
                            VideoEvent.AD_RESUMED,
                            getCurrentPosition()
                        )
                    } else {
                        externalViewabilitySessionManager.trackImpression()
                    }
                }
                else -> {
                    MoPubLog.log(
                        CUSTOM,
                        "Player state changed to ${playerStateToString(playerState)}"
                    )
                }
            }
        }

        override fun onPlaybackCompleted(player: SessionPlayer) {
            stopRunnables()
            updateCountdown()
            isComplete = true

            if (!videoError && vastVideoConfig.remainingProgressTrackerCount == 0) {
                externalViewabilitySessionManager.recordVideoEvent(
                    VideoEvent.AD_COMPLETE,
                    getCurrentPosition()
                )
                vastVideoConfig.handleComplete(context, getCurrentPosition())
            }

            videoView.visibility = INVISIBLE
            progressBarWidget.visibility = GONE
            if (::iconView.isInitialized) {
                iconView.visibility = GONE
            }

            topGradientStripWidget.notifyVideoComplete()
            bottomGradientStripWidget.notifyVideoComplete()
            ctaButtonWidget.notifyVideoComplete()
            closeButtonWidget.notifyVideoComplete()

            // Show companion ad or blurred last frame
            videoCompleted(true, getDuration())
        }

        override fun onSeekCompleted(player: SessionPlayer, position: Long) {
            mediaPlayer.play()
        }

        private fun playerStateToString(state: Int): String {
            return when (state) {
                PLAYER_STATE_IDLE -> "PLAYER_STATE_IDLE"
                PLAYER_STATE_PAUSED -> "PLAYER_STATE_PAUSED"
                PLAYER_STATE_PLAYING -> "PLAYER_STATE_PLAYING"
                PLAYER_STATE_ERROR -> "PLAYER_STATE_ERROR"
                else -> "UNKNOWN"
            }
        }
    }

    fun setVastCompanionAdConfigsForTesting(companionAdConfigs: List<VastCompanionAdConfig> ) {
        vastCompanionAdConfigs = companionAdConfigs.toMutableSet()
    }
}
