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

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.format.Time;
import android.util.Log;

import com.alley.android.ppi.app.data.PropertyContract;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Utility {

    public final static int VAT_EXCLUSIVE = 1;
    public final static int SECOND_HAND = 2;
    public final static int DEFAULT_TYPE = 3;

    public final static String LOG_TAG = Utility.class.getSimpleName();

    public static String standardiseAddress(String in) {
        if (in != null) {
            String rVal = in.trim().toLowerCase();
            if (!rVal.endsWith(".")) {
                rVal += rVal + ".";
            }
            return rVal;
        }
        return null;
    }

    public static int getPreferredNumberOfDaysToKeep(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String numOfDays = prefs.getString(context.getString(R.string.pref_numberOfDaysToKeepProperty),
                context.getString(R.string.pref_numberOfDaysToKeepPropertyDefault));
        if (numOfDays != null) {
            return Integer.parseInt(numOfDays);
        }
        return 365;
    }

    public static String getPreferredLocation(Context context) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String rVal =  prefs.getString(context.getString(R.string.pref_location_key),
                context.getString(R.string.pref_location_default));
        if (rVal == null || rVal.equalsIgnoreCase("")) {
            String countyKey = context.getString(R.string.pref_county_key);
            String county = prefs.getString(countyKey,
                    context.getString(R.string.pref_county_dublin));
            rVal = county;
        }
        return rVal;
    }

    public static String formatPrice(Context context, String price) {
        Log.i(LOG_TAG, "formatting:" + price);
        String rVal = price;
        if (price != null && !price.equalsIgnoreCase("")) {
            price = price.replace("€", "");
            price = price.replace(",", "");
            if (price.indexOf('.') != -1) {
                price = price.substring(0, price.indexOf('.'));
            }
            Log.i(LOG_TAG, "converting:" + price);
            BigDecimal bdPrice = new BigDecimal(price);
            NumberFormat f = NumberFormat.getInstance();
            rVal = f.format(bdPrice.divide(new BigDecimal(1000), BigDecimal.ROUND_HALF_UP)) + "K";
        }
        return rVal;
    }

    static String formatDate(long dateInMilliseconds) {
        Date date = new Date(dateInMilliseconds);
        return DateFormat.getDateInstance().format(date);
    }

    public static final String DATE_FORMAT = "yyyyMMdd";

    public static String getFriendlyDayString(Context context, long dateInMillis) {
        Time time = new Time();
        time.setToNow();
        long currentTime = System.currentTimeMillis();
        int julianDay = Time.getJulianDay(dateInMillis, time.gmtoff);
        int currentJulianDay = Time.getJulianDay(currentTime, time.gmtoff);

        if (julianDay == currentJulianDay) {
            String today = context.getString(R.string.today);
            int formatId = R.string.format_full_friendly_date;
            return String.format(context.getString(
                    formatId,
                    today,
                    getFormattedMonthDay(context, dateInMillis)));
        } else {
            SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
            return shortenedDateFormat.format(dateInMillis);
        }
    }

    public static String getFormattedMonthDay(Context context, long dateInMillis) {
        Time time = new Time();
        time.setToNow();
        SimpleDateFormat dbDateFormat = new SimpleDateFormat(Utility.DATE_FORMAT);
        SimpleDateFormat monthDayFormat = new SimpleDateFormat("MMMM dd");
        String monthDayString = monthDayFormat.format(dateInMillis);
        return monthDayString;
    }

    public static int getIconResourceForPropType(int propTypeId, int numBeds) {
        if (numBeds == 1) {
            Log.i(LOG_TAG, "getting 1 bed icon");
            return R.drawable.ic_1_bed;
        } else if (numBeds == 2) {
            Log.i(LOG_TAG, "getting 2 bed icon");
            return R.drawable.ic_2_bed;
        } else if (numBeds == 3) {
            Log.i(LOG_TAG, "getting 3 bed icon");
            return R.drawable.ic_3_bed;
        } else if (numBeds > 3) {
            Log.i(LOG_TAG, "getting 4 bed icon");
            return R.drawable.ic_4_bed;
        }

        if (propTypeId == VAT_EXCLUSIVE) {
            Log.i(LOG_TAG, "getting VAT_EXCLUSIVE icon");
            return R.drawable.ic_no_vat;
        } else if (propTypeId == SECOND_HAND) {
            Log.i(LOG_TAG, "getting SECOND_HAND icon");
            return R.drawable.ic_second_hand;
        }
        Log.i(LOG_TAG, "getting DEFAULT icon");
        return R.drawable.ic_spinner;
    }

    public static int getArtResourceForPropType(int propTypeId, int numBeds) {
        if (numBeds == 1) {
            Log.i(LOG_TAG, "getting 1 bed icon");
            return R.drawable.art_1_bed;
        } else if (numBeds == 2) {
            Log.i(LOG_TAG, "getting 2 bed icon");
            return R.drawable.art_2_bed;
        } else if (numBeds == 3) {
            Log.i(LOG_TAG, "getting 3 bed icon");
            return R.drawable.art_3_bed;
        } else if (numBeds > 3) {
            Log.i(LOG_TAG, "getting 4 bed icon");
            return R.drawable.art_4_bed;
        }
        if (propTypeId == VAT_EXCLUSIVE) {
            Log.i(LOG_TAG, "getting VAT_EXCLUSIVE art");
            return R.drawable.art_no_vat;
        } else if (propTypeId == SECOND_HAND) {
            Log.i(LOG_TAG, "getting SECOND_HAND art");
            return R.drawable.art_second_hand;
        }
        Log.i(LOG_TAG, "getting DEFAULT art");
        return R.drawable.art_spinner;
    }

    public static boolean isMatchedAddress(ContentValues brochureValues, String address) {
        return isMatchedAddress((String) brochureValues.get(PropertyContract.PropertyEntry.COLUMN_ADDRESS), address);
    }

    public static boolean isMatchedAddress(String brochureAddress, String searchedAddress) {

        if (brochureAddress != null && searchedAddress != null) {
            brochureAddress = brochureAddress.toLowerCase().replaceAll("road", "rd").replaceAll("street", "st").replaceAll("\\,|\\.|\\;", "");
            searchedAddress = searchedAddress.toLowerCase().replaceAll("road", "rd").replaceAll("street", "st").replaceAll("\\,|\\.|\\;", "");
            if (brochureAddress.length() > 15 && searchedAddress.length() > 15) {
                brochureAddress = brochureAddress.substring(0, 15);
                searchedAddress = searchedAddress.substring(0, 15);
            }
            if (brochureAddress.equalsIgnoreCase(searchedAddress)) {
                Log.i(LOG_TAG, "AddressMatch: '" + brochureAddress + "' and '" + searchedAddress + "' is the same");
                return true;
            }
        }
        Log.i(LOG_TAG, "Address: '" + brochureAddress + "' and '" + searchedAddress + "' not the same");
        return false;
    }
}

  