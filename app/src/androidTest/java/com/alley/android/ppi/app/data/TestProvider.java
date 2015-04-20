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

import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.test.AndroidTestCase;
import android.util.Log;

import com.alley.android.ppi.app.data.PropertyContract.LocationEntry;


public class TestProvider extends AndroidTestCase {

    public static final String LOG_TAG = TestProvider.class.getSimpleName();


    public void deleteAllRecordsFromProvider() {
        mContext.getContentResolver().delete(
                PropertyContract.PropertyEntry.CONTENT_URI,
                null,
                null
        );
        mContext.getContentResolver().delete(
                LocationEntry.CONTENT_URI,
                null,
                null
        );

        Cursor cursor = mContext.getContentResolver().query(
                PropertyContract.PropertyEntry.CONTENT_URI,
                null,
                null,
                null,
                null
        );
        assertEquals("Error: Records not deleted from property table during delete", 0, cursor.getCount());
        cursor.close();

        cursor = mContext.getContentResolver().query(
                LocationEntry.CONTENT_URI,
                null,
                null,
                null,
                null
        );
        assertEquals("Error: Records not deleted from Location table during delete", 0, cursor.getCount());
        cursor.close();
    }

    public void deleteAllRecords() {
        deleteAllRecordsFromProvider();
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deleteAllRecords();
    }

    public void testProviderRegistry() {
        PackageManager pm = mContext.getPackageManager();

        ComponentName componentName = new ComponentName(mContext.getPackageName(),
                PropertyProvider.class.getName());
        try {
            ProviderInfo providerInfo = pm.getProviderInfo(componentName, 0);

            assertEquals("Error: PropertyProvider registered with authority: " + providerInfo.authority +
                    " instead of authority: " + PropertyContract.CONTENT_AUTHORITY,
                    providerInfo.authority, PropertyContract.CONTENT_AUTHORITY);
        } catch (PackageManager.NameNotFoundException e) {
            assertTrue("Error: PropertyProvider not registered at " + mContext.getPackageName(),
                    false);
        }
    }

    public void testGetType() {
        // content://com.alley.android.ppi.app/property/
        String type = mContext.getContentResolver().getType(PropertyContract.PropertyEntry.CONTENT_URI);
        // vnd.android.cursor.dir/com.alley.android.ppi.app/property
        assertEquals("Error: the PropertyEntry CONTENT_URI should return PropertyEntry.CONTENT_TYPE",
                PropertyContract.PropertyEntry.CONTENT_TYPE, type);

        String testLocation = "94074";
        // content://com.alley.android.ppi.app/property/94074
        type = mContext.getContentResolver().getType(
                PropertyContract.PropertyEntry.buildPropertyLocation(testLocation));
        // vnd.android.cursor.dir/com.alley.android.ppi.app/property
        assertEquals("Error: the PropertyEntry CONTENT_URI with location should return PropertyEntry.CONTENT_TYPE",
                PropertyContract.PropertyEntry.CONTENT_TYPE, type);

        long testDate = 1419120000L; // December 21st, 2014
        // content://com.alley.android.ppi.app/property/94074/20140612
        type = mContext.getContentResolver().getType(
                PropertyContract.PropertyEntry.buildPropertyWithAddress("this,WOULD,be,a,test, address"));
        // vnd.android.cursor.item/com.alley.android.ppi.app/property/1419120000
        assertEquals("Error: the PropertyEntry CONTENT_URI with location and date should return PropertyEntry.CONTENT_ITEM_TYPE",
                PropertyContract.PropertyEntry.CONTENT_ITEM_TYPE, type);

        // content://com.alley.android.ppi.app/location/
        type = mContext.getContentResolver().getType(LocationEntry.CONTENT_URI);
        // vnd.android.cursor.dir/com.alley.android.ppi.app/location
        assertEquals("Error: the LocationEntry CONTENT_URI should return LocationEntry.CONTENT_TYPE",
                LocationEntry.CONTENT_TYPE, type);
    }


