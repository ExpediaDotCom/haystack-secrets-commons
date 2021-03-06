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

import com.expedia.open.tracing.Log;
import com.expedia.open.tracing.Span;
import com.expedia.open.tracing.Tag;
import com.expedia.www.haystack.commons.secretDetector.DetectorTestBase;
import com.expedia.www.haystack.commons.secretDetector.FinderNameAndServiceName;
import com.expedia.www.haystack.commons.secretDetector.HaystackFinderEngine;
import com.expedia.www.haystack.commons.secretDetector.NonLocalIpV4AddressFinder;
import com.expedia.www.haystack.commons.secretDetector.span.SpanSecretMasker.Factory;
import com.netflix.servo.monitor.Counter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

import java.util.List;

import static com.expedia.www.haystack.commons.secretDetector.TestConstantsAndCommonCode.BYTES_FIELD_KEY;
import static com.expedia.www.haystack.commons.secretDetector.TestConstantsAndCommonCode.BYTES_TAG_KEY;
import static com.expedia.www.haystack.commons.secretDetector.TestConstantsAndCommonCode.EMAIL_ADDRESSES_AND_IP_ADDRESS_SPAN;
import static com.expedia.www.haystack.commons.secretDetector.TestConstantsAndCommonCode.EMAIL_ADDRESS_IN_TAG_BYTES_AND_LOG_BYTES_SPAN;
import static com.expedia.www.haystack.commons.secretDetector.TestConstantsAndCommonCode.EMAIL_ADDRESS_LOG_SPAN;
import static com.expedia.www.haystack.commons.secretDetector.TestConstantsAndCommonCode.EMAIL_ADDRESS_LOG_SPAN_TAG_AND_VBYTES;
import static com.expedia.www.haystack.commons.secretDetector.TestConstantsAndCommonCode.EMAIL_ADDRESS_SPAN;
import static com.expedia.www.haystack.commons.secretDetector.TestConstantsAndCommonCode.FULLY_POPULATED_SPAN;
import static com.expedia.www.haystack.commons.secretDetector.TestConstantsAndCommonCode.OPERATION_NAME;
import static com.expedia.www.haystack.commons.secretDetector.TestConstantsAndCommonCode.RANDOM;
import static com.expedia.www.haystack.commons.secretDetector.TestConstantsAndCommonCode.SERVICE_NAME;
import static com.expedia.www.haystack.commons.secretDetector.TestConstantsAndCommonCode.STRING_FIELD_KEY;
import static com.expedia.www.haystack.commons.secretDetector.TestConstantsAndCommonCode.STRING_TAG_KEY;
import static com.expedia.www.haystack.commons.secretDetector.span.SpanDetector.COUNTER_NAME;
import static com.expedia.www.haystack.commons.secretDetector.span.SpanDetector.ERRORS_METRIC_GROUP;
import static com.expedia.www.haystack.commons.secretDetector.span.SpanSecretMasker.MASKED_BY_HAYSTACK;
import static com.expedia.www.haystack.commons.secretDetector.span.SpanSecretMasker.MASKED_BY_HAYSTACK_BYTES;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings({"CallToSuspiciousStringMethod", "ConstantConditions"})
@RunWith(MockitoJUnitRunner.class)
public class SpanSecretMaskerTest extends DetectorTestBase {
    private static final String BUCKET = RANDOM.nextLong() + "BUCKET";
    private static final String EMAIL_FINDER_NAME_IN_FINDERS_DEFAULT_DOT_XML = "Email";
    private static final FinderNameAndServiceName EMAIL_FINDER_NAME_AND_SERVICE_NAME =
            new FinderNameAndServiceName(EMAIL_FINDER_NAME_IN_FINDERS_DEFAULT_DOT_XML, SERVICE_NAME);
    private static final FinderNameAndServiceName IPV4_FINDER_NAME_AND_SERVICE_NAME =
            new FinderNameAndServiceName(NonLocalIpV4AddressFinder.FINDER_NAME, SERVICE_NAME);

    @Mock
    private Logger mockLogger;

