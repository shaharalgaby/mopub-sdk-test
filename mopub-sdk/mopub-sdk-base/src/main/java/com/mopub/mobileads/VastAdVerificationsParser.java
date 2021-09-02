// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.ViewabilityVendor;
import com.mopub.mobileads.util.XmlUtils;

import org.w3c.dom.Node;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/*
 * Parsing of the AdVerifications XML Nodes
 *
 *             <AdVerifications>
 *                 <Verification vendor=\"name.com-omid\">
 *                     <JavaScriptResource apiFramework=\"omid\" browserOptional=\"true\">
 *                         <![CDATA[ https://cdn.name.com/dvtp_src.js ]]>
 *                     </JavaScriptResource>
 *                     <TrackingEvents>
 *                         <Tracking event=\"verificationNotExecuted\">
 *                             <![CDATA[\r\nhttps://tps.name.com/visit.jpg?verr=[REASON]&tagtype=video&dvtagver=6.1.img&ctx=818052&cmp=DV064005&sid=123&plc=verificationRejection&advid=818053&crt=omidVerificationNotExecuted&vasttrkevt=impression&dvp_ctx=13337537&dvp_cmp=DV330341&dvp_sid=mopub&dvp_plc=Android-Video-VAST&dvp_advid=3819603&adsrv=189&DVP_PP_BUNDLE_ID=%25%25BUNDLE%25%25&DVP_PP_APP_ID=%25%25PLACEMENTID%25%25&DVP_PP_APP_NAME=%25%25APPNAME%25%25&DVP_MP_2=%25%25PUBID%25%25&DVP_MP_3=%25%25ADUNITID%25%25&DVP_MP_4=%25%25ADGROUPID%25%25&DVPX_PP_IMP_ID=%25%25REQUESTID%25%25&DVP_PP_AUCTION_IP=%25%25IPADDRESS%25%25&DVPX_PP_AUCTION_UA=%25%25USERAGENT%25%25&dup=70185121-e8c2-4962-bf0a-fb12e5679d9f&dvp_zjsver=0.20.9&dvp_vastetrk=1&sblk=1&dvp_sblkts=1588341149396&dvp_sblkst=err&dvp_vastv=4.0-r\r\n]]>
 *                         </Tracking>
 *                     </TrackingEvents>
 *                     <VerificationParameters>
 *                         <![CDATA[\r\ntagtype=video&dvtagver=6.1.src&msrapi=jsOmid&ctx=13337537&cmp=DV330341&sid=mopub&plc=Android-Video-VAST&advid=3819603&adsrv=189&DVP_PP_BUNDLE_ID=%25%25BUNDLE%25%25&DVP_PP_APP_ID=%25%25PLACEMENTID%25%25&DVP_PP_APP_NAME=%25%25APPNAME%25%25&DVP_MP_2=%25%25PUBID%25%25&DVP_MP_3=%25%25ADUNITID%25%25&DVP_MP_4=%25%25ADGROUPID%25%25&DVPX_PP_IMP_ID=%25%25REQUESTID%25%25&DVP_PP_AUCTION_IP=%25%25IPADDRESS%25%25&DVPX_PP_AUCTION_UA=%25%25USERAGENT%25%25&dup=70185121-e8c2-4962-bf0a-fb12e5679d9f&dvp_zjsver=0.20.9&dvp_vastetrk=1&sblk=1&dvp_sblkts=1588341149396&dvp_sblkst=err&dvp_vastv=4.0-r\r\n]]>
 *                     </VerificationParameters>
 *                 </Verification>
 *             </AdVerifications>
 */
class VastAdVerificationsParser {
    private static final String AD_VERIFICATIONS = "AdVerifications";
    private static final String VERIFICATION = "Verification";
    private static final String VENDOR = "vendor";
    private static final String JAVASCRIPT_RESOURCE = "JavaScriptResource";
    private static final String TRACKING_EVENTS = "TrackingEvents";
    private static final String EVENT = "event";
    private static final String VERIFICATION_PARAMETERS = "VerificationParameters";
    private static final String TRACKING = "Tracking";
    private static final String API_FRAMEWORK = "apiFramework";
    private static final String OMID = "omid";
    private static final String VERIFICATION_NOT_EXECUTED = "verificationNotExecuted";

    @NonNull
    private final Set<ViewabilityVendor> viewabilityVendors;

    /**
     * @param adVerificationsParent XML node parent to the AdVerifications node
     */
    VastAdVerificationsParser(@Nullable final Node adVerificationsParent) {
        viewabilityVendors = new HashSet<>();
        if (adVerificationsParent != null) {
            parse(adVerificationsParent);
        }
    }

    private void parse(@NonNull final Node adVerificationParent) {
        final Node adVerifications = XmlUtils.getFirstMatchingChildNode(adVerificationParent, AD_VERIFICATIONS);

        final List<Node> verificationNodes = XmlUtils.getMatchingChildNodes(adVerifications, VERIFICATION);
        if (verificationNodes == null || verificationNodes.isEmpty()) {
            return;
        }

        for (Node verification : verificationNodes) {
            final Node javascriptNode = XmlUtils.getFirstMatchingChildNode(verification,
                    JAVASCRIPT_RESOURCE, API_FRAMEWORK, Collections.singletonList(OMID));

            if (javascriptNode != null) {
                final Node trackingEventsNode = XmlUtils.getFirstMatchingChildNode(verification, TRACKING_EVENTS);
                final Node notExecutedNode = XmlUtils.getFirstMatchingChildNode(trackingEventsNode, TRACKING,
                        EVENT, Collections.singletonList(VERIFICATION_NOT_EXECUTED));
                final Node parametersNode = XmlUtils.getFirstMatchingChildNode(verification, VERIFICATION_PARAMETERS);

                final String javascriptResource = XmlUtils.getNodeValue(javascriptNode);

                final ViewabilityVendor.Builder builder = new ViewabilityVendor.Builder(javascriptResource);
                builder.withApiFramework(OMID)
                        .withVendorKey(XmlUtils.getAttributeValue(verification, VENDOR))
                        .withVerificationParameters(XmlUtils.getNodeValue(parametersNode))
                        .withVerificationNotExecuted(XmlUtils.getNodeValue(notExecutedNode));

                final ViewabilityVendor viewabilityVendor = builder.build();
                if (viewabilityVendor != null) {
                    viewabilityVendors.add(viewabilityVendor);
                }
            }
        }
    }

    @NonNull
    Set<ViewabilityVendor> getViewabilityVendors() {
        return viewabilityVendors;
    }
}