    public void testBasicPropertyQuery() {
        PropertyDbHelper dbHelper = new PropertyDbHelper(mContext);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues testValues = TestUtilities.createTestLocationValues();
        long locationRowId = TestUtilities.insertHaroldsCrossLocationValues(mContext);

        ContentValues propertyValues = TestUtilities.createPropertyValues(locationRowId);

        long propertyRowId = db.insert(PropertyContract.PropertyEntry.TABLE_NAME, null, propertyValues);
        assertTrue("Unable to Insert PropertyEntry into the Database", propertyRowId != -1);

        db.close();

        Cursor propertyCursor = mContext.getContentResolver().query(
                PropertyContract.PropertyEntry.CONTENT_URI,
                null,
                null,
                null,
                null
        );

        TestUtilities.validateCursor("testBasicPropertyQuery", propertyCursor, propertyValues);
    }

    public void testBasicLocationQueries() {
        PropertyDbHelper dbHelper = new PropertyDbHelper(mContext);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues testValues = TestUtilities.createTestLocationValues();
        long locationRowId = TestUtilities.insertHaroldsCrossLocationValues(mContext);

        Cursor locationCursor = mContext.getContentResolver().query(
                LocationEntry.CONTENT_URI,
                null,
                null,
                null,
                null
        );

        TestUtilities.validateCursor("testBasicLocationQueries, location query", locationCursor, testValues);

        if ( Build.VERSION.SDK_INT >= 19 ) {
            assertEquals("Error: Location Query did not properly set NotificationUri",
                    locationCursor.getNotificationUri(), LocationEntry.CONTENT_URI);
        }
    }

    public void testUpdateLocation() {
        ContentValues values = TestUtilities.createTestLocationValues();

        Uri locationUri = mContext.getContentResolver().
                insert(LocationEntry.CONTENT_URI, values);
        long locationRowId = ContentUris.parseId(locationUri);

        assertTrue(locationRowId != -1);
        Log.d(LOG_TAG, "New row id: " + locationRowId);

        ContentValues updatedValues = new ContentValues(values);
        updatedValues.put(LocationEntry._ID, locationRowId);
        updatedValues.put(LocationEntry.COLUMN_CITY_NAME, "Santa's Village");

        Cursor locationCursor = mContext.getContentResolver().query(LocationEntry.CONTENT_URI, null, null, null, null);

        TestUtilities.TestContentObserver tco = TestUtilities.getTestContentObserver();
        locationCursor.registerContentObserver(tco);

        int count = mContext.getContentResolver().update(
                LocationEntry.CONTENT_URI, updatedValues, LocationEntry._ID + "= ?",
                new String[] { Long.toString(locationRowId)});
        assertEquals(count, 1);

        tco.waitForNotificationOrFail();

        locationCursor.unregisterContentObserver(tco);
        locationCursor.close();

        Cursor cursor = mContext.getContentResolver().query(
                LocationEntry.CONTENT_URI,
                null,
                LocationEntry._ID + " = " + locationRowId,
                null,
                null
        );

        TestUtilities.validateCursor("testUpdateLocation.  Error validating location entry update.",
                cursor, updatedValues);

        cursor.close();
    }


