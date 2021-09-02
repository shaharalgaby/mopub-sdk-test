// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common;

import android.app.Activity;

import com.iab.omid.library.mopub.Omid;
import com.iab.omid.library.mopub.ScriptInjector;
import com.mopub.common.test.support.SdkTestRunner;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.Robolectric;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*"})
@PrepareForTest(ScriptInjector.class)
public class ViewabilityManagerTest {
    @Rule
    public PowerMockRule rule = new PowerMockRule();

    private Activity activity;

    @Before
    public void setUp() throws Exception {
        activity = Robolectric.buildActivity(Activity.class).create().get();
    }

    @After
    public void tearDown() {
        ViewabilityManager.setViewabilityEnabled(true);
    }

    @Test
    public void activate_stateIsCorrect() {
        ViewabilityManager.activate(activity);

        assertTrue(ViewabilityManager.isViewabilityEnabled());
        assertNotNull(ViewabilityManager.getPartner());
        assertTrue(Omid.isActive());
        assertTrue(ViewabilityManager.isActive());
    }

    @Test
    public void getOmidVersion_returnsOmsdkVersion() {
        assertEquals(Omid.getVersion(), ViewabilityManager.getOmidVersion());
    }

    @Test
    public void disableViewability_setsFlagToFalse() {
        assertTrue(ViewabilityManager.isViewabilityEnabled());

        ViewabilityManager.disableViewability();

        assertFalse(ViewabilityManager.isViewabilityEnabled());
    }

    @Test
    public void injectScriptContentIntoHtml_injectsJavascript() {
        final String html = "<HTML/>";

        final String subject = ViewabilityManager.injectScriptContentIntoHtml(html);

        assertNotNull(subject);
        assertThat(subject.indexOf(ViewabilityManager.getOmidJsServiceContent())).isGreaterThan(-1);
    }

    @Test
    public void injectScriptUrlIntoHtml_injectsJavascript() {
        final String html = "<HTML/>";
        final String valid_tag = "<script src=\"https://url.com\"></script>";

        final String subject = ViewabilityManager.injectScriptUrlIntoHtml(html, "https://url.com");

        assertNotNull(subject);
        assertThat(subject.indexOf(valid_tag)).isGreaterThan(-1);
    }

    @Test
    public void injectScriptContentIntoHtml_whenViewabilityDisabled_doesNotModifyHtml() {
        final String html = "<HTML/>";
        ViewabilityManager.disableViewability();

        final String subject = ViewabilityManager.injectScriptContentIntoHtml(html);

        assertNotNull(subject);
        assertEquals(html, subject);
    }

    @Test
    public void injectVerificationUrlsIntoHtml_whenViewabilityEnabled_injectsUrlsIntoHtml() throws MalformedURLException {
        final String html = "<HTML/>";
        final ViewabilityVendor vendor1 = mock(ViewabilityVendor.class);
        when(vendor1.getJavascriptResourceUrl()).thenReturn(new URL("https://first_url"));
        final ViewabilityVendor vendor2 = mock(ViewabilityVendor.class);
        when(vendor2.getJavascriptResourceUrl()).thenReturn(new URL("https://second_url"));
        final Set<ViewabilityVendor> vendorSet = new HashSet<>();
        vendorSet.add(vendor1);
        vendorSet.add(null);
        vendorSet.add(vendor2);

        final String subject = ViewabilityManager.injectVerificationUrlsIntoHtml(html, vendorSet);

        assertTrue(subject.contains("<script src=\"" + "https://first_url" + "\"></script>"));
        assertTrue(subject.contains("<script src=\"" + "https://second_url" + "\"></script>"));
    }

    @Test
    public void injectVerificationUrlsIntoHtml_whenVendorsIsNull_returnsUnmodifiedHtml() {
        final String html = "<HTML/>";

        final String subject = ViewabilityManager.injectVerificationUrlsIntoHtml(html, null);

        assertEquals(html, subject);
    }

    @Test
    public void injectScriptContentIntoHtml_whenInjectorThrowsException_doesNotModifyHtml() {
        PowerMockito.mockStatic(ScriptInjector.class);
        when(ScriptInjector.injectScriptContentIntoHtml(anyString(), anyString())).thenThrow(new OutOfMemoryError());

        final String html = "<HTML/>";

        final String subject = ViewabilityManager.injectScriptContentIntoHtml(html);

        assertNotNull(subject);
        assertEquals(html, subject);
    }

    @Test
    public void injectVerificationUrlsIntoHtml_whenInjectorThrowsException_returnsUnmodifiedHtml() throws MalformedURLException {
        PowerMockito.mockStatic(ScriptInjector.class);
        when(ScriptInjector.injectScriptContentIntoHtml(anyString(), anyString())).thenThrow(new OutOfMemoryError());

        final String html = "<HTML/>";
        final ViewabilityVendor vendor1 = mock(ViewabilityVendor.class);
        when(vendor1.getJavascriptResourceUrl()).thenReturn(new URL("https://first_url"));
        final Set<ViewabilityVendor> vendorSet = new HashSet<>();
        vendorSet.add(vendor1);

        final String subject = ViewabilityManager.injectVerificationUrlsIntoHtml(html, vendorSet);

        assertEquals(html, subject);
    }
}
