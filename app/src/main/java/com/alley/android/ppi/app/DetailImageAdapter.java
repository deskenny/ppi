package com.alley.android.ppi.app;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * {@link com.alley.android.ppi.app.DetailImageAdapter} exposes a list of properties
 * from a {@link android.database.Cursor} to a {@link android.widget.ListView}.
 */
public class DetailImageAdapter extends CursorRecyclerViewAdapter<DetailImageAdapter.ViewHolder>{


    public DetailImageAdapter(Context context,Cursor cursor){
        super(context,cursor);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView mImageView;
        public ViewHolder(View view) {
            super(view);
            mImageView = (ImageView) view.findViewById(R.id.list_item_image);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_image, parent, false);
        ViewHolder vh = new ViewHolder(itemView);
        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Cursor cursor) {
        MyListItem myListItem = MyListItem.fromCursor(cursor);
        //            viewHolder.iconView.setImageBitmap(bm);
//

        viewHolder.mImageView.setImageBitmap(myListItem.getBitmap());
        viewHolder.mImageView.setContentDescription(myListItem.getDescription());
    }

}

