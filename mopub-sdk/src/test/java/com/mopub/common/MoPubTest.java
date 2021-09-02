// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.privacy.SyncRequest;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.common.util.AsyncTasks;
import com.mopub.common.util.Reflection;
import com.mopub.mobileads.MoPubRewardedAdListener;
import com.mopub.mobileads.MoPubRewardedAdManager;
import com.mopub.mobileads.MoPubRewardedAds;
import com.mopub.network.MoPubNetworkError;
import com.mopub.network.MoPubRequest;
import com.mopub.network.MoPubRequestQueue;
import com.mopub.network.Networking;
import com.mopub.network.TrackingRequest;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.Robolectric;
import org.robolectric.android.util.concurrent.RoboExecutorService;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import java.util.HashMap;
import java.util.Map;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

// If you encounter a VerifyError with PowerMock then you need to set Android Studio to use JDK version 7u79 or later.
// Go to File > Project Structure > [Platform Settings] > SDK to change the JDK version.
@RunWith(SdkTestRunner.class)
@Config(sdk = 21)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*", "org.json.*", "com.mopub.network.CustomSSLSocketFactory" })
@PrepareForTest({MoPubRewardedAdManager.class, ViewabilityManager.class})
public class MoPubTest {

    public static final String INIT_ADUNIT = "b195f8dd8ded45fe847ad89ed1d016da";

    private Activity mActivity;
    private MediationSettings[] mMediationSettings;
    private SdkInitializationListener mockInitializationListener;
    private MoPubRequestQueue mockRequestQueue;
    private SyncRequest.Listener syncListener;

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    @Before
    public void setup() {
        mActivity = Robolectric.buildActivity(Activity.class).create().get();
        mMediationSettings = new MediationSettings[0];

        mockInitializationListener = mock(SdkInitializationListener.class);
        mockRequestQueue = mock(MoPubRequestQueue.class);
        Networking.setRequestQueueForTesting(mockRequestQueue);
        doAnswer((Answer<Object>) invocationOnMock -> {
            MoPubRequest<?> req = ((MoPubRequest<?>) invocationOnMock.getArguments()[0]);
            if (req.getClass().equals(SyncRequest.class)) {
                syncListener = (SyncRequest.Listener) req.getMoPubListener();
                assert syncListener != null;
                syncListener.onErrorResponse(new MoPubNetworkError.Builder().build());
                return null;
            } else if (req.getClass().equals(TrackingRequest.class)) {
                return null;
            } else {
                throw new Exception(String.format("Request object added to RequestQueue can only be of type" +
                        " SyncRequest, saw %s instead.", req.getClass()));
            }
        }).when(mockRequestQueue).add(any(MoPubRequest.class));

        mockStatic(MoPubRewardedAdManager.class);

        AsyncTasks.setExecutor(new RoboExecutorService());
    }

    @After
    public void tearDown() {
        MoPub.resetMoPub();
        BrowserAgentManager.resetBrowserAgent();
        ClientMetadata.clearForTesting();
    }

    @Test
    public void setBrowserAgent_withDefaultValue_shouldNotChangeBrowserAgent_shouldSetOverriddenFlag() {
        MoPub.setBrowserAgent(MoPub.BrowserAgent.IN_APP);
        assertThat(MoPub.getBrowserAgent()).isEqualTo(MoPub.BrowserAgent.IN_APP);
        assertThat(BrowserAgentManager.getBrowserAgent()).isEqualTo(BrowserAgentManager.BrowserAgent.IN_APP);
        assertThat(BrowserAgentManager.isBrowserAgentOverriddenByClient()).isTrue();
    }

