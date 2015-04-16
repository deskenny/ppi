package com.alley.android.ppi.app;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * {@link OverviewAdapter} exposes a list of Properties
 * from a {@link Cursor} to a {@link android.widget.ListView}.
 */
public class OverviewAdapter extends CursorAdapter {

    private static final int VIEW_TYPE_COUNT = 2;
    private static final int VIEW_TYPE_TODAY = 0;
    private static final int VIEW_TYPE_FUTURE_DAY = 1;

    // Flag to determine if we want to use a separate view for "today".
    private boolean mUseTodayLayout = true;

    /**
     * Cache of the children views for a forecast list item.
     */
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
        // Choose the layout type
        int viewType = getItemViewType(cursor.getPosition());
        int layoutId = -1;
        switch (viewType) {
            case VIEW_TYPE_TODAY: {
                layoutId = R.layout.list_item_forecast_today;
                break;
            }
            case VIEW_TYPE_FUTURE_DAY: {
                layoutId = R.layout.list_item_forecast;
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
            case VIEW_TYPE_TODAY: {
                // Get property icon
                viewHolder.iconView.setImageResource(Utility.getArtResourceForPropType(
                        cursor.getInt(OverviewFragment.COLUMN_PROP_TYPE_ID),
                        cursor.getInt(OverviewFragment.COLUMN_NUM_BEDS)));
                break;
            }
            case VIEW_TYPE_FUTURE_DAY: {
                // Get property icon
                viewHolder.iconView.setImageResource(Utility.getIconResourceForPropType(
                        cursor.getInt(OverviewFragment.COLUMN_PROP_TYPE_ID),
                        cursor.getInt(OverviewFragment.COLUMN_NUM_BEDS)));
                break;
            }
        }

        // Read date from cursor
        long dateInMillis = cursor.getLong(OverviewFragment.COL_DATE);
        // Find TextView and set formatted date on it
        viewHolder.dateView.setText(Utility.getFriendlyDayString(context, dateInMillis));

        // Read property forecast from cursor
        String description = cursor.getString(OverviewFragment.COL_DESCRIPTION);
        // Find TextView and set property forecast on it
        viewHolder.descriptionView.setText(description);

        // For accessibility, add a content description to the icon field
        viewHolder.iconView.setContentDescription(description);

        // Read price from cursor
        String price = cursor.getString(OverviewFragment.COL_PRICE);
        viewHolder.priceView.setText(Utility.formatPrice(context, price));

        // Read low temperature from cursor
        String numBeds = cursor.getString(OverviewFragment.COLUMN_NUM_BEDS);
        if (numBeds != null && !numBeds.equalsIgnoreCase("")) {
            viewHolder.numBedsView.setText(numBeds + " bed");
        }
    }

    public void setUseTodayLayout(boolean useTodayLayout) {
        mUseTodayLayout = useTodayLayout;
    }

    @Override
    public int getItemViewType(int position) {
        //return (position == 0 && mUseTodayLayout) ? VIEW_TYPE_TODAY : VIEW_TYPE_FUTURE_DAY;
        return VIEW_TYPE_FUTURE_DAY;
    }

    @Override
    public int getViewTypeCount() {
        return VIEW_TYPE_COUNT;
    }
}