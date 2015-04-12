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
package com.alley.android.ppi.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.alley.android.ppi.app.data.PropertyContract;
import com.alley.android.ppi.app.sync.PropertyPriceSyncAdapter;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.concurrent.TimeUnit;

public class OverviewFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, SwipeRefreshLayout.OnRefreshListener, OnMapReadyCallback, GoogleMap.OnMarkerClickListener {
    public static final String LOG_TAG = OverviewFragment.class.getSimpleName();
    private OverviewAdapter mOverviewAdapter;

    private ListView mListView;
    private int mPosition = ListView.INVALID_POSITION;
    private boolean mUseTodayLayout;
    private SwipeRefreshLayout mRefreshLayout;

    private static final String SELECTED_KEY = "selected_position";

    private GoogleMap mMap = null;

    private static final int FORECAST_TOP_LEVEL_LOADER = 0;

    private float latitude = 0;
    private float longitude = 0;
    private float zoom = 0;

    private static final String[] PROPERTY_LIST_COLUMNS = {
            // In this case the id needs to be fully qualified with a table name, since
            // the content provider joins the location & property tables in the background
            // (both have an _id column)
            // On the one hand, that's annoying.  On the other, you can search the weather table
            // using the location set by the user, which is only in the Location table.
            // So the convenience is worth it.
            PropertyContract.PropertyEntry.TABLE_NAME + "." + PropertyContract.PropertyEntry._ID,
            PropertyContract.PropertyEntry.COLUMN_DATE,
            PropertyContract.PropertyEntry.COLUMN_PRICE,
            PropertyContract.PropertyEntry.COLUMN_ADDRESS,
            PropertyContract.PropertyEntry.COLUMN_NUM_BEDS,
            PropertyContract.LocationEntry.COLUMN_SEARCH_STRING_USED,
            PropertyContract.PropertyEntry.COLUMN_PROP_TYPE_ID,
            PropertyContract.PropertyEntry.COLUMN_LATITUDE,
            PropertyContract.PropertyEntry.COLUMN_LONGTITUDE
    };

    // These indices are tied to PROPERTY_LIST_COLUMNS.  If PROPERTY_LIST_COLUMNS changes, these
    // must change.
    static final int COL_WEATHER_ID = 0;
    static final int COL_DATE = 1;
    static final int COL_PRICE = 2;
    static final int COL_DESCRIPTION = 3;
    static final int COLUMN_NUM_BEDS = 4;
    static final int COL_SEARCH_STRING_USED = 5;
    static final int COLUMN_PROP_TYPE_ID = 6;
    static final int COL_COORD_LAT = 7;
    static final int COL_COORD_LONG = 8;

    @Override
    public void onRefresh() {
        Toast.makeText(getActivity(), "Staring refresh of properties", Toast.LENGTH_LONG);
        PropertyPriceSyncAdapter.syncImmediately(getActivity());
        mRefreshLayout.setRefreshing(false);
    }

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callback {
        /**
         * DetailFragmentCallback for when an item has been selected.
         */
        public void onItemSelected(Uri dateUri);
    }