    @Test
    public void setBrowserAgent_withNonDefaultValue_shouldChangeBrowserAgent_shouldSetOverriddenFlag() {
        MoPub.setBrowserAgent(MoPub.BrowserAgent.NATIVE);
        assertThat(MoPub.getBrowserAgent()).isEqualTo(MoPub.BrowserAgent.NATIVE);
        assertThat(BrowserAgentManager.getBrowserAgent()).isEqualTo(BrowserAgentManager.BrowserAgent.NATIVE);
        assertThat(BrowserAgentManager.isBrowserAgentOverriddenByClient()).isTrue();
    }

    @Test
    public void setBrowserAgentFromAdServer_whenNotAlreadyOverriddenByClient_shouldSetBrowserAgentFromAdServer() {
        MoPub.setBrowserAgentFromAdServer(MoPub.BrowserAgent.NATIVE);
        assertThat(MoPub.getBrowserAgent()).isEqualTo(MoPub.BrowserAgent.NATIVE);
        assertThat(BrowserAgentManager.getBrowserAgent()).isEqualTo(BrowserAgentManager.BrowserAgent.NATIVE);
        assertThat(BrowserAgentManager.isBrowserAgentOverriddenByClient()).isFalse();
    }

    @Test
    public void setBrowserAgentFromAdServer_whenAlreadyOverriddenByClient_shouldNotChangeBrowserAgent() {
        MoPub.setBrowserAgent(MoPub.BrowserAgent.NATIVE);
        MoPub.setBrowserAgentFromAdServer(MoPub.BrowserAgent.IN_APP);
        assertThat(MoPub.getBrowserAgent()).isEqualTo(MoPub.BrowserAgent.NATIVE);
        assertThat(BrowserAgentManager.getBrowserAgent()).isEqualTo(BrowserAgentManager.BrowserAgent.NATIVE);
        assertThat(BrowserAgentManager.isBrowserAgentOverriddenByClient()).isTrue();
    }

    @Test(expected = NullPointerException.class)
    public void setBrowserAgent_withNullValue_shouldThrowException() {
        MoPub.setBrowserAgent(null);
    }

    @Test(expected = NullPointerException.class)
    public void setBrowserAgentFromAdServer_withNullValue_shouldThrowException() {
        MoPub.setBrowserAgentFromAdServer(null);
    }

    @Test
    public void initializeSdk_withRewardedAd_shouldCallMoPubRewardedAdManager() {
        MoPub.initializeSdk(mActivity,
                new SdkConfiguration.Builder(INIT_ADUNIT).build(),
                mockInitializationListener);

        ShadowLooper.runUiThreadTasks();
        verify(mockInitializationListener).onInitializationFinished();
        verifyStatic();
        MoPubRewardedAdManager.init(mActivity, mMediationSettings);
    }

    @Test
    public void initializeSdk_withRewardedAd_withMediationSettings_shouldCallMoPubRewardedAdManager() {
        MoPub.initializeSdk(mActivity,
                new SdkConfiguration.Builder(INIT_ADUNIT).withMediationSettings(mMediationSettings).build(),
                mockInitializationListener);

        ShadowLooper.runUiThreadTasks();
        verify(mockInitializationListener).onInitializationFinished();
        verifyStatic();
        MoPubRewardedAdManager.init(mActivity, mMediationSettings);
    }

    @Test
    public void initializeSdk_withRewardedAd_withoutActivity_shouldNotCallMoPubRewardedAdManager() {
        // Since we can't verifyStatic with 0 times, we expect this to call the rewarded ad
        // manager exactly twice instead of three times since one of the times is with the
        // application context instead of the activity context.
        MoPub.initializeSdk(mActivity.getApplication(),
                new SdkConfiguration.Builder(INIT_ADUNIT).withMediationSettings(
                        mMediationSettings).build(), mockInitializationListener);

        MoPub.initializeSdk(mActivity,
                new SdkConfiguration.Builder(INIT_ADUNIT).withMediationSettings(
                        mMediationSettings).build(), mockInitializationListener);

        MoPub.initializeSdk(mActivity,
                new SdkConfiguration.Builder(INIT_ADUNIT).withMediationSettings(
                        mMediationSettings).build(), mockInitializationListener);

        verifyStatic(times(2));
        MoPubRewardedAdManager.init(mActivity, mMediationSettings);
        verify(mockInitializationListener);
    }

