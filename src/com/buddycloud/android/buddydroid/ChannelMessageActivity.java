package com.buddycloud.android.buddydroid;

import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smackx.pubsub.Item;
import org.jivesoftware.smackx.pubsub.PayloadItem;
import org.jivesoftware.smackx.pubsub.PublishItem;
import org.jivesoftware.smackx.pubsub.packet.PubSub;

import android.app.Activity;
import android.app.Dialog;
import android.app.ListActivity;
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
import android.widget.TextView;

import com.buddycloud.android.buddydroid.provider.BuddyCloud.ChannelData;
import com.buddycloud.jbuddycloud.packet.BCAtom;

public class ChannelMessageActivity extends ListActivity {

    private int transparent;
    private int owner;
    private int moderator;
    private IBuddycloudService service;
    private String node;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

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

        setListAdapter(new ChannelMessageAdapter(this, messages));

        bindService(new Intent(IBuddycloudService.class.getName()), new ServiceConnection() {
            
            public void onServiceDisconnected(ComponentName name) {
            }
            
            public void onServiceConnected(ComponentName name, IBinder binder) {
                service = IBuddycloudService.Stub.asInterface(binder);
            }
        }, Context.BIND_AUTO_CREATE);
    }

    private class PostOnClick implements OnClickListener {

        private final long id;

        public PostOnClick(long id) {
            this.id = id;
            Log.d("ADD", "ID: " + id);
        }

        public void onClick(View v) {
            Log.d("POST", "ID: " + id);
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
            final long itemId =
                cursor.getLong(cursor.getColumnIndex(ChannelData.ITEM_ID));
            final Dialog post = new Dialog(v.getContext());
            post.setContentView(R.layout.add_channel_msg);
            post.setTitle("Post a reply");
            TextView tv = (TextView) post.findViewById(R.id.orig);
            tv.setText(cursor.getString(cursor.getColumnIndex(
                    ChannelData.CONTENT)));
            post.setOwnerActivity((Activity) v.getContext());
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
