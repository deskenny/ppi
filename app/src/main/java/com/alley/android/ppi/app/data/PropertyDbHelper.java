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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.alley.android.ppi.app.data.PropertyContract.LocationEntry;

/**
 * Manages a local database for weather data.
 */
public class PropertyDbHelper extends SQLiteOpenHelper {

    // If you change the database schema, you must increment the database version.
    private static final int DATABASE_VERSION = 2;

    static final String DATABASE_NAME = "weather.db";

    public PropertyDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        // Create a table to hold locations.  A location consists of the string supplied in the
        // location setting, the city name, and the latitude and longitude
        final String SQL_CREATE_LOCATION_TABLE = "CREATE TABLE " + LocationEntry.TABLE_NAME + " (" +
                LocationEntry._ID + " INTEGER PRIMARY KEY," +
                LocationEntry.COLUMN_SEARCH_STRING_USED + " TEXT UNIQUE, " +
                LocationEntry.COLUMN_CITY_NAME + " TEXT NOT NULL );";

        final String SQL_CREATE_IMAGE_TABLE = "CREATE TABLE " + PropertyContract.ImageEntry.TABLE_NAME + " (" +
                PropertyContract.ImageEntry._ID + " INTEGER PRIMARY KEY," +
                PropertyContract.ImageEntry.COLUMN_ADDRESS + " TEXT NOT NULL, " +
                PropertyContract.ImageEntry.COLUMN_PHOTO + " BLOB NOT NULL," +
                PropertyContract.ImageEntry.COLUMN_IS_PRIMARY + " BOOLEAN DEFAULT FALSE );";


        final String SQL_CREATE_PROPERTY_TABLE = "CREATE TABLE " + PropertyContract.PropertyEntry.TABLE_NAME + " (" +
                // Why AutoIncrement here, and not above?
                // Unique keys will be auto-generated in either case.  But for weather
                // forecasting, it's reasonable to assume the user will want information
                // for a certain date and all dates *following*, so the forecast data
                // should be sorted accordingly.
                PropertyContract.PropertyEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +

                // the ID of the location entry associated with this weather data
                PropertyContract.PropertyEntry.COLUMN_LOC_KEY + " INTEGER NOT NULL, " +
                PropertyContract.PropertyEntry.COLUMN_DATE + " INTEGER NOT NULL, " +
                PropertyContract.PropertyEntry.COLUMN_ADDRESS + " TEXT NOT NULL, " +
                PropertyContract.PropertyEntry.COLUMN_CONTENT_DESC + " TEXT, " +
                PropertyContract.PropertyEntry.COLUMN_HEADER_FEATURES + " TEXT, " +
                PropertyContract.PropertyEntry.COLUMN_BER_DETAILS + " TEXT, " +
                PropertyContract.PropertyEntry.COLUMN_ACCOMMODATION + " TEXT, " +
                PropertyContract.PropertyEntry.COLUMN_MY_HOME_BROCHURE_URL + " TEXT, " +
                PropertyContract.PropertyEntry.COLUMN_PROPERTY_PRICE_REGISTER_URL + " TEXT, " +
                PropertyContract.PropertyEntry.COLUMN_BROCHURE_READ_ATTEMPTED + " BOOLEAN DEFAULT FALSE, " +
                PropertyContract.PropertyEntry.COLUMN_BROCHURE_SUCCESS + " BOOLEAN DEFAULT FALSE, " +
                PropertyContract.PropertyEntry.COLUMN_PROP_TYPE_ID + " INTEGER DEFAULT 0," +
                PropertyContract.PropertyEntry.COLUMN_BROCHURE_PRICE + " REAL DEFAULT 0, " +
                PropertyContract.PropertyEntry.COLUMN_PRICE + " REAL NOT NULL, " +

                PropertyContract.PropertyEntry.COLUMN_NUM_BEDS + " INTEGER, " +
                PropertyContract.PropertyEntry.COLUMN_SQUARE_AREA + " REAL, " +
                PropertyContract.PropertyEntry.COLUMN_LATITUDE + " REAL, " +
                PropertyContract.PropertyEntry.COLUMN_LONGTITUDE + " REAL, " +

                // Set up the location column as a foreign key to location table.
                " FOREIGN KEY (" + PropertyContract.PropertyEntry.COLUMN_LOC_KEY + ") REFERENCES " +
                LocationEntry.TABLE_NAME + " (" + LocationEntry._ID + "), " +

                // To assure the application have just one weather entry per day
                // per location, it's created a UNIQUE constraint with REPLACE strategy
                " UNIQUE (" + PropertyContract.PropertyEntry.COLUMN_ADDRESS + ") ON CONFLICT REPLACE);";


        sqLiteDatabase.execSQL(SQL_CREATE_LOCATION_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_PROPERTY_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_IMAGE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        // Note that this only fires if you change the version number for your database.
        // It does NOT depend on the version number for your application.
        // If you want to update the schema without wiping data, commenting out the next 2 lines
        // should be your top priority before modifying this method.
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + LocationEntry.TABLE_NAME);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + PropertyContract.PropertyEntry.TABLE_NAME);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + PropertyContract.ImageEntry.TABLE_NAME);
        onCreate(sqLiteDatabase);
    }
}
