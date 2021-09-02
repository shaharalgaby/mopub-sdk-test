// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

import com.mopub.common.logging.MoPubLog;
import com.mopub.common.privacy.PersonalInfoManager;
import com.mopub.common.util.Reflection;
import com.mopub.network.Networking;
import com.mopub.network.PlayServicesUrlRewriter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import static com.mopub.common.ExternalViewabilitySessionManager.ViewabilityVendor;
import static com.mopub.common.logging.MoPubLog.SdkLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.SdkLogEvent.ERROR_WITH_THROWABLE;
import static com.mopub.common.logging.MoPubLog.SdkLogEvent.INIT_FINISHED;
import static com.mopub.common.logging.MoPubLog.SdkLogEvent.INIT_STARTED;

public class MoPub {
    public static final String SDK_VERSION = "5.18.0";

    public enum LocationAwareness { NORMAL, TRUNCATED, DISABLED }

    /**
     * Browser agent to handle URIs.
     * @deprecated in favor of {@link BrowserAgentManager.BrowserAgent}.
     */
    @Deprecated
    public enum BrowserAgent {
        /**
         * MoPub's in-app browser
         */
        IN_APP,

        /**
         * Default browser application on device
         */
        NATIVE;

        /**
         * Maps header value from MoPub's AdServer to browser agent:
         * 0 is MoPub's in-app browser (IN_APP), and 1 is device's default browser (NATIVE).
         * For null or all other undefined values, returns default browser agent IN_APP.
         * @param browserAgent Integer header value from MoPub's AdServer.
         * @return IN_APP for 0, NATIVE for 1, and IN_APP for null or all other undefined values.
         *
         * @deprecated Use {@link BrowserAgentManager.BrowserAgent#fromHeader(Integer)} instead.
         */
        @Deprecated
        @NonNull
        public static BrowserAgent fromHeader(@Nullable final Integer browserAgent) {
            if (browserAgent == null) {
                return IN_APP;
            }

            return browserAgent == 1 ? NATIVE : IN_APP;
        }

        @Deprecated
        @NonNull BrowserAgentManager.BrowserAgent toBrowserAgentFromManager() {
            return (this == BrowserAgent.IN_APP) ? BrowserAgentManager.BrowserAgent.IN_APP :
                    BrowserAgentManager.BrowserAgent.NATIVE;
        }
    }

    private static final String MOPUB_REWARDED_ADS =
            "com.mopub.mobileads.MoPubRewardedAds";
    private static final String MOPUB_REWARDED_AD_MANAGER =
            "com.mopub.mobileads.MoPubRewardedAdManager";

    private static boolean sSearchedForUpdateActivityMethod = false;
    @Nullable private static Method sUpdateActivityMethod;
    private static boolean sSdkInitialized = false;
    private static boolean sSdkInitializing = false;
    private static AdapterConfigurationManager sAdapterConfigurationManager;
    private static PersonalInfoManager sPersonalInfoManager;

    @NonNull
    public static LocationAwareness getLocationAwareness() {

        return LocationService.getInstance().getLocationAwareness();
    }

    public static void setLocationAwareness(@NonNull final LocationAwareness locationAwareness) {
        Preconditions.checkNotNull(locationAwareness);

        LocationService.getInstance().setLocationAwareness(locationAwareness);
    }

    public static int getLocationPrecision() {
        return LocationService.getInstance().getLocationPrecision();
    }

    /**
     * Sets the precision to use when the SDK's location awareness is set
     * to {@link com.mopub.common.MoPub.LocationAwareness#TRUNCATED}.
     */
    public static void setLocationPrecision(int precision) {
        LocationService.getInstance().setLocationPrecision(precision);
    }

    public static void setMinimumLocationRefreshTimeMillis(
            final long minimumLocationRefreshTimeMillis) {
        LocationService.getInstance().setMinimumLocationRefreshTimeMillis(minimumLocationRefreshTimeMillis);
    }

    public static long getMinimumLocationRefreshTimeMillis() {
        return LocationService.getInstance().getMinimumLocationRefreshTimeMillis();
    }

