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
package com.expedia.www.haystack.commons.secretDetector;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static com.expedia.www.haystack.commons.secretDetector.NonLocalIpV4AddressFinder.FINDER_NAME;
import static com.expedia.www.haystack.commons.secretDetector.TestConstantsAndCommonCode.RANDOM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NonLocalIpV4AddressFinderTest {
    private static final String TEN_DOT_PREFIX = "10.";
    private static final String TEN_DOT_FORMAT = TEN_DOT_PREFIX + "%d.%d.%d";
    private static final String ONE92_DOT_168 = "192.168.";
    private static final String ONE92_DOT_168_FORMAT = ONE92_DOT_168 + "%d.%d";
    private static final int IP_V4_PIECE_MAX = 256;
    private static final String TEN_DOT_ADDRESS = String.format(TEN_DOT_FORMAT,
            RANDOM.nextInt(IP_V4_PIECE_MAX), RANDOM.nextInt(IP_V4_PIECE_MAX), RANDOM.nextInt(IP_V4_PIECE_MAX));
    private static final String ONE92_DOT_168_ADDRESS = String.format(ONE92_DOT_168_FORMAT,
            RANDOM.nextInt(IP_V4_PIECE_MAX), RANDOM.nextInt(IP_V4_PIECE_MAX));
    @SuppressWarnings("NonConstantFieldWithUpperCaseName")
    private static String NON_INTERNAL_IP_V4_ADDRESS;

    static {
        //noinspection NonFinalStaticVariableUsedInClassInitialization
        do {
            NON_INTERNAL_IP_V4_ADDRESS = String.format("%d.%d.%d.%d", RANDOM.nextInt(IP_V4_PIECE_MAX),
                    RANDOM.nextInt(IP_V4_PIECE_MAX), RANDOM.nextInt(IP_V4_PIECE_MAX), RANDOM.nextInt(IP_V4_PIECE_MAX));
        } while (NON_INTERNAL_IP_V4_ADDRESS.startsWith(TEN_DOT_PREFIX)
                || NON_INTERNAL_IP_V4_ADDRESS.startsWith(ONE92_DOT_168));
    }

    private static final String SHOULD_NOT_BE_SECRET_FORMAT = "%s should not have been marked as secret";
    private NonLocalIpV4AddressFinder nonLocalIpV4AddressFinder;

    @Before
    public void setUp() {
        nonLocalIpV4AddressFinder = new NonLocalIpV4AddressFinder();
    }

    @Test
    public void test10DotAddress() {
        testAddressIsInternal(TEN_DOT_ADDRESS);
    }

    @Test
    public void test192Dot168Address() {
        testAddressIsInternal(ONE92_DOT_168_ADDRESS);
    }

    @Test
    public void test10DotInternalAddressSubstring() {
        testAddressIsInternal("Endpoint{serviceName=my-service, ipv4=10.42.61.90, ipv6=null, port=null}");
    }

    @Test
    public void test192Dot168InternalAddressSubstring() {
        testAddressIsInternal("Endpoint{serviceName=my-service, ipv4=192.168.61.90, ipv6=null, port=null}");
    }

    @Test
    public void test10Dot() {
        testAddressIsInternal("10.38.85.225");
    }

    @Test
    public void testLocalHost() {
        testAddressIsInternal("127.0.0.1");
    }

    private void testAddressIsInternal(String address) {
        final String message = String.format(SHOULD_NOT_BE_SECRET_FORMAT, address);
        assertTrue(message, nonLocalIpV4AddressFinder.find(address).isEmpty());
    }

    @Test
    public void testFindCollection() {
        final List<String> internalAddresses = ImmutableList.of(TEN_DOT_ADDRESS, ONE92_DOT_168_ADDRESS);
        final String message = String.format(SHOULD_NOT_BE_SECRET_FORMAT,
                TEN_DOT_ADDRESS + ',' + ONE92_DOT_168_ADDRESS);
        assertTrue(message, nonLocalIpV4AddressFinder.find(internalAddresses).isEmpty());
    }

    @Test
    public void testGetName() {
        assertEquals(FINDER_NAME, nonLocalIpV4AddressFinder.getName());
    }

    @Test
    public void testAddressIsNotInternal() {
        assertFalse(nonLocalIpV4AddressFinder.find(NON_INTERNAL_IP_V4_ADDRESS).isEmpty());
    }

    @Test
    public void testStringIsNotAnIpAddress() {
        assertTrue(nonLocalIpV4AddressFinder.find(TEN_DOT_FORMAT).isEmpty());
    }
}
