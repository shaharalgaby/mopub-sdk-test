// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads

import com.mopub.common.test.support.SdkTestRunner
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(SdkTestRunner::class)
class EndCardTypeTest {

    @Test
    fun `fromVastResourceType with HTML_RESOURCE should return INTERACTIVE`() {
        assertEquals(
            EndCardType.INTERACTIVE,
            EndCardType.fromVastResourceType(VastResource.Type.HTML_RESOURCE)
        )
    }

    @Test
    fun `fromVastResourceType with STATIC_RESOURCE should return STATIC`() {
        assertEquals(
            EndCardType.STATIC,
            EndCardType.fromVastResourceType(VastResource.Type.STATIC_RESOURCE)
        )
    }

    @Test
    fun `fromVastResourceType with IFRAME_RESOURCE should return STATIC`() {
        assertEquals(
            EndCardType.STATIC,
            EndCardType.fromVastResourceType(VastResource.Type.IFRAME_RESOURCE)
        )
    }

    @Test
    fun `fromVastResourceType with BLURRED_LAST_FRAME should return NONE`() {
        assertEquals(
            EndCardType.NONE,
            EndCardType.fromVastResourceType(VastResource.Type.BLURRED_LAST_FRAME)
        )
    }

    @Test
    fun `fromVastResourceType with null should return null`() {
        assertNull(EndCardType.fromVastResourceType(null))
    }
}
