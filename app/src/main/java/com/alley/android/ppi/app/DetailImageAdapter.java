package com.alley.android.ppi.app;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * {@link com.alley.android.ppi.app.DetailImageAdapter} exposes a list of weather forecasts
 * from a {@link android.database.Cursor} to a {@link android.widget.ListView}.
 */
public class DetailImageAdapter extends CursorAdapter {

    public static class ViewHolder {
        public final ImageView iconView;

        public ViewHolder(View view) {
            iconView = (ImageView) view.findViewById(R.id.list_item_icon);
        }
    }

    public DetailImageAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        // Choose the layout type
        int viewType = getItemViewType(cursor.getPosition());
        int layoutId = R.layout.list_item_image;

        View view = LayoutInflater.from(context).inflate(layoutId, parent, false);

        ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);

        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();
        // For accessibility, add a content description to the icon field
        byte [] photo = cursor.getBlob(DetailFragment.COL_PHOTO);
        if (photo != null) {
            Bitmap bm = BitmapFactory.decodeByteArray(photo, 0, photo.length);
            viewHolder.iconView.setImageBitmap(bm);
            viewHolder.iconView.setContentDescription("House image " + cursor.getPosition());
        }

    }


    @Override
    public int getViewTypeCount() {
        return 1;
    }
}