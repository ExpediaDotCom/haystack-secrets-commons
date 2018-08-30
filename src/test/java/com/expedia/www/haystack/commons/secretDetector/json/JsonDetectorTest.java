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
package com.expedia.www.haystack.commons.secretDetector.json;

import com.expedia.www.haystack.commons.secretDetector.S3ConfigFetcher;
import com.google.gson.Gson;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.dataapps.chlorine.finder.FinderEngine;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.expedia.www.haystack.commons.secretDetector.TestConstantsAndCommonCode.EMAIL_ADDRESS;
import static com.expedia.www.haystack.commons.secretDetector.TestConstantsAndCommonCode.RANDOM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class JsonDetectorTest {
    private static final String BUCKET = RANDOM.nextLong() + "BUCKET";
    private static final FinderEngine FINDER_ENGINE = new FinderEngine();
    private static final String JSON_TEMPLATE =
            "{\"rootElement\": {\"childMap\": {\"childKey\":%s}, \"childArray\": [%s]}}";
    private static final String NOT_A_SECRET_STRING = "\"NotASecret\"";
    private static final String EMAIL_ADDRESS_WITH_QUOTES = '"' + EMAIL_ADDRESS + '"';
    private static final String EMAIL_FINDER_NAME_IN_FINDERS_DEFAULT_DOT_XML = "Email";

    @Mock
    private S3ConfigFetcher mockS3ConfigFetcher;

    private JsonDetector jsonDetector;

    @Before
    public void setUp() {
        jsonDetector = new JsonDetector(FINDER_ENGINE, mockS3ConfigFetcher);
    }

    @After
    public void tearDown() {
        verifyNoMoreInteractions(mockS3ConfigFetcher);
    }

    @Test
    public void testSmallConstructor() {
        new JsonDetector(BUCKET);
    }

    @Test
    public void testFindSecretsNoSecrets() {
        final Map<String, List<String>> secrets =
                jsonDetector.findSecrets(createJsonString(NOT_A_SECRET_STRING, NOT_A_SECRET_STRING));

        assertTrue(secrets.isEmpty());
    }

    @Test
    public void testFindSecretEmailInMap() {
        final Map<String, List<String>> secrets =
                jsonDetector.findSecrets(createJsonString(EMAIL_ADDRESS_WITH_QUOTES, NOT_A_SECRET_STRING));

        assertEquals(1, secrets.size());
        assertEquals(EMAIL_FINDER_NAME_IN_FINDERS_DEFAULT_DOT_XML, secrets.keySet().iterator().next());
        final Collection<List<String>> values = secrets.values();
        assertEquals(1, values.size());
        assertEquals("rootElement.childMap.childKey", values.iterator().next().get(0));
    }

    @Test
    public void testFindSecretsEmailInArray() {
        final Map<String, List<String>> secrets =
                jsonDetector.findSecrets(createJsonString(NOT_A_SECRET_STRING, EMAIL_ADDRESS_WITH_QUOTES));

        assertEquals(1, secrets.size());
        assertEquals(EMAIL_FINDER_NAME_IN_FINDERS_DEFAULT_DOT_XML, secrets.keySet().iterator().next());
        final Collection<List<String>> values = secrets.values();
        assertEquals(1, values.size());
        assertEquals("rootElement.childArray.[0]", values.iterator().next().get(0));
    }

    @Test
    public void testFindSecretsEmailInMapAndArray() {
        final Map<String, List<String>> secrets =
                jsonDetector.findSecrets(createJsonString(EMAIL_ADDRESS_WITH_QUOTES, EMAIL_ADDRESS_WITH_QUOTES));

        assertEquals(1, secrets.size());
        assertEquals(EMAIL_FINDER_NAME_IN_FINDERS_DEFAULT_DOT_XML, secrets.keySet().iterator().next());
        final Collection<List<String>> values = secrets.values();
        assertEquals(1, values.size());
        final List<String> ids = values.iterator().next();
        assertEquals("rootElement.childMap.childKey", ids.get(0));
        assertEquals("rootElement.childArray.[0]", ids.get(1));
    }

    private static JsonObject createJsonString(String mapValue, String arrayValue) {
        final String jsonString = String.format(JSON_TEMPLATE, mapValue, arrayValue);
        return new Gson().fromJson(jsonString, JsonObject.class);
    }

    @Test
    public void testFindSecretsNullAsRootJsonElement() {
        final Map<String, List<String>> secrets = jsonDetector.findSecrets(
                null, Collections.emptyMap(), new LinkedList<>());

        assertTrue(secrets.isEmpty());
    }

    @Test
    public void testFindSecretsJsonNullAsRootJsonElement() {
        final Map<String, List<String>> secrets = jsonDetector.findSecrets(
                JsonNull.INSTANCE, new HashMap<>(), new LinkedList<>());

        assertTrue(secrets.isEmpty());
    }

    @Test
    public void testFindSecretsJsonPrimitiveNumberAsRootJsonElement() {
        final Map<String, List<String>> secrets = jsonDetector.findSecrets(
                new JsonPrimitive(42), new HashMap<>(), new LinkedList<>());

        assertTrue(secrets.isEmpty());
    }
}
