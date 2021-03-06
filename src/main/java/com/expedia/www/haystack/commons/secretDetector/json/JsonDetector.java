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

import com.expedia.www.haystack.commons.secretDetector.DetectorBase;
import com.expedia.www.haystack.commons.secretDetector.HaystackFinderEngine;
import com.expedia.www.haystack.commons.secretDetector.S3ConfigFetcher;
import com.expedia.www.haystack.commons.secretDetector.S3ConfigFetcherBase;
import com.expedia.www.haystack.metrics.MetricObjects;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.netflix.servo.util.VisibleForTesting;

import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.expedia.www.haystack.commons.config.Configuration.WHITELIST_S3_ITEM_NAME;

@SuppressWarnings("WeakerAccess")
public class JsonDetector extends DetectorBase {
    public JsonDetector(String bucket, String subsystem, String application) {
        this(new HaystackFinderEngine(new MetricObjects(), subsystem, application),
                new S3ConfigFetcher(S3ConfigFetcherBase.Prefix.JSON, bucket, WHITELIST_S3_ITEM_NAME));
    }

    public JsonDetector(HaystackFinderEngine haystackFinderEngine, S3ConfigFetcher s3ConfigFetcher) {
        super(haystackFinderEngine, s3ConfigFetcher);
    }

    /**
     * Finds secrets in an XML Document
     *
     * @param jsonElement the JSON object in which to look for secrets
     * @return keys of the secrets (the names of the attributes or elements whose values are secrets)
     */
    public Map<String, List<String>> findSecrets(JsonElement jsonElement) {
        return findSecrets(jsonElement, new HashMap<>(), new LinkedList<>());
    }

    @VisibleForTesting
    Map<String, List<String>> findSecrets(JsonElement rootJsonElement,
                                          Map<String, List<String>> mapOfTypeToKeysOfSecrets,
                                          LinkedList<Object> ids) {
        if(rootJsonElement != null) {
            if (rootJsonElement.isJsonObject()) {
                handleJsonObject(mapOfTypeToKeysOfSecrets, ids, rootJsonElement.getAsJsonObject());
            } else if (rootJsonElement.isJsonArray()) {
                handleJsonArray(mapOfTypeToKeysOfSecrets, ids, rootJsonElement.getAsJsonArray());
            } else if (rootJsonElement.isJsonPrimitive()) {
                handleJsonPrimitive(mapOfTypeToKeysOfSecrets, ids, rootJsonElement.getAsJsonPrimitive());
            }
        }
        return mapOfTypeToKeysOfSecrets;
    }

    private void handleJsonObject(Map<String, List<String>> mapOfTypeToKeysOfSecrets,
                                  LinkedList<Object> ids,
                                  JsonObject childJsonObject) {
        for (Map.Entry<String, JsonElement> entry : childJsonObject.entrySet()) {
            ids.push(entry.getKey());
            findSecrets(entry.getValue(), mapOfTypeToKeysOfSecrets, ids);
            ids.pop();
        }
    }

    private void handleJsonArray(Map<String, List<String>> mapOfTypeToKeysOfSecrets,
                                 LinkedList<Object> ids,
                                 JsonArray jsonArray) {
        int childJsonElementIndex = 0;
        for (final JsonElement childJsonElement : jsonArray) {
            //noinspection ValueOfIncrementOrDecrementUsed
            ids.push(childJsonElementIndex++);
            findSecrets(childJsonElement, mapOfTypeToKeysOfSecrets, ids);
            ids.pop();
        }
    }

    private void handleJsonPrimitive(Map<String, List<String>> mapOfTypeToKeysOfSecrets,
                                     Deque<Object> ids,
                                     JsonPrimitive value) {
        if (value.isString()) {
            final Map<String, List<String>> secrets = haystackFinderEngine.findWithType(value.getAsString());
            if (!secrets.isEmpty()) {
                putKeysOfSecretsIntoMap(mapOfTypeToKeysOfSecrets, getCompleteHierarchy(ids), secrets);
            }
            /* // Secrets are not yet being looked for in Numbers, as phone number false positives would be very common.
               // When positives are aggregated into a daily email that shows keys that can be whitelisted, we will
               // turn on number detection and whitelist the false positives that result. We will do this for both JSON
               // and XML BLOBs, and perhaps for fastinfoset BLOBs too.
        } else if (value.isNumber()) {
            final Map<String, List<String>> secrets = haystackFinderEngine.findWithType(value.getAsNumber().toString());
            if (!secrets.isEmpty()) {
                putKeysOfSecretsIntoMap(mapOfTypeToKeysOfSecrets, getCompleteHierarchy(ids), secrets);
            }
            */
        }
    }

    private static String getCompleteHierarchy(Deque<Object> ids) {
        @SuppressWarnings("StringBufferWithoutInitialCapacity") final StringBuilder stringBuilder = new StringBuilder();
        final Iterator<Object> iterator = ids.descendingIterator();
        while (iterator.hasNext()) {
            final Object id = iterator.next();
            final boolean isInteger = id instanceof Integer;
            if (isInteger) {
                stringBuilder.append('[');
            }
            stringBuilder.append(id);
            if (isInteger) {
                stringBuilder.append(']');
            }
            if (iterator.hasNext()) {
                stringBuilder.append('.');
            }
        }
        return stringBuilder.toString();
    }
}
