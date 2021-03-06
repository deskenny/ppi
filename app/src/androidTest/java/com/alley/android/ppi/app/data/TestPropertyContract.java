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

import android.net.Uri;
import android.test.AndroidTestCase;

import com.alley.android.ppi.app.Utility;


public class TestPropertyContract extends AndroidTestCase {

    // intentionally includes a slash to make sure Uri is getting quoted correctly
    private static final String TEST_PPI_LOCATION = "/Harolds Cross";
    private static final long TEST_PPI_DATE = 1419033600L;  // December 20th, 2014

    public void testBuildPropertyLocation() {
        Uri locationUri = PropertyContract.PropertyEntry.buildPropertyLocation(TEST_PPI_LOCATION);
        assertNotNull("Error: Null Uri returned.  You must fill-in buildPropertyLocation in " +
                        "PropertyContract.",
                locationUri);
        assertEquals("Error: Property location not properly appended to the end of the Uri",
                TEST_PPI_LOCATION, locationUri.getLastPathSegment());
        assertEquals("Error: Property location Uri doesn't match our expected result",
                locationUri.toString(),
                "content://com.alley.android.ppi.app/property/%2FHarolds%20Cross");
    }


    public void testMatcher() {
        //187 Cashel Road' and '187 CASHEL RD
        //145 Kildare Roa' and '145 KILDARE RD,

        assertTrue(Utility.isMatchedAddress("187 Cashel Road", "187 CASHEL RD"));
        assertTrue(Utility.isMatchedAddress("145 Kildare Road","145 KILDARE RD,"));
    }
}
