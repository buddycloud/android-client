package com.buddycloud.android.buddydroid;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
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
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.buddycloud.android.buddydroid.provider.BuddyCloud.ChannelData;
import com.buddycloud.android.buddydroid.provider.BuddyCloud.Roster;
import com.buddycloud.android.buddydroid.util.HumanTime;

public class ChannelMessageActivity extends Activity {

    private String node;
    private String name;
    private Activity currentActivity = null;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        currentActivity = this;

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.channel_layout);

        node = getIntent().getData().toString().substring(8);

        Cursor messages =
            managedQuery(
                ChannelData.CONTENT_URI,
                ChannelData.PROJECTION_MAP,
                ChannelData.NODE_NAME + "=?",
                new String[]{node},
                ChannelData.LAST_UPDATED + " DESC, " +
                ChannelData.ITEM_ID + " ASC"
            );

        Cursor channel = managedQuery(
                Roster.CONTENT_URI, Roster.PROJECTION_MAP,
                Roster.JID + "=?", new String[]{node}, null);
        while (channel.isBeforeFirst()) {
            channel.moveToNext();
        }

        name = channel.getString(channel.getColumnIndex(Roster.NAME));
        String jid = channel.getString(channel.getColumnIndex(Roster.JID));
        if (jid.startsWith("/user/")) {
            jid = jid.substring(6);
            jid = jid.substring(0, jid.indexOf('/'));
            name = name + "'s personal channel";
        } else
        if (jid.startsWith("/channel/")) {
            jid = jid.substring(9);
            name = name + " channel";
        }

        TextView titleView = ((TextView)findViewById(R.id.channel_title));
        titleView.setText(name);
        titleView.setOnClickListener(new PostOnClick(-1));
        TextView jidView = ((TextView)findViewById(R.id.channel_jid));
        jidView.setText(jid);
        jidView.setOnClickListener(new PostOnClick(-1));

        ListView listView = ((ListView)findViewById(R.id.message_list));
        listView.setFadingEdgeLength(0);
        listView.setAdapter(new ChannelMessageAdapter(this, messages));
        listView.setDivider(null);
        listView.setDividerHeight(0);
        listView.setSmoothScrollbarEnabled(false);

    }

    private class PostOnClick implements OnClickListener {

        private final long id;

        public PostOnClick(long id) {
            this.id = id;
        }

        public void onClick(View v) {
            Log.d("POST", "ID: " + id);

            Intent post = new Intent(currentActivity, PostActivity.class);
            post.putExtra("id", id);
            post.putExtra("name", name);
            post.putExtra("node", node);
            startActivity(post);
        }

    }

    private class ChannelMessageAdapter extends CursorAdapter {

        public ChannelMessageAdapter(Context context, Cursor c) {
            super(context, c);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            final float scale = context.getResources()
                                        .getDisplayMetrics().density;

            long parent =
                cursor.getLong(cursor.getColumnIndex(ChannelData.PARENT));
            String message =
                cursor.getString(cursor.getColumnIndex(ChannelData.CONTENT));
            String town =
                cursor.getString(cursor.getColumnIndex(
                        ChannelData.GEOLOC_LOCALITY)
                );
            String country =
                cursor.getString(cursor.getColumnIndex(
                        ChannelData.GEOLOC_COUNTRY)
                );
            long id = cursor.getLong(cursor.getColumnIndex(ChannelData._ID));
            String jid = 
                cursor.getString(cursor.getColumnIndex(ChannelData.AUTHOR_JID));
            String affiliation =
                cursor.getString(cursor.getColumnIndex(
                        ChannelData.AUTHOR_AFFILIATION)
                );
            long time =
                cursor.getLong(cursor.getColumnIndex(ChannelData.ITEM_ID));

            boolean unread =
                cursor.getInt(cursor.getColumnIndex(ChannelData.UNREAD)) == 1;

            cursor.moveToNext();
            boolean endOfList = cursor.isAfterLast() ||
                cursor.getLong(cursor.getColumnIndex(ChannelData.PARENT)) <= 0;
            cursor.move(-1);

            TextView messageView = (TextView)view.findViewById(R.id.message);
            messageView.setText(message);

            TextView jidView = (TextView)view.findViewById(R.id.jid);
            jidView.setText(jid);

            if ("owner".equals(affiliation)) {
                jidView.setTextColor(Color.rgb(150, 15, 20));
            } else
            if ("moderator".equals(affiliation)) {
                jidView.setTextColor(Color.rgb(200, 130, 50));
            } else {
                jidView.setTextColor(Color.BLACK);
            }

            String humanTime = HumanTime.humanReadableString(
                    System.currentTimeMillis() - time);

            TextView locationView = (TextView)view.findViewById(R.id.location);
            if (town != null && town.length() > 0) {
                if (country != null && country.length() > 0) {
                    locationView.setText(town + ", " + country + ", " +
                            humanTime);
                } else {
                    locationView.setText(town + ", " + humanTime);
                }
            } else {
                if (country != null && country.length() > 0) {
                    locationView.setText(country + ", " + humanTime);
                } else {
                    locationView.setText(humanTime);
                }
            }

            LinearLayout messageContainer = (LinearLayout)
                view.findViewById(R.id.message_container);

            LinearLayout messageInlineContainer = (LinearLayout)
            view.findViewById(R.id.message_inline_container);

            LinearLayout topShadowLayout = (LinearLayout)
                view.findViewById(R.id.top_shadow);

            ImageView addIcon = (ImageView)
                view.findViewById(R.id.add_icon);

            if (parent <= 0) {
                view.setOnClickListener(new PostOnClick(id));
                topShadowLayout.setVisibility(LinearLayout.VISIBLE);
                messageInlineContainer.setBackgroundColor(Color.TRANSPARENT);
                messageContainer.setPadding(
                    (int)(3f * scale),
                    (int)(5f * scale),
                    0,
                    (int)(2f * scale)
                );
                addIcon.setVisibility(ImageView.VISIBLE);
            } else {
                view.setOnClickListener(null);
                topShadowLayout.setVisibility(LinearLayout.GONE);
                messageInlineContainer.setBackgroundColor(
                        Color.rgb(200, 200, 200)
                );
                if (endOfList) {
                    messageContainer.setPadding(
                            (int)(30f * scale),
                            (int)(5f * scale),
                            0,
                            (int)(2f * scale)
                    );
                } else {
                    messageContainer.setPadding(
                            (int)(30f * scale),
                            (int)(5f * scale),
                            0,
                            (int)(6f * scale)
                    );
                }
                addIcon.setVisibility(ImageView.GONE);
            }

            LinearLayout bottomShadowLayout = (LinearLayout)
                view.findViewById(R.id.bottom_shadow);

            if (endOfList) {
                bottomShadowLayout.setVisibility(LinearLayout.VISIBLE);
            } else {
                bottomShadowLayout.setVisibility(LinearLayout.GONE);
            }

            if (unread && hasWindowFocus()) {
                ContentValues values = new ContentValues();
                values.put(ChannelData.UNREAD, Boolean.FALSE);
                getContentResolver().update(
                    ChannelData.CONTENT_URI,
                    values,
                    ChannelData.NODE_NAME + "=?",
                    new String[]{node}
                );
            }
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return getLayoutInflater().inflate(R.layout.channel_row, null);
        }
    }

}
