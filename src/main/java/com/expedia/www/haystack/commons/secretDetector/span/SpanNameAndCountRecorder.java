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

import com.netflix.servo.util.VisibleForTesting;
import org.slf4j.Logger;

import java.time.Clock;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This class facilitates the logging of information about which span tags contain confidential data, and how often a
 * particular tag is seen. Typical usage is to simply call add() whenever a secret is found. At one hour intervals, the
 * call to add() will log the output returned by SpanNameAndCountRecorder.toString(). The class is thread safe but spans
 * seen near the time at which the confidential data locations are logged (calls by other threads, obviously) may not
 * be tracked.
 */
@SuppressWarnings({"FieldNotUsedInToString", "WeakerAccess"})
public class SpanNameAndCountRecorder {
    @VisibleForTesting
    static final long ONE_HOUR = TimeUnit.HOURS.toMillis(1);
    @VisibleForTesting
    static final String CONFIDENTIAL_DATA_LOCATIONS = "Confidential data locations: %s";

    //            FinderName, ServiceName, OperationName, TagName, Count
    private final Map<String, Map<String, Map<String, Map<String, AtomicInteger>>>> map = new ConcurrentHashMap<>();

    private final Action toStringAction = new ToStringAction();
    private final Action clearAction = new ClearAction();
    private final Logger logger;
    private final AtomicLong lastLogTimeMS = new AtomicLong(0);
    private final Clock clock;

    public SpanNameAndCountRecorder(Logger logger, Clock clock) {
        this.logger = logger;
        this.clock = clock;
    }

    public void add(String finderName, String serviceName, String operationName, String tagName) {
        final AtomicInteger count = map
                .computeIfAbsent(finderName, v -> new ConcurrentHashMap<>())
                .computeIfAbsent(serviceName, v -> new ConcurrentHashMap<>())
                .computeIfAbsent(operationName, v -> new ConcurrentHashMap<>())
                .computeIfAbsent(tagName, v -> new AtomicInteger(0));
        count.incrementAndGet();
        logIfTimeToLog();
    }

    private void logIfTimeToLog() {
        final long now = clock.millis();
        if (now > (lastLogTimeMS.get() + ONE_HOUR)) {
            synchronized (lastLogTimeMS) {
                logIfTimeToLogInnerCheck(now);
            }
        }
    }

    @VisibleForTesting
    void logIfTimeToLogInnerCheck(long now) {
        // Checking again inside the synchronized block ensures that the map is only logged once
        if (now > (lastLogTimeMS.get() + ONE_HOUR)) {
            final Set<String> set = loop(new TreeSet<>(), toStringAction, clearAction);
            logger.info(String.format(CONFIDENTIAL_DATA_LOCATIONS, set));
            if(lastLogTimeMS.get() == 0) {
                lastLogTimeMS.set(now);
            } else {
                lastLogTimeMS.addAndGet(ONE_HOUR);
            }
        }
    }

    @Override
    public String toString() {
        final Set<String> set = loop(new TreeSet<>(), toStringAction);
        return set.toString();
    }

    /**
     * Loop over the entire map wrapped by this class, executing the Action(s) specified. The use case for more than one
     * value in the actions parameter is the logging use case from
     * {@link SpanNameAndCountRecorder#add(String, String, String, String)} in which the first Action is
     * {@link SpanNameAndCountRecorder#toStringAction} and the second action is
     * {@link SpanNameAndCountRecorder#clearAction}. Because there is no synchronization in the innermost loop of this
     * method (the loop over actions), it's possible for another thread to call
     * {@link SpanNameAndCountRecorder#add(String, String, String, String)} between the actions; this is the reason for
     * the statement "spans seen near the time at which the confidential data locations are logged (calls by
     * other threads, obviously) may not be tracked" in the Javadoc for this class.
     *
     * @param set  the object to pass into each Action.execute() method call
     * @param actions the actions(s) to execute
     * @return the object parameter
     */
    @SuppressWarnings("MethodWithMultipleLoops")
    private Set<String> loop(Set<String> set, Action... actions) {
        for (Map.Entry<String, Map<String, Map<String, Map<String, AtomicInteger>>>>
                finderNameEntrySet : map.entrySet()) {
            for (Map.Entry<String, Map<String, Map<String, AtomicInteger>>>
                    serviceNameEntrySet : finderNameEntrySet.getValue().entrySet()) {
                for (Map.Entry<String, Map<String, AtomicInteger>>
                        operationNameEntrySet : serviceNameEntrySet.getValue().entrySet()) {
                    for (final Map.Entry<String, AtomicInteger>
                            tagNameAndCount : operationNameEntrySet.getValue().entrySet()) {
                        for (final Action action : actions) {
                            action.execute(set,
                                    finderNameEntrySet.getKey(),
                                    serviceNameEntrySet.getKey(),
                                    operationNameEntrySet.getKey(),
                                    tagNameAndCount);
                        }
                    }
                }
            }
        }
        return set;
    }

    @FunctionalInterface
    private interface Action {
        void execute(Set<String> idsAndCountsOfObjectsContainingSecrets,
                     String finderName,
                     String serviceName,
                     String operationName,
                     Map.Entry<String, AtomicInteger> tagNameAndCount);
    }

    private static class ToStringAction implements Action {
        public void execute(Set<String> set,
                            String finderName,
                            String serviceName,
                            String operationName,
                            Map.Entry<String, AtomicInteger> tagNameAndCount) {
            final String idOfObjectContainingSecrets = String.format("%s;%s;%s;%s",
                    finderName, serviceName, operationName, tagNameAndCount);
            set.add(idOfObjectContainingSecrets);
        }
    }

    private static class ClearAction implements Action {
        @Override
        public void execute(Set<String> set,
                            String finderName,
                            String serviceName,
                            String operationName,
                            Map.Entry<String, AtomicInteger> tagNameAndCount) {
            tagNameAndCount.getValue().set(0);
        }
    }
}
