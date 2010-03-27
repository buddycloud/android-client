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
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

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
                null);

        Log.d("provider", "cursor is: " + buddies);

        follow.setOnClickListener(this);

        listAdapter = new RosterAdapter(this, buddies);

        rosterList.setAdapter(listAdapter);
        rosterList.setOnItemClickListener(this);

        registerForContextMenu(rosterList);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        listAdapter.notifyDataSetChanged();
    }

    public void onItemClick(
        AdapterView<?> parent,
        View view,
        int position,
        long id
    ) {
        Cursor buddy = (Cursor) listAdapter.getItem(position);
        String jid = buddy.getString(buddy.getColumnIndex(Roster.JID));

        final Intent openChannel = new Intent();
        openChannel.setClassName(
            "com.buddycloud.android.buddydroid",
            ChannelMessageActivity.class.getCanonicalName()
        );
        openChannel.setData(Uri.parse("channel:" + jid));
        openChannel.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        openChannel.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);

        runOnUiThread(new Runnable() { public void run() {
            getApplication().startActivity(openChannel);
        }});
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

    public void onClick(View v) {
        startActivity(new Intent(this, FollowActivity.class));
    }

    private class RosterAdapter extends CursorAdapter {

        public RosterAdapter(Context context, Cursor c) {
            super(context, c);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return getLayoutInflater().inflate(R.layout.roster_row, null);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {

            // Read information form cursor

            String locNext = cursor.getString(
                    cursor.getColumnIndex(Roster.GEOLOC_NEXT)
            );
            String locPrev = cursor.getString(
                    cursor.getColumnIndex(Roster.GEOLOC_PREV)
            );
            String loc = cursor.getString(
                    cursor.getColumnIndex(Roster.GEOLOC)
            );
            String mood = cursor.getString(
                    cursor.getColumnIndex(Roster.STATUS)
            );
            int unread = cursor.getInt(
                    cursor.getColumnIndex(Roster.UNREAD_MESSAGES)
            );
            String name = cursor.getString(cursor.getColumnIndex(Roster.NAME));
            String jid = cursor.getString(cursor.getColumnIndex(Roster.JID));

            // Helpers

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

            boolean hasMood = mood != null && mood.length() > 0;
            boolean hasLoc = loc != null && loc.length() > 0;
            boolean hasLocNext = locNext != null && locNext.length() > 0;
            boolean hasLocPrev = locPrev != null && locPrev.length() > 0;

            // Fetch needed views

            TextView jidView = (TextView) view.findViewById(R.id.jid);
            TextView moodView = (TextView) view.findViewById(R.id.mood);
            TextView locView = (TextView) view.findViewById(R.id.loc_current);
            TextView locNextView = (TextView)
                                    view.findViewById(R.id.loc_next);
            TextView locPrevView = (TextView)
                                    view.findViewById(R.id.loc_prev);

            ImageView iconView = (ImageView) view.findViewById(R.id.icon);
            ImageView starView = (ImageView) view.findViewById(R.id.star);
            ImageView prevIconView = (ImageView)
                                        view.findViewById(R.id.loc_prev_icon);
            ImageView nextIconView = (ImageView)
                                        view.findViewById(R.id.loc_next_icon);
            LinearLayout locPrevNextContainer = (LinearLayout)
                                view.findViewById(R.id.loc_prev_next_container);

            //  Cursor Data -> UI

            if (unread > 0) {
                starView.setVisibility(View.VISIBLE);
            } else {
                starView.setVisibility(View.GONE);
            }

            locPrevView.setPaintFlags(
                    locPrevView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
            );

            if (isChannel) {
                iconView.setImageResource(R.drawable.channel);

                jidView.setText(name);
                moodView.setText(jid);

            } else {

                iconView.setImageResource(R.drawable.contact);

                jidView.setText(name + " (" + jid + ")");

                if (hasMood) {
                    moodView.setText(mood);
                }

            }

            if (hasLoc) {
                // We move the location into the mood field if we don't have
                // a mood.
                if (!hasMood) {
                    moodView.setText(loc);
                    locView.setVisibility(View.GONE);
                } else {
                    locView.setVisibility(View.VISIBLE);
                    locView.setText(loc);
                }
            } else {
                if (!hasMood) {
                    moodView.setText("");
                }
                locView.setVisibility(View.GONE);
            }

            if (!hasLocNext && !hasLocPrev) {
                locPrevNextContainer.setVisibility(View.GONE);
            } else {
                locPrevNextContainer.setVisibility(View.VISIBLE);

                if (hasLocPrev) {
                    prevIconView.setVisibility(View.VISIBLE);
                    locPrevView.setText(locPrev);
                } else {
                    prevIconView.setVisibility(View.INVISIBLE);
                    locPrevView.setText("");
                }

                if (hasLocNext) {
                    nextIconView.setVisibility(View.VISIBLE);
                    locNextView.setText(locNext);
                } else {
                    nextIconView.setVisibility(View.INVISIBLE);
                    locNextView.setText("");
                }
            }

        }
    }

}