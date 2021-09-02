// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import androidx.annotation.NonNull;

import com.mopub.common.ViewabilityVendor;
import com.mopub.common.test.support.SdkTestRunner;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.w3c.dom.Node;

import java.util.Set;

import static com.mopub.mobileads.test.support.VastUtils.createNode;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.fest.assertions.api.Assertions.assertThat;


@RunWith(SdkTestRunner.class)
public class VastAdVerificationsParserTest {
    private static final String VENDOR_1 = "vendor name 1";
    private static final String VENDOR_2 = "vendor name 2";
    private static final String JS_URL_1 = "https://name1.com";
    private static final String JS_URL_2 = "https://name2.com";
    private static final String VERIFICATION_NOT_EXECUTED_1 = "verification_not_executed_1";
    private static final String VERIFICATION_NOT_EXECUTED_2 = "verification_not_executed_2";
    private static final String VERIFICATION_PARAMETERS_1 = "verification parameters 1";
    private static final String VERIFICATION_PARAMETERS_2 = "verification parameters 2";
    private static final String OMID = "omid";
    private static final String NOT_OMID = "not omid";

    private static final String XML_TEMPLATE = "<Parent>\n" +
            "       <AdVerifications>\n" +
            "%s%s%s" +
            "       </AdVerifications>\n" +
            "   </Parent>\n";


    private static final String VERIFICATION_NODE_1 =
            "           <Verification vendor='" + VENDOR_1 + "'>\n" +
                    "               <JavaScriptResource apiFramework='" + OMID + "' browserOptional='true'>\n" +
                    "                   <![CDATA[" + JS_URL_1 + "]]>\n" +
                    "               </JavaScriptResource>\n" +
                    "               <TrackingEvents>\n" +
                    "                   <Tracking event='verificationNotExecuted'>\n" +
                    "                       <![CDATA[" + VERIFICATION_NOT_EXECUTED_1 + "]]>\n" +
                    "                   </Tracking>\n" +
                    "               </TrackingEvents>\n" +
                    "               <VerificationParameters>\n" +
                    "                   <![CDATA[" + VERIFICATION_PARAMETERS_1 + "]]>\n" +
                    "               </VerificationParameters>\n" +
                    "           </Verification>";

    private static final String VERIFICATION_NODE_2 =
            "           <Verification vendor='" + VENDOR_2 + "'>\n" +
                    "               <JavaScriptResource apiFramework='" + OMID + "' browserOptional='true'>\n" +
                    "                   <![CDATA[" + JS_URL_2 + "]]>\n" +
                    "               </JavaScriptResource>\n" +
                    "               <TrackingEvents>\n" +
                    "                   <Tracking event='verificationNotExecuted'>\n" +
                    "                       <![CDATA[" + VERIFICATION_NOT_EXECUTED_2 + "]]>\n" +
                    "                   </Tracking>\n" +
                    "               </TrackingEvents>\n" +
                    "               <VerificationParameters>\n" +
                    "                   <![CDATA[" + VERIFICATION_PARAMETERS_2 + "]]>\n" +
                    "               </VerificationParameters>\n" +
                    "           </Verification>\n";

    private static final String VERIFICATION_NODE_WRONG_API =
            "           <Verification vendor='" + VENDOR_2 + "'>\n" +
                    "               <JavaScriptResource apiFramework='" + NOT_OMID + "' browserOptional='true'>\n" +
                    "                   <![CDATA[" + JS_URL_2 + "]]>\n" +
                    "               </JavaScriptResource>\n" +
                    "               <TrackingEvents>\n" +
                    "                   <Tracking event='verificationNotExecuted'>\n" +
                    "                       <![CDATA[" + VERIFICATION_NOT_EXECUTED_2 + "]]>\n" +
                    "                   </Tracking>\n" +
                    "               </TrackingEvents>\n" +
                    "               <VerificationParameters>\n" +
                    "                   <![CDATA[" + VERIFICATION_PARAMETERS_2 + "]]>\n" +
                    "               </VerificationParameters>\n" +
                    "           </Verification>";

    @Test
    public void constructor_emptyParent_returnsEmptyList() throws Exception {
        String PARENT_EMPTY = "<Parent> </Parent>\n";
        final Node parent = createNode(PARENT_EMPTY);
        final VastAdVerificationsParser subject = new VastAdVerificationsParser(parent);

        final Set<ViewabilityVendor> vendors = subject.getViewabilityVendors();
        assertNotNull(vendors);
        assertThat(vendors.size()).isEqualTo(0);
    }

