package org.louiswilliams.queueupplayer.activity;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.louiswilliams.queueupplayer.R;

import java.util.List;

public class DrawerListAdapter extends BaseAdapter {

    List<String> mListItems;
    Context mContext;
    int mSelection = -1;
    int mResource;

    public DrawerListAdapter(Context context, int resource, List<String> items) {
        mListItems = items;
        mContext = context;
        mResource = resource;
    }

    public void setSelection(int selection) {
        mSelection = selection;
        this.notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;

        if (convertView == null) {
            view = LayoutInflater.from(mContext).inflate(mResource, parent, false);
        } else {
            view = convertView;
        }

        TextView title = (TextView) view.findViewById(R.id.drawer_item_title);
        title.setText(mListItems.get(position));

        if (position == mSelection) {
            view.setBackgroundColor(mContext.getResources().getColor(R.color.primary_dark_material_light));
            title.setTextColor(mContext.getResources().getColor(R.color.primary_material_light));
        } else {
            view.setBackgroundColor(mContext.getResources().getColor(R.color.background_material_light));
            title.setTextColor(mContext.getResources().getColor(R.color.primary_material_dark));
        }

        return view;
    }

    @Override
    public int getCount() {
        return mListItems.size();
    }

    @Override
    public Object getItem(int position) {
        return mListItems.get(0);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }
}
