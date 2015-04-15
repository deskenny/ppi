package com.alley.android.ppi.app.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.test.AndroidTestCase;

import com.alley.android.ppi.app.utils.PollingCheck;

import java.util.Map;
import java.util.Set;

/*
    Students: These are functions and some test data to make it easier to test your database and
    Content Provider.  Note that you'll want your PropertyContract class to exactly match the one
    in our solution to use these as-given.
 */
public class TestUtilities extends AndroidTestCase {
    static final String TEST_LOCATION = "99705";
    static final String TEST_ADDRESS = "\"Aster,oids\"";
    static final long TEST_DATE = 1419033600L;  // December 20th, 2014
    static final long TEST_DATE_SECOND = 1619033600L;


    static void validateCursor(String error, Cursor valueCursor, ContentValues expectedValues) {
        assertTrue("Empty cursor returned. " + error, valueCursor.moveToFirst());
        validateCurrentRecord(error, valueCursor, expectedValues);
        valueCursor.close();
    }

    static void validateCurrentRecord(String error, Cursor valueCursor, ContentValues expectedValues) {
        Set<Map.Entry<String, Object>> valueSet = expectedValues.valueSet();
        for (Map.Entry<String, Object> entry : valueSet) {
            String columnName = entry.getKey();
            int idx = valueCursor.getColumnIndex(columnName);
            assertFalse("Column '" + columnName + "' not found. " + error, idx == -1);
            String expectedValue = entry.getValue().toString();
            assertEquals("Value '" + entry.getValue().toString() +
                    "' did not match the expected value '" +
                    expectedValue + "'. " + error, expectedValue, valueCursor.getString(idx));
        }
    }

    static ContentValues createWeatherValues(long locationRowId) {
        ContentValues weatherValues = new ContentValues();
        weatherValues.put(PropertyContract.PropertyEntry.COLUMN_LOC_KEY, locationRowId);
        weatherValues.put(PropertyContract.PropertyEntry.COLUMN_DATE, TEST_DATE);
        weatherValues.put(PropertyContract.PropertyEntry.COLUMN_LONGTITUDE, 1.1);
        weatherValues.put(PropertyContract.PropertyEntry.COLUMN_NUM_BEDS, 1.2);
        weatherValues.put(PropertyContract.PropertyEntry.COLUMN_SQUARE_AREA, 1.3);
        weatherValues.put(PropertyContract.PropertyEntry.COLUMN_PRICE, 75);
        weatherValues.put(PropertyContract.PropertyEntry.COLUMN_BROCHURE_PRICE, 65);
        weatherValues.put(PropertyContract.PropertyEntry.COLUMN_ADDRESS, TEST_ADDRESS);
        weatherValues.put(PropertyContract.PropertyEntry.COLUMN_PRICE, 5.5);
        weatherValues.put(PropertyContract.PropertyEntry.COLUMN_PROP_TYPE_ID, 321);

        return weatherValues;
    }

    static ContentValues createWeatherValuesSecond(long locationRowId) {
        ContentValues weatherValues = new ContentValues();
        weatherValues.put(PropertyContract.PropertyEntry.COLUMN_LOC_KEY, locationRowId);
        weatherValues.put(PropertyContract.PropertyEntry.COLUMN_DATE, TEST_DATE_SECOND);
        weatherValues.put(PropertyContract.PropertyEntry.COLUMN_LONGTITUDE, 2.1);
        weatherValues.put(PropertyContract.PropertyEntry.COLUMN_NUM_BEDS, 2.2);
        weatherValues.put(PropertyContract.PropertyEntry.COLUMN_SQUARE_AREA, 2.3);
        weatherValues.put(PropertyContract.PropertyEntry.COLUMN_PRICE, 85);
        weatherValues.put(PropertyContract.PropertyEntry.COLUMN_BROCHURE_PRICE, 95);
        weatherValues.put(PropertyContract.PropertyEntry.COLUMN_ADDRESS, "Second Aster,oids");
        weatherValues.put(PropertyContract.PropertyEntry.COLUMN_PRICE, 6.5);
        weatherValues.put(PropertyContract.PropertyEntry.COLUMN_PROP_TYPE_ID, 321);

        return weatherValues;
    }

    static ContentValues createImageValues() {
        ContentValues weatherValues = new ContentValues();
        byte [] testBytes = new byte[3];
        testBytes[0] = '1';
        testBytes[1]= '2';
        testBytes[2]= '3';
        weatherValues.put(PropertyContract.ImageEntry.COLUMN_ADDRESS, "testAddress");
        weatherValues.put(PropertyContract.ImageEntry.COLUMN_IS_PRIMARY, "1");
        weatherValues.put(PropertyContract.ImageEntry.COLUMN_PHOTO, testBytes);

        return weatherValues;
    }
    /*
        Students: You can uncomment this helper function once you have finished creating the
        LocationEntry part of the PropertyContract.
     */
    static ContentValues createNorthPoleLocationValues() {
        // Create a new map of values, where column names are the keys
        ContentValues testValues = new ContentValues();
        testValues.put(PropertyContract.LocationEntry.COLUMN_SEARCH_STRING_USED, TEST_LOCATION);
        testValues.put(PropertyContract.LocationEntry.COLUMN_CITY_NAME, "North Pole");

        return testValues;
    }

    /*
        Students: You can uncomment this function once you have finished creating the
        LocationEntry part of the PropertyContract as well as the WeatherDbHelper.
     */
    static long insertNorthPoleLocationValues(Context context) {
        // insert our test records into the database
        PropertyDbHelper dbHelper = new PropertyDbHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues testValues = TestUtilities.createNorthPoleLocationValues();

        long locationRowId;
        locationRowId = db.insert(PropertyContract.LocationEntry.TABLE_NAME, null, testValues);

        // Verify we got a row back.
        assertTrue("Error: Failure to insert North Pole Location Values", locationRowId != -1);

        return locationRowId;
    }

    /*
        Students: The functions we provide inside of TestProvider use this utility class to test
        the ContentObserver callbacks using the PollingCheck class that we grabbed from the Android
        CTS tests.

        Note that this only tests that the onChange function is called; it does not test that the
        correct Uri is returned.
     */
    static class TestContentObserver extends ContentObserver {
        final HandlerThread mHT;
        boolean mContentChanged;

        static TestContentObserver getTestContentObserver() {
            HandlerThread ht = new HandlerThread("ContentObserverThread");
            ht.start();
            return new TestContentObserver(ht);
        }

        private TestContentObserver(HandlerThread ht) {
            super(new Handler(ht.getLooper()));
            mHT = ht;
        }

        // On earlier versions of Android, this onChange method is called
        @Override
        public void onChange(boolean selfChange) {
            onChange(selfChange, null);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            mContentChanged = true;
        }

        public void waitForNotificationOrFail() {
            // Note: The PollingCheck class is taken from the Android CTS (Compatibility Test Suite).
            // It's useful to look at the Android CTS source for ideas on how to test your Android
            // applications.  The reason that PollingCheck works is that, by default, the JUnit
            // testing framework is not running on the main Android application thread.
            new PollingCheck(5000) {
                @Override
                protected boolean check() {
                    return mContentChanged;
                }
            }.run();
            mHT.quit();
        }
    }

    static TestContentObserver getTestContentObserver() {
        return TestContentObserver.getTestContentObserver();
    }


}

