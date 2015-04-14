package com.alley.android.ppi.app;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

/**
 * Created by des on 14/04/2015.
 */
public class MyListItem{
    private Bitmap bitmap;
    private String description;

    public void setBitmap(Bitmap bitmap){
        this.bitmap=bitmap;
    }
    public Bitmap getBitmap(){
        return bitmap;
    }

    public void setDescription(String description){
        this.description=description;
    }
    public String getDescription(){
        return description;
    }


    public static MyListItem fromCursor(Cursor cursor) {
        MyListItem item = new MyListItem();
        byte [] photo = cursor.getBlob(DetailFragment.COL_PHOTO);
        if (photo != null) {
            Bitmap bm = BitmapFactory.decodeByteArray(photo, 0, photo.length);
            item.setBitmap(bm);
            item.setDescription("House image " + cursor.getPosition());
//            viewHolder.iconView.setImageBitmap(bm);
//            viewHolder.iconView.setContentDescription("House image " + cursor.getPosition());
        }
        return item;
    }
}