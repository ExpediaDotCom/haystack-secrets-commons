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

import io.dataapps.chlorine.finder.FinderEngine;
import io.dataapps.chlorine.pattern.RegexFinder;

public final class HaystackFinderEngine extends FinderEngine {
    public HaystackFinderEngine() {
        // default_finders.xml contains HaystackPhoneNumber finders, which are parsed by FinderEngine into RegexFinder
        // objects that contain an empty pattern; remove them
        getFinders().removeIf(next -> next instanceof RegexFinder
                && ((RegexFinder) next).getPattern().toString().isEmpty());
        // TODO Use HaystackFinderProvider to add the HaystackPhoneNumber finders
    }
}
