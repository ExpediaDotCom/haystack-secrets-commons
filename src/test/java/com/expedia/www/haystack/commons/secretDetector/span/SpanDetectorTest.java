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

import com.expedia.www.haystack.commons.secretDetector.DetectorTestBase;
import com.expedia.www.haystack.commons.secretDetector.FinderNameAndServiceName;
import com.expedia.www.haystack.commons.secretDetector.HaystackFinderEngine;
import com.expedia.www.haystack.commons.secretDetector.NonLocalIpV4AddressFinder;
import com.expedia.www.haystack.commons.secretDetector.TestConstantsAndCommonCode;
import com.expedia.www.haystack.commons.secretDetector.span.SpanDetector.Factory;
import com.netflix.servo.monitor.Counter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.expedia.www.haystack.commons.secretDetector.TestConstantsAndCommonCode.BYTES_FIELD_KEY;
import static com.expedia.www.haystack.commons.secretDetector.TestConstantsAndCommonCode.BYTES_TAG_KEY;
import static com.expedia.www.haystack.commons.secretDetector.TestConstantsAndCommonCode.CREDIT_CARD_LOG_SPAN;
import static com.expedia.www.haystack.commons.secretDetector.TestConstantsAndCommonCode.EMAIL_ADDRESS_IN_TAG_BYTES_AND_LOG_BYTES_SPAN;
import static com.expedia.www.haystack.commons.secretDetector.TestConstantsAndCommonCode.EMAIL_ADDRESS_LOG_SPAN;
import static com.expedia.www.haystack.commons.secretDetector.TestConstantsAndCommonCode.EMAIL_ADDRESS_LOG_SPAN_TAG_AND_VBYTES;
import static com.expedia.www.haystack.commons.secretDetector.TestConstantsAndCommonCode.EMAIL_ADDRESS_SPAN;
import static com.expedia.www.haystack.commons.secretDetector.TestConstantsAndCommonCode.FULLY_POPULATED_SPAN;
import static com.expedia.www.haystack.commons.secretDetector.TestConstantsAndCommonCode.IP_ADDRESS;
import static com.expedia.www.haystack.commons.secretDetector.TestConstantsAndCommonCode.IP_ADDRESS_SPAN;
import static com.expedia.www.haystack.commons.secretDetector.TestConstantsAndCommonCode.OPERATION_NAME;
import static com.expedia.www.haystack.commons.secretDetector.TestConstantsAndCommonCode.RANDOM;
import static com.expedia.www.haystack.commons.secretDetector.TestConstantsAndCommonCode.STRING_FIELD_KEY;
import static com.expedia.www.haystack.commons.secretDetector.TestConstantsAndCommonCode.STRING_TAG_KEY;
import static com.expedia.www.haystack.commons.secretDetector.span.SpanDetector.COUNTER_NAME;
import static com.expedia.www.haystack.commons.secretDetector.span.SpanDetector.ERRORS_METRIC_GROUP;
import static com.expedia.www.haystack.commons.secretDetector.span.SpanDetector.FINDERS_TO_LOG;
import static com.expedia.www.haystack.commons.secretDetector.span.SpanDetector.getEmailText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SpanDetectorTest extends DetectorTestBase {
    private static final String BUCKET = RANDOM.nextLong() + "BUCKET";
    private static final String FINDER_NAME = RANDOM.nextLong() + "FINDER_NAME";
    private static final String SERVICE_NAME = RANDOM.nextLong() + "SERVICE_NAME";
    private static final String CREDIT_CARD_FINDER_NAME = "Credit_Card";
    private static final String CREDIT_CARD_FINDER_NAME_IN_FINDERS_DEFAULT_DOT_XML = CREDIT_CARD_FINDER_NAME;
    private static final String EMAIL_FINDER_NAME_IN_FINDERS_DEFAULT_DOT_XML = "Email";
    private static final FinderNameAndServiceName FINDER_NAME_AND_SERVICE_NAME
            = new FinderNameAndServiceName(FINDER_NAME, SERVICE_NAME);

    @Mock
    private Logger mockLogger;

    @Mock
    private Factory mockFactory;

    @Mock
    private Counter mockCounter;

    @Mock
    private SpanS3ConfigFetcher mockSpanS3ConfigFetcher;

    private SpanDetector spanDetector;
    private Factory factory;

    @Before
    public void setUp() {
        final HaystackFinderEngine haystackFinderEngine =
                new HaystackFinderEngine(mockMetricObjects, SUBSYSTEM, APPLICATION);
        spanDetector = new SpanDetector(
                mockLogger, haystackFinderEngine, mockFactory, mockSpanS3ConfigFetcher, APPLICATION);
        factory = new Factory(mockMetricObjects);
    }

    @After
    public void tearDown() {
        SpanDetector.COUNTERS.clear();
        verifyNoMoreInteractions(mockLogger);
        verifyNoMoreInteractions(mockFactory);
        verifyNoMoreInteractions(mockCounter);
        verifyNoMoreInteractions(mockSpanS3ConfigFetcher);
    }

    @Test
    public void testSmallConstructor() {
        new SpanDetector(BUCKET, SUBSYSTEM, APPLICATION);
    }

    @Test
    public void testFindSecretsHaystackEmailAddress() {
        whensForFindSecrets();
        when(mockFactory.createCounter(any(), anyString())).thenReturn(mockCounter);

        final Map<String, List<String>> secrets = spanDetector.findSecrets(EMAIL_ADDRESS_SPAN);

        verifyHaystackEmailAddressFound(secrets, STRING_TAG_KEY);
        verifiesForFindSecrets(40, 1);
    }

    @Test
    public void testFindSecretsHaystackEmailAddressInTagBytes() {
        whensForFindSecrets();
        when(mockFactory.createCounter(any(), anyString())).thenReturn(mockCounter);

        final Map<String, List<String>> secrets = spanDetector.findSecrets(EMAIL_ADDRESS_IN_TAG_BYTES_AND_LOG_BYTES_SPAN);

        assertEquals(1, secrets.size());
        final List<String> tagsThatContainEmails = secrets.get(EMAIL_FINDER_NAME_IN_FINDERS_DEFAULT_DOT_XML);
        assertEquals(2, tagsThatContainEmails.size());
        final Iterator<String> iterator = tagsThatContainEmails.iterator();
        assertEquals(BYTES_TAG_KEY, iterator.next());
        assertEquals(BYTES_FIELD_KEY, iterator.next());
        verifiesForFindSecrets(28, 1);
    }

    @Test
    public void testFindSecretsHaystackEmailAddressInLog() {
        whensForFindSecrets();
        when(mockFactory.createCounter(any(), anyString())).thenReturn(mockCounter);

        final Map<String, List<String>> secrets = spanDetector.findSecrets(EMAIL_ADDRESS_LOG_SPAN_TAG_AND_VBYTES);

        verifyHaystackEmailAddressFound(secrets, BYTES_TAG_KEY, STRING_FIELD_KEY, BYTES_FIELD_KEY);
        verifiesForFindSecrets(16, 1);
    }

    private static void verifyHaystackEmailAddressFound(Map<String, List<String>> secrets, String... keysOfSecret) {
        assertEquals(1, secrets.size());
        assertEquals(EMAIL_FINDER_NAME_IN_FINDERS_DEFAULT_DOT_XML, secrets.keySet().iterator().next());
        final List<String> strings = secrets.get(EMAIL_FINDER_NAME_IN_FINDERS_DEFAULT_DOT_XML);
        assertEquals(keysOfSecret.length, strings.size());
        final Iterator<String> iterator = strings.iterator();
        for (String keyOfSecret : keysOfSecret) {
            assertEquals(keyOfSecret, iterator.next());
        }
    }

    @Test
    public void testFindSecretsIpAddress() {
        whensForFindSecrets();
        when(mockFactory.createCounter(any(), anyString())).thenReturn(mockCounter);
        final Map<String, List<String>> secrets = spanDetector.findSecrets(IP_ADDRESS_SPAN);

        assertEquals(IP_ADDRESS + " should have been flagged as a secret", 1, secrets.size());
        assertEquals(NonLocalIpV4AddressFinder.FINDER_NAME, secrets.keySet().iterator().next());
        assertEquals(STRING_TAG_KEY, secrets.get(NonLocalIpV4AddressFinder.FINDER_NAME).iterator().next());
        verifiesForFindSecrets(42, 1);
    }

    @Test
    public void testFindSecretsNoSecret() {
        whensForFindSecrets();

        assertTrue(spanDetector.findSecrets(FULLY_POPULATED_SPAN).isEmpty());

        verifiesForFindSecrets(52, 1);
    }

    @Test
    public void testApplyNoSecret() {
        whensForFindSecrets();

        final Iterable<String> iterable = spanDetector.apply(FULLY_POPULATED_SPAN);

        assertFalse(iterable.iterator().hasNext());
        verifiesForFindSecrets(52, 1);
    }

    @Test
    public void testApplyCreditCardInLog() {
        whensForFindSecrets();
        when(mockFactory.createCounter(any(), anyString())).thenReturn(mockCounter);
        when(mockSpanS3ConfigFetcher.isInWhiteList(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(false);
        final Iterator<String> iterator = spanDetector.apply(CREDIT_CARD_LOG_SPAN).iterator();

        final String emailText = getEmailText(
                CREDIT_CARD_LOG_SPAN, Collections.singletonMap(CREDIT_CARD_FINDER_NAME_IN_FINDERS_DEFAULT_DOT_XML,
                        Collections.singletonList(STRING_FIELD_KEY)));
        assertEquals(emailText, iterator.next());
        assertFalse(iterator.hasNext());
        verify(mockSpanS3ConfigFetcher).isInWhiteList(
                CREDIT_CARD_FINDER_NAME, TestConstantsAndCommonCode.SERVICE_NAME, OPERATION_NAME, STRING_FIELD_KEY);
        if (FINDERS_TO_LOG.contains(CREDIT_CARD_FINDER_NAME)) {
            verify(mockLogger).info(emailText);
        }
        verifyCounterIncrement();
        verifiesForFindSecrets(41, 1);
    }

    @Test
    public void testApplyEMailAddressInLog() {
        whensForFindSecrets();
        when(mockFactory.createCounter(any(), anyString())).thenReturn(mockCounter);
        when(mockSpanS3ConfigFetcher.isInWhiteList(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(true, false);
        for (int i = 0; i < 2; i++) {
            final Iterator<String> iterator = spanDetector.apply(EMAIL_ADDRESS_LOG_SPAN).iterator();
            final String emailText = getEmailText(
                    EMAIL_ADDRESS_LOG_SPAN, Collections.singletonMap(
                            EMAIL_FINDER_NAME_IN_FINDERS_DEFAULT_DOT_XML, Collections.singletonList(STRING_FIELD_KEY)));
            if (i > 0) {
                assertEquals(emailText, iterator.next());
            }
            assertFalse(iterator.hasNext());
        }
        verify(mockSpanS3ConfigFetcher, times(2)).isInWhiteList(
                "Email", TestConstantsAndCommonCode.SERVICE_NAME, OPERATION_NAME, STRING_FIELD_KEY);
        FinderNameAndServiceName finderNameAndServiceName =
                new FinderNameAndServiceName("Email", TestConstantsAndCommonCode.SERVICE_NAME);
        verify(mockFactory).createCounter(finderNameAndServiceName, APPLICATION);
        verify(mockCounter).increment();
        verifiesForFindSecrets(80, 1);
    }

    private void verifyCounterIncrement() {
        final FinderNameAndServiceName finderAndServiceName =
                new FinderNameAndServiceName(CREDIT_CARD_FINDER_NAME_IN_FINDERS_DEFAULT_DOT_XML,
                        TestConstantsAndCommonCode.SERVICE_NAME);
        verify(mockFactory).createCounter(finderAndServiceName, APPLICATION);
        verify(mockCounter, times(1)).increment();
    }

    @Test
    public void testFactoryCreateCounter() {
        when(mockMetricObjects.createAndRegisterResettingCounter(
                anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(mockCounter);

        final Counter counter = factory.createCounter(FINDER_NAME_AND_SERVICE_NAME, APPLICATION);

        assertSame(mockCounter, counter);
        verify(mockMetricObjects).createAndRegisterResettingCounter(
                ERRORS_METRIC_GROUP, APPLICATION, FINDER_NAME, SERVICE_NAME, COUNTER_NAME);
    }
}
