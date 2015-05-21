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
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.alley.android.ppi.app.data.PropertyContract;
import com.alley.android.ppi.app.data.PropertyDbHelper;

/**
 * A placeholder fragment containing a simple view.
 */
public class DetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String LOG_TAG = DetailFragment.class.getSimpleName();
    static final String DETAIL_URI = "URI";

    private static final String PROPERTY_PRICE_SHARE_HASHTAG = " #PropertyPriceIreland";

    private ShareActionProvider mShareActionProvider;
    private String mPropertyDescription;
    private Uri mDetailUri;
    private Uri mDetailImageUri;

    private static final int DETAIL_LOADER = 0;
    private static final int DETAIL_IMAGE_LOADER = 1;

    private static final String[] DETAIL_COLUMNS = {
            PropertyContract.PropertyEntry.TABLE_NAME + "." + PropertyContract.PropertyEntry._ID,
            PropertyContract.PropertyEntry.COLUMN_DATE,
            PropertyContract.PropertyEntry.COLUMN_ADDRESS,
            PropertyContract.PropertyEntry.COLUMN_PRICE,
            PropertyContract.PropertyEntry.COLUMN_BROCHURE_PRICE,
            PropertyContract.PropertyEntry.COLUMN_NUM_BEDS,
            PropertyContract.PropertyEntry.COLUMN_SQUARE_AREA,
            PropertyContract.PropertyEntry.COLUMN_LATITUDE,
            PropertyContract.PropertyEntry.COLUMN_LONGTITUDE,
            PropertyContract.PropertyEntry.COLUMN_PROP_TYPE_ID,
            PropertyContract.PropertyEntry.COLUMN_CONTENT_DESC,
            PropertyContract.PropertyEntry.COLUMN_HEADER_FEATURES,
            PropertyContract.PropertyEntry.COLUMN_ACCOMMODATION,
            PropertyContract.PropertyEntry.COLUMN_BER_DETAILS,
            PropertyContract.PropertyEntry.COLUMN_BROCHURE_SUCCESS,
            PropertyContract.PropertyEntry.COLUMN_APARTMENT_HOUSE,

            // This works because the PropertyProvider returns location data joined with
            // property data, even though they're stored in two different tables.
            PropertyContract.LocationEntry.COLUMN_SEARCH_STRING_USED
    };

    private static final String[] IMAGE_COLUMNS = {
            PropertyContract.ImageEntry.TABLE_NAME + "." + PropertyContract.ImageEntry._ID,
            PropertyContract.ImageEntry.COLUMN_IS_PRIMARY,
            PropertyContract.ImageEntry.COLUMN_PHOTO
    };

    public static final int COL_IS_PRIMARY = 1;
    public static final int COL_PHOTO = 2;

    // These indices are tied to DETAIL_COLUMNS.  If DETAIL_COLUMNS changes, these
    // must change.
    public static final int COLUMN_PROP_TYPE_ID = 0;
    public static final int COL_PROPERTY_DATE = 1;
    public static final int COL_PROPERTY_DESC = 2;
    public static final int COL_PRICE = 3;
    public static final int COL_PROPERTY_MIN_TEMP = 4;
    public static final int COL_NUM_BEDS = 5;
    public static final int COL_SQUARE_AREA = 6;
    public static final int COL_LATITUDE = 7;
    public static final int COL_LONGTITUDE = 8;
    public static final int COL_PROPERTY_CONDITION_ID = 9;
    public static final int COL_CONTENT_DESCRIPTION = 10;
    public static final int COL_HEADER_FEATURES = 11;
    public static final int COLUMN_ACCOMMODATION = 12;
    public static final int COLUMN_BER = 13;
    public static final int COLUMN_BROCHURE_SUCCESS = 14;
    public static final int COLUMN_APARTMENT_HOUSE = 15;

    public static final int COL_LOCATION_SETTING = 16;

    private ImageView mIconView;
    private TextView mDateView;
    private TextView mDescriptionView;
    private TextView mPriceView;
    private TextView mContentDescription;
    private TextView mFeaturesDescription;
    private TextView mAccommodation;
    private TextView mBer;
    private TextView mTitleDescription;
    private TextView mTitleFeatures;
    private TextView mTitleAccommodation;
    private TextView mTitleBer;
    private RecyclerView recList;
    private DetailImageAdapter imageAdapter;

    public DetailFragment() {
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Bundle arguments = getArguments();
        if (arguments != null) {
            mDetailUri = arguments.getParcelable(DetailFragment.DETAIL_URI);
            String address = PropertyContract.PropertyEntry.getAddressFromUri(mDetailUri);
            mDetailImageUri = PropertyContract.ImageEntry.buildImageUriFromAddress(address);;
        }

        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);
        mIconView = (ImageView) rootView.findViewById(R.id.detail_icon);
        mDateView = (TextView) rootView.findViewById(R.id.detail_date_textview);
        mDescriptionView = (TextView) rootView.findViewById(R.id.detail_address_textview);
        mPriceView = (TextView) rootView.findViewById(R.id.detail_price_textview);
        mContentDescription = (TextView) rootView.findViewById(R.id.detail_content_description_textview);
        mFeaturesDescription = (TextView) rootView.findViewById(R.id.detail_features_textview);
        mAccommodation = (TextView) rootView.findViewById(R.id.detail_accommodation_textview);
        mBer = (TextView) rootView.findViewById(R.id.detail_ber_textview);


        mTitleDescription = (TextView) rootView.findViewById(R.id.detail_title_description_textview);
        mTitleFeatures = (TextView) rootView.findViewById(R.id.detail_title_features_textview);
        mTitleAccommodation = (TextView) rootView.findViewById(R.id.detail_title_accommodation_textview);
        mTitleBer = (TextView) rootView.findViewById(R.id.detail_title_ber_textview);

        recList = (RecyclerView) rootView.findViewById(R.id.cardList);
        recList.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(getActivity());
        llm.setOrientation(LinearLayoutManager.HORIZONTAL);
        recList.setLayoutManager(llm);

        imageAdapter = new DetailImageAdapter(this.getActivity(), null, getScreenSize());
        recList.setAdapter(imageAdapter);

        return rootView;
    }

    private Point getScreenSize() {
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        Point size = new Point();
        // dont use this anymore, probably can remove. Only if we wanted detail popup image full screen size.
        // display.getSize(size);
        return size;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.detailfragment, menu);

        MenuItem menuItem = menu.findItem(R.id.action_share);

        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);

        if (mPropertyDescription != null) {
            mShareActionProvider.setShareIntent(createSharePropertyIntent());
        }
    }

    private Intent createSharePropertyIntent() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, mPropertyDescription + PROPERTY_PRICE_SHARE_HASHTAG);
        return shareIntent;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(DETAIL_LOADER, null, this);
        getLoaderManager().initLoader(DETAIL_IMAGE_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    void onLocationChanged( String newLocation ) {
        Uri uri = mDetailUri;
        if (null != uri) {
            String address = PropertyContract.PropertyEntry.getAddressFromUri(uri);
            mDetailUri = PropertyContract.PropertyEntry.buildPropertyWithAddress(address);
            mDetailImageUri = PropertyContract.ImageEntry.buildImageUriFromAddress(address);;
            getLoaderManager().restartLoader(DETAIL_LOADER, null, this);
            getLoaderManager().restartLoader(DETAIL_IMAGE_LOADER, null, this);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if ( null != mDetailUri) {
            if (id == DETAIL_LOADER) {
                return new CursorLoader(
                        getActivity(),
                        mDetailUri,
                        DETAIL_COLUMNS,
                        null,
                        null,
                        null
                );
            }
            else if (id == DETAIL_IMAGE_LOADER)  {
                return new CursorLoader(
                        getActivity(),
                        mDetailImageUri,
                        IMAGE_COLUMNS,
                        null,
                        null,
                        null
                );
            }
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (loader.getId() == DETAIL_LOADER) {
            onLoadFinishedDetail(loader, cursor);
        } else {
            onLoadFinishedImages(loader, cursor);
        }
    }

    public void onLoadFinishedImages(Loader<Cursor> loader, Cursor cursor) {
        imageAdapter.changeCursor(cursor);
    }

    public void onLoadFinishedDetail(Loader<Cursor> loader, Cursor data) {
        if (data != null && data.moveToFirst()) {
            int propertyId = data.getInt(COL_PROPERTY_CONDITION_ID);
            int numberOfBeds = data.getInt(COL_NUM_BEDS);
            float squareArea = data.getFloat(COL_SQUARE_AREA);
            int aptHouse = data.getInt(COLUMN_APARTMENT_HOUSE);


//            mIconView.setImageResource(Utility.getArtResourceForPropType(propertyId, numberOfBeds));

            int artWork = Utility.getArtResourceForPropType(propertyId, numberOfBeds, aptHouse);
            mIconView.setImageResource(artWork);

            long date = data.getLong(COL_PROPERTY_DATE);
            String dateText = Utility.getFormattedMonthDay(getActivity(), date);

            if (R.drawable.art_spinner == artWork) {
                dateText =  dateText  + " - " + getActivity().getString(R.string.come_back_later);
                recList.setVisibility(View.GONE);
            }
            else if (artWork == R.drawable.art_second_hand || artWork == R.drawable.art_no_vat) {
                dateText =  dateText  + " - " + getActivity().getString(R.string.could_not_find_full_brochure);
                recList.setVisibility(View.GONE);
            }
            else {
                dateText = dateText + " - " + numberOfBeds + " bed, " + squareArea + "m²";
                recList.setVisibility(View.VISIBLE);
            }

            mDateView.setText(dateText);

            String description = data.getString(COL_PROPERTY_DESC);
            mDescriptionView.setText(description);

            mIconView.setContentDescription(description);

            String price = data.getString(COL_PRICE);
            String priceString = Utility.formatPrice(getActivity(), price);
            mPriceView.setText(priceString);

            String contentDescription = data.getString(COL_CONTENT_DESCRIPTION);
            mContentDescription.setText(contentDescription);

            String featuresDescription = data.getString(COL_HEADER_FEATURES);
            mFeaturesDescription.setText(featuresDescription);

            String accommodation = data.getString(COLUMN_ACCOMMODATION);
            mAccommodation.setText(accommodation);

            String ber = data.getString(COLUMN_BER);
            mBer.setText(ber);

            mPropertyDescription = String.format("%s - %s - %s", dateText, description, price);

            if (mShareActionProvider != null) {
                mShareActionProvider.setShareIntent(createSharePropertyIntent());
            }
            String brochureSuccess = data.getString(COLUMN_BROCHURE_SUCCESS);
            if (brochureSuccess != null && brochureSuccess.equalsIgnoreCase("1")) {
                hideShowBrochureFields(View.VISIBLE);
            }
            else {
                hideShowBrochureFields(View.INVISIBLE);
            }
        }
        // temporarily
        //delete();
    }


    private void delete() {
        SQLiteDatabase db = new PropertyDbHelper(getActivity()).getWritableDatabase();
        Log.i(LOG_TAG, "database is open - " + db.isOpen());

        int i = db.delete(PropertyContract.PropertyEntry.TABLE_NAME, PropertyContract.PropertyEntry.COLUMN_ADDRESS + " like ?", new String [] {"336%"});
        Log.i(LOG_TAG, "deleted - " + i + " records");
    }


    private void hideShowBrochureFields(int visibility) {
        mContentDescription.setVisibility(visibility);
        mFeaturesDescription.setVisibility(visibility);
        mAccommodation.setVisibility(visibility);
        mBer.setVisibility(visibility);

        if (mTitleDescription != null) {
            mTitleDescription.setVisibility(visibility);
        }
        if (mTitleFeatures != null) {
            mTitleFeatures.setVisibility(visibility);
        }
        if (mTitleAccommodation != null) {
            mTitleAccommodation.setVisibility(visibility);
        }
        if (mTitleBer != null) {
            mTitleBer.setVisibility(visibility);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (loader.getId() == DETAIL_IMAGE_LOADER) {
            recList.setAdapter(null);
        }
    }
}