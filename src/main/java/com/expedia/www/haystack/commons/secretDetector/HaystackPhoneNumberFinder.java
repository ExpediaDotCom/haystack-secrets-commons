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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("WeakerAccess")
public class HaystackPhoneNumberFinder implements Finder {
    @VisibleForTesting
    public static final String FINDER_NAME = "Phone_Number";
    private static final Pattern ALPHAS_PATTERN = Pattern.compile("[A-Za-z]+");
    private static final Pattern ALL_NUMBERS_PATTERN = Pattern.compile("^\\d+$");
    private final PhoneNumberUtil phoneNumberUtil;
    private final String regionCode;

    public HaystackPhoneNumberFinder(PhoneNumberUtil phoneNumberUtil, CldrRegion cldrRegion) {
        this.phoneNumberUtil = phoneNumberUtil;
        this.regionCode = cldrRegion.getRegionCode();
    }

    /**
     * All HaystackPhoneNumberFinder instances have the same name, so that the reports and/or metrics when secrets are
     * found aggregates all phone number secrets, regardless of the region in which a phone number matched.
     *
     * @return the String "Phone_Number"
     */
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
            if (!containsAnyAlphabeticCharacters(input)) { // Strings that contain any letters will not be checked
                if(!isIpV4Address(input)) {                // Strings that look like IP addresses will not be checked
                    if(!containsOnlyNumbers(input)) {      // Straight numbers, like database IDs, will not be checked
                        final PhoneNumber phoneNumber = phoneNumberUtil.parseAndKeepRawInput(input, this.regionCode);
                        if (phoneNumberUtil.isValidNumberForRegion(phoneNumber, regionCode)) {
                            return Collections.singletonList(phoneNumber.getRawInput());
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

    private static boolean isIpV4Address(String input) {
        return !NonLocalIpV4AddressFinder.IPV4_FINDER.find(input).isEmpty();
    }

    private static boolean containsOnlyNumbers(String input) {
        return ALL_NUMBERS_PATTERN.matcher(input).find();
    }
}
