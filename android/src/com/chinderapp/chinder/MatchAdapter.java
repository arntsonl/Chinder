package com.chinderapp.chinder;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import java.util.List;

/**
 * Created by Stealth7 on 3/30/2015.
 */
public class MatchAdapter extends BaseAdapter {
    public final static String TAG = "MATCH_ADAPTER";

    private Context context;
    private List<List<String>> items;
    protected ViewHolder holder;

    public MatchAdapter (Context context, List<List<String>> items) {
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

        View view = null;

        // Don't recycle our image view, it causes WAY too many issues
        if ( convertView == null ) {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            view = inflater.inflate(R.layout.adapter_match_view, parent, false);
            holder = new ViewHolder();
            holder.picture = (ImageView) view.findViewById(R.id.matchImageView);
            holder.title = (TextView) view.findViewById(R.id.matchImageText);
            holder.message = (TextView) view.findViewById(R.id.matchMessageText);
            view.setTag(holder);
        } else
        {
            view = convertView;
            holder = (ViewHolder) view.getTag();
        }

        // load the image using our universal image loader
        if (item != null && !item.equals("")) {
            holder.title.setText(item.get(1));
            holder.message.setText(item.get(2));
            if ( item.get(0).matches("null") == false ) {
                DisplayImageOptions options = new DisplayImageOptions.Builder()
                        .showImageOnLoading(R.drawable.default_profile) // resource or drawable
                        .cacheOnDisk(true) // default
                        .build();
                ImageLoader.getInstance().displayImage(item.get(0), holder.picture, options, new SimpleImageLoadingListener() {
                    @Override
                    public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                        Bitmap roundedImage = new UtilityClass().getRoundedCornerBitmap(loadedImage, 24);
                        holder.picture.setImageBitmap(roundedImage);
                    }
                });
            }
            else
            {
                Drawable drawable = context.getResources().getDrawable( R.drawable.default_profile );
                holder.picture.setImageDrawable(drawable);
            }
            if ( item.get(4).matches("message") ) {
                // change the border to a new match
                holder.picture.getBackground().setColorFilter(Color.parseColor("#0000ff"), PorterDuff.Mode.DARKEN);
            } else if ( item.get(4).matches("match") ) {
                // change the border to a new message
                holder.picture.getBackground().setColorFilter(Color.parseColor("#00ff00"), PorterDuff.Mode.DARKEN);
            }
        }
        return view;
    }

    public class ViewHolder {
        ImageView picture;
        TextView title;
        TextView message;
    }
}