    public OverviewFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Log.i(LOG_TAG, "onCreate");
        setHasOptionsMenu(true);
        readCameraSettingsFromPreferences();
    }

    private void readCameraSettingsFromPreferences() {
        if (latitude == 0 && longitude == 0 && zoom == 0) {
            SharedPreferences settings = getActivity().getSharedPreferences("MAP_SETTINGS", 0);
            latitude = settings.getFloat("latitude", (float) 0);
            longitude = settings.getFloat("longitude", (float) 0);
            zoom = settings.getFloat("zoom", 0);
            //Log.i(LOG_TAG, "readCameraSettingsFromPreferences read from preferences latitude:" + latitude + " longtitude:" + longitude + " zoom:" + zoom);
        }
    }

    private void restoreMapSettings() {
        if (mMap != null) {
            readCameraSettingsFromPreferences();
            //Log.i(LOG_TAG, "restoreMapSettings latitude:" + latitude + " longtitude:" + longitude + " zoom:" + zoom);
            LatLng startPosition = new LatLng(latitude, longitude); //with longitude and latitude
            CameraUpdate camPos = CameraUpdateFactory.newLatLngZoom(startPosition, zoom);
            mMap.moveCamera(camPos);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            updateWeather();
            return true;
        }
        if (id == R.id.action_map) {
            openPreferredLocationInMap();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

         mOverviewAdapter = new OverviewAdapter(getActivity(), null, 0);

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        SupportMapFragment m = ((SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.property_map));
        m.getMapAsync(this);

        mRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.refresh);
        mRefreshLayout.setColorSchemeResources(R.color.primary_dark);
        mRefreshLayout.setOnRefreshListener(this);
        TypedValue typed_value = new TypedValue();
        getActivity().getTheme().resolveAttribute(android.support.v7.appcompat.R.attr.actionBarSize, typed_value, true);
        mRefreshLayout.setProgressViewOffset(false, 0, getResources().getDimensionPixelSize(typed_value.resourceId));
        Log.i(OverviewFragment.LOG_TAG, "setting refreshing to true from onCreateView");
        mRefreshLayout.setRefreshing(true);
        Log.i(OverviewFragment.LOG_TAG, "setting mRefreshLayout " + mRefreshLayout);

        // Get a reference to the ListView, and attach this adapter to it.
        mListView = (ListView) rootView.findViewById(R.id.listview_forecast);
        mListView.setAdapter(mOverviewAdapter);
        View emptyView = rootView.findViewById(R.id.frame_empty);
        Log.i(LOG_TAG, "setting empty view " + emptyView);
        mListView.setEmptyView(emptyView);

        // We'll call our MainActivity
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                // CursorAdapter returns a cursor at the correct position for getItem(), or null
                // if it cannot seek to that position.
                Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
                if (cursor != null) {
                    String locationSetting = Utility.getPreferredLocation(getActivity());
                    ((Callback) getActivity())
                            .onItemSelected(PropertyContract.PropertyEntry.buildPropertyWithAddress(cursor.getString(COL_DESCRIPTION)
                            ));
                }
                mPosition = position;
            }
        });

        // If there's instance state, mine it for useful information.
        // The end-goal here is that the user never knows that turning their device sideways
        // does crazy lifecycle related things.  It should feel like some stuff stretched out,
        // or magically appeared to take advantage of room, but data or place in the app was never
        // actually *lost*.
        if (savedInstanceState != null && savedInstanceState.containsKey(SELECTED_KEY)) {
            // The listview probably hasn't even been populated yet.  Actually perform the
            // swapout in onLoadFinished.
            mPosition = savedInstanceState.getInt(SELECTED_KEY);
        }

        mOverviewAdapter.setUseTodayLayout(mUseTodayLayout);

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(FORECAST_TOP_LEVEL_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    // since we read the location when we create the loader, all we need to do is restart things
    void onLocationChanged() {
        updateWeather();
        getLoaderManager().restartLoader(FORECAST_TOP_LEVEL_LOADER, null, this);
        Log.i(OverviewFragment.LOG_TAG, "setting refreshing to true from onLocationChanged");
        //mRefreshLayout.setRefreshing(true);
    }

    private void updateWeather() {
        PropertyPriceSyncAdapter.syncImmediately(getActivity());
    }

    private void openPreferredLocationInMap() {
        // Using the URI scheme for showing a location found on a map.  This super-handy
        // intent can is detailed in the "Common Intents" page of Android's developer site:
        // http://developer.android.com/guide/components/intents-common.html#Maps
        if (null != mOverviewAdapter) {
            Cursor c = mOverviewAdapter.getCursor();
            if (null != c) {
                c.moveToPosition(0);
                String posLat = c.getString(COL_COORD_LAT);
                String posLong = c.getString(COL_COORD_LONG);
                Uri geoLocation = Uri.parse("geo:" + posLat + "," + posLong);

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(geoLocation);

                if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                    startActivity(intent);
                } else {
                    Log.d(LOG_TAG, "Couldn't call " + geoLocation.toString() + ", no receiving apps installed!");
                }
            }

        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // When tablets rotate, the currently selected list item needs to be saved.
        // When no item is selected, mPosition will be set to Listview.INVALID_POSITION,
        // so check for that before storing.
        if (mPosition != ListView.INVALID_POSITION) {
            outState.putInt(SELECTED_KEY, mPosition);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        // This is called when a new Loader needs to be created.  This
        // fragment only uses one loader, so we don't care about checking the id.

        // To only show current and future dates, filter the query to return weather only for
        // dates after or including today.
        // Sort order:  Ascending, by date.
        String sortOrder = PropertyContract.PropertyEntry.COLUMN_DATE + " DESC";
        String locationSetting = Utility.getPreferredLocation(getActivity());
        Uri propertyForLocationUri = null;

        propertyForLocationUri = PropertyContract.PropertyEntry.buildPropertiesLocationWithStartDate(
                locationSetting, System.currentTimeMillis() - TimeUnit.DAYS.toMillis(PropertyPriceSyncAdapter.NUM_DAYS_TO_CLEANUP));

        return new CursorLoader(getActivity(),
                propertyForLocationUri,
                PROPERTY_LIST_COLUMNS,
                null,
                null,
                sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

        if (loader.getId() == FORECAST_TOP_LEVEL_LOADER) {
            Toast.makeText(getActivity(), "Staring refresh of properties", Toast.LENGTH_LONG);
        }

        mOverviewAdapter.swapCursor(cursor);

        if (mPosition != ListView.INVALID_POSITION) {
            // If we don't need to restart the loader, and there's a desired position to restore
            // to, do so now.
            mListView.smoothScrollToPosition(mPosition);
        }

        if (mRefreshLayout != null) {
            Log.i(OverviewFragment.LOG_TAG, "setting refreshing to false from onLoadFinished");
            mRefreshLayout.setRefreshing(false);
        } else {
            Log.i(OverviewFragment.LOG_TAG, "mRefreshLayout is null");
        }
        paintHousesOnMap(cursor);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(LOG_TAG, "onDestroy");
        if (mMap != null) {
            CameraPosition mMyCam = mMap.getCameraPosition();
            float longitude = (float) mMyCam.target.longitude;
            float latitude = (float) mMyCam.target.latitude;
            float zoom = mMyCam.zoom;

            SharedPreferences settings = getActivity().getSharedPreferences("MAP_SETTINGS", 0);
            SharedPreferences.Editor editor = settings.edit();
            editor.putFloat("longitude", longitude);
            editor.putFloat("latitude", latitude);
            editor.putFloat("zoom", zoom);

            editor.commit();
            Log.i(LOG_TAG, "saved camera positions latitude:" + latitude + " longitude:" + longitude + " zoom:" + zoom);
        }
    }

    private void paintHousesOnMap(Cursor cursor) {
        int count =0;
        if (cursor != null) {
            Log.i(LOG_TAG, "Cursor had " + cursor.getCount() + " entries, current position" + cursor.getPosition());
            if (mMap == null) {
                // Log.e(LOG_TAG, "Camera was null - ");
                return;
            }
            mMap.clear();
            int cursorCount = cursor.getCount();
            Log.i(LOG_TAG, "After movetoFirst Cursor had " + cursor.getCount() + " entries, current position" + cursor.getPosition());
            LatLng house = null;
            while (count < cursorCount) { // this sucks but moveToFirst loses first entry so cant use cursor.moveToNext
                cursor.moveToPosition(count);
                Log.i(LOG_TAG, "In loop Cursor had " + cursor.getCount() + " entries, current position" + cursor.getPosition());
                float lat = cursor.getFloat(COL_COORD_LAT);
                float lon = cursor.getFloat(COL_COORD_LONG);
                if (lat != 0 && lon != 0) {
                    house = new LatLng(lat, lon);
                    if (latitude == 0 && longitude == 0 && zoom == 0 && lat != 0 && lon != 0) {
                        if (house != null) {
                            Log.i(LOG_TAG, " moving to house " + house.toString());
                            latitude = lat;
                            longitude = lon;
                            zoom = 15;
                            CameraUpdate center = CameraUpdateFactory.newLatLngZoom(house, zoom);
                            mMap.moveCamera(center);
                        }
                    }
                    mMap.addMarker(new MarkerOptions()
                            .position(house)
                            .title(cursor.getString(OverviewFragment.COL_PRICE))
                            .snippet(cursor.getString(OverviewFragment.COL_SEARCH_STRING_USED) + "/" + cursor.getString(OverviewFragment.COL_DESCRIPTION))
                            .icon(getBitmap(cursor.getString(OverviewFragment.COLUMN_NUM_BEDS))));
                    Log.i(LOG_TAG, count + " " + cursor.getString(OverviewFragment.COL_PRICE) + " " + house.toString());
                }
                else {
                    Log.i(LOG_TAG, count + " lat " + lat + " long " + lon);
                }
                count++;
            }
        }
    }

    public BitmapDescriptor getBitmap(String numberOfBeds) {
        if (numberOfBeds != null) {
            if (numberOfBeds.equalsIgnoreCase("1")) {
                return BitmapDescriptorFactory.fromResource(R.drawable.ic_1_bed);
            } else if (numberOfBeds.equalsIgnoreCase("2")) {
                return BitmapDescriptorFactory.fromResource(R.drawable.ic_2_bed);
            } else if (numberOfBeds.equalsIgnoreCase("3")) {
                return BitmapDescriptorFactory.fromResource(R.drawable.ic_3_bed);
            }
        }
        return BitmapDescriptorFactory.fromResource(R.drawable.ic_4_bed);
    }

    @Override
    public boolean onMarkerClick(com.google.android.gms.maps.model.Marker marker) {
        Log.i(LOG_TAG, "MARKER SNIPPET" + marker.getSnippet());
        String snippet = marker.getSnippet();
        if (snippet != null && snippet.indexOf("/") != -1) {
            String address = snippet.substring(snippet.indexOf("/") + 1);
            String location = snippet.substring(0, snippet.indexOf("/"));

            ((Callback) getActivity())
                    .onItemSelected(PropertyContract.PropertyEntry.buildPropertyWithAddress(address));
        }
        return true;
    }

    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;
        map.setOnMarkerClickListener(this);
        Cursor cursor = mOverviewAdapter.getCursor();
        Log.i(LOG_TAG, "map ready cursor:" + cursor);
        if (cursor != null) {
            paintHousesOnMap(cursor);
        }
        restoreMapSettings();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        Log.i(OverviewFragment.LOG_TAG, "setting refreshing to true from onLoaderReset");
        mRefreshLayout.setRefreshing(true);
        mOverviewAdapter.swapCursor(null);
    }

    public void setUseTodayLayout(boolean useTodayLayout) {
        mUseTodayLayout = useTodayLayout;
        if (mOverviewAdapter != null) {
            mOverviewAdapter.setUseTodayLayout(mUseTodayLayout);
        }
    }
}
