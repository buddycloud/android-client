package com.buddycloud.view;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.buddycloud.R;
import com.buddycloud.content.BuddyCloud.ChannelData;
import com.buddycloud.content.BuddyCloud.Roster;
import com.buddycloud.util.HumanTime;
import com.github.droidfu.widgets.WebImageView;

/**
 * Displays the messages in a given channel, whether personal or global.
 * <p>Requires an Intent with the channel ID in the format:
 * {@code channel:/path/to/channel}</p>
 */
public class ChannelMessageActivity extends BCActivity {

    private String node;
    private String name;
    private TextView nameView;
    private Activity currentActivity = null;
    private TextView jidView;
    private ListView listView;
    protected String jid;

    public void onCreate(Bundle savedInstanceState) {
        node = getIntent().getData().toString().substring("channel:".length());

        super.onCreate(savedInstanceState);

        currentActivity = this;

        setContentView(R.layout.channel_layout);

        nameView = ((TextView)findViewById(R.id.channel_title));
        nameView.setText("");
        nameView.setOnClickListener(new PostOnClick(-1));
        jidView = ((TextView)findViewById(R.id.channel_jid));
        jidView.setOnClickListener(new PostOnClick(-1));

        listView = ((ListView)findViewById(R.id.message_list));
        listView.setFadingEdgeLength(0);
        listView.setDivider(null);
        listView.setDividerHeight(0);
        listView.setSmoothScrollbarEnabled(false);

        onNewIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        node = getIntent().getData().toString().substring("channel:".length());
        new Thread() {
            public void run() {
                final Cursor messages =
                    managedQuery(
                        ChannelData.CONTENT_URI,
                        ChannelData.PROJECTION_MAP,
                        ChannelData.NODE_NAME + "=?",
                        new String[]{node},
                        ChannelData.LAST_UPDATED + " DESC, " +
                        ChannelData.ITEM_ID + " ASC"
                    );
                ChannelMessageActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        listView.setAdapter(new ChannelMessageAdapter(
                                ChannelMessageActivity.this, messages));
                    }
                });
            }
        }.start();

        new Thread() {
            public void run() {
                Cursor channel = managedQuery(
                        Roster.CONTENT_URI, Roster.PROJECTION_MAP,
                        Roster.JID + "=?", new String[]{node}, null);
                while (channel.isBeforeFirst()) {
                    channel.moveToNext();
                }

                name = channel.getString(channel.getColumnIndex(Roster.NAME));
                jid = channel.getString(channel.getColumnIndex(Roster.JID));
                if (jid.startsWith("/user/")) {
                    jid = jid.substring("/user/".length());
                    jid = jid.substring(0, jid.indexOf('/'));
                    if (name == null) {
                        name = "personal channel of " + jid;
                    } else {
                        name = name + "'s personal channel";
                    }
                } else {
                    if (jid.startsWith("/channel/")) {
                        jid = jid.substring("/channel/".length());
                        if (name == null) {
                            name = jid;
                        } else {
                            name = name + " channel";
                        }
                    }
                }
                ChannelMessageActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        jidView.setText(jid);
                        nameView.setText(name);
                    }
                });
            }

        }.start();
        if (service != null && service.asBinder().isBinderAlive()) {
            new Thread() {
                @Override
                public void run() {
                    try {
                        service.updateChannel(node);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }.start();
        }
    }

    @Override
    protected void onBuddycloudServiceBound() {
        new Thread() {
            @Override
            public void run() {
                try {
                    service.updateChannel(node);
                } catch (RemoteException e) {
                    e.printStackTrace();
                 }
            }
        }.start();
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

            WebImageView userIcon = (WebImageView)
                                view.findViewById(R.id.user_icon);;
            String jidFragment[] = jid.split("@");
            if (jidFragment.length == 2) {
                userIcon.setImageUrl(
                    "http://media.buddycloud.com/channel/54x54/" +
                    jidFragment[1] + "/" +
                    jidFragment[0] + ".png"
                );
                userIcon.loadImage();
            }


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

            int httpIndex = message.lastIndexOf(
                    "http://media.buddycloud.com/dl/"
                );
                if (httpIndex == -1) {
                    view.findViewById(R.id.webimage).setVisibility(View.GONE);
                } else {
                    WebImageView imgView = (WebImageView)
                                                view.findViewById(R.id.webimage);
                    String v = message.substring(httpIndex);
                    for (int i = 31; i < v.length(); i++) {
                        if (Character.isWhitespace(v.charAt(i))) {
                            v = v.substring(0, i);
                        }
                    }
                    int ending = v.toLowerCase().indexOf(".jpg");
                    if (ending != -1) {
                        v = v.substring(0, ending + 4);
                        imgView.setImageUrl(v);
                        imgView.setVisibility(View.VISIBLE);
                        imgView.loadImage();
                    } else {
                        ending = v.toLowerCase().indexOf(".png");
                        if (ending != -1) {
                            v = v.substring(0, ending + 4);
                            imgView.setImageUrl(v);
                            imgView.setVisibility(View.VISIBLE);
                            imgView.loadImage();
                        } else {
                            view.findViewById(R.id.webimage).setVisibility(View.GONE);
                        }
                    }
                }

                LinearLayout bottomShadowLayout = (LinearLayout)
                view.findViewById(R.id.bottom_shadow);

            if (endOfList) {
                bottomShadowLayout.setVisibility(LinearLayout.VISIBLE);
            } else {
                bottomShadowLayout.setVisibility(LinearLayout.GONE);
            }

            if (unread && hasWindowFocus()) {
                final ContentValues values = new ContentValues();
                values.put(ChannelData.UNREAD, Boolean.FALSE);
                new Thread() {
                    public void run() {
                        getContentResolver().update(
                            ChannelData.CONTENT_URI,
                            values,
                            ChannelData.NODE_NAME + "=?",
                            new String[]{node}
                        );
                    }
                }.start();
            }
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return getLayoutInflater().inflate(R.layout.channel_row, null);
        }
    }

}
