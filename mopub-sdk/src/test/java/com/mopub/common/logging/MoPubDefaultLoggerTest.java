// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common.logging;

import com.mopub.common.test.support.SdkTestRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(SdkTestRunner.class)
public class MoPubDefaultLoggerTest {

    private static final String SHORT_MESSAGE = "short message.";
    private static String LONG_MESSAGE;

    static {
        final StringBuilder sb = new StringBuilder();
        // 9990 bytes
        for (int i = 0; i < 999; i++) {
            sb.append("0123456789");
        }
        LONG_MESSAGE = sb.toString();
    }

    @Test
    public void split_withShortMessage_shouldReturnArrayWithOneElement() {
        final String[] result = MoPubDefaultLogger.split(SHORT_MESSAGE);

        assertThat(result).isEqualTo(new String[]{SHORT_MESSAGE});
    }

    @Test
    public void split_withLongMessage_shouldReturnArrayWithProperSegments() {
        final String[] result = MoPubDefaultLogger.split(LONG_MESSAGE);

        assertThat(result.length).isEqualTo(
                (LONG_MESSAGE.length() / MoPubDefaultLogger.MAX_MESSAGE_LENGTH_BYTES) + 1);
        final StringBuilder resultString = new StringBuilder();
        for (String segment : result) {
            resultString.append(segment);
        }
        assertThat(resultString.toString()).isEqualTo(LONG_MESSAGE);
    }

    @Test
    public void split_withEmptyMessage_shouldReturnArrayWithEmptyString() {
        final String[] result = MoPubDefaultLogger.split("");

        assertThat(result).isEqualTo(new String[]{""});
    }

    @Test
    public void split_withNullMessage_shouldReturnArrayWithNullString() {
        final String[] result = MoPubDefaultLogger.split(null);

        assertThat(result).isEqualTo(new String[]{null});
    }
}
