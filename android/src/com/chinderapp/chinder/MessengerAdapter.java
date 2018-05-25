package com.chinderapp.chinder;

import android.app.Activity;
import android.content.Context;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.List;

/**
 * Created by Stealth7 on 3/30/2015.
 */
public class MessengerAdapter extends BaseAdapter {
    public final static String TAG = "MESSENGER_ADAPTER";

    private Context context;
    private List<List<String>> items;
    protected ViewHolder holder;

    public MessengerAdapter(Context context, List<List<String>> items) {
        this.context = context;
        this.items = items;
    }

    /**
     *  add one item to list
     */
    public void add(List<String> item) {
        this.items.add(item);
    }

    /**
     *  add one item to list
     */
    public void add(int position, List<String> item) {
        this.items.add(position, item);
    }

    /**
     *  remove one item from list
     */
    public void remove(List<String> item) {
        this.items.remove(item);
    }

    public void clear() {
        this.items.clear();
    }

    public void matchClickListener(View v)
    {
    }

    @Override
    public int getCount() {
        return this.items.size();
    }

    @Override
    public List<String> getItem(int position) {
        return this.items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @SuppressWarnings("deprecation")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        final List<String> item = getItem(position);

        //View view = convertView;
        View view = null;

        if ( item.get(0).matches("from") )
        {
            if (view == null) {
                LayoutInflater inflater = ((Activity) context).getLayoutInflater();
                view = inflater.inflate(R.layout.messenger_from_adapter, parent, false);
                holder = new ViewHolder();
                holder.picture = (ImageView) view.findViewById(R.id.messengerImage);
                holder.message = (TextView) view.findViewById(R.id.messengerFromText);
                holder.sentAt = (TextView) view.findViewById(R.id.messengerFromAt);
                view.setTag(holder);
            } else {
                holder = (ViewHolder) view.getTag();
            }

            // load the image using our universal image loader
            if (item != null && !item.equals("")) {
                holder.message.setText(item.get(1));
                holder.sentAt.setText(item.get(2));
                if ( item.get(3).matches("null") == false ) {
                    DisplayImageOptions options = new DisplayImageOptions.Builder()
                            .showImageOnLoading(R.drawable.default_profile) // resource or drawable
                            .cacheOnDisk(true) // default
                            .build();
                    ImageLoader.getInstance().displayImage(item.get(3), holder.picture, options, null);
                }
                else
                {
                    Drawable drawable = context.getResources().getDrawable( R.drawable.default_profile );
                    holder.picture.setImageDrawable(drawable);
                }
            }
        }
        else if ( item.get(0).matches("to") )
        {
//        // Load our default view if nothing is supplied
            if (view == null) {
                LayoutInflater inflater = ((Activity) context).getLayoutInflater();
                view = inflater.inflate(R.layout.messenger_to_adapter, parent, false);
                holder = new ViewHolder();
                holder.message = (TextView) view.findViewById(R.id.messengerToText);
                holder.sentAt = (TextView) view.findViewById(R.id.messengerToSentAt);
                view.setTag(holder);
            } else {
                holder = (ViewHolder) view.getTag();
            }

            // load the image using our universal image loader
            if (item != null && !item.equals("")) {
                holder.message.setText(item.get(1));
                holder.sentAt.setText(item.get(2));
            }
        }

        return view;
    }

    public class ViewHolder {
        ImageView picture;
        TextView message;
        TextView sentAt;
    }
}