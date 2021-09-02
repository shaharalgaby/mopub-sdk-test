// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common;

import org.junit.Test;

import static org.fest.assertions.api.Assertions.assertThat;

public class CreativeOrientationTest {

    @Test
    public void fromString_nullParam_shouldBeDevice() {
        assertThat(CreativeOrientation.fromString(null)).isEqualTo(CreativeOrientation.DEVICE);
    }

    @Test
    public void fromString_emptyParam_shouldBeDevice() {
        assertThat(CreativeOrientation.fromString("")).isEqualTo(CreativeOrientation.DEVICE);
    }

    @Test
    public void fromString_withGarbage_shouldBeDevice() {
        assertThat(CreativeOrientation.fromString("p0rtr41t")).isEqualTo(CreativeOrientation.DEVICE);
    }

    @Test
    public void fromString_lParam_shouldBeLandscape() {
        assertThat(CreativeOrientation.fromString("l")).isEqualTo(CreativeOrientation.LANDSCAPE);
    }

    @Test
    public void fromString_uppercaseL_shouldBeLandscape() {
        assertThat(CreativeOrientation.fromString("L")).isEqualTo(CreativeOrientation.LANDSCAPE);
    }

    @Test
    public void fromString_pParam_shouldBePortrait() {
        assertThat(CreativeOrientation.fromString("p")).isEqualTo(CreativeOrientation.PORTRAIT);
    }

    @Test
    public void fromString_uppercaseP_shouldBePortrait() {
        assertThat(CreativeOrientation.fromString("P")).isEqualTo(CreativeOrientation.PORTRAIT);
    }

    @Test
    public void fromString_dParam_shouldBeDevice() {
        assertThat(CreativeOrientation.fromString("d")).isEqualTo(CreativeOrientation.DEVICE);
    }

    @Test
    public void fromString_uppercaseD_shouldBeDevice() {
        assertThat(CreativeOrientation.fromString("D")).isEqualTo(CreativeOrientation.DEVICE);
    }
}
