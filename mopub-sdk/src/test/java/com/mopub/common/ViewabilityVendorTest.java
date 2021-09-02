// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common;

import com.mopub.common.test.support.SdkTestRunner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Set;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(SdkTestRunner.class)
public class ViewabilityVendorTest {
    private static final String JAVASCRIPT_RESOURCE_URL = "javascriptResourceUrl";
    private static final String VENDOR_KEY = "vendorKey";
    private static final String VERIFICATION_PARAMETERS = "verificationParameters";
    private static final String API_FRAMEWORK = "apiFramework";

    private final String VENDOR1 = "vendor1";
    private final String VENDOR2 = "vendor2";

    @Test
    public void createFromJson_returnsValidObject() throws JSONException {
        final JSONObject jsonObject = new JSONObject(VENDOR1_JSON);

        ViewabilityVendor subject = ViewabilityVendor.createFromJson(jsonObject);

        assertNotNull(subject);
        assertEquals(subject.getVendorKey(), jsonObject.getString(VENDOR_KEY));
        assertEquals(subject.getJavascriptResourceUrl().toString(), jsonObject.getString(JAVASCRIPT_RESOURCE_URL));
        assertEquals(subject.getVerificationParameters(), jsonObject.getString(VERIFICATION_PARAMETERS));
        assertNull(subject.getVerificationNotExecuted());
    }

    @Test
    public void createFromJson_withInvalidJavascriptResourceUrl_returnsNull() throws JSONException {
        final JSONObject jsonObject = new JSONObject(VENDOR1_JSON);
        jsonObject.put(JAVASCRIPT_RESOURCE_URL, "invalid url");

        ViewabilityVendor subject = ViewabilityVendor.createFromJson(jsonObject);

        assertNull(subject);
    }

    @Test
    public void createFromJson_withInvalidApiFramework_returnsNull() throws JSONException {
        final JSONObject jsonObject = new JSONObject(VENDOR1_JSON);
        jsonObject.put(API_FRAMEWORK, "moat");

        ViewabilityVendor subject = ViewabilityVendor.createFromJson(jsonObject);

        assertNull(subject);
    }

    @Test
    public void hashCode_forTheSameContent_isEqual() throws JSONException {
        final JSONObject jsonObject1 = new JSONObject(VENDOR1_JSON);

        final ViewabilityVendor vendor1 = ViewabilityVendor.createFromJson(jsonObject1);
        final ViewabilityVendor vendor2 = ViewabilityVendor.createFromJson(jsonObject1);

        assertNotNull(vendor1);
        assertNotNull(vendor2);
        assertNotSame(vendor1, vendor2);
        assertEquals(vendor1.hashCode(), vendor2.hashCode());
    }

    @Test
    public void hashCode_forDifferentContent_isNotEqual() throws JSONException {
        final JSONObject jsonObject1 = new JSONObject(VENDOR1_JSON);
        final JSONObject jsonObject2 = new JSONObject(VENDOR2_JSON);

        final ViewabilityVendor vendor1 = ViewabilityVendor.createFromJson(jsonObject1);
        final ViewabilityVendor vendor2 = ViewabilityVendor.createFromJson(jsonObject2);

        assertNotNull(vendor1);
        assertNotNull(vendor2);
        assertNotEquals(vendor1.hashCode(), vendor2.hashCode());
    }

    @Test
    public void equals_forTheSameContent_isTrue() throws JSONException {
        final JSONObject jsonObject1 = new JSONObject(VENDOR1_JSON);

        final ViewabilityVendor vendor1 = ViewabilityVendor.createFromJson(jsonObject1);
        final ViewabilityVendor vendor2 = ViewabilityVendor.createFromJson(jsonObject1);

        assertNotNull(vendor1);
        assertNotNull(vendor2);
        assertNotSame(vendor1, vendor2);
        Assert.assertEquals(vendor1, vendor2);
    }

    @Test
    public void equals_forDifferentContent_isFalse() throws JSONException {
        final JSONObject jsonObject1 = new JSONObject(VENDOR1_JSON);
        final JSONObject jsonObject2 = new JSONObject(VENDOR2_JSON);

        final ViewabilityVendor vendor1 = ViewabilityVendor.createFromJson(jsonObject1);
        final ViewabilityVendor vendor2 = ViewabilityVendor.createFromJson(jsonObject2);

        assertNotNull(vendor1);
        assertNotNull(vendor2);
        assertNotEquals(vendor1, vendor2);
    }