    public void testInsertReadProvider() {
        ContentValues testValues = TestUtilities.createTestLocationValues();

        TestUtilities.TestContentObserver tco = TestUtilities.getTestContentObserver();
        mContext.getContentResolver().registerContentObserver(LocationEntry.CONTENT_URI, true, tco);
        Uri locationUri = mContext.getContentResolver().insert(LocationEntry.CONTENT_URI, testValues);

        tco.waitForNotificationOrFail();
        mContext.getContentResolver().unregisterContentObserver(tco);

        long locationRowId = ContentUris.parseId(locationUri);

        assertTrue(locationRowId != -1);

        Cursor cursor = mContext.getContentResolver().query(
                LocationEntry.CONTENT_URI,
                null,
                null,
                null,
                null
        );

        TestUtilities.validateCursor("testInsertReadProvider. Error validating LocationEntry.",
                cursor, testValues);

        ContentValues propertyValues = TestUtilities.createPropertyValues(locationRowId);
        tco = TestUtilities.getTestContentObserver();

        mContext.getContentResolver().registerContentObserver(PropertyContract.PropertyEntry.CONTENT_URI, true, tco);

        Uri propertyInsertUri = mContext.getContentResolver()
                .insert(PropertyContract.PropertyEntry.CONTENT_URI, propertyValues);
        assertTrue(propertyInsertUri != null);

        tco.waitForNotificationOrFail();
        mContext.getContentResolver().unregisterContentObserver(tco);

        Cursor propertyCursor = mContext.getContentResolver().query(
                PropertyContract.PropertyEntry.CONTENT_URI,  // Table to Query
                null, // leaving "columns" null just returns all the columns.
                null, // cols for "where" clause
                null, // values for "where" clause
                null // columns to group by
        );

        TestUtilities.validateCursor("testInsertReadProvider. Error validating PropertyEntry insert.",
                propertyCursor, propertyValues);

        // Get the joined Property data for a specific date
        propertyCursor = mContext.getContentResolver().query(
                PropertyContract.PropertyEntry.buildPropertyWithAddress(TestUtilities.TEST_ADDRESS),
                null,
                null,
                null,
                null
        );
        TestUtilities.validateCursor("testInsertReadProvider.  Error validating joined Property and Location data for a specific address.",
                propertyCursor, propertyValues);
    }

    public void testDeleteRecords() {
        testInsertReadProvider();

        TestUtilities.TestContentObserver locationObserver = TestUtilities.getTestContentObserver();
        mContext.getContentResolver().registerContentObserver(LocationEntry.CONTENT_URI, true, locationObserver);

        TestUtilities.TestContentObserver propertyObserver = TestUtilities.getTestContentObserver();
        mContext.getContentResolver().registerContentObserver(PropertyContract.PropertyEntry.CONTENT_URI, true, propertyObserver);

        deleteAllRecordsFromProvider();

        locationObserver.waitForNotificationOrFail();
        propertyObserver.waitForNotificationOrFail();

        mContext.getContentResolver().unregisterContentObserver(locationObserver);
        mContext.getContentResolver().unregisterContentObserver(propertyObserver);
    }


    static private final int BULK_INSERT_RECORDS_TO_INSERT = 10;
    static ContentValues[] createBulkInsertPropertyValues(long locationRowId) {
        long currentTestDate = TestUtilities.TEST_DATE;
        long millisecondsInADay = 1000*60*60*24;
        ContentValues[] returnContentValues = new ContentValues[BULK_INSERT_RECORDS_TO_INSERT];

        for ( int i = 0; i < BULK_INSERT_RECORDS_TO_INSERT; i++, currentTestDate+= millisecondsInADay ) {
            ContentValues propertyValues = new ContentValues();
            propertyValues.put(PropertyContract.PropertyEntry.COLUMN_LOC_KEY, locationRowId);
            propertyValues.put(PropertyContract.PropertyEntry.COLUMN_DATE, currentTestDate + i);
            propertyValues.put(PropertyContract.PropertyEntry.COLUMN_LONGTITUDE, 1.1);
            propertyValues.put(PropertyContract.PropertyEntry.COLUMN_NUM_BEDS, 1.2 + 0.01 * (float) i);
            propertyValues.put(PropertyContract.PropertyEntry.COLUMN_SQUARE_AREA, 1.3 - 0.01 * (float) i);
            propertyValues.put(PropertyContract.PropertyEntry.COLUMN_PRICE, 75 + i);
            propertyValues.put(PropertyContract.PropertyEntry.COLUMN_BROCHURE_PRICE, 65 - i);
            propertyValues.put(PropertyContract.PropertyEntry.COLUMN_ADDRESS, "Asteroids " + i);
            propertyValues.put(PropertyContract.PropertyEntry.COLUMN_PRICE, 5.5 + 0.2 * (float) i);
            propertyValues.put(PropertyContract.PropertyEntry.COLUMN_PROP_TYPE_ID, 321);
            returnContentValues[i] = propertyValues;
        }
        return returnContentValues;
    }

