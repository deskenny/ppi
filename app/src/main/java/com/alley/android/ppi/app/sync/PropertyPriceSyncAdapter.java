package com.alley.android.ppi.app.sync;

import android.accounts.Account;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncRequest;
import android.content.SyncResult;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.format.Time;
import android.util.Log;

import com.alley.android.ppi.app.MainActivity;
import com.alley.android.ppi.app.R;
import com.alley.android.ppi.app.Utility;
import com.alley.android.ppi.app.data.PropertyContract;

import org.json.JSONException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PropertyPriceSyncAdapter extends AbstractThreadedSyncAdapter {
    private String sortOrder = PropertyContract.PropertyEntry.COLUMN_DATE + " DESC";
    public int NUM_DAYS_TO_CLEANUP = 365;
    public final String LOG_TAG = PropertyPriceSyncAdapter.class.getSimpleName();

    private static final long DAY_IN_MILLIS = 1000 * 60 * 60 * 24;
    private static final int PPI_NOTIFICATION_ID = 3004;


    private static final String[] NOTIFY_PPI_PROJECTION = new String[]{
            PropertyContract.PropertyEntry.COLUMN_PROP_TYPE_ID,
            PropertyContract.PropertyEntry.COLUMN_PRICE,
            PropertyContract.PropertyEntry.COLUMN_BROCHURE_PRICE,
            PropertyContract.PropertyEntry.COLUMN_ADDRESS,
            PropertyContract.PropertyEntry.COLUMN_NUM_BEDS,
    };

    // these indices must match the projection
    private static final int INDEX_PROP_TYPE_ID = 0;
    private static final int INDEX_PRICE = 1;
    private static final int INDEX_MIN_TEMP = 2;
    private static final int INDEX_SHORT_DESC = 3;
    private static final int INDEX_NUM_BEDS = 4;

    private Pattern linkPattern = Pattern.compile("href=\"(.*?)\"");
    private Pattern stripLinkPattern = Pattern.compile("href=.*>(.*?)</a>");
    private GoogleAdapterHelper googleHelper = new GoogleAdapterHelper();

    public PropertyPriceSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    public static void syncImmediately(Context context) {
        SyncAccountHelper.syncImmediately(context);
    }

    public static void initializeSyncAdapter(Context context) {
        SyncAccountHelper.getSyncAccount(context);
    }

    // <a href="eStampUNID/UNID-FECC4897E7A9411A80257E0C004CF7E7?OpenDocument" tabindex="7">Apartment 3, Orwell Lodge, Orwell Road  Rathgar, Dublin 6</a>
    private String readLink(String in) {
        if (in != null) {
            Matcher m = linkPattern.matcher(in);
            String link = null;
            if (m.find()) {
                link = m.group(1); // this variable should contain the link URL
                return link;
            }
        }
        return in;
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        Log.d(LOG_TAG, "Starting sync");
        NUM_DAYS_TO_CLEANUP = Utility.getPreferredNumberOfDaysToKeep(getContext());
        Cursor newBrochuresCursor = null;
        try {
            newBrochuresCursor = getNewBrochureList();
            if (newBrochuresCursor.getCount() == 0) {
                doDeleteOld();
                doOverviewSync(account, extras, authority, provider, syncResult);
                doDetailSync(newBrochuresCursor, account, extras, authority, provider, syncResult);
            } else {
                doDetailSync(newBrochuresCursor, account, extras, authority, provider, syncResult);
            }

        } catch (Exception e) {
            Log.e(LOG_TAG, "Error in on perform sync", e);
        } finally {
            if (newBrochuresCursor != null) {
                newBrochuresCursor.close();
            }
        }

    }

    private void doDeleteOld() {
        // want to delete the old property brochures and also more importantly the images!!!
        // could I do cascade delete here, if I had proper foreign key?
        Time dayTime = new Time();
        dayTime.setToNow();

        int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

        dayTime = new Time();

        getContext().getContentResolver().delete(PropertyContract.PropertyEntry.CONTENT_URI,
                PropertyContract.PropertyEntry.COLUMN_DATE + " <= ?",
                new String[]{Long.toString(dayTime.setJulianDay(julianStartDay - NUM_DAYS_TO_CLEANUP))});

        getContext().getContentResolver().delete(PropertyContract.ImageEntry.CONTENT_URI,
                PropertyContract.ImageEntry.COLUMN_DATE + " <= ?",
                new String[]{Long.toString(dayTime.setJulianDay(julianStartDay - NUM_DAYS_TO_CLEANUP))});


    }

    private static final String sLoadBrochureSelection =
            PropertyContract.PropertyEntry.TABLE_NAME + "." + PropertyContract.PropertyEntry.COLUMN_BROCHURE_READ_ATTEMPTED + " = ?";


    private Cursor getNewBrochureList() {
        return getContext().getContentResolver().query(
                PropertyContract.PropertyEntry.CONTENT_URI,
                new String[]{PropertyContract.PropertyEntry._ID, PropertyContract.PropertyEntry.COLUMN_LOC_KEY,
                        PropertyContract.PropertyEntry.COLUMN_ADDRESS,
                        PropertyContract.PropertyEntry.COLUMN_PROPERTY_PRICE_REGISTER_URL,
                        PropertyContract.PropertyEntry.COLUMN_DATE,
                        PropertyContract.PropertyEntry.COLUMN_PRICE
                },
                PropertyContract.PropertyEntry.COLUMN_BROCHURE_READ_ATTEMPTED + " = ?",
                new String[]{"FALSE"},
                sortOrder);
    }

    private HashSet<String> getPreviouslyAttemptedAddresses() {
        Cursor cur = null;
        HashSet<String> addresses = new HashSet<String>();
        try {
            cur = getContext().getContentResolver().query(
                    PropertyContract.PropertyEntry.CONTENT_URI,
                    new String[]{
                            PropertyContract.PropertyEntry.COLUMN_ADDRESS},
                    PropertyContract.PropertyEntry.COLUMN_BROCHURE_READ_ATTEMPTED + " = ?",
                    new String[]{"1"},
                    null);
            while (cur.moveToNext()) {
                addresses.add(cur.getString(0));
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error in getPreviouslyAttemptedAddresses", e);
        } finally {
            if (cur != null) {
                cur.close();
            }
        }
        return addresses;
    }

    private void doDetailSync(Cursor newBrochuresCursor, Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        int brochureCount = 0;
        try {
            Log.d(LOG_TAG, "Starting detail sync");
            // get a list of the properties that have not yet had their brochure download attempted.
            int loopCount = 0;

            Log.i(LOG_TAG, "cursor had " + newBrochuresCursor.getCount() + " records indicated brochure not read");
            Vector<ContentValues> cVVector = new Vector<ContentValues>(4);

            // look up the property id
            while (newBrochuresCursor.moveToNext()) {
                ContentValues values = new ContentValues();
                String propertyId = newBrochuresCursor.getString(0);
                String locationId = newBrochuresCursor.getString(1);
                String address = newBrochuresCursor.getString(2);
                String ppiURL = newBrochuresCursor.getString(3);
                String date = newBrochuresCursor.getString(4);
                String price = newBrochuresCursor.getString(5);
                //values.put(PropertyContract.PropertyEntry._ID, propertyId);
                values.put(PropertyContract.PropertyEntry.COLUMN_LOC_KEY, locationId);
                values.put(PropertyContract.PropertyEntry.COLUMN_ADDRESS, address);
                values.put(PropertyContract.PropertyEntry.COLUMN_PROPERTY_PRICE_REGISTER_URL, ppiURL);
                values.put(PropertyContract.PropertyEntry.COLUMN_DATE, date);
                values.put(PropertyContract.PropertyEntry.COLUMN_PRICE, price);
                values.put(PropertyContract.PropertyEntry.COLUMN_BROCHURE_PRICE, price);

                Log.i(LOG_TAG, "doDetailSync:address. " + address);
                //Log.i(LOG_TAG, "attempting lookup of " + propertyId + ". " + address + ". " + ppiURL);
                // read the brochure details
                readDetailsPage(ppiURL, values);
                boolean brochureFound = googleHelper.readGoogle(address, values, getContext());
                cVVector.add(values);

                if (brochureFound) {
                    brochureCount++;
                }
                if (loopCount % 3 == 0) {
                    addPropertyPrices(cVVector);
                }
                loopCount++;
            }
            Log.i(LOG_TAG, "Attempted read the details for " + cVVector.size() + " properties");
            Log.i(LOG_TAG, "Read brochures for " + brochureCount + " properties");
            addPropertyPrices(cVVector);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error in detail sync", e);
        }
    }

    private void doOverviewSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        Log.d(LOG_TAG, "Starting overivew sync");

        String locationQuery = Utility.getPreferredLocation(getContext());

        HashSet<String> previousAddresses = getPreviouslyAttemptedAddresses();

        // These two need to be declared outside the try/catch
        // so that they can be closed in the finally block.
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");


        try {
            // https://www.propertypriceregister.ie/website/npsra/PPR/npsra-ppr.nsf/PPR-By-Date?SearchView&Query=%5Bdt_execution_date%5D%3E%3D01%2F01%2F2015%2520AND%2520%255Bdt_execution_date%255D%253C01%2F2%2F2015%2520AND%2520%255Baddress%255D%3D*downpatrick*%2520AND%2520%255Bdc_county%255D%3D%2522Dublin%2522&County=Dublin&Year=2015&StartMonth=01&EndMonth=02&Address=downpatrick
            // https://www.propertypriceregister.ie/website/npsra/PPR/npsra-ppr.nsf/PPR-By-Date?SearchView&Query=%5Bdt_execution_date%5D%3E=01/01/2015%20AND%20%5Bdt_execution_date%5D%3C01/2/2015%20AND%20%5Baddress%5D=*downpatrick*%20AND%20%5Bdc_county%5D=%22Dublin%22&County=Dublin&Year=2015&StartMonth=01&EndMonth=02&Address=downpatrick
            // https://www.propertypriceregister.ie/Website/npsra/PPR/npsra-ppr.nsf/PPR-By-Date?SearchView&Query=%5Bdt_execution_date%5D%3E=01/01/2015%20AND%20%5Bdt_execution_date%5D%3C01/5/2015%20AND%20%5Baddress%5D=*downpatrick*%20AND%20%5Bdc_county%5D=%22Dublin%22&County=Dublin&Year=2015&StartMonth=01&EndMonth=04&Address=downpatrick
            final String PROPERTY_PRICE_BASE_URL = "https://www.propertypriceregister.ie/website/npsra/PPR/npsra-ppr.nsf/";
            final String FORECAST_BASE_URL = PROPERTY_PRICE_BASE_URL + "PPR-By-Date?SearchView&";
            final String QUERY_PARAM = "Query";
            final String COUNTY_PARAM = "County";
            final String YEAR_PARAM = "Year";
            final String START_MONTH_PARAM = "StartMonth";
            final String END_MONTH_PARAM = "EndMonth";
            final String ADDRESS_PARAM = "Address";

            Context context = getContext();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            String countyKey = context.getString(R.string.pref_county_key);
            String locationKey = context.getString(R.string.pref_location_key);
            String monthsBackKey = context.getString(R.string.pref_months_ago_to_search);

            String county = prefs.getString(countyKey,
                    context.getString(R.string.pref_county_dublin));
            String location = prefs.getString(locationKey,
                    context.getString(R.string.pref_location_default));

            String sMonthsAgoToSearch = prefs.getString(monthsBackKey,
                    context.getString(R.string.pref_months_ago_to_search_default));


            int monthsAgoToSearch = 3;
            try {
                monthsAgoToSearch = Integer.parseInt(sMonthsAgoToSearch);
            }
            catch (Exception pe) {
                Log.e(LOG_TAG, "problem reading monthsAgoToSearch" + sMonthsAgoToSearch);
            }
            Calendar cal = GregorianCalendar.getInstance();
            Calendar currentTime = GregorianCalendar.getInstance();
            cal.add(Calendar.MONTH, -(monthsAgoToSearch));
            String fromDate = "01/" + (cal.get(Calendar.MONTH)+1) + "/" + cal.get(Calendar.YEAR);
            String toDate = currentTime.get(Calendar.DAY_OF_MONTH) + "/" + (currentTime.get(Calendar.MONTH) + 1) + "/" + currentTime.get(Calendar.YEAR);
            Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                    .appendQueryParameter("Start", "1")
                    .appendQueryParameter("SearchMax", "0")
                    .appendQueryParameter("SearchOrder", "4")
                    .appendQueryParameter(QUERY_PARAM, "[dt_execution_date]>=" + fromDate + " AND [dt_execution_date]<" + toDate + " AND [address]=*" + location + "* AND [dc_county]=\"" + county + "\"")
                    .appendQueryParameter(YEAR_PARAM, "" + currentTime.get(Calendar.YEAR))
                    .appendQueryParameter(START_MONTH_PARAM, "" + cal.get(Calendar.MONTH))
                    .appendQueryParameter(END_MONTH_PARAM, "" + currentTime.get(Calendar.MONTH))
                    .appendQueryParameter(ADDRESS_PARAM, location)
                    .build();

            long locationId = addLocation(locationQuery, county);

            Vector<ContentValues> cVVector = new Vector<ContentValues>(4);

            String sURI = builtUri.toString();
            URL url = new URL(sURI);
            Log.i(LOG_TAG, "Checking register with: " + sURI);

            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                // Nothing to do.
                return;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));
            //Pattern patt = Pattern.compile("<td>[.*]</td>.*<td>[.*]</td>.*<td>[.*]</td>.*<td>[.*]</td>", Pattern.DOTALL | Pattern.UNIX_LINES);
            advanceReader(reader, "<table class=\"resultsTable\">");
            advanceReader(reader, "<tbody>");

            String content = getURLContent(reader);
            for (String line : content.split("<tr>")) {
                ContentValues propertyValues = new ContentValues();
                if (line == null || line.contains("No Results Found")) {
                    Log.i(LOG_TAG, "no results founds so we're skipping");
                    return;
                }
                String[] td = line.split("<td>");
                if (td != null && td.length >= 4) {
                    propertyValues.put(PropertyContract.PropertyEntry.COLUMN_DATE, sdf.parse(stripTrailingTd(td[1])).getTime());
                    propertyValues.put(PropertyContract.PropertyEntry.COLUMN_PRICE, stripTrailingTd(td[2]));
                    String fullAddress = td[3];
                    Log.i(LOG_TAG, "fullAddress:address. " + fullAddress);
                    String address = Utility.standardiseAddress(stripLink(fullAddress));
                    if (previousAddresses.contains(address)) {
                        continue; // we did this place before don't reinsert
                    }
                    propertyValues.put(PropertyContract.PropertyEntry.COLUMN_ADDRESS, Utility.standardiseAddress(address));
                    String link = PROPERTY_PRICE_BASE_URL + readLink(fullAddress);
                    propertyValues.put(PropertyContract.PropertyEntry.COLUMN_PROPERTY_PRICE_REGISTER_URL, link);
                    propertyValues.put(PropertyContract.PropertyEntry.COLUMN_READ_FROM_REGISTER_DATE, System.currentTimeMillis());

                    // all this stuff here, just because dont want to delete yet
                    propertyValues.put(PropertyContract.PropertyEntry.COLUMN_LOC_KEY, locationId);
                    propertyValues.put(PropertyContract.PropertyEntry.COLUMN_BROCHURE_PRICE, "");
//                    propertyValues.put(PropertyContract.PropertyEntry.COLUMN_LONGTITUDE, "0.0");

                    cVVector.add(propertyValues);
                }
            }

            addPropertyPrices(cVVector);
        } catch (ParseException pe) {
            Log.e(LOG_TAG, "Error parsing ", pe);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error ", e);
            // If the code didn't successfully get the property data, there's no point in attempting
            // to parse it.
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }
        return;
    }

    private String stripTrailingTd(String in) {
        if (in != null && in.indexOf("<") > 0) {
            return in.substring(0, in.indexOf("<"));
        }
        return in;
    }

    private void addPropertyPrices(Vector<ContentValues> cVVector)
            throws JSONException {

        if (cVVector.size() > 0) {
            ContentValues[] cvArray = new ContentValues[cVVector.size()];
            cVVector.toArray(cvArray);
            getContext().getContentResolver().bulkInsert(PropertyContract.PropertyEntry.CONTENT_URI, cvArray);
            notifyProperties();
        }

        Log.d(LOG_TAG, "Sync Complete. " + cVVector.size() + " Inserted");

    }

    private void notifyProperties() {

        Context context = getContext();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String displayNotificationsKey = context.getString(R.string.pref_enable_notifications_key);
        boolean displayNotifications = prefs.getBoolean(displayNotificationsKey,
                Boolean.parseBoolean(context.getString(R.string.pref_enable_notifications_default)));

        if (displayNotifications) {
            String notificationsLastOpenKey = context.getString(R.string.pref_notifications_last_open_key);
            long lastAppOpenTime = prefs.getLong(notificationsLastOpenKey, System.currentTimeMillis());

            String lastNotificationKey = context.getString(R.string.pref_last_notification);
            long lastNotificationTime = prefs.getLong(lastNotificationKey, 0);

            // check to see how many properties have come in since last we opened... not sure about my logic here
            if (lastNotificationTime - lastAppOpenTime >= 0) {
                String locationQuery = Utility.getPreferredLocation(context);
                Uri propertyUri = PropertyContract.PropertyEntry.buildPropertyLocation(locationQuery);

                Cursor cursor = context.getContentResolver().query(propertyUri, NOTIFY_PPI_PROJECTION,
                        PropertyContract.PropertyEntry.COLUMN_READ_FROM_REGISTER_DATE + " > ?",
                        new String[] {String.valueOf(lastAppOpenTime)}, null);

                if (cursor.moveToFirst()) {

                    int numberRecords = cursor.getCount();
                    int iconId = Utility.getIconResourceForPropType(-1, 4);
                    Resources resources = context.getResources();
                    Bitmap largeIcon = BitmapFactory.decodeResource(resources,
                            Utility.getArtResourceForPropType(-1, 4));
                    String title = context.getString(R.string.app_name);

                    SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy");

                    NotificationCompat.Builder mBuilder =
                            new NotificationCompat.Builder(getContext())
                                    .setColor(resources.getColor(R.color.ppi_light_blue))
                                    .setSmallIcon(iconId)
                                    .setLargeIcon(largeIcon)
                                    .setContentTitle(title)
                                    .setContentText(numberRecords + " properties found matching " + locationQuery + " since " + sdf.format(lastAppOpenTime));

                    Intent resultIntent = new Intent(context, MainActivity.class);

                    TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
                    stackBuilder.addNextIntent(resultIntent);
                    PendingIntent resultPendingIntent =
                            stackBuilder.getPendingIntent(
                                    0,
                                    PendingIntent.FLAG_UPDATE_CURRENT
                            );
                    mBuilder.setContentIntent(resultPendingIntent);

                    NotificationManager mNotificationManager =
                            (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
                    mNotificationManager.notify(PPI_NOTIFICATION_ID, mBuilder.build());

                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putLong(lastNotificationKey, System.currentTimeMillis());
                    editor.commit();
                }
                cursor.close();
            }
        }
    }

    /**
     * Helper method to handle insertion of a new location in the property database.
     *
     * @param locationSetting The location string used to request updates from the server.
     * @param cityName        A human-readable city name, e.g "Mountain View"
     * @return the row ID of the added location.
     */
    long addLocation(String locationSetting, String cityName) {
        long locationId;

        // First, check if the location with this city name exists in the db
        Cursor locationCursor = getContext().getContentResolver().query(
                PropertyContract.LocationEntry.CONTENT_URI,
                new String[]{PropertyContract.LocationEntry._ID},
                PropertyContract.LocationEntry.COLUMN_SEARCH_STRING_USED + " = ?",
                new String[]{locationSetting},
                null);

        if (locationCursor.moveToFirst()) {
            int locationIdIndex = locationCursor.getColumnIndex(PropertyContract.LocationEntry._ID);
            locationId = locationCursor.getLong(locationIdIndex);
        } else {
            // Now that the content provider is set up, inserting rows of data is pretty simple.
            // First create a ContentValues object to hold the data you want to insert.
            ContentValues locationValues = new ContentValues();

            // Then add the data, along with the corresponding name of the data type,
            // so the content provider knows what kind of value is being inserted.
            locationValues.put(PropertyContract.LocationEntry.COLUMN_CITY_NAME, cityName);
            locationValues.put(PropertyContract.LocationEntry.COLUMN_SEARCH_STRING_USED, locationSetting);

            // Finally, insert location data into the database.
            Uri insertedUri = getContext().getContentResolver().insert(
                    PropertyContract.LocationEntry.CONTENT_URI,
                    locationValues
            );

            // The resulting URI contains the ID for the row.  Extract the locationId from the Uri.
            locationId = ContentUris.parseId(insertedUri);
        }

        locationCursor.close();
        // Wait, that worked?  Yes!
        return locationId;
    }

    /**
     * Helper method to schedule the sync adapter periodic execution
     */
    public static void configurePeriodicSync(Context context, int syncInterval, int flexTime) {
        Account account = SyncAccountHelper.getSyncAccount(context);
        String authority = context.getString(R.string.content_authority);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // we can enable inexact timers in our periodic sync
            SyncRequest request = new SyncRequest.Builder().
                    syncPeriodic(syncInterval, flexTime).
                    setSyncAdapter(account, authority).
                    setExtras(new Bundle()).build();
            ContentResolver.requestSync(request);
        } else {
            ContentResolver.addPeriodicSync(account,
                    authority, new Bundle(), syncInterval);
        }
    }


    private void readDetailsPage(String link, ContentValues propertyValues) throws IOException {
        Log.i(LOG_TAG, "link was: " + link);
        Document detailDoc = Jsoup.connect(link).get();
        boolean secondHand = false;
        boolean vatExclusive = false;
        for (Element detailtable : detailDoc.select("table[id=SaleInfo]")) {
            for (Element detailrow : detailtable.select("tr")) {
                Elements detailtds = detailrow.select("td");
                if (detailtds.get(0).text().equalsIgnoreCase("Description of Property:")) {
                    String propDescription = detailtds.get(1).text();
                    if (propDescription != null && propDescription.startsWith("Second")) {
                        secondHand = true;
                    }
                } else if (detailtds.get(0).text().equalsIgnoreCase("VAT Exclusive:")) {
                    String svatExclusive = detailtds.get(1).text();
                    if (svatExclusive != null && svatExclusive.startsWith("Yes")) {
                        vatExclusive = true;
                    }
                }
            }
        }
        calPropertyType(vatExclusive, secondHand, propertyValues);
    }

    private void calPropertyType(boolean vatExclusive, boolean secondHand, ContentValues propertyValues) {
        if (vatExclusive) {
            propertyValues.put(PropertyContract.PropertyEntry.COLUMN_PROP_TYPE_ID, Utility.VAT_EXCLUSIVE);
        } else if (secondHand) {
            propertyValues.put(PropertyContract.PropertyEntry.COLUMN_PROP_TYPE_ID, Utility.SECOND_HAND);
        } else {
            propertyValues.put(PropertyContract.PropertyEntry.COLUMN_PROP_TYPE_ID, Utility.DEFAULT_TYPE);
        }
    }

    private void advanceReader(BufferedReader reader, String stringToFind) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            //Log.i(LOG_TAG, "REad" + line);
            if (line.contains(stringToFind)) {
                break;
            }
        }
    }

    private String getURLContent(BufferedReader reader) throws IOException {
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.contains("</tbody>")) {
                break;
            }
            sb.append(line);
        }
        return sb.toString();
    }

    private String stripCloseTD(String in) {
        if (in != null) {
            int index = in.indexOf("</td>");
            if (index != -1) {
                return in.substring(0, index);
            }
        }
        return in;
    }

    // <a href="eStampUNID/UNID-FECC4897E7A9411A80257E0C004CF7E7?OpenDocument" tabindex="7">Apartment 3, Orwell Lodge, Orwell Road  Rathgar, Dublin 6</a>
    private String stripLink(String in) {
        if (in != null) {

            Matcher m = stripLinkPattern.matcher(in);
            String addr = null;
            if (m.find()) {
                addr = m.group(1);
                return addr;
            }
        }
        return in;
    }


}