    @Test
    public void createFromJsonArray_returnsValidObjects() throws JSONException {
        final JSONObject jsonObject1 = new JSONObject(VENDOR1_JSON);
        final JSONObject jsonObject2 = new JSONObject(VENDOR2_JSON);
        final JSONArray jsonArray = new JSONArray();
        jsonArray.put(jsonObject1);
        jsonArray.put(jsonObject2);

        final Set<ViewabilityVendor> subject = ViewabilityVendor.createFromJsonArray(jsonArray);

        final ViewabilityVendor vendor1 = ViewabilityVendor.createFromJson(jsonObject1);
        final ViewabilityVendor vendor2 = ViewabilityVendor.createFromJson(jsonObject2);

        assertNotNull(subject);
        assertNotNull(vendor1);
        assertNotNull(vendor2);
        assertEquals(subject.size(), jsonArray.length());
        assertTrue(subject.contains(vendor1));
        assertTrue(subject.contains(vendor2));
    }

    @Test
    public void createFromJsonArray_ignoresDuplicateValues() throws JSONException {
        final JSONObject jsonObject1 = new JSONObject(VENDOR1_JSON);
        final JSONObject jsonObject2 = new JSONObject(VENDOR1_JSON);
        final JSONArray jsonArray = new JSONArray();
        jsonArray.put(jsonObject1);
        jsonArray.put(jsonObject2);

        final Set<ViewabilityVendor> subject = ViewabilityVendor.createFromJsonArray(jsonArray);

        final ViewabilityVendor vendor1 = ViewabilityVendor.createFromJson(jsonObject1);

        assertNotNull(subject);
        assertNotNull(vendor1);
        assertEquals(jsonArray.length(), 2);
        assertEquals(subject.size(), 1);
        assertTrue(subject.contains(vendor1));
    }

    @Test
    public void builder_withCorrectValues_createsValidObject() {
        final ViewabilityVendor.Builder builder = new ViewabilityVendor.Builder("https://name.com");
        builder.withApiFramework("omid")
                .withVerificationNotExecuted("not_executed")
                .withVerificationParameters("parameters")
                .withVendorKey("vendor_key");

        final ViewabilityVendor subject = builder.build();

        assertNotNull(subject);
        assertEquals(subject.getVerificationNotExecuted(), "not_executed");
        assertEquals(subject.getVerificationParameters(), "parameters");
        assertEquals(subject.getVendorKey(), "vendor_key");
    }

    @Test
    public void builder_withInvalidApiFramework_returnsNull() {
        final ViewabilityVendor.Builder builder = new ViewabilityVendor.Builder("https://name.com");
        builder.withApiFramework("moat")
                .withVerificationNotExecuted("not_executed")
                .withVerificationParameters("parameters")
                .withVendorKey("vendor_key");

        final ViewabilityVendor subject = builder.build();

        assertNull(subject);
    }

    @Test
    public void builder_withInvalidJavascriptResourceUrl_returnsNull() {
        final ViewabilityVendor.Builder builder = new ViewabilityVendor.Builder("invalid url");
        builder.withApiFramework("omid")
                .withVerificationNotExecuted("not_executed")
                .withVerificationParameters("parameters")
                .withVendorKey("vendor_key");

        final ViewabilityVendor subject = builder.build();

        assertNull(subject);
    }

    // internal data
    private final String VENDOR1_JSON =
            "{"
                    + "'vendorKey': " + VENDOR1 + ".com-omid',"
                    + "'apiFramework': 'omid',"
                    + "'javascriptResourceUrl': 'https://" + VENDOR1 + ".com/verification.js',"
                    + "'verificationParameters': '[" + VENDOR1 + " parameters string]'"
                    + "}";
    private final String VENDOR2_JSON =
            "{"
                    + "'vendorKey': " + VENDOR2 + ".com-omid',"
                    + "'apiFramework': 'omid',"
                    + "'javascriptResourceUrl': 'https://" + VENDOR2 + ".com/verification.js',"
                    + "'verificationParameters': '[" + VENDOR2 + " parameters string]'"
                    + "}";
}
