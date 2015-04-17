package com.alley.android.ppi.app.sync;

import android.content.ContentValues;
import android.content.Context;
import android.util.Log;

import com.alley.android.ppi.app.Utility;
import com.alley.android.ppi.app.data.PropertyContract;

import org.json.JSONException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

public class GoogleAdapterHelper {

    public final String LOG_TAG = GoogleAdapterHelper.class.getSimpleName();

    public boolean readGoogle(String address, ContentValues propertyValues, Context context, int numberOfDaysToKeep) {
        try {
            String google = "http://www.google.com/search?q=";
            String search = address;
            String charset = "UTF-8";
            String userAgent = "PropertyPriceBot 1.0 (+http://propertyprice.ie/bot)"; // Change this to your company's name and bot homepage!

            Elements links = Jsoup.connect(google + URLEncoder.encode(search, charset)).userAgent(userAgent).get().select("li.g>h3>a");
            MyHomeAdapterHelper myHomeHelper = new MyHomeAdapterHelper(numberOfDaysToKeep);
            for (Element link : links) {
                String title = link.text();
                String url = link.absUrl("href"); // Google returns URLs in format "http://www.google.com/url?q=<url>&sa=U&ei=<someKey>".
                url = URLDecoder.decode(url.substring(url.indexOf('=') + 1, url.indexOf('&')), "UTF-8");

                if (!url.startsWith("http")) {
                    continue; // Ads/news/etc.
                }

                Log.d(LOG_TAG, "Title: " + title);
                Log.d(LOG_TAG, "URL: " + url);

                if (url != null && url.contains("myhome.ie") && url.contains("brochure")) {
                    ContentValues brochureValues = new ContentValues();
                    boolean wasMatch = myHomeHelper.readMyHomeBrochurePage(url, brochureValues, address, context);
                    if (wasMatch) {
                        brochureValues.remove(PropertyContract.PropertyEntry.COLUMN_ADDRESS);
                        propertyValues.putAll(brochureValues);
                        propertyValues.put(PropertyContract.PropertyEntry.COLUMN_BROCHURE_READ_ATTEMPTED, true);
                        propertyValues.put(PropertyContract.PropertyEntry.COLUMN_BROCHURE_SUCCESS, true);
                        return true;
                    }
                }
            }

            propertyValues.put(PropertyContract.PropertyEntry.COLUMN_BROCHURE_READ_ATTEMPTED, true);
            propertyValues.put(PropertyContract.PropertyEntry.COLUMN_BROCHURE_SUCCESS, false);


        } catch (Exception e) {
            Log.e(LOG_TAG, "Problem reading Google - " + e.getMessage());
        }
        Log.i(LOG_TAG, "Did not find link in Google for " + address);
        return false;
    }
}
