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

import com.expedia.www.haystack.metrics.MetricObjects;
import com.netflix.servo.monitor.Stopwatch;
import com.netflix.servo.monitor.Timer;
import io.dataapps.chlorine.finder.Finder;
import io.dataapps.chlorine.finder.FinderEngine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class HaystackFinderEngine extends FinderEngine {
    private final MetricObjects metricObjects;
    private final String subsystem;
    private final String application;
    private final Map<String, Timer> timerMap;

    public HaystackFinderEngine(MetricObjects metricObjects, String subsystem, String application) {
        super((new HaystackFinderProvider()).getFinders(), false);
        this.metricObjects = metricObjects;
        this.subsystem = subsystem;
        this.application = application;
        timerMap = new ConcurrentHashMap<>();
    }

    // Override the method from CompositeFinder to quit checking once a secret is found.
    // Also measure the time that each timer takes to run.
    @Override
    public Map<String, List<String>> findWithType(String input) {
        final Map<String, List<String>> map = new HashMap<>();
        final Iterator<Finder> iterator = getFinders().iterator();
        while(iterator.hasNext() && map.isEmpty()) {
            final Finder finder = iterator.next();
            final String name = finder.getName();
            final Timer timer = timerMap.computeIfAbsent(name, k -> {
                final String klass = finder.getClass().getName();
                final String upperCase = name.toUpperCase();
                return metricObjects.createAndRegisterBasicTimer(
                        subsystem, application, klass, upperCase, MILLISECONDS);
            });
            final Stopwatch stopwatch = timer.start();
            final List<String> matches = finder.find(input);
            stopwatch.stop();
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
