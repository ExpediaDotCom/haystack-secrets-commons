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
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.netflix.servo.util.VisibleForTesting;
import io.dataapps.chlorine.finder.Finder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("WeakerAccess")
public class HaystackCompositePhoneNumberFinder implements Finder {
    @VisibleForTesting
    static final String FINDER_NAME = "Phone_Number";
    private static final Pattern ALPHAS_PATTERN = Pattern.compile("[A-Za-z]+");
    private static final Pattern ALL_NUMBERS_PATTERN = Pattern.compile("^\\d+$");
    private final PhoneNumberUtil phoneNumberUtil;
    private final List<String> regions = new ArrayList<>();

    public HaystackCompositePhoneNumberFinder() {
        this(PhoneNumberUtil.getInstance());
    }

    public HaystackCompositePhoneNumberFinder(PhoneNumberUtil phoneNumberUtil) {
        this.phoneNumberUtil = phoneNumberUtil;
        regions.addAll(Arrays.asList(
                // G7 countries to start, we'll see how well this size of list performs
                CldrRegion.CANADA.getRegionCode(),
                CldrRegion.FRANCE.getRegionCode(),
                CldrRegion.GERMANY.getRegionCode(),
                CldrRegion.ITALY.getRegionCode(),
                CldrRegion.JAPAN.getRegionCode(),
                CldrRegion.UNITED_KINGDOM.getRegionCode(),
                CldrRegion.UNITED_STATES.getRegionCode()
        ));
    }

    @SuppressWarnings("SuspiciousGetterSetter")
    @Override
    public String getName() {
        return FINDER_NAME;
    }

    @Override
    public List<String> find(Collection<String> inputs) {
        final List<String> list = new ArrayList<>();
        for (String input : inputs) {
            list.addAll(find(input));
        }
        return list;
    }

    @SuppressWarnings("OverlyNestedMethod") // the nesting makes debugging easier
    @Override
    public List<String> find(String input) {
        try {
            if (!containsAnyAlphabeticCharacters(input)) {
                if(isNotIpV4Address(input)) {
                    if(!containsOnlyNumbers(input)) {
                        for (final String region : regions) {
                            final PhoneNumber phoneNumber = phoneNumberUtil.parseAndKeepRawInput(input, region);
                            if (phoneNumberUtil.isValidNumberForRegion(phoneNumber, region)) {
                                return Collections.singletonList(phoneNumber.getRawInput());
                            }
                        }
                    }
                }
            }
        } catch (NumberParseException e) {
            // Just ignore, it's not a phone number
        }
        return Collections.emptyList();
    }

    private static boolean containsAnyAlphabeticCharacters(String input) {
        final Matcher matcher = ALPHAS_PATTERN.matcher(input);
        return matcher.find();
    }

    private static boolean isNotIpV4Address(String input) {
        return NonLocalIpV4AddressFinder.IPV4_FINDER.find(input).isEmpty();
    }

    private static boolean containsOnlyNumbers(String input) {
        return ALL_NUMBERS_PATTERN.matcher(input).find();
    }
}