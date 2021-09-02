// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mraid;

import com.mopub.common.test.support.SdkTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(SdkTestRunner.class)
public class MraidWebViewClientTest {

    private MraidWebViewClient subject;

    @Before
    public void setUp() throws  Exception {
        subject = new MraidWebViewClient();
    }

    @Test
    public void matchesInjectionUrl_withMopubMraidJsUrls_shouldMatch() throws Exception {
        assertThat(subject.matchesInjectionUrl("https://ads.mopub.com/mraid.js")).isTrue();
    }

    @Test
    public void matchesInjectionUrl_withOtherMraidJsUrls_shouldMatch() throws Exception {
        assertThat(subject.matchesInjectionUrl("https://mraid.iab.net/compliance/mraid.js"))
                .isTrue();
    }

    @Test
    public void matchesInjectionUrl_withRelativeMraidJsUrl_shouldMatch() throws Exception {
        assertThat(subject.matchesInjectionUrl("mraid.js")).isTrue();
    }

    @Test
    public void matchesInjectionUrl_withCasedMraidJsUrl_shouldMatch() throws Exception {
        assertThat(subject.matchesInjectionUrl("mrAid.Js")).isTrue();
    }

    @Test
    public void matchesInjectionUrl_withMraidJsUrl_withQueryParams_shouldMatch() throws Exception {
        assertThat(subject.matchesInjectionUrl("mraid.js?foo=bar")).isTrue();
    }

    @Test
    public void matchesInjectionUrl_withoutMraidJs_shouldNotMatch() throws Exception {
        assertThat(subject.matchesInjectionUrl("mmraid.js")).isFalse();
        assertThat(subject.matchesInjectionUrl("maid.js")).isFalse();
        assertThat(subject.matchesInjectionUrl("mraidjs")).isFalse();
        assertThat(subject.matchesInjectionUrl("mraid.jsS")).isFalse();
    }

    @Test
    public void matchesInjectionUrl_withOpaqueUri_shouldNotMatch() throws Exception {
        assertThat(subject.matchesInjectionUrl("mailto:mraid.js@js.com")).isFalse();
    }
}