    /**
     * @deprecated Use {@link BrowserAgentManager#setBrowserAgent(BrowserAgentManager.BrowserAgent)} instead.
     */
    @Deprecated
    public static void setBrowserAgent(@NonNull final BrowserAgent browserAgent) {
        Preconditions.checkNotNull(browserAgent);

        BrowserAgentManager.setBrowserAgent(browserAgent.toBrowserAgentFromManager());
    }

    /**
     * @deprecated Use {@link BrowserAgentManager#setBrowserAgentFromAdServer(BrowserAgentManager.BrowserAgent)}
     * instead.
     */
    @Deprecated
    public static void setBrowserAgentFromAdServer(@NonNull final BrowserAgent adServerBrowserAgent) {
        Preconditions.checkNotNull(adServerBrowserAgent);

        BrowserAgentManager.setBrowserAgentFromAdServer(adServerBrowserAgent.toBrowserAgentFromManager());
    }

    /**
     * @deprecated Use {@link BrowserAgentManager#getBrowserAgent()} instead.
     */
    @Deprecated
    @NonNull
    public static BrowserAgent getBrowserAgent() {
        return (BrowserAgentManager.getBrowserAgent() == BrowserAgentManager.BrowserAgent.IN_APP) ?
                BrowserAgent.IN_APP : BrowserAgent.NATIVE;
    }

    /**
     * Set optional application engine information, for example {'unity', "123"}
     *
     * @param engineInfo {@link com.mopub.common.AppEngineInfo}
     */
    public static void setEngineInformation(@NonNull final AppEngineInfo engineInfo) {
        Preconditions.checkNotNull(engineInfo);

        if (!TextUtils.isEmpty(engineInfo.mName) && !TextUtils.isEmpty(engineInfo.mVersion)) {
            BaseUrlGenerator.setAppEngineInfo(engineInfo);
        }
    }

    /**
     * Sets the wrapper version. This is not meant for publisher use.
     *
     * @param wrapperVersion The wrapper version number.
     */
    public static void setWrapperVersion(@NonNull final String wrapperVersion) {
        Preconditions.checkNotNull(wrapperVersion);

        BaseUrlGenerator.setWrapperVersion(wrapperVersion);
    }

    /**
     * Initializes the MoPub SDK. Call this before making any ad requests. This will do the
     * rewarded base ads initialization any number of times, but the SDK itself can only be
     * initialized once, and the rewarded subsystem can only be initialized once.
     *
     * @param context                   Recommended to be an activity context.
     *                                  Rewarded ads initialization requires an Activity.
     * @param sdkConfiguration          Configuration data to initialize the SDK.
     * @param sdkInitializationListener Callback for when SDK initialization finishes.
     */
    public static void initializeSdk(@NonNull final Context context,
                                     @NonNull final SdkConfiguration sdkConfiguration,
                                     @Nullable final SdkInitializationListener sdkInitializationListener) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(sdkConfiguration);

        MoPubLog.setLogLevel(sdkConfiguration.getLogLevel());

        MoPubLog.log(INIT_STARTED);
        MoPubLog.log(CUSTOM, "SDK initialize has been called with ad unit: " + sdkConfiguration.getAdUnitId());
        final ApplicationInfo appInfo = context.getApplicationInfo();
        if (appInfo != null) {
            MoPubLog.log(CUSTOM, context.getPackageName() +
                    " was built with target SDK version of " + appInfo.targetSdkVersion);
        }

        ViewabilityManager.activate(context.getApplicationContext());

        if (context instanceof Activity) {
            final Activity activity = (Activity) context;
            initializeRewardedAd(activity, sdkConfiguration);
        }

        if (sSdkInitialized) {
            MoPubLog.log(CUSTOM, "MoPub SDK is already initialized");
            initializationFinished(sdkInitializationListener);
            return;
        }
        if (sSdkInitializing) {
            MoPubLog.log(CUSTOM, "MoPub SDK is currently initializing.");
            return;
        }

        if (Looper.getMainLooper() != Looper.myLooper()) {
            MoPubLog.log(CUSTOM, "MoPub can only be initialized on the main thread.");
            return;
        }

        sSdkInitializing = true;

