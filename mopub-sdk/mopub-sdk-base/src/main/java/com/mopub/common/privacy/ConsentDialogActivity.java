// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common.privacy;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.view.Window;
import android.view.WindowManager;

import com.mopub.common.MoPub;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.Intents;
import com.mopub.exceptions.IntentNotResolvableException;
import com.mopub.mobileads.MoPubErrorCode;

import static com.mopub.common.logging.MoPubLog.ConsentLogEvent.SHOW_FAILED;
import static com.mopub.common.logging.MoPubLog.ConsentLogEvent.SHOW_SUCCESS;
import static com.mopub.common.logging.MoPubLog.SdkLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.SdkLogEvent.CUSTOM_WITH_THROWABLE;

public class ConsentDialogActivity extends Activity {
    private static final int CLOSE_BUTTON_DELAY_MS = 10000;
    private static final String KEY_HTML_PAGE = "html-page-content";

    @Nullable
    ConsentDialogLayout mView;
    @Nullable
    private Runnable mEnableCloseButtonRunnable;
    @Nullable
    Handler mCloseButtonHandler;

    @Nullable
    ConsentStatus mConsentStatus;

    static void start(@NonNull final Context context, @NonNull String htmlData) {
        Preconditions.checkNotNull(context);

        if (TextUtils.isEmpty(htmlData)) {
            MoPubLog.log(CUSTOM, "ConsentDialogActivity htmlData can't be empty string.");
            MoPubLog.log(SHOW_FAILED, MoPubErrorCode.INTERNAL_ERROR.getIntCode(),
                    MoPubErrorCode.INTERNAL_ERROR);
            return;
        }

        Intent intent = createIntent(context, htmlData);
        try {
            Intents.startActivity(context, intent);
        } catch (ActivityNotFoundException | IntentNotResolvableException e) {
            MoPubLog.log(CUSTOM, "ConsentDialogActivity not found - did you declare it in AndroidManifest.xml?");
            MoPubLog.log(SHOW_FAILED, MoPubErrorCode.INTERNAL_ERROR.getIntCode(),
                    MoPubErrorCode.INTERNAL_ERROR);
        }
    }

    @NonNull
    static Intent createIntent(@NonNull final Context context, @NonNull final String htmlPageContent) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(htmlPageContent);

        Bundle extra = new Bundle();
        extra.putString(KEY_HTML_PAGE, htmlPageContent);
        return Intents.getStartActivityIntent(context, ConsentDialogActivity.class, extra);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        String htmlBody = intent.getStringExtra(KEY_HTML_PAGE);
        if (TextUtils.isEmpty(htmlBody)) {
            MoPubLog.log(CUSTOM, "Web page for ConsentDialogActivity is empty");
            MoPubLog.log(SHOW_FAILED, MoPubErrorCode.INTERNAL_ERROR.getIntCode(),
                    MoPubErrorCode.INTERNAL_ERROR);
            finish();
            return;
        }

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        try {
            mView = new ConsentDialogLayout(this);
        } catch (RuntimeException e) {
            // Notably, android.webkit.WebViewFactory$MissingWebViewPackageException
            MoPubLog.log(CUSTOM_WITH_THROWABLE, "Unable to create WebView", e);
            MoPubLog.log(SHOW_FAILED, MoPubErrorCode.INTERNAL_ERROR.getIntCode(),
                    MoPubErrorCode.INTERNAL_ERROR);
            finish();
            return;
        }

        mView.setConsentClickListener(new ConsentDialogLayout.ConsentListener() {
            @Override
            public void onConsentClick(ConsentStatus status) {
                saveConsentStatus(status);
                setCloseButtonVisibility(false);
            }

            @Override
            public void onCloseClick() {
                finish();
            }
        });

        mEnableCloseButtonRunnable = new Runnable() {
            @Override
            public void run() {
                setCloseButtonVisibility(true);
            }
        };

        setContentView(mView);

        mView.startLoading(htmlBody, new ConsentDialogLayout.PageLoadListener() {
            @Override
            public void onLoadProgress(int progress) {
                if (progress == ConsentDialogLayout.FINISHED_LOADING) {
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        mCloseButtonHandler = new Handler();
        mCloseButtonHandler.postDelayed(mEnableCloseButtonRunnable, CLOSE_BUTTON_DELAY_MS);
    }

    @Override
    protected void onResume() {
        super.onResume();
        MoPubLog.log(SHOW_SUCCESS);
    }

    @Override
    protected void onStop() {
        super.onStop();
        setCloseButtonVisibility(true);
    }

    @Override
    protected void onDestroy() {
        final PersonalInfoManager infoManager = MoPub.getPersonalInformationManager();
        if (infoManager != null && mConsentStatus != null) {
            infoManager.changeConsentStateFromDialog(mConsentStatus);
        }
        super.onDestroy();
    }


    void setCloseButtonVisibility(boolean visible) {
        if (mCloseButtonHandler != null) {
            mCloseButtonHandler.removeCallbacks(mEnableCloseButtonRunnable);
        }
        if (mView != null) {
            mView.setCloseVisible(visible);
        }
    }

    private void saveConsentStatus(@NonNull final ConsentStatus status) {
        Preconditions.checkNotNull(status);
        mConsentStatus = status;
    }
}
