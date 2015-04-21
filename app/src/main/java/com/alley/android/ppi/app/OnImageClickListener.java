package com.alley.android.ppi.app;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.Toast;


/**
 * Created by des on 20/04/2015.
 */
public class OnImageClickListener implements View.OnClickListener  {
    private static final String LOG_TAG = OnImageClickListener.class.getSimpleName();
    private Bitmap bitmap;
    private Point screensize;

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public OnImageClickListener() {
    }

    public void setScreensize(Point screensize) {
        this.screensize = screensize;
    }

    @Override
    public void onClick(View v) {

        int screenWidth = v.getWidth();
        int screenHeight = v.getHeight();

        int bitmapHeight = bitmap.getHeight();
        int bitmapWidth = bitmap.getWidth();

        float renderHeight = bitmap.getHeight();
        float renderWidth = bitmap.getWidth();

        if (bitmapHeight > bitmapWidth) {
            // portrait style image
            Log.i(LOG_TAG, "bitmapHeight > bitmapWidth bitmapHeight= " + bitmapHeight + " bitmapWidth" + bitmapWidth);
            renderWidth =  ((float) screenHeight/bitmapHeight) * bitmapWidth;
            renderHeight = screenHeight;
        } else {
            // landscape ... always goes in here for some reason. Anyway, seems to work!
            Log.i(LOG_TAG, "bitmapHeight < bitmapWidth bitmapHeight= " + bitmapHeight + " bitmapWidth" + bitmapWidth);
            renderWidth = screenWidth;
            renderHeight = ((float) screenWidth/bitmapWidth) * bitmapHeight;
        }

        BitmapDrawable resizedBitmap = new BitmapDrawable(v.getResources(), Bitmap.createScaledBitmap(bitmap, (int)renderWidth, (int)renderHeight, false));

        Dialog dialog = new Dialog(v.getContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.thumbnail);

        ImageView image = (ImageView) dialog.findViewById(R.id.imageview);

        image.setBackground(resizedBitmap);

        dialog.getWindow().setBackgroundDrawable(null);

        dialog.show();
    }
}
