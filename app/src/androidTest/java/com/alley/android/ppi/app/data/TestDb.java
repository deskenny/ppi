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

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.test.AndroidTestCase;

import java.util.HashSet;

public class TestDb extends AndroidTestCase {

    public static final String LOG_TAG = TestDb.class.getSimpleName();

    void deleteTheDatabase() {
        mContext.deleteDatabase(PropertyDbHelper.DATABASE_NAME);
    }

    public void setUp() {
        deleteTheDatabase();
    }

    public void testCreateDb() throws Throwable {
        final HashSet<String> tableNameHashSet = new HashSet<String>();
        tableNameHashSet.add(PropertyContract.LocationEntry.TABLE_NAME);
        tableNameHashSet.add(PropertyContract.PropertyEntry.TABLE_NAME);

        mContext.deleteDatabase(PropertyDbHelper.DATABASE_NAME);
        SQLiteDatabase db = new PropertyDbHelper(
                this.mContext).getWritableDatabase();
        assertEquals(true, db.isOpen());

        Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);

        assertTrue("Error: This means that the database has not been created correctly",
                c.moveToFirst());

        do {
            tableNameHashSet.remove(c.getString(0));
        } while( c.moveToNext() );

        assertTrue("Error: Your database was created without both the location entry and property entry tables",
                tableNameHashSet.isEmpty());

        c = db.rawQuery("PRAGMA table_info(" + PropertyContract.LocationEntry.TABLE_NAME + ")",
                null);

        assertTrue("Error: This means that we were unable to query the database for table information.",
                c.moveToFirst());

        final HashSet<String> locationColumnHashSet = new HashSet<String>();
        locationColumnHashSet.add(PropertyContract.LocationEntry._ID);
        locationColumnHashSet.add(PropertyContract.LocationEntry.COLUMN_CITY_NAME);
        locationColumnHashSet.add(PropertyContract.LocationEntry.COLUMN_SEARCH_STRING_USED);

        int columnNameIndex = c.getColumnIndex("name");
        do {
            String columnName = c.getString(columnNameIndex);
            locationColumnHashSet.remove(columnName);
        } while(c.moveToNext());

        assertTrue("Error: The database doesn't contain all of the required location entry columns",
                locationColumnHashSet.isEmpty());
        db.close();
    }

    public void testLocationTable() {
        insertLocation();
    }

    public void testPropertyTable() {

        long locationRowId = insertLocation();

        assertFalse("Error: Location Not Inserted Correctly", locationRowId == -1L);

        PropertyDbHelper dbHelper = new PropertyDbHelper(mContext);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues propertyValues = TestUtilities.createPropertyValues(locationRowId);

        long propertyRowId = db.insert(PropertyContract.PropertyEntry.TABLE_NAME, null, propertyValues);
        assertTrue(propertyRowId != -1);

        Cursor propertyCursor = db.query(
                PropertyContract.PropertyEntry.TABLE_NAME,
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertTrue( "Error: No Records returned from location query", propertyCursor.moveToFirst() );

        TestUtilities.validateCurrentRecord("testInsertReadDb propertyEntry failed to validate",
                propertyCursor, propertyValues);

        assertFalse( "Error: More than one record returned from property query",
                propertyCursor.moveToNext() );

        propertyCursor.close();
        dbHelper.close();
    }

    public long insertLocation() {
        PropertyDbHelper dbHelper = new PropertyDbHelper(mContext);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues testValues = TestUtilities.createTestLocationValues();

        long locationRowId;
        locationRowId = db.insert(PropertyContract.LocationEntry.TABLE_NAME, null, testValues);

        assertTrue(locationRowId != -1);

        Cursor cursor = db.query(
                PropertyContract.LocationEntry.TABLE_NAME,
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertTrue( "Error: No Records returned from location query", cursor.moveToFirst() );

        TestUtilities.validateCurrentRecord("Error: Location Query Validation Failed",
                cursor, testValues);

        assertFalse( "Error: More than one record returned from location query",
                cursor.moveToNext() );

        cursor.close();
        db.close();
        return locationRowId;
    }


    public void testDoubleDatePropertyTable() {
        long locationRowId = insertLocation();

        assertFalse("Error: Location Not Inserted Correctly", locationRowId == -1L);

        PropertyDbHelper dbHelper = new PropertyDbHelper(mContext);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues propertyValues = TestUtilities.createPropertyValues(locationRowId);

        long propertyRowId = db.insert(PropertyContract.PropertyEntry.TABLE_NAME, null, propertyValues);
        assertTrue(propertyRowId != -1);

        final long TEST_DATE_SECOND = 1419033600L;
        ContentValues propertyValuesSecond = TestUtilities.createPropertyValues(locationRowId);
        propertyValuesSecond.put(PropertyContract.PropertyEntry.COLUMN_ADDRESS, "Another Property");
        propertyValuesSecond.put(PropertyContract.PropertyEntry.COLUMN_DATE, TEST_DATE_SECOND);
        long propertyRowIdSecond = db.insert(PropertyContract.PropertyEntry.TABLE_NAME, null, propertyValuesSecond);
        assertTrue(propertyRowIdSecond != -1);


        Cursor propertyCursor = db.query(
                PropertyContract.PropertyEntry.TABLE_NAME,
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertTrue( "Error: No Records returned from location query", propertyCursor.moveToFirst() );

        assertEquals(2, propertyCursor.getCount());

        TestUtilities.validateCurrentRecord("Error: Location Query Validation Failed",
                propertyCursor, propertyValues);

        assertTrue(propertyCursor.moveToNext());

        TestUtilities.validateCurrentRecord("Error: Second Location Query Validation Failed",
                propertyCursor, propertyValuesSecond);

        propertyCursor.close();
        dbHelper.close();
    }

    public void testImageTable() {

        PropertyDbHelper dbHelper = new PropertyDbHelper(mContext);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues imageValues = TestUtilities.createImageValues();

        long imageRowId = db.insert(PropertyContract.ImageEntry.TABLE_NAME, null, imageValues);
        assertTrue(imageRowId != -1);

        Cursor propertyCursor = db.query(
                PropertyContract.ImageEntry.TABLE_NAME,
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertTrue( "Error: No Records returned from location query", propertyCursor.moveToFirst() );

        assertEquals(propertyCursor.getString(1), "testAddress");
        byte [] blob = propertyCursor.getBlob(2);
        assertEquals(blob[0], '1');
        assertEquals(blob[1], '2');
        assertEquals(blob[2], '3');


        assertFalse( "Error: More than one record returned from image query",
                propertyCursor.moveToNext() );

        propertyCursor.close();
        dbHelper.close();
    }
}