    public void testBulkInsert() {
        ContentValues testValues = TestUtilities.createTestLocationValues();
        Uri locationUri = mContext.getContentResolver().insert(LocationEntry.CONTENT_URI, testValues);
        long locationRowId = ContentUris.parseId(locationUri);

        assertTrue(locationRowId != -1);

        Cursor cursor = mContext.getContentResolver().query(
                LocationEntry.CONTENT_URI,
                null, // leaving "columns" null just returns all the columns.
                null, // cols for "where" clause
                null, // values for "where" clause
                null  // sort order
        );

        TestUtilities.validateCursor("testBulkInsert. Error validating LocationEntry.",
                cursor, testValues);

        ContentValues[] bulkInsertContentValues = createBulkInsertPropertyValues(locationRowId);

        TestUtilities.TestContentObserver propertyObserver = TestUtilities.getTestContentObserver();
        mContext.getContentResolver().registerContentObserver(PropertyContract.PropertyEntry.CONTENT_URI, true, propertyObserver);

        int insertCount = mContext.getContentResolver().bulkInsert(PropertyContract.PropertyEntry.CONTENT_URI, bulkInsertContentValues);

        propertyObserver.waitForNotificationOrFail();
        mContext.getContentResolver().unregisterContentObserver(propertyObserver);

        assertEquals(insertCount, BULK_INSERT_RECORDS_TO_INSERT);

        cursor = mContext.getContentResolver().query(
                PropertyContract.PropertyEntry.CONTENT_URI,
                null, // leaving "columns" null just returns all the columns.
                null, // cols for "where" clause
                null, // values for "where" clause
                PropertyContract.PropertyEntry.COLUMN_DATE + " ASC"  // sort order == by DATE ASCENDING
        );

        assertEquals(cursor.getCount(), BULK_INSERT_RECORDS_TO_INSERT);

        cursor.moveToFirst();
        for ( int i = 0; i < BULK_INSERT_RECORDS_TO_INSERT; i++, cursor.moveToNext() ) {
            TestUtilities.validateCurrentRecord("testBulkInsert.  Error validating PropertyEntry " + i,
                    cursor, bulkInsertContentValues[i]);
        }
        cursor.close();
    }


    public void testInsertReadProviderImage() {
        ContentValues testValues = TestUtilities.createImageValues();

        TestUtilities.TestContentObserver tco = TestUtilities.getTestContentObserver();
        mContext.getContentResolver().registerContentObserver(PropertyContract.ImageEntry.CONTENT_URI, true, tco);
        Uri imageUri = mContext.getContentResolver().insert(PropertyContract.ImageEntry.CONTENT_URI, testValues);

        tco.waitForNotificationOrFail();
        mContext.getContentResolver().unregisterContentObserver(tco);

        long imageRowId = ContentUris.parseId(imageUri);

        assertTrue(imageRowId != -1);

        Cursor cursor = mContext.getContentResolver().query(
                PropertyContract.ImageEntry.CONTENT_URI,
                null, // leaving "columns" null just returns all the columns.
                null, // cols for "where" clause
                null, // values for "where" clause
                null  // sort order
        );

        assertTrue( "Error: No Records returned from location query", cursor.moveToFirst() );

        assertEquals(cursor.getString(1), "testAddress");
        byte [] blob = cursor.getBlob(2);
        assertEquals(blob[0], '1');
        assertEquals(blob[1], '2');
        assertEquals(blob[2], '3');

        cursor.close();

        cursor = mContext.getContentResolver().query(
                PropertyContract.ImageEntry.CONTENT_URI,
                null, // leaving "columns" null just returns all the columns.
                "address = 'testAddress'", // cols for "where" clause
                null, // values for "where" clause
                null  // sort order
        );

        assertTrue( "Error: No Records returned from location query", cursor.moveToFirst() );

        assertEquals(cursor.getString(1), "testAddress");
        blob = cursor.getBlob(2);
        assertEquals(blob[0], '1');
        assertEquals(blob[1], '2');
        assertEquals(blob[2], '3');
    }    
}
