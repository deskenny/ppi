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

import android.content.ContentResolver;
import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Defines table and column names for the property database.
 */
public class PropertyContract {

    // The "Content authority" is a name for the entire content provider, similar to the
    // relationship between a domain name and its website.  A convenient string to use for the
    // content authority is the package name for the app, which is guaranteed to be unique on the
    // device.
    public static final String CONTENT_AUTHORITY = "com.alley.android.ppi.app";

    // Use CONTENT_AUTHORITY to create the base of all URI's which apps will use to contact
    // the content provider.
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    // Possible paths (appended to base content URI for possible URI's)
    // For instance, content://com.alley.android.ppi.app/weather/ is a valid path for
    // looking at weather data. content://com.alley.android.ppi.app/givemeroot/ will fail,
    // as the ContentProvider hasn't been given any information on what to do with "givemeroot".
    // At least, let's hope not.  Don't be that dev, reader.  Don't be that dev.
    public static final String PATH_PROPERTY = "property";
    public static final String PATH_LOCATION = "location";
    public static final String PATH_IMAGE = "image";

    /* Inner class that defines the table contents of the location table */
    public static final class LocationEntry implements BaseColumns {

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_LOCATION).build();

        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_LOCATION;
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_LOCATION;

        // Table name
        public static final String TABLE_NAME = "location";

        public static final String COLUMN_SEARCH_STRING_USED = "search_string_used";

        public static final String COLUMN_CITY_NAME = "city_name";

        public static Uri buildLocationUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }
    }

    /* Inner class that defines the table contents of the weather table */
    public static final class PropertyEntry implements BaseColumns {

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_PROPERTY).build();

        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_PROPERTY;
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_PROPERTY;

        public static final String TABLE_NAME = "property";

        // Column with the foreign key into the location table.
        public static final String COLUMN_LOC_KEY = "location_id";
        // Date, stored as long in milliseconds since the epoch
        public static final String COLUMN_DATE = "date";

        public static final String COLUMN_PROP_TYPE_ID = "prop_type_id";

        public static final String COLUMN_ADDRESS = "address";

        public static final String COLUMN_CONTENT_DESC = "content_desc";

        public static final String COLUMN_BROCHURE_READ_ATTEMPTED = "brochure_read_attempted";
        public static final String COLUMN_BROCHURE_SUCCESS = "brochure_success";

        public static final String COLUMN_HEADER_FEATURES = "header_features";
        public static final String COLUMN_BER_DETAILS = "ber_details";
        public static final String COLUMN_ACCOMMODATION = "accommodation";

        public static final String COLUMN_MY_HOME_BROCHURE_URL = "my_home_url";
        public static final String COLUMN_PROPERTY_PRICE_REGISTER_URL = "ppr_url";

        public static final String COLUMN_BROCHURE_PRICE = "brochure_price";
        public static final String COLUMN_PRICE = "price";

        // real representing number of bedrooms
        public static final String COLUMN_NUM_BEDS = "num_beds";

        // Square Area is stored as a float representing percentage
        public static final String COLUMN_SQUARE_AREA = "square_area";

        // Latitude is stored as a float representing location
        public static final String COLUMN_LATITUDE = "latitude";

        // Longtitude is stored as a float representing location
        public static final String COLUMN_LONGTITUDE = "longtitude";

        public static Uri buildLocationUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

        public static Uri buildPropertyLocation(String locationSetting) {
            return CONTENT_URI.buildUpon().appendPath(locationSetting).build();
        }

        public static Uri buildPropertiesLocationWithStartDate(
                String locationSetting, long startDate) {
            long normalizedDate = startDate;
            return CONTENT_URI.buildUpon().appendPath(locationSetting)
                    .appendQueryParameter(COLUMN_DATE, Long.toString(normalizedDate)).build();
        }

        public static Uri buildPropertyWithAddress(String address) {
            return CONTENT_URI.buildUpon().appendPath("doesntmatter")
                    .appendPath(address).build();
        }

        public static String getLocationSettingFromUri(Uri uri) {
            return uri.getPathSegments().get(1);
        }

        public static String getAddressFromUri(Uri uri) {
            return uri.getPathSegments().get(2);
        }

        public static long getStartDateFromUri(Uri uri) {
            String dateString = uri.getQueryParameter(COLUMN_DATE);
            if (null != dateString && dateString.length() > 0)
                return Long.parseLong(dateString);
            else
                return 0;
        }
    }

    /* Inner class that defines the table contents of the location table */
    public static final class ImageEntry implements BaseColumns {

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_IMAGE).build();

        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_IMAGE;
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_IMAGE;

        public static final String TABLE_NAME = "image";

        public static final String COLUMN_PHOTO = "photo";

        public static final String COLUMN_ADDRESS = "address";

        public static final String COLUMN_IS_PRIMARY = "is_primary";

        public static Uri buildImageUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

        public static Uri buildImageUriFromAddress(String address) {
            return CONTENT_URI.buildUpon().appendPath(address).build();
        }

        public static String getAddressFromUri(Uri uri) {
            return uri.getPathSegments().get(1);
        }
    }
}
