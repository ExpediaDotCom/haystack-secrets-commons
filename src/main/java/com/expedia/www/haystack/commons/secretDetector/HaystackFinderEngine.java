/*
 * Copyright 2018 Expedia, Inc.
 *
 *       Licensed under the Apache License, Version 2.0 (the "License")),
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

import io.dataapps.chlorine.finder.Finder;
import io.dataapps.chlorine.finder.FinderEngine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class HaystackFinderEngine extends FinderEngine {
    public HaystackFinderEngine() {
        super((new HaystackFinderProvider()).getFinders(), false);
    }

    // Override the method from CompositeFinder to quit checking once a secret is found.
    @Override
    public Map<String, List<String>> findWithType(String input) {
        final Map<String, List<String>> map = new HashMap<>();
        final Iterator<Finder> iterator = getFinders().iterator();
        while(iterator.hasNext() && map.isEmpty()) {
            final Finder finder = iterator.next();
            final List<String> matches = finder.find(input);
            addToMap(map, finder, matches);
        }
        return map;
    }

    private void addToMap(Map<String, List<String>> map, Finder finder, List<String> matches) {
        if (!matches.isEmpty()) {
            map.computeIfAbsent(finder.getName(), k -> new ArrayList<>()).addAll(matches);
        }
    }

}