    @Mock
    private Factory mockFactory;

    @Mock
    private SpanS3ConfigFetcher mockSpanS3ConfigFetcher;

    @Mock
    private Counter mockCounter;

    @Mock
    private SpanNameAndCountRecorder mockSpanNameAndCountRecorder;

    private SpanSecretMasker spanSecretMasker;
    private Factory factory;

    @Before
    public void setUp() {
        final HaystackFinderEngine haystackFinderEngine =
                new HaystackFinderEngine(mockMetricObjects, SUBSYSTEM, APPLICATION);
        spanSecretMasker = new SpanSecretMasker(
                haystackFinderEngine, mockFactory, mockSpanS3ConfigFetcher, mockSpanNameAndCountRecorder, APPLICATION);
        factory = new Factory(mockMetricObjects);
    }

    @After
    public void tearDown() {
        SpanSecretMasker.COUNTERS.clear();
        verifyNoMoreInteractions(mockLogger);
        verifyNoMoreInteractions(mockFactory);
        verifyNoMoreInteractions(mockSpanS3ConfigFetcher);
        verifyNoMoreInteractions(mockCounter);
        verifyNoMoreInteractions(mockSpanNameAndCountRecorder);
    }

    @Test
    public void testSmallConstructor() {
        new SpanSecretMasker(BUCKET, SUBSYSTEM, APPLICATION);
    }

    @Test
    public void testApplyNoSecret() {
        whensForFindSecrets();

        final Span span = spanSecretMasker.apply(FULLY_POPULATED_SPAN);

        assertEquals(FULLY_POPULATED_SPAN, span);
        verifiesForFindSecrets(52, 1);
    }

    @Test
    public void testFindSecretsHaystackEmailAddressInTagString() {
        whensForFindSecrets();
        when(mockFactory.createCounter(any(), anyString())).thenReturn(mockCounter);

        final Span span = spanSecretMasker.apply(EMAIL_ADDRESS_SPAN);

        assertNotEquals(EMAIL_ADDRESS_SPAN, span);
        assertEquals(MASKED_BY_HAYSTACK, findTag(span, STRING_TAG_KEY).getVStr());
        verify(mockSpanS3ConfigFetcher).isInWhiteList(
                EMAIL_FINDER_NAME_IN_FINDERS_DEFAULT_DOT_XML, SERVICE_NAME, OPERATION_NAME, STRING_TAG_KEY);
        verify(mockFactory).createCounter(EMAIL_FINDER_NAME_AND_SERVICE_NAME, APPLICATION);
        verify(mockCounter).increment();
        verify(mockSpanNameAndCountRecorder).add(
                EMAIL_FINDER_NAME_IN_FINDERS_DEFAULT_DOT_XML, SERVICE_NAME, OPERATION_NAME, STRING_TAG_KEY);
        verifiesForFindSecrets(40, 1);
    }

    @Test
    public void testFindSecretsHaystackEmailAddressInTagStringTagWhitelisted() {
        whensForFindSecrets();
        when(mockSpanS3ConfigFetcher.isInWhiteList(Matchers.<String>anyVararg())).thenReturn(true);
        when(mockFactory.createCounter(any(), anyString())).thenReturn(mockCounter);

        final Span span = spanSecretMasker.apply(EMAIL_ADDRESS_SPAN);

        assertEquals(EMAIL_ADDRESS_SPAN, span);
        verify(mockSpanS3ConfigFetcher).isInWhiteList(
                EMAIL_FINDER_NAME_IN_FINDERS_DEFAULT_DOT_XML, SERVICE_NAME, OPERATION_NAME, STRING_TAG_KEY);
        verifiesForFindSecrets(40, 1);
    }

