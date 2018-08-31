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

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static com.expedia.www.haystack.commons.secretDetector.CldrRegion.FRANCE;
import static com.expedia.www.haystack.commons.secretDetector.CldrRegion.UNITED_KINGDOM;
import static com.expedia.www.haystack.commons.secretDetector.CldrRegion.UNITED_STATES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HaystackPhoneNumberFinderTest {
    private static final String[] VALID_PHONE_NUMBERS_US = {
            "1-800-555-1212", "1 (800) 555-1212", "800-555-1212", "(800) 555-1212"
    };
    private static final String[] VALID_PHONE_NUMBERS_FR = {
            "+33.06.23.12.45.54"
    };
    private static final String[] VALID_PHONE_NUMBERS_UK = {
            "(020) 1234 5678"
    };

    private static final String[] INVALID_US_PHONE_NUMBERS = {
            "4640-1234-5678-9120",
            "/minify/min-2487701102.js",
            "50.242.105.69",
            "8005551212",
            "18005551212",
    };

    @Mock
    private PhoneNumberUtil mockPhoneNumberUtil;

    private HaystackPhoneNumberFinder haystackPhoneNumberFinderUS;
    private HaystackPhoneNumberFinder haystackPhoneNumberFinderFR;
    private HaystackPhoneNumberFinder haystackPhoneNumberFinderUK;

    @Before
    public void setUp() {
        haystackPhoneNumberFinderUS = new HaystackPhoneNumberFinder(PhoneNumberUtil.getInstance(), UNITED_STATES);
        haystackPhoneNumberFinderFR = new HaystackPhoneNumberFinder(PhoneNumberUtil.getInstance(), FRANCE);
        haystackPhoneNumberFinderUK = new HaystackPhoneNumberFinder(PhoneNumberUtil.getInstance(), UNITED_KINGDOM);
    }

    @After
    public void tearDown() {
        verifyNoMoreInteractions(mockPhoneNumberUtil);
    }

    @Test
    public void testGetName() {
        final String expected = HaystackPhoneNumberFinder.class.getSimpleName()
                .replace("Haystack", "")
                .replace("Finder", "");
        assertEquals(expected, haystackPhoneNumberFinderUS.getName());
    }

    @Test
    public void testFindStringValidNumbersUS() {
        for (String phoneNumber : VALID_PHONE_NUMBERS_US) {
            final List<String> strings = haystackPhoneNumberFinderUS.find(phoneNumber);
            assertEquals(phoneNumber, 1, strings.size());
        }
    }

    @Test
    public void testFindStringValidNumbersFR() {
        for (String phoneNumber : VALID_PHONE_NUMBERS_FR) {
            final List<String> strings = haystackPhoneNumberFinderFR.find(phoneNumber);
            assertEquals(phoneNumber, 1, strings.size());
        }
    }

    @Test
    public void testFindStringValidNumbersUK() {
        for (String phoneNumber : VALID_PHONE_NUMBERS_UK) {
            final List<String> strings = haystackPhoneNumberFinderUK.find(phoneNumber);
            assertEquals(phoneNumber, 1, strings.size());
        }
    }

    @Test
    public void testFindStringInvalidNumber() {
        for (String phoneNumber : INVALID_US_PHONE_NUMBERS) {
            final List<String> strings = haystackPhoneNumberFinderUS.find(phoneNumber);
            assertEquals(phoneNumber, 0, strings.size());
        }
    }

    @Test
    public void testFindStringsValidNumbers() {
        final List<String> phoneNumbers = Arrays.asList(VALID_PHONE_NUMBERS_US);
        final List<String> strings = haystackPhoneNumberFinderUS.find(phoneNumbers);
        assertEquals(VALID_PHONE_NUMBERS_US.length, strings.size());
        final Iterator<String> phoneNumbersIterator = phoneNumbers.iterator();
        final Iterator<String> stringsIterator = strings.iterator();
        while (phoneNumbersIterator.hasNext()) {
            assertEquals(phoneNumbersIterator.next(), stringsIterator.next());
        }
    }

    @Test
    public void testFindStringsInvalidNumbers() {
        final List<String> phoneNumbers = Arrays.asList(INVALID_US_PHONE_NUMBERS);
        final List<String> strings = haystackPhoneNumberFinderUS.find(phoneNumbers);
        assertEquals(0, strings.size());
    }

    @Test
    public void testFindNumberParseException() throws NumberParseException {
        when(mockPhoneNumberUtil.parseAndKeepRawInput(anyString(), anyString())).thenThrow(
                new NumberParseException(NumberParseException.ErrorType.TOO_LONG, "Test"));
        haystackPhoneNumberFinderUS = new HaystackPhoneNumberFinder(mockPhoneNumberUtil, UNITED_STATES);

        final List<String> phoneNumbers = haystackPhoneNumberFinderUS.find(VALID_PHONE_NUMBERS_US[0]);

        assertTrue(phoneNumbers.isEmpty());
        verify(mockPhoneNumberUtil).parseAndKeepRawInput(VALID_PHONE_NUMBERS_US[0], UNITED_STATES.getRegionCode());
    }
}
