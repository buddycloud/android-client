package com.buddycloud.android.buddydroid;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;

import com.buddycloud.android.buddydroid.provider.BuddyCloud.Roster;

public class RosterActivity extends Activity
    implements OnItemClickListener, OnClickListener {

    protected static final String TAG = "RosterActivity";

    protected ListView rosterList;
    protected RosterAdapter listAdapter;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.roster);

        rosterList = (ListView) findViewById(R.id.roster_list);
        Button follow = (Button) findViewById(R.id.follow);

        Cursor buddies = managedQuery(
                Roster.VIEW_CONTENT_URI,
                Roster.PROJECTION_MAP,
                null,
                null,
                "itsMe DESC, last_updated DESC, cache_update_timestamp DESC");

        Log.d("provider", "cursor is: " + buddies);

        follow.setOnClickListener(this);
        listAdapter = new RosterAdapter(this, buddies);
        rosterList.setAdapter(listAdapter);
        rosterList.setOnItemClickListener(this);
        rosterList.setOnItemSelectedListener(new OnItemSelectedListener() {

            public void onItemSelected(AdapterView<?> arg0, View arg1,
                    int position, long arg3) {
                listAdapter.toggle(position);
            }

            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        registerForContextMenu(rosterList);
    }

    public void onItemClick(
        AdapterView<?> parent,
        View view,
        int position,
        long id
    ) {
        listAdapter.toggle(position);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {

        getMenuInflater().inflate(R.menu.roster_context, menu);

        Intent openChannel = new Intent();
        openChannel.setClassName("com.buddycloud.android.buddydroid",
                ChannelMessageActivity.class.getCanonicalName());

        Cursor buddy = (Cursor) listAdapter.getItem(
                ((AdapterContextMenuInfo) menuInfo).position);
        String jid = buddy.getString(buddy.getColumnIndex(Roster.JID));

        openChannel.setData(Uri.parse("channel:" + jid));

        menu.findItem(R.id.open_channel).setIntent(openChannel);

        super.onCreateContextMenu(menu, v, menuInfo);
    }

    private class RosterAdapter extends CursorAdapter {

        private int mExpandedPosition;

        public RosterAdapter(Context context, Cursor c) {
            super(context, c);
        }

        public void toggle(int position) {
            if (mExpandedPosition != position)
                mExpandedPosition = position;
            else
                mExpandedPosition = -1;
            notifyDataSetChanged();
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return getLayoutInflater().inflate(R.layout.roster_row, null);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {

            boolean hasNextLocation = (cursor.getString(cursor
                    .getColumnIndex(Roster.GEOLOC_NEXT)) != null && !cursor
                    .getString(cursor.getColumnIndex(Roster.GEOLOC_NEXT))
                    .equals("null"));

            String jid = cursor.getString(cursor.getColumnIndex(Roster.JID));
            int unread = cursor.getInt(
                    cursor.getColumnIndex(Roster.UNREAD_MESSAGES));

            boolean isChannel = false;

            if (jid.startsWith("/user/")) {
                jid = jid.substring(6);
                if (jid.endsWith("/channel")) {
                    jid = jid.substring(0, jid.length() - 8);
                }
            } else if (jid.startsWith("/channel/")) {
                jid = jid.substring(9);
                isChannel = true;
            }

            TextView tv = (TextView) view.findViewById(R.id.title);
            tv.setText(cursor.getString(cursor.getColumnIndex(Roster.NAME))
                    + " (" + jid + ")");

            if (cursor.getPosition() == mExpandedPosition && !isChannel) {
                tv = (TextView) view.findViewById(R.id.desc);
                tv.setText(cursor.getString(cursor
                        .getColumnIndex(Roster.STATUS)));

                tv = (TextView) view.findViewById(R.id.loc_prev);
                tv.setText(cursor.getString(cursor
                        .getColumnIndex(Roster.GEOLOC_PREV)));
                tv.setPaintFlags(tv.getPaintFlags()
                        | Paint.STRIKE_THRU_TEXT_FLAG);
                tv = (TextView) view.findViewById(R.id.loc_current);
                tv.setText(cursor.getString(cursor
                        .getColumnIndex(Roster.GEOLOC)));
                tv = (TextView) view.findViewById(R.id.loc_next);
                if (hasNextLocation)
                    tv.setText(cursor.getString(cursor
                            .getColumnIndex(Roster.GEOLOC_NEXT)));
                else
                    tv.setText("");

                view.findViewById(R.id.desc).setVisibility(View.VISIBLE);
                view.findViewById(R.id.arrow).setVisibility(View.VISIBLE);
                ImageView iv = (ImageView) view.findViewById(R.id.arrow);
                iv.setImageResource((hasNextLocation ? R.drawable.history2
                        : R.drawable.history1));

                view.findViewById(R.id.loc_prev).setVisibility(View.VISIBLE);
                view.findViewById(R.id.loc_current).setVisibility(View.VISIBLE);
                view.findViewById(R.id.loc_next).setVisibility(
                        hasNextLocation ? View.VISIBLE : View.GONE);

                ((ImageView) view.findViewById(R.id.icon))
                        .setImageResource(R.drawable.contact);
            } else {
                tv = (TextView) view.findViewById(R.id.desc);
                tv.setText(cursor.getString(cursor
                        .getColumnIndex(Roster.GEOLOC)));

                view.findViewById(R.id.arrow).setVisibility(View.GONE);
                view.findViewById(R.id.loc_prev).setVisibility(View.GONE);
                view.findViewById(R.id.loc_current).setVisibility(View.GONE);
                view.findViewById(R.id.loc_next).setVisibility(View.GONE);

                if (isChannel) {
                    ((ImageView) view.findViewById(R.id.icon))
                            .setImageResource(R.drawable.channel);
                } else {
                    ((ImageView) view.findViewById(R.id.icon))
                            .setImageResource(R.drawable.contact);
                }
            }

            if (unread == 0) {
                view.findViewById(R.id.unread).setVisibility(View.GONE);
            } else {
                TextView unreadView = (TextView) view.findViewById(R.id.unread);
                unreadView.setText(Integer.toString(unread));
                unreadView.setVisibility(View.VISIBLE);
            }
            // view.setTag(jid);
            return;
        }
    }

    public void onClick(View v) {
        startActivity(new Intent(this, FollowActivity.class));
    }
}