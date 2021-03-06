/*
 * Copyright 2018 Expedia, Inc.
 *
 *       Licensed under the Apache License, Version 2.0 (the "License");
 *       you may not use this file except in compliance with the License.
 *       You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *       Unless required by applicable law or agreed to in writing, software
 *       distributed under the License is distributed on an "AS IS" BASIS,
 *       WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *       See the License for the specific language governing permissions and
 *       limitations under the License.
 *
 */
package com.expedia.www.haystack.commons.secretDetector.span;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

import java.time.Clock;

import static com.expedia.www.haystack.commons.secretDetector.span.SpanNameAndCountRecorder.CONFIDENTIAL_DATA_LOCATIONS_SIZE;
import static com.expedia.www.haystack.commons.secretDetector.span.SpanNameAndCountRecorder.ONE_HOUR;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings("MagicNumber")
@RunWith(MockitoJUnitRunner.class)
public class SpanNameAndCountRecorderTest {
    // All of the *_NAMES arrays are purposely out of sorted order to verify that the SpanNameAndCountRecorder.toString() sorts
    private static final String F1 = "FinderName1";
    private static final String F2 = "FinderName2";
    private static final String[] FINDER_NAMES = {F2, F1};
    private static final String S1 = "ServiceName1";
    private static final String S2 = "ServiceName2";
    private static final String[] SERVICE_NAMES = {S2, S1};
    private static final String O1 = "OperationName1";
    private static final String O2 = "OperationName2";
    private static final String[] OPERATION_NAMES = {O2, O1};
    private static final String T1 = "TagName1";
    private static final String T2 = "TagName2";
    private static final String[] TAG_NAMES = {T2, T1};
    private static final String S = "%s;%s;%s;%s=%d";
    private static final String S16 = "[%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s]";
    private static final String FORMAT = String.format(S16, S, S, S, S, S, S, S, S, S, S, S, S, S, S, S, S);

    @Mock
    private Logger mockLogger;

    @Mock
    private Clock mockClock;

    private SpanNameAndCountRecorder spanNameAndCountRecorder;

    private int countOfAddCalls;

    @SuppressWarnings("MethodWithMultipleLoops")
    @Before
    public void setUp() {
        spanNameAndCountRecorder = new SpanNameAndCountRecorder(mockLogger, mockClock);
        int count = 0;
        for (String finderName : FINDER_NAMES) {
            for (String serviceName : SERVICE_NAMES) {
                for (String operationName : OPERATION_NAMES) {
                    for (String tagKey : TAG_NAMES) {
                        count++;
                        for (int i = 0; i < count; i++) {
                            add(finderName, serviceName, operationName, tagKey);
                        }
                    }
                }
            }
        }
    }

    @After
    public void tearDown() {
        verifyNoMoreInteractions(mockLogger, mockClock);
    }

    @Test
    public void testToString() {
        final String expected = String.format(FORMAT,
                F1, S1, O1, T1, 16,
                F1, S1, O1, T2, 15,
                F1, S1, O2, T1, 14,
                F1, S1, O2, T2, 13,
                F1, S2, O1, T1, 12,
                F1, S2, O1, T2, 11,
                F1, S2, O2, T1, 10,
                F1, S2, O2, T2, 9,
                F2, S1, O1, T1, 8,
                F2, S1, O1, T2, 7,
                F2, S1, O2, T1, 6,
                F2, S1, O2, T2, 5,
                F2, S2, O1, T1, 4,
                F2, S2, O1, T2, 3,
                F2, S2, O2, T1, 2,
                F2, S2, O2, T2, 1);
        assertEquals(expected, spanNameAndCountRecorder.toString());
        verify(mockClock, times(countOfAddCalls)).millis();
    }

    @Test
    public void testClearBecauseOneHourHasPassed() {
        when(mockClock.millis()).thenReturn(ONE_HOUR + 1);
        add(F1, S1, O1, T1);
        verify(mockClock, times(countOfAddCalls)).millis();
        verify(mockLogger).info(String.format(CONFIDENTIAL_DATA_LOCATIONS_SIZE, 16));
    }

    @Test
    public void testLogIfTimeToLogAddAndGetOneHour() {
        final long now = System.currentTimeMillis();
        when(mockClock.millis()).thenReturn(now + ONE_HOUR, now + 1 + (2 * ONE_HOUR));
        add(F1, S1, O1, T1);
        verify(mockLogger).info(String.format(CONFIDENTIAL_DATA_LOCATIONS_SIZE, 16));
        add(F1, S1, O1, T1);
        verify(mockLogger, times(2)).info(String.format(CONFIDENTIAL_DATA_LOCATIONS_SIZE, 16));
        verify(mockClock, times(countOfAddCalls)).millis();
    }

    @Test
    public void testLogIfTimeToLogInnerCheckNotTimeYet() {
        spanNameAndCountRecorder.logIfTimeToLogInnerCheck(0L);
        verify(mockClock, times(countOfAddCalls)).millis();
    }

    private void add(String finderName, String serviceName, String operationName, String tagKey) {
        spanNameAndCountRecorder.add(finderName, serviceName, operationName, tagKey);
        ++countOfAddCalls;
    }

}