    @Test
    public void testFindSecretsHaystackEmailAddressInTagStringAndTagBytesAndIpAddressInTagBytes() {
        whensForFindSecrets();
        when(mockFactory.createCounter(any(), anyString())).thenReturn(mockCounter);

        final Span span = spanSecretMasker.apply(EMAIL_ADDRESSES_AND_IP_ADDRESS_SPAN);

        assertNotEquals(EMAIL_ADDRESSES_AND_IP_ADDRESS_SPAN, span);
        assertEquals(MASKED_BY_HAYSTACK, findTag(span, STRING_TAG_KEY).getVStr());
        assertArrayEquals(MASKED_BY_HAYSTACK_BYTES, findTag(span, BYTES_TAG_KEY).getVBytes().toByteArray());
        assertArrayEquals(MASKED_BY_HAYSTACK_BYTES, findLogFieldTag(span, BYTES_FIELD_KEY).getVBytes().toByteArray());
        verify(mockSpanS3ConfigFetcher).isInWhiteList(
                EMAIL_FINDER_NAME_IN_FINDERS_DEFAULT_DOT_XML, SERVICE_NAME, OPERATION_NAME, BYTES_TAG_KEY);
        verify(mockSpanS3ConfigFetcher).isInWhiteList(
                EMAIL_FINDER_NAME_IN_FINDERS_DEFAULT_DOT_XML, SERVICE_NAME, OPERATION_NAME, BYTES_FIELD_KEY);
        verify(mockSpanS3ConfigFetcher).isInWhiteList(
                NonLocalIpV4AddressFinder.FINDER_NAME, SERVICE_NAME, OPERATION_NAME, STRING_TAG_KEY);
        verify(mockFactory).createCounter(EMAIL_FINDER_NAME_AND_SERVICE_NAME, APPLICATION);
        verify(mockFactory).createCounter(IPV4_FINDER_NAME_AND_SERVICE_NAME, APPLICATION);
        verify(mockCounter, times(3)).increment();
        verify(mockSpanNameAndCountRecorder).add(
                EMAIL_FINDER_NAME_IN_FINDERS_DEFAULT_DOT_XML, SERVICE_NAME, OPERATION_NAME, BYTES_TAG_KEY);
        verify(mockSpanNameAndCountRecorder).add(
                EMAIL_FINDER_NAME_IN_FINDERS_DEFAULT_DOT_XML, SERVICE_NAME, OPERATION_NAME, BYTES_FIELD_KEY);
        verify(mockSpanNameAndCountRecorder).add(
                NonLocalIpV4AddressFinder.FINDER_NAME, SERVICE_NAME, OPERATION_NAME, STRING_TAG_KEY);
        verifiesForFindSecrets(18, 1);
    }

    @SuppressWarnings("MethodWithMultipleLoops")
    @Test
    public void testFindSecretsHaystackEmailAddressInTagBytesAndLogBytes() {
        whensForFindSecrets();
        when(mockFactory.createCounter(any(), anyString())).thenReturn(mockCounter);

        final Span span = spanSecretMasker.apply(EMAIL_ADDRESS_IN_TAG_BYTES_AND_LOG_BYTES_SPAN);

        assertNotEquals(EMAIL_ADDRESS_IN_TAG_BYTES_AND_LOG_BYTES_SPAN, span);
        assertArrayEquals(MASKED_BY_HAYSTACK_BYTES, findTag(span, BYTES_TAG_KEY).getVBytes().toByteArray());
        assertArrayEquals(MASKED_BY_HAYSTACK_BYTES, findLogFieldTag(span, BYTES_FIELD_KEY).getVBytes().toByteArray());
        verify(mockSpanS3ConfigFetcher).isInWhiteList(
                EMAIL_FINDER_NAME_IN_FINDERS_DEFAULT_DOT_XML, SERVICE_NAME, OPERATION_NAME, BYTES_TAG_KEY);
        verify(mockSpanS3ConfigFetcher).isInWhiteList(
                EMAIL_FINDER_NAME_IN_FINDERS_DEFAULT_DOT_XML, SERVICE_NAME, OPERATION_NAME, BYTES_FIELD_KEY);
        verify(mockFactory).createCounter(EMAIL_FINDER_NAME_AND_SERVICE_NAME, APPLICATION);
        verify(mockCounter, times(2)).increment();
        verify(mockSpanNameAndCountRecorder).add(
                EMAIL_FINDER_NAME_IN_FINDERS_DEFAULT_DOT_XML, SERVICE_NAME, OPERATION_NAME, BYTES_TAG_KEY);
        verify(mockSpanNameAndCountRecorder).add(
                EMAIL_FINDER_NAME_IN_FINDERS_DEFAULT_DOT_XML, SERVICE_NAME, OPERATION_NAME, BYTES_FIELD_KEY);
        verifiesForFindSecrets(28, 1);
    }

