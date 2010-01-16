package com.buddycloud.android.buddydroid;

import android.app.ListActivity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.buddycloud.android.buddydroid.provider.BuddyCloud.ChannelData;

public class ChannelMessageActivity extends ListActivity {

    private int transparent;
    private int owner;
    private int moderator;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        transparent = Color.argb(0, 0, 0, 0);
        owner = Color.argb(255, 241, 27, 27);
        moderator = Color.argb(255, 246, 169, 43);

        String node = getIntent().getData().toString().substring(8);

        Cursor messages =
            managedQuery(
                ChannelData.CONTENT_URI,
                ChannelData.PROJECTION_MAP,
                ChannelData.NODE_NAME + "=?",
                new String[]{node},
                ChannelData.LAST_UPDATED + " DESC, " +
                ChannelData.ITEM_ID + " ASC"
            );

        setListAdapter(new ChannelMessageAdapter(this, messages));
    }

    private static class PostOnClick implements OnClickListener {

        private final long id;

        public PostOnClick(long id) {
            this.id = id;
            Log.d("ADD", "ID: " + id);
        }

        public void onClick(View v) {
            Log.d("POST", "ID: " + id);
        }

    }

    private class ChannelMessageAdapter extends CursorAdapter {

        public ChannelMessageAdapter(Context context, Cursor c) {
            super(context, c);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            TextView tv = (TextView) view.findViewById(R.id.text);
            long p = cursor.getLong(cursor.getColumnIndex(ChannelData.PARENT));
            tv.setText(
                cursor.getString(cursor.getColumnIndex(ChannelData.CONTENT))
            );
            view.setOnClickListener(new PostOnClick(
                cursor.getLong(cursor.getColumnIndex(ChannelData._ID))
            ));
            ImageView iv = (ImageView) view.findViewById(R.id.add);
            if (p == 0l) {
                tv.setPadding(
                    5,
                    tv.getPaddingTop(),
                    tv.getPaddingRight(),
                    tv.getPaddingBottom()
                );
                iv.setVisibility(ImageView.VISIBLE);
            } else {
                tv.setPadding(
                    30,
                    tv.getPaddingTop(),
                    tv.getPaddingRight(),
                    tv.getPaddingBottom()
                );
                iv.setVisibility(ImageView.INVISIBLE);
            }
            tv = (TextView) view.findViewById(R.id.tag);
            String affiliation =
                cursor.getString(cursor.getColumnIndex(
                        ChannelData.AUTHOR_AFFILIATION));
            if (affiliation.equals("owner")) {
                tv.setBackgroundColor(owner);
            } else
            if (affiliation.equals("moderator")) {
                tv.setBackgroundColor(moderator);
            } else {
                tv.setBackgroundColor(transparent);
            }
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return getLayoutInflater().inflate(R.layout.channel_row, null);
        }
    }

}
