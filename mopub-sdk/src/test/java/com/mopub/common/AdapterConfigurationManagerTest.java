// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.MoPubErrorCode;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.mopub.common.BaseAdapterConfiguration.CUSTOM_EVENT_PREF_NAME;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(SdkTestRunner.class)
public class AdapterConfigurationManagerTest {

    private Context context;
    private AdapterConfigurationManager subject;
    private SdkInitializationListener mockInitializationListener;

    @Before
    public void setup() {
        context = Robolectric.buildActivity(Activity.class).create().get();
        mockInitializationListener = mock(SdkInitializationListenerMockClass.class);
        subject = new AdapterConfigurationManager(mockInitializationListener);

    }

    @Test
    public void initialize_getTokensAsJsonString_withAdapterConfigurationClass_shouldAddOneMoPubAdvancedBidder_shouldSetUpAdapterConfiguration() {
        final Set<String> set = new HashSet<>();
        set.add(ACMTestAdapterConfiguration.class.getName());
        set.add(ACMNoTokenAdapterConfiguration.class.getName());

        subject.initialize(context, set, new HashMap<String, Map<String, String>>(),
                new HashMap<String, Map<String, String>>());

        assertThat(subject.getTokensAsJsonString(context)).isEqualTo(
                "{\"AdapterConfigurationTest\":{\"token\":\"AdapterConfigurationTestToken\"}}");
        verify(mockInitializationListener).onInitializationFinished();
        assertThat(subject.getAdapterConfiguration(
                ACMTestAdapterConfiguration.class)).isNotNull();
    }

    @Test
    public void initialize_getTokensAsJsonString_withNoAdapterConfigurations_shouldReturnNull() {
        final Set<String> set = new HashSet<>();

        subject.initialize(context, set, new HashMap<String, Map<String, String>>(),
                new HashMap<String, Map<String, String>>());

        assertThat(subject.getTokensAsJsonString(context)).isNull();
        verify(mockInitializationListener).onInitializationFinished();
    }

    @Test
    public void initialize_getTokensAsJsonString_withNoTokens_shouldReturnNull() {
        final Set<String> set = new HashSet<>();
        set.add(ACMNoTokenAdapterConfiguration.class.getName());

        subject.initialize(context, set, new HashMap<String, Map<String, String>>(),
                new HashMap<String, Map<String, String>>());

        assertThat(subject.getTokensAsJsonString(context)).isNull();
        verify(mockInitializationListener).onInitializationFinished();
    }

    @Test
    public void initialize_withAdapterConfigurationClass_shouldMergeInitializationParameters() {
        final Set<String> set = new HashSet<>();
        set.add(ACMTestAdapterConfiguration.class.getName());
        Map<String, Map<String, String>> networkMediationConfigurations = new HashMap<>();
        Map<String, Map<String, String>> moPubRequestOptions = new HashMap<>();
        Map<String, String> networkMediationConfiguration = new HashMap<>();
        networkMediationConfiguration.put("key1", "value1");
        networkMediationConfiguration.put("key2", "value2");
        networkMediationConfigurations.put(
                ACMTestAdapterConfiguration.class.getName(),
                networkMediationConfiguration);
        Map<String, String> moPubRequestOption = new HashMap<>();
        moPubRequestOption.put("key3", "value3");
        moPubRequestOption.put("key4", "value4");
        moPubRequestOptions.put(ACMTestAdapterConfiguration.class.getName(),
                moPubRequestOption);
        final SharedPreferences sharedPreferences = SharedPreferencesHelper.getSharedPreferences(
                context, CUSTOM_EVENT_PREF_NAME);
        sharedPreferences.edit().putString(
                ACMTestAdapterConfiguration.class.getName(),
                "{\"key1\":\"oldValue\",\"key5\":\"value5\"}").apply();

        subject.initialize(context, set, networkMediationConfigurations, moPubRequestOptions);

        AdapterConfiguration adapterConfiguration = subject.getAdapterConfiguration(
                ACMTestAdapterConfiguration.class);
        Map<String, String> actualNetworkMediationConfigurations = adapterConfiguration.getCachedInitializationParameters(context);
        Map<String, String> actualMoPubRequestOptions = adapterConfiguration.getMoPubRequestOptions();
        assertThat(actualNetworkMediationConfigurations.get("key1")).isEqualTo("value1");
        assertThat(actualNetworkMediationConfigurations.get("key2")).isEqualTo("value2");
        assertThat(actualNetworkMediationConfigurations.get("key5")).isEqualTo("value5");
        assertThat(actualMoPubRequestOptions.get("key3")).isEqualTo("value3");
        assertThat(actualMoPubRequestOptions.get("key4")).isEqualTo("value4");
    }

    private static class ACMTestAdapterConfiguration extends BaseAdapterConfiguration {

        @NonNull
        @Override
        public String getAdapterVersion() {
            return "adapterVersion";
        }

        @Nullable
        @Override
        public String getBiddingToken(@NonNull final Context context) {
            return "AdapterConfigurationTestToken";
        }

        @NonNull
        @Override
        public String getMoPubNetworkName() {
            return "AdapterConfigurationTest";
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
            listener.onNetworkInitializationFinished(
                    ACMTestAdapterConfiguration.class,
                    MoPubErrorCode.ADAPTER_INITIALIZATION_SUCCESS);
        }
    }

    private static class ACMNoTokenAdapterConfiguration extends BaseAdapterConfiguration {

        @NonNull
        @Override
        public String getAdapterVersion() {
            return "adapterVersion";
        }

        @Nullable
        @Override
        public String getBiddingToken(@NonNull final Context context) {
            return null;
        }

        @NonNull
        @Override
        public String getMoPubNetworkName() {
            return "AdapterConfigurationTest";
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
            listener.onNetworkInitializationFinished(
                    ACMNoTokenAdapterConfiguration.class,
                    MoPubErrorCode.ADAPTER_INITIALIZATION_SUCCESS);
        }
    }

    // Creating extra class to prevent Robolectric cache conflict with
    // MoPub.initializeSdk_withCallbackSet_shouldCallCallback
    private abstract class SdkInitializationListenerMockClass implements SdkInitializationListener {
    }

}
