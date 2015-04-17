// soup is leaky... do I want to keep this?

//package com.alley.android.ppi.app.sync;
//
//import android.accounts.Account;
//import android.content.AbstractThreadedSyncAdapter;
//import android.content.ContentProviderClient;
//import android.content.ContentValues;
//import android.content.Context;
//import android.content.SharedPreferences;
//import android.content.SyncResult;
//import android.net.Uri;
//import android.os.Bundle;
//import android.preference.PreferenceManager;
//import android.util.Log;
//
//import com.alley.android.ppi.app.R;
//import com.alley.android.ppi.app.Utility;
//import com.alley.android.ppi.app.data.PropertyContract;
//
//import org.json.JSONException;
//import org.jsoup.Jsoup;
//import org.jsoup.nodes.Document;
//import org.jsoup.nodes.Element;
//import org.jsoup.select.Elements;
//
//import java.io.BufferedReader;
//import java.io.IOException;
//import java.net.HttpURLConnection;
//import java.text.ParseException;
//import java.text.SimpleDateFormat;
//import java.util.Calendar;
//import java.util.GregorianCalendar;
//import java.util.Vector;
//
//public class OldSoup extends AbstractThreadedSyncAdapter {
//    public final String LOG_TAG = OldSoup.class.getSimpleName();
//
//    public void onPerformSyncWithJSoup(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
//        Log.d(LOG_TAG, "Starting sync");
//        GoogleAdapterHelper googleHelper = new GoogleAdapterHelper();
//
//        String locationQuery = Utility.getPreferredLocation(getContext());
//
//        // These two need to be declared outside the try/catch
//        // so that they can be closed in the finally block.
//        HttpURLConnection urlConnection = null;
//        BufferedReader reader = null;
//
//        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
//        try {
//            // https://www.propertypriceregister.ie/website/npsra/PPR/npsra-ppr.nsf/PPR-By-Date?SearchView&Query=%5Bdt_execution_date%5D%3E%3D01%2F01%2F2015%2520AND%2520%255Bdt_execution_date%255D%253C01%2F2%2F2015%2520AND%2520%255Baddress%255D%3D*downpatrick*%2520AND%2520%255Bdc_county%255D%3D%2522Dublin%2522&County=Dublin&Year=2015&StartMonth=01&EndMonth=02&Address=downpatrick
//            // https://www.propertypriceregister.ie/website/npsra/PPR/npsra-ppr.nsf/PPR-By-Date?SearchView&Query=%5Bdt_execution_date%5D%3E=01/01/2015%20AND%20%5Bdt_execution_date%5D%3C01/2/2015%20AND%20%5Baddress%5D=*downpatrick*%20AND%20%5Bdc_county%5D=%22Dublin%22&County=Dublin&Year=2015&StartMonth=01&EndMonth=02&Address=downpatrick
//            // https://www.propertypriceregister.ie/Website/npsra/PPR/npsra-ppr.nsf/PPR-By-Date?SearchView&Query=%5Bdt_execution_date%5D%3E=01/01/2015%20AND%20%5Bdt_execution_date%5D%3C01/5/2015%20AND%20%5Baddress%5D=*downpatrick*%20AND%20%5Bdc_county%5D=%22Dublin%22&County=Dublin&Year=2015&StartMonth=01&EndMonth=04&Address=downpatrick
//            final String PROPERTY_PRICE_BASE_URL = "https://www.propertypriceregister.ie/website/npsra/PPR/npsra-ppr.nsf/";
//            final String FORECAST_BASE_URL = PROPERTY_PRICE_BASE_URL + "PPR-By-Date?SearchView&";
//            final String QUERY_PARAM = "Query";
//            final String COUNTY_PARAM = "County";
//            final String YEAR_PARAM = "Year";
//            final String START_MONTH_PARAM = "StartMonth";
//            final String END_MONTH_PARAM = "EndMonth";
//            final String ADDRESS_PARAM = "Address";
//
//            Context context = getContext();
//            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
//            String countyKey = context.getString(R.string.pref_county_key);
//            String locationKey = context.getString(R.string.pref_location_key);
//            String county = prefs.getString(countyKey,
//                    context.getString(R.string.pref_county_dublin));
//            String location = prefs.getString(locationKey,
//                    context.getString(R.string.pref_location_default));
//
//            Calendar cal = GregorianCalendar.getInstance();
//            String fromDate = "01/01/" + cal.get(Calendar.YEAR);
//            String toDate = cal.get(Calendar.DAY_OF_MONTH) + "/" + (cal.get(Calendar.MONTH) + 1) + "/" + cal.get(Calendar.YEAR);
//            Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
//                    .appendQueryParameter("Start", "1")
//                    .appendQueryParameter("SearchMax", "0")
//                    .appendQueryParameter("SearchOrder", "4")
//                    .appendQueryParameter(QUERY_PARAM, "[dt_execution_date]>=" + fromDate + " AND [dt_execution_date]<" + toDate + " AND [address]=*" + location + "* AND [dc_county]=\"" + county + "\"")
//                    .appendQueryParameter(YEAR_PARAM, "" + cal.get(Calendar.YEAR))
//                    .appendQueryParameter(START_MONTH_PARAM, "01")
//                    .appendQueryParameter(END_MONTH_PARAM, "" + cal.get(Calendar.MONTH))
//                    .appendQueryParameter(ADDRESS_PARAM, location)
//                    .build();
//
//            String url2 = builtUri.toString();
//            Document doc = Jsoup.connect(url2).get();
//            //long locationId = addLocation(locationQuery, county);
//
//            Vector<ContentValues> cVVector = new Vector<ContentValues>(4);
//            Log.i(LOG_TAG, "URL: " + url2);
//
//            for (Element table : doc.select("table[class=resultsTable]")) {
//
//                int loopcount = 0;
//                // Identify all the table row's(tr)
//                for (Element row : table.select("tr")) {
//                    if (loopcount == 0) {
//                        loopcount++;
//                        continue;
//                    }
//                    // Identify all the table cell's(td)
//                    Elements tds = row.select("td");
//
//                    ContentValues propertyValues = new ContentValues();
//
//                    // Retrive Jsoup Elements
//                    // Get the first td
//                    String firstTd = tds.get(0).text();
//                    if (firstTd == null || firstTd.equalsIgnoreCase("No Results Found")) {
//                        Log.i(LOG_TAG, "no results founds so we're skipping");
//                        return;
//                    }
//                    String address = tds.get(2).text();
//                    propertyValues.put(PropertyContract.PropertyEntry.COLUMN_DATE, sdf.parse(firstTd).getTime() + loopcount); // adding a loop count just to make the date unique
//                    propertyValues.put(PropertyContract.PropertyEntry.COLUMN_PRICE, tds.get(1).text());
//                    propertyValues.put(PropertyContract.PropertyEntry.COLUMN_ADDRESS, address);
//                    String link = tds.get(2).select("a").first().attr("abs:href");
//
//                    Log.i(LOG_TAG, "link was: " + link);
//
//                    // all this stuff here, just because dont want to delete yet
//                    propertyValues.put(PropertyContract.PropertyEntry.COLUMN_LOC_KEY, locationId);
//                    propertyValues.put(PropertyContract.PropertyEntry.COLUMN_BROCHURE_PRICE, "");
//                    propertyValues.put(PropertyContract.PropertyEntry.COLUMN_LONGTITUDE, "0.0");
//
////                    readDetailsPage(link, propertyValues);
////                    googleHelper.readGoogle(address, "", propertyValues);
//                    cVVector.add(propertyValues);
//                }
//            }
//
//            addPropertyPrice(cVVector);
//        } catch (
//                ParseException pe
//                )
//
//        {
//            Log.e(LOG_TAG, "Error parsing ", pe);
//        } catch (
//                IOException e
//                )
//
//        {
//            Log.e(LOG_TAG, "Error ", e);
//            // If the code didn't successfully get the property data, there's no point in attempting
//            // to parse it.
//        } catch (
//                JSONException e
//                )
//
//        {
//            Log.e(LOG_TAG, e.getMessage(), e);
//            e.printStackTrace();
//        } finally
//
//        {
//            if (urlConnection != null) {
//                urlConnection.disconnect();
//            }
//            if (reader != null) {
//                try {
//                    reader.close();
//                } catch (final IOException e) {
//                    Log.e(LOG_TAG, "Error closing stream", e);
//                }
//            }
//        }
//
//        return;
//    }
//
//}
