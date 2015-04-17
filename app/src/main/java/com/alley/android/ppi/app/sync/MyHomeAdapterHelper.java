package com.alley.android.ppi.app.sync;


import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.format.Time;
import android.util.Log;

import com.alley.android.ppi.app.Utility;
import com.alley.android.ppi.app.data.PropertyContract;

import org.json.JSONException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MyHomeAdapterHelper {

    public final String LOG_TAG = MyHomeAdapterHelper.class.getSimpleName();

    public MyHomeAdapterHelper() {
    }

    // URL would be something like http://www.myhome.ie/residential/brochure/146-downpatrick-road-crumlin-dublin-12/2896646
    public boolean readMyHomeBrochurePage(String link, ContentValues brochureValues, String address, Context context) {
        try {
            Log.i(LOG_TAG, "URL: " + link);
            brochureValues.put(PropertyContract.PropertyEntry.COLUMN_MY_HOME_BROCHURE_URL, link);

            Document doc = Jsoup.connect(link).get();
            readDiv(doc, brochureValues, PropertyContract.PropertyEntry.COLUMN_ADDRESS, "brochureAddress");
            String addressFromBrochure = (String) brochureValues.get(PropertyContract.PropertyEntry.COLUMN_ADDRESS);
            Log.i(LOG_TAG, "addressFromBrochure: " + addressFromBrochure);
            addressFromBrochure = Utility.standardiseAddress(addressFromBrochure);
            if (Utility.isMatchedAddress(addressFromBrochure, address)) {
                brochureValues.remove(PropertyContract.PropertyEntry.COLUMN_ADDRESS);
                brochureValues.put(PropertyContract.PropertyEntry.COLUMN_ADDRESS, address);
                String detailedDescription = "";
                Elements elements = doc.select("h2[class=brochureDescription]");
                for (Element element : elements) {
                    detailedDescription = element.text();
                    // Element will be something like ..... Sale Agreed - 3 Bed Terraced House 60 m² / 646 ft² Sale Agreed
                    Log.i(LOG_TAG, "elementText: " + detailedDescription);
                    int indexOfBed = detailedDescription.indexOf("Bed");
                    int indexOfHyphen = detailedDescription.indexOf("-");
                    int indexOfMetresSquared = detailedDescription.indexOf("m²");

                    readMyHomeReadNumberBedrooms(detailedDescription, indexOfBed, indexOfHyphen, brochureValues);
                    readMyHomeSquareFootage(detailedDescription, indexOfBed, indexOfHyphen, indexOfMetresSquared, brochureValues);
                }
                readLocation(doc, brochureValues);
                readDiv(doc, brochureValues, PropertyContract.PropertyEntry.COLUMN_CONTENT_DESC, "contentDescription content0");
                readDiv(doc, brochureValues, PropertyContract.PropertyEntry.COLUMN_HEADER_FEATURES, "contentFeatures content1");
                readDiv(doc, brochureValues, PropertyContract.PropertyEntry.COLUMN_BER_DETAILS, "contentBER Details content2");
                readDiv(doc, brochureValues, PropertyContract.PropertyEntry.COLUMN_ACCOMMODATION, "contentAccommodation content3");
                storeImages(doc, brochureValues, context);
                calculateClass(brochureValues);
                return true;
            }
        } catch (Exception e ) {
            Log.e(LOG_TAG, e.getMessage());
        }
        return false;
    }


    private void calculateClass(ContentValues values) {
        String description = (String) values.get(PropertyContract.PropertyEntry.COLUMN_CONTENT_DESC);
        if (description != null) {
            if (description.contains("in need of modernisation")) {
                values.put(PropertyContract.PropertyEntry.COLUMN_CLASS, 0);
                return;
            }
            else if (description.contains("in need of some modernisation")) {
                values.put(PropertyContract.PropertyEntry.COLUMN_CLASS, 0);
                return;
            }
            else if (description.contains("excellent condition")) {
                values.put(PropertyContract.PropertyEntry.COLUMN_CLASS, 5);
                return;
            }
        }
        values.put(PropertyContract.PropertyEntry.COLUMN_CLASS, 3);
    }

    private void storeImages(Document doc, ContentValues propertyValues, Context context) throws JSONException {
        Vector<ContentValues> cVVectorImages = new Vector<ContentValues>();
        Elements elements = doc.select("img.colorboxGallery");
        boolean first = true;
        for (Element element : elements) {
            String imageSrc = element.absUrl("longdesc");
            byte[] bytes = getBitmap(imageSrc);
            if (bytes != null) {
                ContentValues values = new ContentValues();
                String address = (String) propertyValues.get(PropertyContract.PropertyEntry.COLUMN_ADDRESS);
                values.put(PropertyContract.ImageEntry.COLUMN_ADDRESS, address);
                values.put(PropertyContract.ImageEntry.COLUMN_IS_PRIMARY, first);
                values.put(PropertyContract.ImageEntry.COLUMN_PHOTO, bytes);
                values.put(PropertyContract.ImageEntry.COLUMN_DATE, System.currentTimeMillis());
                cVVectorImages.add(values);
                first = false;
                Log.i(LOG_TAG, "storeImages-address " + address);
                Log.d(LOG_TAG, "Image insert " + propertyValues.get(PropertyContract.PropertyEntry.COLUMN_ADDRESS) + ".");
            }
        }
        addImages(cVVectorImages, context);

    }


    private void addImages(Vector<ContentValues> cVVector, Context context)
            throws JSONException {

        Time dayTime = new Time();
        dayTime.setToNow();

        int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

        dayTime = new Time();


        int inserted = 0;
        // add to database
        if (cVVector.size() > 0) {
            ContentValues[] cvArray = new ContentValues[cVVector.size()];
            cVVector.toArray(cvArray);
            context.getContentResolver().bulkInsert(PropertyContract.ImageEntry.CONTENT_URI, cvArray);

            // delete old data so we don't build up an endless history
            context.getContentResolver().delete(PropertyContract.ImageEntry.CONTENT_URI,
                    PropertyContract.ImageEntry.COLUMN_DATE + " <= ?",
                    new String[]{Long.toString(dayTime.setJulianDay(julianStartDay - PropertyPriceSyncAdapter.NUM_DAYS_TO_CLEANUP))});

        }

        Log.d(LOG_TAG, "Image Sync Complete. " + cVVector.size() + " Inserted");

    }

    private byte[] getBitmap(String url) {
        try {
            Bitmap bitmap = null;
            URL imageUrl = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) imageUrl
                    .openConnection();
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);
            conn.setInstanceFollowRedirects(true);
            InputStream is = conn.getInputStream();

            Bitmap bmp = BitmapFactory.decodeStream(is, null, null);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            byte[] byteArray = stream.toByteArray();
            return byteArray;
        } catch (Throwable ex) {
            ex.printStackTrace();
            return null;
        }
    }

    // Decodes image and scales it to reduce memory consumption
    private Bitmap decodeFile(File f) {
        try {
            // Decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            FileInputStream stream1 = new FileInputStream(f);
            BitmapFactory.decodeStream(stream1, null, o);
            stream1.close();

            // Find the correct scale value. It should be the power of 2.
            // Recommended Size 512
            final int REQUIRED_SIZE = 70;
            int width_tmp = o.outWidth, height_tmp = o.outHeight;
            int scale = 1;
            while (true) {
                if (width_tmp / 2 < REQUIRED_SIZE
                        || height_tmp / 2 < REQUIRED_SIZE)
                    break;
                width_tmp /= 2;
                height_tmp /= 2;
                scale *= 2;
            }

            // Decode with inSampleSize
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            FileInputStream stream2 = new FileInputStream(f);
            Bitmap bitmap = BitmapFactory.decodeStream(stream2, null, o2);
            stream2.close();
            return bitmap;
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void readDiv(Document doc, ContentValues propertyValues, String columnName, String className) {
        Elements elements = doc.select("div[class=" + className + "]");
        for (Element element : elements) {
            String contentDescription = element.text();
            propertyValues.put(columnName, contentDescription);
            break;
        }
    }

    private Pattern latlongPat = Pattern.compile("\"Latitude\":(.+?),\"Longitude\":(.+?),"); // Regex for the value of the latitude

    // Map: {"Latitude":53.326577099999987,"Longitude":-6.3045311999999285,"IsAutoGeocoded":true,"Polygons":[]},
    private void readLocation(Document doc, ContentValues propertyValues) {
        Elements elementsLocation = doc.select("script");
        for (Element elementLoc : elementsLocation) {
            String javascript = elementLoc.html();
            if (javascript != null && !javascript.equalsIgnoreCase("") && javascript.contains("Latitude")) {
                Matcher m = latlongPat.matcher(javascript);
                if (m.find()) {
                    String latitude = m.group(1);
                    String longtitude = m.group(2);
                    Log.i(LOG_TAG, "Latitude: " + latitude); // Lat
                    Log.i(LOG_TAG, "Longtitude: " + longtitude); // Long
                    propertyValues.put(PropertyContract.PropertyEntry.COLUMN_LATITUDE, latitude);
                    propertyValues.put(PropertyContract.PropertyEntry.COLUMN_LONGTITUDE, longtitude);
                    break;
                }
            }
        }
    }

    private void readMyHomeReadNumberBedrooms(String detailedDescription, int indexOfBed, int indexOfHyphen, ContentValues propertyValues) {
        if (detailedDescription != null && indexOfBed != -1 && indexOfHyphen != -1 && indexOfBed > indexOfHyphen) {
            String sNumberBedroom = detailedDescription.substring(indexOfHyphen + 1, indexOfBed).trim();
            try {
                propertyValues.put(PropertyContract.PropertyEntry.COLUMN_NUM_BEDS, Integer.parseInt(sNumberBedroom));
            } catch (NumberFormatException nfe) {
                Log.e(LOG_TAG, "had a problem reading number of bedrooms from " + detailedDescription + " substring " + sNumberBedroom + " indexOfHyphen " + indexOfHyphen + " indexOfBed " + indexOfBed);
            }
        }
    }

    private void readMyHomeSquareFootage(String detailedDescription, int indexOfBed, int indexOfHyphen, int indexOfMetresSquared, ContentValues propertyValues) {
        if (detailedDescription != null && indexOfMetresSquared != -1 && indexOfBed != -1 && indexOfMetresSquared > indexOfBed) {
            String sSizeString = detailedDescription.substring(indexOfBed, indexOfMetresSquared).trim();
            sSizeString = sSizeString.replaceAll("[^\\d.]", ""); // strip the non numerics
            if (sSizeString != null) {
                sSizeString = sSizeString.trim();
                try {
                    propertyValues.put(PropertyContract.PropertyEntry.COLUMN_SQUARE_AREA, Integer.parseInt(sSizeString));
                } catch (NumberFormatException nfe) {
                    Log.e(LOG_TAG, "had a problem reading square footage " + detailedDescription);
                }
            }
        }

    }
}
