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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;

import java.util.HashMap;
import java.util.Map;

import static com.mopub.common.BaseAdapterConfiguration.CUSTOM_EVENT_PREF_NAME;
import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(SdkTestRunner.class)
public class BaseAdapterConfigurationTest {

    private Context context;
    private BaseAdapterConfiguration subject;

    @Before
    public void setup() {
        context = Robolectric.buildActivity(Activity.class).create().get();

        subject = new TestAdapterConfiguration();
    }

    @After
    public void tearDown() {
        final SharedPreferences sharedPreferences = SharedPreferencesHelper.getSharedPreferences(
                context, CUSTOM_EVENT_PREF_NAME);
        sharedPreferences.edit().clear().apply();
    }

    @Test
    public void setCachedInitializationParameters_withInitializationParameters_shouldSetSharedPreferences() {
        final Map<String, String> initializationParameters = new HashMap<>();
        initializationParameters.put("key1", "value1");
        initializationParameters.put("key2", "value2");

        subject.setCachedInitializationParameters(context, initializationParameters);

        final SharedPreferences sharedPreferences = SharedPreferencesHelper.getSharedPreferences(
                context, CUSTOM_EVENT_PREF_NAME);
        Map<String, ?> sharedPreferencesMap = sharedPreferences.getAll();
        assertThat(sharedPreferencesMap.size()).isEqualTo(1);
        assertThat(sharedPreferencesMap.get(TestAdapterConfiguration.class.getName()))
                .isEqualTo("{\"key1\":\"value1\",\"key2\":\"value2\"}");
    }

    @Test
    public void getCachedInitializationParameters_withInitializationParameters_shouldReturnMapOfInitializationParameters() {
        final SharedPreferences sharedPreferences = SharedPreferencesHelper.getSharedPreferences(
                context, CUSTOM_EVENT_PREF_NAME);
        sharedPreferences.edit().putString(TestAdapterConfiguration.class.getName(),
                "{\"key1\":\"value1\",\"key2\":\"value2\"}").apply();

        final Map<String, String> initializationParameters =
                subject.getCachedInitializationParameters(context);

        assertThat(initializationParameters.size()).isEqualTo(2);
        assertThat(initializationParameters.get("key1")).isEqualTo("value1");
        assertThat(initializationParameters.get("key2")).isEqualTo("value2");
    }

    @Test
    public void getCachedInitializationParameters_withInvalidInitializationParameters_shouldReturnEmptyMap() {
        final SharedPreferences sharedPreferences = SharedPreferencesHelper.getSharedPreferences(
                context, CUSTOM_EVENT_PREF_NAME);
        // missing the last curly brace
        sharedPreferences.edit().putString(TestAdapterConfiguration.class.getName(),
                "{\"key1\":\"value1\",\"key2\":\"value2\"").apply();

        final Map<String, String> initializationParameters =
                subject.getCachedInitializationParameters(context);

        assertThat(initializationParameters.isEmpty());
    }

    @Test
    public void getCachedInitializationParameters_withNoInitializationParameters_shouldReturnEmptyMap() {
        final Map<String, String> initializationParameters =
                subject.getCachedInitializationParameters(context);

        assertThat(initializationParameters.isEmpty());
    }

    @Test
    public void setCachedInitializationParameters_getCachedInitializationParameters_withInitializationParameters_shouldReturnMapOfInitializationParameters() {
        final Map<String, String> inputInitializationParameters = new HashMap<>();
        inputInitializationParameters.put("key1", "value1");
        inputInitializationParameters.put("key2", "value2");

        subject.setCachedInitializationParameters(context, inputInitializationParameters);
        final Map<String, String> outputInitializationParameters =
                subject.getCachedInitializationParameters(context);

        assertThat(outputInitializationParameters.size()).isEqualTo(2);
        assertThat(outputInitializationParameters.get("key1")).isEqualTo("value1");
        assertThat(outputInitializationParameters.get("key2")).isEqualTo("value2");

    }

    public static class TestAdapterConfiguration extends BaseAdapterConfiguration {
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
            return "networkName";
        }

        @NonNull
        @Override
        public String getNetworkSdkVersion() {
            return "networkSdkVersion";
        }

        @Override
        public void initializeNetwork(@NonNull final Context context,
                @Nullable final Map<String, String> configuration,
                @NonNull final OnNetworkInitializationFinishedListener listener) {
            listener.onNetworkInitializationFinished(this.getClass(),
                    MoPubErrorCode.ADAPTER_INITIALIZATION_SUCCESS);
        }
    }
}