    @Test
    public void updateActivity_withReflection_shouldExist() throws Exception {
        assertThat(Reflection.getDeclaredMethodWithTraversal(MoPubRewardedAdManager.class,
                "updateActivity", Activity.class)).isNotNull();
    }

    @Test
    public void updateActivity_withValidActivity_shouldCallMoPubRewardedAdManager() {
        MoPub.updateActivity(mActivity);

        verifyStatic();
        MoPubRewardedAdManager.updateActivity(mActivity);
    }

    @Test
    public void setRewardedAdListener_withReflection_shouldExist() throws Exception {
        assertThat(Reflection.getDeclaredMethodWithTraversal(MoPubRewardedAds.class,
                "setRewardedAdListener", MoPubRewardedAdListener.class)).isNotNull();
    }

    @Test
    public void loadRewardedAd_withReflection_shouldExist() throws Exception {
        assertThat(Reflection.getDeclaredMethodWithTraversal(MoPubRewardedAds.class,
                "loadRewardedAd", String.class,
                MoPubRewardedAdManager.RequestParameters.class,
                MediationSettings[].class)).isNotNull();
    }

    @Test
    public void hasRewardedAd_withReflection_shouldExist() throws Exception {
        assertThat(Reflection.getDeclaredMethodWithTraversal(MoPubRewardedAds.class,
                "hasRewardedAd", String.class)).isNotNull();
    }

    @Test
    public void initializeSdk_withOneAdvancedBidder_shouldSetAdvancedBiddingTokens() {
        SdkConfiguration sdkConfiguration = new SdkConfiguration.Builder(
                INIT_ADUNIT).withAdditionalNetwork(
                AdapterConfigurationTestClass.class.getName()).build();

        MoPub.initializeSdk(mActivity, sdkConfiguration, null);

        ShadowLooper.runUiThreadTasks();
        assertThat(MoPub.getAdvancedBiddingTokensJson(mActivity)).isEqualTo(
                "{\"AdvancedBidderTestClassName\":{\"token\":\"AdvancedBidderTestClassToken\"}}");
    }

    @Test
    public void initializeSdk_withMultipleInitializations_shouldSetAdvancedBiddingTokensOnce() {
        SdkConfiguration sdkConfiguration = new SdkConfiguration.Builder
                (INIT_ADUNIT).withAdditionalNetwork(
                AdapterConfigurationTestClass.class.getName()).build();

        MoPub.initializeSdk(mActivity, sdkConfiguration, null);

        ShadowLooper.runUiThreadTasks();
        assertThat(MoPub.getAdvancedBiddingTokensJson(mActivity)).isEqualTo(
                "{\"AdvancedBidderTestClassName\":{\"token\":\"AdvancedBidderTestClassToken\"}}");

        // Attempting to initialize twice
        sdkConfiguration = new SdkConfiguration.Builder(INIT_ADUNIT)
                .withAdditionalNetwork(SecondAdapterConfigurationTestClass.class.getName()).build();
        MoPub.initializeSdk(mActivity, sdkConfiguration, null);

        // This should not do anything, and getAdvancedBiddingTokensJson() should return the
        // original Advanced Bidder.
        assertThat(MoPub.getAdvancedBiddingTokensJson(mActivity)).isEqualTo(
                "{\"AdvancedBidderTestClassName\":{\"token\":\"AdvancedBidderTestClassToken\"}}");
    }

    @Test
    public void initializeSdk_withCallbackSet_shouldCallCallback() {
        MoPub.initializeSdk(mActivity, new SdkConfiguration.Builder(
                INIT_ADUNIT).build(), mockInitializationListener);
        ShadowLooper.runUiThreadTasks();

        verify(mockInitializationListener).onInitializationFinished();
    }

