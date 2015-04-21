package com.alley.android.ppi.app;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class OverviewAdapter extends CursorAdapter {

    private static final int VIEW_TYPE_COUNT = 1;
    private static final int VIEW_TYPE_SUMMARY = 1;


    public static class ViewHolder {
        public final ImageView iconView;
        public final TextView dateView;
        public final TextView descriptionView;
        public final TextView priceView;
        public final TextView numBedsView;

        public ViewHolder(View view) {
            iconView = (ImageView) view.findViewById(R.id.list_item_icon);
            dateView = (TextView) view.findViewById(R.id.list_item_date_textview);
            descriptionView = (TextView) view.findViewById(R.id.list_item_forecast_textview);
            priceView = (TextView) view.findViewById(R.id.list_item_price_textview);
            numBedsView = (TextView) view.findViewById(R.id.list_item_num_beds_textview);
        }
    }

    public OverviewAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        int viewType = getItemViewType(cursor.getPosition());
        int layoutId = -1;
        switch (viewType) {
            case VIEW_TYPE_SUMMARY: {
                layoutId = R.layout.list_item_property;
                break;
            }
        }

        View view = LayoutInflater.from(context).inflate(layoutId, parent, false);

        ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);

        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        ViewHolder viewHolder = (ViewHolder) view.getTag();

        int viewType = getItemViewType(cursor.getPosition());
        switch (viewType) {
            case VIEW_TYPE_SUMMARY: {
                viewHolder.iconView.setImageResource(Utility.getIconResourceForPropType(
                        cursor.getInt(OverviewFragment.COLUMN_PROP_TYPE_ID),
                        cursor.getInt(OverviewFragment.COLUMN_NUM_BEDS)));
                break;
            }
        }

        long dateInMillis = cursor.getLong(OverviewFragment.COL_DATE);
        viewHolder.dateView.setText(Utility.getFriendlyDayString(context, dateInMillis));

        String description = cursor.getString(OverviewFragment.COL_DESCRIPTION);
        viewHolder.descriptionView.setText(description);

        viewHolder.iconView.setContentDescription(description);

        String price = cursor.getString(OverviewFragment.COL_PRICE);
        viewHolder.priceView.setText(Utility.formatPrice(context, price));

        String numBeds = cursor.getString(OverviewFragment.COLUMN_NUM_BEDS);
        if (numBeds != null && !numBeds.equalsIgnoreCase("")) {
            viewHolder.numBedsView.setText(numBeds + " bed");
        }
    }

    @Override
    public int getItemViewType(int position) {
        //return (position == 0 && mUseTodayLayout) ? VIEW_TYPE_TODAY : VIEW_TYPE_SUMMARY;
        return VIEW_TYPE_SUMMARY;
    }

    @Override
    public int getViewTypeCount() {
        return VIEW_TYPE_COUNT;
    }
}