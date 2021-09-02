// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.mopub.common.ExternalViewabilitySessionManager;
import com.mopub.common.ViewabilityManager;
import com.mopub.common.ViewabilityObstruction;
import com.mopub.common.logging.MoPubLog;

import static com.mopub.common.logging.MoPubLog.SdkLogEvent.CUSTOM;

public class BaseWebViewViewability extends BaseWebView {
    protected enum State {INIT, STARTED, IMPRESSED, STOPPED}

    protected State state = State.INIT;

    @NonNull
    protected ExternalViewabilitySessionManager externalViewabilityTracker;
    protected boolean viewabilityEnabled;
    protected boolean automaticImpression = true;
    protected boolean pageLoaded = false;
    protected boolean pageVisible = false;

    public BaseWebViewViewability(Context context) {
        super(context);
        viewabilityEnabled = ViewabilityManager.isViewabilityEnabled();
        externalViewabilityTracker = ExternalViewabilitySessionManager.create();
        if(viewabilityEnabled){
            delayDestroy = true;
        }
        log("BaseWebViewViewability() " + this);
    }

    private void changeState(@NonNull final State newState) {
        if (!viewabilityEnabled) {
            return;
        }

        boolean modified = false;
        switch (newState) {
            case STARTED:
                if (state == State.INIT && pageLoaded) {
                    externalViewabilityTracker.createWebViewSession(this);
                    externalViewabilityTracker.startSession();
                    modified = true;
                }
                break;
            case IMPRESSED:
                if (state == State.STARTED && pageVisible) {
                    externalViewabilityTracker.trackImpression();
                    modified = true;
                }
                break;
            case STOPPED:
                if (state != State.INIT && state != State.STOPPED) {
                    externalViewabilityTracker.endSession();
                    modified = true;
                }
        }

        if (modified) {
            state = newState;
        } else {
            log("Skip state transition " + state + " to " + newState);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        log("onAttachedToWindow() " + this);
        if(pageLoaded) {
            changeState(State.STARTED);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        changeState(State.STOPPED);
        super.onDetachedFromWindow();
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        log("onVisibilityChanged: " + visibility + " " + this);
        pageVisible = visibility == VISIBLE;
        if (automaticImpression) {
            changeState(State.IMPRESSED);
        }
    }

    public void disableTracking() {
        viewabilityEnabled = false;
    }

    public void enableTracking() {
        viewabilityEnabled = true;
    }
    public void disableAutomaticImpression() {
        automaticImpression = false;
    }

    public void setPageLoaded() {
        log("setPageLoaded() "  + this);
        pageLoaded = true;
        changeState(State.STARTED);
        if (automaticImpression) {
            changeState(State.IMPRESSED);
        }
    }

    public void notifyImpression() {
        changeState(State.IMPRESSED);
    }

    void registerFriendlyObstruction(@NonNull final View view, @NonNull final ViewabilityObstruction purpose) {
        if (viewabilityEnabled) {
            externalViewabilityTracker.registerFriendlyObstruction(view, purpose);
        }
    }

    @VisibleForTesting
    void setMockExternalTracker(@NonNull final ExternalViewabilitySessionManager mockExternalTracker) {
        externalViewabilityTracker = mockExternalTracker;
    }

    private void log(@NonNull final String message) {
        if (ViewabilityManager.isViewabilityEnabled()) {
            MoPubLog.log(CUSTOM, "OMSDK " + message);
        }
    }
}
