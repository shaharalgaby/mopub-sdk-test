// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.network;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;

import com.mopub.common.ClientMetadata;
import com.mopub.common.MoPub;
import com.mopub.common.privacy.AdvertisingId;
import com.mopub.common.privacy.MoPubIdentifier;
import com.mopub.common.privacy.MoPubIdentifierTest;
import com.mopub.common.privacy.PersonalInfoManager;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.common.util.Reflection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
public class PlayServicesUrlRewriterTest {

    @Mock
    private ClientMetadata mockClientMetadata;
    private PersonalInfoManager mockPersonalInfoManager;

    private Context context;
    private PlayServicesUrlRewriter subject;

    @Before
    public void setUp() throws Exception {
        context = Robolectric.buildActivity(Activity.class).create().get();
        MoPubIdentifierTest.writeAdvertisingInfoToSharedPreferences(context, false);
        mockPersonalInfoManager = mock(PersonalInfoManager.class);
        new Reflection.MethodBuilder(null, "setPersonalInfoManager")
                .setStatic(MoPub.class)
                .setAccessible()
                .addParam(PersonalInfoManager.class, mockPersonalInfoManager)
                .execute();

        ClientMetadata.getInstance(context);
        subject = new PlayServicesUrlRewriter();
    }

    @After
    public void tearDown(){
        ClientMetadata.clearForTesting();
        MoPubIdentifierTest.clearPreferences(context);
    }

    @Test
    public void rewriteUrl_shouldUseAdvertisingIdValue(){
        when(mockClientMetadata.getMoPubIdentifier()).thenReturn(new MoPubIdentifier(context));
        when(mockPersonalInfoManager.canCollectPersonalInformation()).thenReturn(true);

        MoPubIdentifier identifier = ClientMetadata.getInstance().getMoPubIdentifier();
        AdvertisingId adId = identifier.getAdvertisingInfo();
        String encodedIfa = Uri.encode(adId.getIdentifier(true));
        String encodedMoPubId = Uri.encode(adId.getIdentifier(false));
        String actualUrl = subject.rewriteUrl(
                "https://ads.mopub.com/m/ad?ad_id=abcece&ifa=mp_tmpl_advertising_id&dnt=mp_tmpl_do_not_track&tas=mp_tmpl_tas&mid=mp_tmpl_mopub_id");

        assertThat(actualUrl).isEqualToIgnoringCase("https://ads.mopub.com/m/ad?ad_id=abcece&ifa=" +
                encodedIfa + "&dnt=0&tas=authorized&mid=" + encodedMoPubId);
    }

    @Test
    public void rewriteUrl_noTemplates_shouldReturnIdentical() throws Exception {
        assertThat(subject.rewriteUrl("https://ads.mopub.com/m/ad")).isEqualTo("https://ads.mopub.com/m/ad");
    }
}
