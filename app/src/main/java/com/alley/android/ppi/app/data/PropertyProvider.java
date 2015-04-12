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

import android.annotation.TargetApi;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

public class PropertyProvider extends ContentProvider {
    public static final String LOG_TAG = PropertyProvider.class.getSimpleName();
    // The URI Matcher used by this content provider.
    private static final UriMatcher sUriMatcher = buildUriMatcher();
    private PropertyDbHelper mOpenHelper;

    static final int PROPERTY = 100;
    static final int PROPERTY_WITH_LOCATION = 101;
    static final int PROPERTY_WITH_LOCATION_AND_ADDRESS = 102;
    static final int LOCATION = 300;

    private static final SQLiteQueryBuilder sWeatherByLocationSettingQueryBuilder;

    static{
        sWeatherByLocationSettingQueryBuilder = new SQLiteQueryBuilder();
        
        //This is an inner join which looks like
        //weather INNER JOIN location ON weather.location_id = location._id
        sWeatherByLocationSettingQueryBuilder.setTables(
                PropertyContract.PropertyEntry.TABLE_NAME + " INNER JOIN " +
                        PropertyContract.LocationEntry.TABLE_NAME +
                        " ON " + PropertyContract.PropertyEntry.TABLE_NAME +
                        "." + PropertyContract.PropertyEntry.COLUMN_LOC_KEY +
                        " = " + PropertyContract.LocationEntry.TABLE_NAME +
                        "." + PropertyContract.LocationEntry._ID);
    }

    //location.location_setting = ?
    private static final String sLocationSettingSelection =
            PropertyContract.PropertyEntry.TABLE_NAME+
                    "." + PropertyContract.PropertyEntry.COLUMN_ADDRESS + " like ? ";

    //location.location_setting = ? AND date >= ?
    private static final String sLocationSettingWithStartDateSelection =
            PropertyContract.PropertyEntry.TABLE_NAME+
                    "." + PropertyContract.PropertyEntry.COLUMN_ADDRESS + " like ? AND " +
                    PropertyContract.PropertyEntry.COLUMN_DATE + " >= ? ";

    //location.location_setting = ? AND date = ?
    private static final String sAddressSelection =
                    PropertyContract.PropertyEntry.COLUMN_ADDRESS + " = ? ";

