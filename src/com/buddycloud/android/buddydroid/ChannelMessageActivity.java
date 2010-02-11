package com.buddycloud.android.buddydroid;

import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smackx.pubsub.Item;
import org.jivesoftware.smackx.pubsub.PayloadItem;
import org.jivesoftware.smackx.pubsub.PublishItem;
import org.jivesoftware.smackx.pubsub.packet.PubSub;

import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.buddycloud.android.buddydroid.provider.BuddyCloud.ChannelData;
import com.buddycloud.android.buddydroid.provider.BuddyCloud.Roster;
import com.buddycloud.android.buddydroid.util.HumanTime;
import com.buddycloud.jbuddycloud.packet.BCAtom;

public class ChannelMessageActivity extends Activity {

    private int transparent;
    private int owner;
    private int moderator;
    private IBuddycloudService service;
    private String node;
    private String name;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.channel_layout);

        transparent = Color.argb(0, 0, 0, 0);
        owner = Color.argb(255, 241, 27, 27);
        moderator = Color.argb(255, 246, 169, 43);

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
        listView.setAdapter(new ChannelMessageAdapter(this, messages));
        listView.setDivider(null);
        listView.setDividerHeight(0);
        listView.setBackgroundResource(R.drawable.channel_view_background);

        bindBCService();
    }

    private class PostOnClick implements OnClickListener {

        private final long id;

        public PostOnClick(long id) {
            this.id = id;
        }

        public void onClick(View v) {
            Log.d("POST", "ID: " + id);
            final long itemId;
            final Dialog post = new Dialog(v.getContext());

            post.setContentView(R.layout.add_channel_msg);
            if (id != -1) {
                v = v.getRootView();
                Cursor cursor = v.getContext().getContentResolver().query(
                    ChannelData.CONTENT_URI,
                    ChannelData.PROJECTION_MAP,
                    ChannelData._ID + "=" + id,
                    null,
                    null
                );
                if (!cursor.moveToFirst()) {
                    return;
                }
                final long parent =
                    cursor.getLong(cursor.getColumnIndex(ChannelData.PARENT));
                if (parent != 0) {
                    return;
                }
                itemId =
                    cursor.getLong(cursor.getColumnIndex(ChannelData.ITEM_ID));
                post.setTitle("Post a reply");
                TextView tv = (TextView) post.findViewById(R.id.orig);
                tv.setText(cursor.getString(cursor.getColumnIndex(
                    ChannelData.CONTENT)));
                post.setOwnerActivity((Activity) v.getContext());
            } else {
                itemId = 0;
                post.setTitle("Post a message");
                TextView tv = (TextView) post.findViewById(R.id.orig);
                tv.setText("Post to " + name);
            }

            ((Button)post.findViewById(R.id.abort)).setOnClickListener(
                new OnClickListener() {
                    public void onClick(View v) {
                        post.dismiss();
                    }
            });
            ((Button)post.findViewById(R.id.post)).setOnClickListener(
            new OnClickListener() {
                public void onClick(View v) {

                    Log.d("POST", "start post");

                    BCAtom atom = new BCAtom();

                    TextView tv = (TextView)post.findViewById(R.id.edit);
                    atom.setContent(tv.getText().toString());
                    String jid = null;
                    try {
                        jid = service.getJidWithResource();
                        int pos = jid.indexOf('/');
                        if (pos > 0) {
                            jid = jid.substring(0, pos);
                        }
                        atom.setAuthorJid(jid);
                    } catch (RemoteException e) {
                        e.printStackTrace(System.err);
                    }
                    if (itemId != 0) {
                        atom.setParentId(itemId);
                    }

                    PayloadItem<BCAtom> item =
                        new PayloadItem<BCAtom>(null, atom);
                    PublishItem<Item> publish =
                        new PublishItem<Item>(node, item);

                    PubSub pubSub = new PubSub();
                    pubSub.setFrom(jid);
                    pubSub.setTo("broadcaster.buddycloud.com");
                    pubSub.setType(Type.SET);

                    pubSub.addExtension(publish);

                    try {
                        Log.d("POST", pubSub.toXML());
                        service.send(pubSub.toXML());
                    } catch (RemoteException e) {
                        e.printStackTrace(System.err);
                    }

                    post.dismiss();
                }
            });
            ((Button)post.findViewById(R.id.abort)).setOnClickListener(
                new OnClickListener() {
                    public void onClick(View v) {
                        post.dismiss();
                    }
            });
            post.show();
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
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return getLayoutInflater().inflate(R.layout.channel_row, null);
        }
    }

    private final void bindBCService() {
        
        bindService(new Intent(IBuddycloudService.class.getName()), new ServiceConnection() {
            
            public void onServiceDisconnected(ComponentName name) {
            }
            
            public void onServiceConnected(ComponentName name, IBinder binder) {
                service = IBuddycloudService.Stub.asInterface(binder);
            }
        }, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
        bindBCService();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        bindBCService();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        bindBCService();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        bindBCService();
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindBCService();
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindBCService();
    }

}
