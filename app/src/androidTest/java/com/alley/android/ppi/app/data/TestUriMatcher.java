/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alley.android.ppi.app.data;

import android.content.UriMatcher;
import android.net.Uri;
import android.test.AndroidTestCase;

public class TestUriMatcher extends AndroidTestCase {
    private static final String LOCATION_QUERY = "London, UK";
    private static final String TEST_ADDRESS = "Aster,oids";  // December 20th, 2014
    private static final long TEST_LOCATION_ID = 10L;

    // content://com.alley.android.ppi.app/weather"
    private static final Uri TEST_WEATHER_DIR = PropertyContract.PropertyEntry.CONTENT_URI;
    private static final Uri TEST_WEATHER_WITH_LOCATION_DIR = PropertyContract.PropertyEntry.buildPropertyLocation(LOCATION_QUERY);
    private static final Uri TEST_WEATHER_WITH_LOCATION_AND_ADDRESS_DIR = PropertyContract.PropertyEntry.buildPropertyWithAddress(TEST_ADDRESS);
    // content://com.alley.android.ppi.app/location"
    private static final Uri TEST_LOCATION_DIR = PropertyContract.LocationEntry.CONTENT_URI;

    public void testUriMatcher() {
        UriMatcher testMatcher = PropertyProvider.buildUriMatcher();

        assertEquals("Error: The PROPERTY URI was matched incorrectly.",
                testMatcher.match(TEST_WEATHER_DIR), PropertyProvider.PROPERTY);
        assertEquals("Error: The PROPERTY WITH LOCATION URI was matched incorrectly.",
                testMatcher.match(TEST_WEATHER_WITH_LOCATION_DIR), PropertyProvider.PROPERTY_WITH_LOCATION);
        assertEquals("Error: The PROPERTY WITH LOCATION AND DATE URI was matched incorrectly.",
                testMatcher.match(TEST_WEATHER_WITH_LOCATION_AND_ADDRESS_DIR), PropertyProvider.PROPERTY_WITH_LOCATION_AND_ADDRESS);
        assertEquals("Error: The LOCATION URI was matched incorrectly.",
                testMatcher.match(TEST_LOCATION_DIR), PropertyProvider.LOCATION);
    }
}