    private Cursor getWeatherByLocationSetting(Uri uri, String[] projection, String sortOrder) {
        String locationSetting = PropertyContract.PropertyEntry.getLocationSettingFromUri(uri);
        long startDate = PropertyContract.PropertyEntry.getStartDateFromUri(uri);

        String[] selectionArgs;
        String selection;

        if (startDate == 0) {
            selection = sLocationSettingSelection;
            selectionArgs = new String[]{"%" +locationSetting + "%"};
        } else {
            selectionArgs = new String[]{"%" +locationSetting + "%", Long.toString(startDate)};
            selection = sLocationSettingWithStartDateSelection;
        }

        return sWeatherByLocationSettingQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
        );
    }

    private Cursor getPropertyByAddress(
            Uri uri, String[] projection, String sortOrder) {
        String address = PropertyContract.PropertyEntry.getAddressFromUri(uri);

        return sWeatherByLocationSettingQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                projection,
                sAddressSelection,
                new String[]{address},
                null,
                null,
                sortOrder
        );
    }

    /*
        Students: Here is where you need to create the UriMatcher. This UriMatcher will
        match each URI to the PROPERTY, PROPERTY_WITH_LOCATION, PROPERTY_WITH_LOCATION_AND_ADDRESS,
        and LOCATION integer constants defined above.  You can test this by uncommenting the
        testUriMatcher test within TestUriMatcher.
     */
    static UriMatcher buildUriMatcher() {
        // I know what you're thinking.  Why create a UriMatcher when you can use regular
        // expressions instead?  Because you're not crazy, that's why.

        // All paths added to the UriMatcher have a corresponding code to return when a match is
        // found.  The code passed into the constructor represents the code to return for the root
        // URI.  It's common to use NO_MATCH as the code for this case.
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = PropertyContract.CONTENT_AUTHORITY;

        // For each type of URI you want to add, create a corresponding code.
        matcher.addURI(authority, PropertyContract.PATH_PROPERTY, PROPERTY);
        matcher.addURI(authority, PropertyContract.PATH_PROPERTY + "/*", PROPERTY_WITH_LOCATION);
        matcher.addURI(authority, PropertyContract.PATH_PROPERTY + "/*/*", PROPERTY_WITH_LOCATION_AND_ADDRESS);

        matcher.addURI(authority, PropertyContract.PATH_LOCATION, LOCATION);
        return matcher;
    }

    /*
        Students: We've coded this for you.  We just create a new WeatherDbHelper for later use
        here.
     */
    @Override
    public boolean onCreate() {
        mOpenHelper = new PropertyDbHelper(getContext());
        return true;
    }

    @Override
    public String getType(Uri uri) {

        // Use the Uri Matcher to determine what kind of URI this is.
        final int match = sUriMatcher.match(uri);

        switch (match) {
            // Student: Uncomment and fill out these two cases
            case PROPERTY_WITH_LOCATION_AND_ADDRESS:
                return PropertyContract.PropertyEntry.CONTENT_ITEM_TYPE;
            case PROPERTY_WITH_LOCATION:
                return PropertyContract.PropertyEntry.CONTENT_TYPE;
            case PROPERTY:
                return PropertyContract.PropertyEntry.CONTENT_TYPE;
            case LOCATION:
                return PropertyContract.LocationEntry.CONTENT_TYPE;

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        // Here's the switch statement that, given a URI, will determine what kind of request it is,
        // and query the database accordingly.
        Cursor retCursor;
        switch (sUriMatcher.match(uri)) {
            // "property/*/*"
            case PROPERTY_WITH_LOCATION_AND_ADDRESS:
            {
                retCursor = getPropertyByAddress(uri, projection, sortOrder);
                break;
            }
            // "property/*"
            case PROPERTY_WITH_LOCATION: {
                retCursor = getWeatherByLocationSetting(uri, projection, sortOrder);
                break;
            }
            // "property"
            case PROPERTY: {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        PropertyContract.PropertyEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            // "location"
            case LOCATION: {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        PropertyContract.LocationEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        retCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return retCursor;
    }

    /*
        Student: Add the ability to insert Locations to the implementation of this function.
     */
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        Uri returnUri;
        Log.i(LOG_TAG, "Uri:" + uri);
        switch (match) {
            case PROPERTY: {
                long _id = db.insert(PropertyContract.PropertyEntry.TABLE_NAME, null, values);
                if ( _id > 0 )
                    returnUri = PropertyContract.PropertyEntry.buildLocationUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            case LOCATION: {
                long _id = db.insert(PropertyContract.LocationEntry.TABLE_NAME, null, values);
                if ( _id > 0 )
                    returnUri = PropertyContract.LocationEntry.buildLocationUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsDeleted;
        // this makes delete all rows return the number of rows deleted
        if ( null == selection ) selection = "1";
        switch (match) {
            case PROPERTY:
                rowsDeleted = db.delete(
                        PropertyContract.PropertyEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case LOCATION:
                rowsDeleted = db.delete(
                        PropertyContract.LocationEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        // Because a null deletes all rows
        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsDeleted;
    }

    @Override
    public int update(
            Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsUpdated;

        switch (match) {
            case PROPERTY:
                rowsUpdated = db.update(PropertyContract.PropertyEntry.TABLE_NAME, values, selection,
                        selectionArgs);
                break;
            case LOCATION:
                rowsUpdated = db.update(PropertyContract.LocationEntry.TABLE_NAME, values, selection,
                        selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsUpdated;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PROPERTY:
                db.beginTransaction();
                int returnCount = 0;
                try {
                    for (ContentValues value : values) {
                        long _id = db.insert(PropertyContract.PropertyEntry.TABLE_NAME, null, value);
                        if (_id != -1) {
                            returnCount++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                getContext().getContentResolver().notifyChange(uri, null);
                return returnCount;
            default:
                return super.bulkInsert(uri, values);
        }
    }

    // You do not need to call this method. This is a method specifically to assist the testing
    // framework in running smoothly. You can read more at:
    // http://developer.android.com/reference/android/content/ContentProvider.html#shutdown()
    @Override
    @TargetApi(11)
    public void shutdown() {
        mOpenHelper.close();
        super.shutdown();
    }
}