    @SuppressWarnings("MethodWithMultipleLoops")
    @Test
    public void testFindSecretsHaystackEmailAddressInLogString() {
        whensForFindSecrets();
        when(mockFactory.createCounter(any(), anyString())).thenReturn(mockCounter);

        final Span span = spanSecretMasker.apply(EMAIL_ADDRESS_LOG_SPAN_TAG_AND_VBYTES);

        assertNotEquals(EMAIL_ADDRESS_LOG_SPAN, span);
        assertEquals(MASKED_BY_HAYSTACK, findLogFieldTag(span, STRING_FIELD_KEY).getVStr());
        verify(mockSpanS3ConfigFetcher).isInWhiteList(
                EMAIL_FINDER_NAME_IN_FINDERS_DEFAULT_DOT_XML, SERVICE_NAME, OPERATION_NAME, BYTES_TAG_KEY);
        verify(mockSpanS3ConfigFetcher).isInWhiteList(
                EMAIL_FINDER_NAME_IN_FINDERS_DEFAULT_DOT_XML, SERVICE_NAME, OPERATION_NAME, STRING_FIELD_KEY);
        verify(mockSpanS3ConfigFetcher).isInWhiteList(
                EMAIL_FINDER_NAME_IN_FINDERS_DEFAULT_DOT_XML, SERVICE_NAME, OPERATION_NAME, BYTES_FIELD_KEY);
        verify(mockFactory).createCounter(EMAIL_FINDER_NAME_AND_SERVICE_NAME, APPLICATION);
        verify(mockCounter, times(3)).increment();
        verify(mockSpanNameAndCountRecorder).add(
                EMAIL_FINDER_NAME_IN_FINDERS_DEFAULT_DOT_XML, SERVICE_NAME, OPERATION_NAME, BYTES_TAG_KEY);
        verify(mockSpanNameAndCountRecorder).add(
                EMAIL_FINDER_NAME_IN_FINDERS_DEFAULT_DOT_XML, SERVICE_NAME, OPERATION_NAME, STRING_FIELD_KEY);
        verify(mockSpanNameAndCountRecorder).add(
                EMAIL_FINDER_NAME_IN_FINDERS_DEFAULT_DOT_XML, SERVICE_NAME, OPERATION_NAME, BYTES_FIELD_KEY);
        verifiesForFindSecrets(16, 1);
    }

    private static Tag findTag(Span span, String key) {
        final List<Tag> tags = span.getTagsList();
        for (Tag tag : tags) {
            if (tag.getKey().equals(key)) {
                return tag;
            }
        }
        return null;
    }

    @SuppressWarnings("MethodWithMultipleLoops")
    private static Tag findLogFieldTag(Span span, String key) {
        final List<Log> logs = span.getLogsList();
        for (Log log : logs) {
            for (Tag tag : log.getFieldsList()) {
                if (tag.getKey().equals(key)) {
                    return tag;
                }
            }
        }
        return null;
    }

    @Test
    public void testFactoryCreateCounter() {
        when(mockMetricObjects.createAndRegisterResettingCounter(
                anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(mockCounter);

        final Counter counter = factory.createCounter(EMAIL_FINDER_NAME_AND_SERVICE_NAME, APPLICATION);

        assertSame(counter, mockCounter);
        verify(mockMetricObjects).createAndRegisterResettingCounter(ERRORS_METRIC_GROUP,
                APPLICATION, EMAIL_FINDER_NAME_IN_FINDERS_DEFAULT_DOT_XML, SERVICE_NAME, COUNTER_NAME);
    }
}