    @Test
    public void initializeSdk_withNoLegitimateInterestAllowedValue_shouldCallPersonalInfoManagerSetAllowLegitimateInterest_withLegitimateInterestAllowedFalse() {
        MoPub.initializeSdk(mActivity, new SdkConfiguration.Builder(
                INIT_ADUNIT).build(), null);
        ShadowLooper.runUiThreadTasks();

        final boolean actual = MoPub.shouldAllowLegitimateInterest();

        assertThat(actual).isFalse();
    }

    @Test
    public void initializeSdk_withLegitimateInterestAllowedFalse_shouldCallPersonalInfoManagerSetAllowLegitimateInterest_withLegitimateInterestAllowedFalse() {
        MoPub.initializeSdk(mActivity, new SdkConfiguration.Builder(
                INIT_ADUNIT).withLegitimateInterestAllowed(false).build(), null);
        ShadowLooper.runUiThreadTasks();

        final boolean actual = MoPub.shouldAllowLegitimateInterest();

        assertThat(actual).isFalse();
    }

    @Test
    public void initializeSdk_withLegitimateInterestAllowedTrue_shouldCallPersonalInfoManagerSetAllowLegitimateInterest_withLegitimateInterestAllowedTrue() {
        MoPub.initializeSdk(mActivity, new SdkConfiguration.Builder(
                INIT_ADUNIT).withLegitimateInterestAllowed(true).build(), null);
        ShadowLooper.runUiThreadTasks();

        final boolean actual = MoPub.shouldAllowLegitimateInterest();

        assertThat(actual).isTrue();
    }

    @Test
    public void disableViewability_callsViewabilityManager() {
        MoPub.disableViewability();

        verifyStatic(ViewabilityManager.class);
        ViewabilityManager.disableViewability();
    }

    private static class AdapterConfigurationTestClass extends BaseAdapterConfiguration {
        @NonNull
        @Override
        public String getAdapterVersion() {
            return "adapterVersion";
        }

        @Nullable
        @Override
        public String getBiddingToken(@NonNull final Context context) {
            return "AdvancedBidderTestClassToken";
        }

        @NonNull
        @Override
        public String getMoPubNetworkName() {
            return "AdvancedBidderTestClassName";
        }

        @NonNull
        @Override
        public String getNetworkSdkVersion() {
            return "networkVersion";
        }

        @Override
        public void initializeNetwork(@NonNull final Context context,
                @Nullable final Map<String, String> configuration,
                @NonNull final OnNetworkInitializationFinishedListener listener) {

        }
    }

    private static class SecondAdapterConfigurationTestClass implements AdapterConfiguration {
        @NonNull
        @Override
        public String getAdapterVersion() {
            return "adapterVersion";
        }

        @Nullable
        @Override
        public String getBiddingToken(@NonNull final Context context) {
            return "SecondAdvancedBidderTestClassToken";
        }

        @NonNull
        @Override
        public String getMoPubNetworkName() {
            return "SecondAdvancedBidderTestClassName";
        }

        @Nullable
        @Override
        public Map<String, String> getMoPubRequestOptions() {
            return null;
        }

        @NonNull
        @Override
        public String getNetworkSdkVersion() {
            return "networkVersion";
        }

        @Override
        public void initializeNetwork(@NonNull final Context context,
                @Nullable final Map<String, String> configuration,
                @NonNull final OnNetworkInitializationFinishedListener listener) {
        }

        @Override
        public void setCachedInitializationParameters(@NonNull final Context context,
                @Nullable final Map<String, String> configuration) {
        }

        @NonNull
        @Override
        public Map<String, String> getCachedInitializationParameters(
                @NonNull final Context context) {
            return new HashMap<>();
        }

        @Override
        public void setMoPubRequestOptions(
                @Nullable final Map<String, String> moPubRequestOptions) {
        }
    }
}
