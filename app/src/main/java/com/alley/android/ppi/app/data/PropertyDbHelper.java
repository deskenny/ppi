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
 * Manages a local database for property data.
 */
public class PropertyDbHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 2;

    static final String DATABASE_NAME = "property.db";

    public PropertyDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        final String SQL_CREATE_LOCATION_TABLE = "CREATE TABLE " + LocationEntry.TABLE_NAME + " (" +
                LocationEntry._ID + " INTEGER PRIMARY KEY," +
                LocationEntry.COLUMN_SEARCH_STRING_USED + " TEXT UNIQUE, " +
                LocationEntry.COLUMN_CITY_NAME + " TEXT NOT NULL );";

        final String SQL_CREATE_IMAGE_TABLE = "CREATE TABLE " + PropertyContract.ImageEntry.TABLE_NAME + " (" +
                PropertyContract.ImageEntry._ID + " INTEGER PRIMARY KEY," +
                PropertyContract.ImageEntry.COLUMN_ADDRESS + " TEXT NOT NULL, " +
                PropertyContract.ImageEntry.COLUMN_PHOTO + " BLOB NOT NULL," +
                PropertyContract.ImageEntry.COLUMN_IS_PRIMARY + " BOOLEAN DEFAULT FALSE, " +
                PropertyContract.ImageEntry.COLUMN_DATE + " DATE NOT NULL );";


        final String SQL_CREATE_PROPERTY_TABLE = "CREATE TABLE " + PropertyContract.PropertyEntry.TABLE_NAME + " (" +
                PropertyContract.PropertyEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +

                PropertyContract.PropertyEntry.COLUMN_LOC_KEY + " INTEGER NOT NULL, " +
                PropertyContract.PropertyEntry.COLUMN_DATE + " INTEGER NOT NULL, " +
                PropertyContract.PropertyEntry.COLUMN_READ_FROM_REGISTER_DATE + " INTEGER NULL, " +
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
                PropertyContract.PropertyEntry.COLUMN_CLASS + " INTEGER DEFAULT 0, " +
                PropertyContract.PropertyEntry.COLUMN_APARTMENT_HOUSE + " INTEGER DEFAULT 0, " +

                PropertyContract.PropertyEntry.COLUMN_NUM_BEDS + " INTEGER, " +
                PropertyContract.PropertyEntry.COLUMN_SQUARE_AREA + " REAL, " +
                PropertyContract.PropertyEntry.COLUMN_LATITUDE + " REAL, " +
                PropertyContract.PropertyEntry.COLUMN_LONGTITUDE + " REAL, " +

                " FOREIGN KEY (" + PropertyContract.PropertyEntry.COLUMN_LOC_KEY + ") REFERENCES " +
                LocationEntry.TABLE_NAME + " (" + LocationEntry._ID + "), " +

                " UNIQUE (" + PropertyContract.PropertyEntry.COLUMN_ADDRESS + ") ON CONFLICT REPLACE);";


        sqLiteDatabase.execSQL(SQL_CREATE_LOCATION_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_PROPERTY_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_IMAGE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + LocationEntry.TABLE_NAME);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + PropertyContract.PropertyEntry.TABLE_NAME);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + PropertyContract.ImageEntry.TABLE_NAME);
        onCreate(sqLiteDatabase);
    }
}