        // Guarantees initialization of the request queue on the main thread.
        Networking.setUrlRewriter(new PlayServicesUrlRewriter());
        Networking.getRequestQueue(context);

        final InternalSdkInitializationListener internalSdkInitializationListener =
                new InternalSdkInitializationListener(sdkInitializationListener);

        final SdkInitializationListener compositeSdkInitializationListener =
                new CompositeSdkInitializationListener(internalSdkInitializationListener, 2);

        sPersonalInfoManager = new PersonalInfoManager(context, sdkConfiguration.getAdUnitId(),
                compositeSdkInitializationListener);
        sPersonalInfoManager.setAllowLegitimateInterest(sdkConfiguration.getLegitimateInterestAllowed());

        ClientMetadata.getInstance(context);

        sAdapterConfigurationManager = new AdapterConfigurationManager(compositeSdkInitializationListener);
        sAdapterConfigurationManager.initialize(context,
                sdkConfiguration.getAdapterConfigurationClasses(),
                sdkConfiguration.getMediatedNetworkConfigurations(),
                sdkConfiguration.getMoPubRequestOptions());
    }

    /**
     * @return true if SDK is initialized.
     */
    public static boolean isSdkInitialized() {
        return sSdkInitialized;
    }

    /**
     * Check this to see if you are allowed to collect personal user data.
     *
     * @return True if allowed, false otherwise.
     */
    public static boolean canCollectPersonalInformation() {
        return sPersonalInfoManager != null && sPersonalInfoManager.canCollectPersonalInformation();
    }

    /**
     * Set the allowance of legitimate interest.
     * This API can be used if you want to allow supported SDK networks to collect user information on the basis of legitimate interest.
     *
     * @param allowed should be true if legitimate interest is allowed. False if it isn't allowed.
     */
    public static void setAllowLegitimateInterest(final boolean allowed) {
        if (sPersonalInfoManager != null) {
            sPersonalInfoManager.setAllowLegitimateInterest(allowed);
        }
    }

    /**
     * Check this to see if legitimate interest is allowed.
     *
     * @return True if allowed, false otherwise.
     */
    public static boolean shouldAllowLegitimateInterest() {
        return sPersonalInfoManager != null && sPersonalInfoManager.shouldAllowLegitimateInterest();
    }

    @Nullable
    static String getAdvancedBiddingTokensJson(@NonNull final Context context) {
        Preconditions.checkNotNull(context);

        if (sAdapterConfigurationManager == null) {
            return null;
        }
        return sAdapterConfigurationManager.getTokensAsJsonString(context);
    }

    /**
     * Gets the consent manager for handling user data.
     *
     * @return A PersonalInfoManager that handles consent management.
     */
    @Nullable
    public static PersonalInfoManager getPersonalInformationManager() {
        return sPersonalInfoManager;
    }

    //////// MoPub LifecycleListener messages ////////

    public static void onCreate(@NonNull final Activity activity) {
        MoPubLifecycleManager.getInstance(activity).onCreate(activity);
        updateActivity(activity);
    }

    public static void onStart(@NonNull final Activity activity) {
        MoPubLifecycleManager.getInstance(activity).onStart(activity);
        updateActivity(activity);
    }

    public static void onPause(@NonNull final Activity activity) {
        MoPubLifecycleManager.getInstance(activity).onPause(activity);
    }

    public static void onResume(@NonNull final Activity activity) {
        MoPubLifecycleManager.getInstance(activity).onResume(activity);
        updateActivity(activity);
    }

    public static void onRestart(@NonNull final Activity activity) {
        MoPubLifecycleManager.getInstance(activity).onRestart(activity);
        updateActivity(activity);
    }

    public static void onStop(@NonNull final Activity activity) {
        MoPubLifecycleManager.getInstance(activity).onStop(activity);
    }

    public static void onDestroy(@NonNull final Activity activity) {
        MoPubLifecycleManager.getInstance(activity).onDestroy(activity);
    }

    public static void onBackPressed(@NonNull final Activity activity) {
        MoPubLifecycleManager.getInstance(activity).onBackPressed(activity);
    }

    @UiThread
    public static void disableViewability() {
        ViewabilityManager.disableViewability();
    }

    /**
     * @deprecated as of 5.14.0. Use {@link #disableViewability()}
     */
    @Deprecated
    public static void disableViewability(@NonNull final ViewabilityVendor vendor) {
        ViewabilityManager.disableViewability();
    }

    @Nullable
    public static List<String> getAdapterConfigurationInfo() {
        final AdapterConfigurationManager configurationManager = sAdapterConfigurationManager;
        if (configurationManager != null) {
            return configurationManager.getAdapterConfigurationInfo();
        }
        return null;
    }

    private static void initializeRewardedAd(@NonNull Activity activity, @NonNull SdkConfiguration sdkConfiguration) {
        Preconditions.checkNotNull(activity);
        Preconditions.checkNotNull(sdkConfiguration);

        try {
            new Reflection.MethodBuilder(null, "initializeRewardedAds")
                    .setStatic(Class.forName(MOPUB_REWARDED_ADS))
                    .setAccessible()
                    .addParam(Activity.class, activity)
                    .addParam(SdkConfiguration.class, sdkConfiguration).execute();
        } catch (ClassNotFoundException e) {
            MoPubLog.log(CUSTOM, "initializeRewardedAds was called without the fullscreen module");
        } catch (NoSuchMethodException e) {
            MoPubLog.log(CUSTOM, "initializeRewardedAds was called without the fullscreen module");
        } catch (Exception e) {
            MoPubLog.log(ERROR_WITH_THROWABLE, "Error while initializing rewarded ads", e);
        }
    }

    private static void initializationFinished(@Nullable final SdkInitializationListener sdkInitializationListener) {
        sSdkInitializing = false;
        sSdkInitialized = true;
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (sdkInitializationListener != null) {
                    sdkInitializationListener.onInitializationFinished();
                }
            }
        });
    }

    private static class InternalSdkInitializationListener implements SdkInitializationListener {
        @Nullable
        private SdkInitializationListener mSdkInitializationListener;

        InternalSdkInitializationListener(@Nullable SdkInitializationListener sdkInitializationListener) {
            mSdkInitializationListener = sdkInitializationListener;
        }

        @Override
        public void onInitializationFinished() {
            final AdapterConfigurationManager adapterConfigurationManager = sAdapterConfigurationManager;
            if (adapterConfigurationManager != null) {
                MoPubLog.log(INIT_FINISHED, adapterConfigurationManager.getAdapterConfigurationInfo());
            }
            initializationFinished(mSdkInitializationListener);
            mSdkInitializationListener = null;
        }
    }

    @VisibleForTesting
    static void updateActivity(@NonNull Activity activity) {
        if (!sSearchedForUpdateActivityMethod) {
            sSearchedForUpdateActivityMethod = true;
            try {
                Class moPubRewardedAdManagerClass = Class.forName(
                        MOPUB_REWARDED_AD_MANAGER);
                sUpdateActivityMethod = Reflection.getDeclaredMethodWithTraversal(
                        moPubRewardedAdManagerClass, "updateActivity", Activity.class);
            } catch (ClassNotFoundException e) {
                // fullscreen module not included
            } catch (NoSuchMethodException e) {
                // fullscreen module not included
            }
        }

        if (sUpdateActivityMethod != null) {
            try {
                sUpdateActivityMethod.invoke(null, activity);
            } catch (IllegalAccessException e) {
                MoPubLog.log(ERROR_WITH_THROWABLE, "Error while attempting to access the update " +
                        "activity method - this should not have happened", e);
            } catch (InvocationTargetException e) {
                MoPubLog.log(ERROR_WITH_THROWABLE, "Error while attempting to access the update " +
                        "activity method - this should not have happened", e);
            }
        }
    }

    @Deprecated
    @VisibleForTesting
    static void resetMoPub() {
        sAdapterConfigurationManager = null;
        sPersonalInfoManager = null;
        sSdkInitialized = false;
        sSdkInitializing = false;
    }

    @Deprecated
    @VisibleForTesting
    static void setPersonalInfoManager(@Nullable final PersonalInfoManager personalInfoManager) {
        sPersonalInfoManager = personalInfoManager;
    }
}