    @Test
    public void constructor_emptyAdVerificationsNode_returnsEmptyList() throws Exception {
        final String xmlString = prepareXmlString(XML_TEMPLATE, "", "", "");
        final Node parent = createNode(xmlString);
        final VastAdVerificationsParser subject = new VastAdVerificationsParser(parent);

        final Set<ViewabilityVendor> vendors = subject.getViewabilityVendors();
        assertNotNull(vendors);
        assertThat(vendors.size()).isEqualTo(0);
    }

    @Test
    public void constructor_singleValidNode_returnsCorrectViewabilityVendor() throws Exception {
        final String xmlString = prepareXmlString(XML_TEMPLATE, VERIFICATION_NODE_1, "", "");
        final Node parent = createNode(xmlString);
        final VastAdVerificationsParser subject = new VastAdVerificationsParser(parent);

        final Set<ViewabilityVendor> vendors = subject.getViewabilityVendors();

        assertNotNull(vendors);
        assertThat(vendors.size()).isEqualTo(1);
        assertTrue(vendors.contains(createVendor1()));
    }

    @Test
    public void constructor_withSingleInvalidNode_returnsEmptyList() throws Exception {
        final String xmlString = prepareXmlString(XML_TEMPLATE, VERIFICATION_NODE_WRONG_API, "", "");
        final Node parent = createNode(xmlString);
        final VastAdVerificationsParser subject = new VastAdVerificationsParser(parent);

        final Set<ViewabilityVendor> vendors = subject.getViewabilityVendors();

        assertNotNull(vendors);
        assertThat(vendors.size()).isEqualTo(0);
    }

    @Test
    public void constructor_withTwoNodes_returnsListWithTwoElements() throws Exception {
        final String xmlString = prepareXmlString(XML_TEMPLATE, VERIFICATION_NODE_1, VERIFICATION_NODE_2, "");
        final Node parent = createNode(xmlString);
        final VastAdVerificationsParser subject = new VastAdVerificationsParser(parent);

        final Set<ViewabilityVendor> vendors = subject.getViewabilityVendors();

        assertNotNull(vendors);
        assertThat(vendors.size()).isEqualTo(2);
        assertTrue(vendors.contains(createVendor1()));
        assertTrue(vendors.contains(createVendor2()));
    }

    @Test
    public void constructor_withTwoNodesAndOneInvalidNode_returnsListWithTwoElements() throws Exception {
        final String xmlString = prepareXmlString(XML_TEMPLATE, VERIFICATION_NODE_1, VERIFICATION_NODE_2, VERIFICATION_NODE_WRONG_API);
        final Node parent = createNode(xmlString);
        final VastAdVerificationsParser subject = new VastAdVerificationsParser(parent);

        final Set<ViewabilityVendor> vendors = subject.getViewabilityVendors();

        assertNotNull(vendors);
        assertThat(vendors.size()).isEqualTo(2);
        assertTrue(vendors.contains(createVendor1()));
        assertTrue(vendors.contains(createVendor2()));
    }

    @Test
    public void constructor_withDuplicateNodes_returnsListWithOneElements() throws Exception {
        final String xmlString = prepareXmlString(XML_TEMPLATE, VERIFICATION_NODE_1, VERIFICATION_NODE_1, "");
        final Node parent = createNode(xmlString);
        final VastAdVerificationsParser subject = new VastAdVerificationsParser(parent);

        final Set<ViewabilityVendor> vendors = subject.getViewabilityVendors();

        assertNotNull(vendors);
        assertThat(vendors.size()).isEqualTo(1);
        assertTrue(vendors.contains(createVendor1()));
    }

    // helper functions
    private String prepareXmlString(@NonNull final String format, String... args) {
        return String.format(format, (Object[]) args);
    }

    private static ViewabilityVendor createVendor1() {
        return new ViewabilityVendor.Builder(JS_URL_1)
                .withVendorKey(VENDOR_1)
                .withVerificationParameters(VERIFICATION_PARAMETERS_1)
                .withVerificationNotExecuted(VERIFICATION_NOT_EXECUTED_1)
                .withApiFramework(OMID).build();
    }

    private static ViewabilityVendor createVendor2() {
        return new ViewabilityVendor.Builder(JS_URL_2)
                .withVendorKey(VENDOR_2)
                .withVerificationParameters(VERIFICATION_PARAMETERS_2)
                .withVerificationNotExecuted(VERIFICATION_NOT_EXECUTED_2)
                .withApiFramework(OMID).build();
    }
}
