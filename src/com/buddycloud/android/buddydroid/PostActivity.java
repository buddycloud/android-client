package com.buddycloud.android.buddydroid;

import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smackx.pubsub.Item;
import org.jivesoftware.smackx.pubsub.PayloadItem;
import org.jivesoftware.smackx.pubsub.PublishItem;
import org.jivesoftware.smackx.pubsub.packet.PubSub;

import android.app.Activity;
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
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.buddycloud.android.buddydroid.provider.BuddyCloud.ChannelData;
import com.buddycloud.android.buddydroid.util.HumanTime;
import com.buddycloud.jbuddycloud.packet.BCAtom;

public class PostActivity extends Activity implements OnClickListener {

    private long itemId;
    private String posting;
    private ServiceConnection connection;
    private IBuddycloudService service;
    private String node;

    private final synchronized void bindBCService() {
        if (connection == null) {
            connection = new ServiceConnection() {

                public void onServiceDisconnected(ComponentName name) {
                }

                public void onServiceConnected(
                        ComponentName name,
                        IBinder binder
                ) {
                    service =
                        IBuddycloudService.Stub.asInterface(binder);
                }
            };
        }
        if (service != null) {
            if (service.asBinder().isBinderAlive()) {
                return;
            } else {
                service = null;
            }
        }
        bindService(new Intent(
            IBuddycloudService.class.getName()),
            connection,
            Context.BIND_AUTO_CREATE
        );
    }

    private final synchronized void unbindBCService() {
        if (connection == null) {
            return;
        }
        try {
            unbindService(connection);
        } catch (IllegalArgumentException e) {
            /* How could we verify that there is a pending connection request?
             * 
             * Looks like this is impossible. Silence the exception is evil
             * but somehow works around the problem.
             */
        }
    }

    @Override
    protected void onDestroy() {
        unbindBCService();
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        unbindBCService();
        super.onPause();
    }

    @Override
    protected void onRestart() {
        bindBCService();
        super.onRestart();
    }

    @Override
    protected void onResume() {
        bindBCService();
        super.onResume();
    }

    @Override
    protected void onStart() {
        bindBCService();
        super.onStart();
    }

    @Override
    protected void onStop() {
        unbindBCService();
        super.onStop();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus) {
            bindBCService();
        } else {
            unbindBCService();
        }
        super.onWindowFocusChanged(hasFocus);
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.post_message);

        int width = getWindowManager().getDefaultDisplay().getWidth();
        int height = getWindowManager().getDefaultDisplay().getHeight();

        if (width <= height) {
            height = (55 * height) / 100;
        } else {
            height = (90 * height) / 100;
            width = (80 * width) / 100;
        }
        getWindow().setLayout(width, height);

        bindBCService();

        long id = getIntent().getLongExtra("id", -1);
        String name = getIntent().getCharSequenceExtra("name").toString();
        node = getIntent().getCharSequenceExtra("node").toString();

        TextView titleView = (TextView) findViewById(R.id.title);
        TextView textView = (TextView) findViewById(R.id.message);
        TextView jidView = (TextView) findViewById(R.id.jid);
        TextView locationView = (TextView) findViewById(R.id.location);
        Button abortButton = (Button) findViewById(R.id.abort);
        Button postButton = (Button) findViewById(R.id.post);

        abortButton.setOnClickListener(this);
        postButton.setOnClickListener(this);

        if (id != -1) {
            Cursor cursor = getContentResolver().query(
                ChannelData.CONTENT_URI,
                ChannelData.PROJECTION_MAP,
                ChannelData._ID + "=" + id,
                null,
                null
            );
            if (!cursor.moveToFirst()) {
                setResult(0);
                finish();
                return;
            }

            final long parent =
                cursor.getLong(cursor.getColumnIndex(ChannelData.PARENT));

            if (parent != 0) {
                cursor.close();
                return;
            }

            itemId =
                cursor.getLong(cursor.getColumnIndex(ChannelData.ITEM_ID));
            final String originalText = cursor.getString(cursor.getColumnIndex(
                    ChannelData.CONTENT));
            final long timestamp = cursor.getLong(cursor.getColumnIndex(
                    ChannelData.ITEM_ID));
            final String town = cursor.getString(cursor.getColumnIndex(
                    ChannelData.GEOLOC_LOCALITY));
            final String country = cursor.getString(cursor.getColumnIndex(
                    ChannelData.GEOLOC_COUNTRY));
            final String jid =  cursor.getString(cursor.getColumnIndex(
                    ChannelData.AUTHOR_JID));
            final String affiliation = cursor.getString(cursor.getColumnIndex(
                    ChannelData.AUTHOR_AFFILIATION));

            cursor.close();

            titleView.setText("Reply");
            textView.setText(originalText);

            String humanTime = HumanTime.humanReadableString(
                    System.currentTimeMillis() - timestamp);

            jidView.setText(jid);

            if ("owner".equals(affiliation)) {
                jidView.setTextColor(Color.rgb(150, 15, 20));
            } else
            if ("moderator".equals(affiliation)) {
                jidView.setTextColor(Color.rgb(200, 130, 50));
            } else {
                jidView.setTextColor(Color.BLACK);
            }

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

        } else {
            itemId = 0l;
            titleView.setText("Post to " + name);
            textView.setVisibility(View.GONE);
        }

    }

    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.abort:
            setResult(0);
            finish();
            break;
        case R.id.post:
            posting = ((EditText)findViewById(R.id.posting)).getText().toString();
            if (post()) {
                setResult(0);
                finish();
            }
            break;
        }
    }

    private boolean post() {
        BCAtom atom = new BCAtom();

        atom.setContent(posting);

        String jid = null;
        try {
            jid = service.getJidWithResource();
            if (jid != null) {
                int pos = jid.indexOf('/');
                if (pos > 0) {
                    jid = jid.substring(0, pos);
                }
                atom.setAuthorJid(jid);
            }
        } catch (RemoteException e) {
            e.printStackTrace(System.err);
            unbindBCService();
            bindBCService();
        } catch (IllegalStateException e) {
            e.printStackTrace(System.err);
            unbindBCService();
            bindBCService();
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
            return true;
        } catch (RemoteException e) {
            e.printStackTrace(System.err);
            unbindBCService();
            bindBCService();
        } catch (IllegalStateException e) {
            e.printStackTrace(System.err);
            unbindBCService();
            bindBCService();
        }
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            Log.d("REPOST", pubSub.toXML());
            service.send(pubSub.toXML());
            return true;
        } catch (RemoteException e) {
            e.printStackTrace(System.err);
            unbindBCService();
            bindBCService();
        } catch (IllegalStateException e) {
            e.printStackTrace(System.err);
            unbindBCService();
            bindBCService();
        }
        return false;
    }

}
