package com.buddycloud.android.buddydroid.sync;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.pubsub.Subscription;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

import com.buddycloud.android.buddydroid.BuddycloudService;
import com.buddycloud.android.buddydroid.provider.BuddyCloud.ChannelData;
import com.buddycloud.android.buddydroid.provider.BuddyCloud.Roster;
import com.buddycloud.jbuddycloud.BuddycloudClient;
import com.buddycloud.jbuddycloud.packet.ChannelFetch;
import com.buddycloud.jbuddycloud.provider.BCPubSubManager;

public class ChannelSync extends Thread {

    private final BuddycloudClient client;
    private final ContentResolver resolver;
    private final BuddycloudService service;

    public ChannelSync(BuddycloudService service,
            BuddycloudClient client, ContentResolver resolver) {
        this.client = client;
        this.resolver = resolver;
        this.service = service;
        this.start();
    }

    @Override
    public void run() {
        try {
            Cursor query = resolver.query(
                ChannelData.CONTENT_URI,
                new String[]{ChannelData.ITEM_ID},
                    null, null, ChannelData.ITEM_ID + " DESC"
            );
            if (query.getCount() > 0) {
                if (query.moveToFirst()) {
                    long latest = 
                        query.getLong(query.getColumnIndex(ChannelData.ITEM_ID));
                    service.deltaUpdate(latest);
                }
            } else {
                service.deltaUpdate(0l);
            }
            query.close();

            Log.d("BC", "read channels");
            long time = -System.currentTimeMillis();

            ArrayList<ContentValues> newEntries = new ArrayList<ContentValues>();

            HashMap<String, String> oldChannels = new HashMap<String, String>();
            query = resolver.query(
                Roster.CONTENT_URI,
                new String[]{Roster.JID, Roster.NAME},
                null, null, null
            );
            if (query.getCount() > 0) {
                if (query.isBeforeFirst()) {
                    query.moveToNext();
                }
                while (!query.isAfterLast()) {
                    String jid = query.getString(
                            query.getColumnIndex(Roster.JID));
                    String name = query.getString(
                            query.getColumnIndex(Roster.NAME));
                    oldChannels.put(jid, name);
                    query.moveToNext();
                }
            }
            query.close();

            Log.d("BC", "fetch subscription");

            List<Subscription> subscriptions =
                client.getPubSubManager().getSubscriptions();
            for (Subscription subscription : subscriptions) {
                String channel = subscription.getNode();
                if (channel.startsWith("/user")) {
                    if (!channel.endsWith("/channel")) {
                        continue;
                    }
                    if (oldChannels.containsKey(channel)) {
                        oldChannels.remove(channel);
                        continue;
                    }
                    updateUser(channel);
                    continue;
                }
                if (oldChannels.containsKey(channel)) {
                    oldChannels.remove(channel);
                    continue;
                }
                Log.d("BC", "add channel " + channel);
                ContentValues values = new ContentValues();
                try {
                    BCPubSubManager pubSubManager = client.getPubSubManager();
                    String title = pubSubManager.fetchChannelTitle(
                            pubSubManager.getNode(channel)
                    );
                    values.put(Roster.JID, channel);
                    values.put(Roster.NAME, title);
                    newEntries.add(values);
                } catch (Throwable t) {
                    t.printStackTrace(System.err);
                }
            }
            resolver.bulkInsert(
                Roster.CONTENT_URI,
                newEntries.toArray(new ContentValues[newEntries.size()])
            );
            if (oldChannels.size() > 0) {
                StringBuilder where = new StringBuilder(Roster.JID);
                where.append(" IN (");
                for (int i = 1, l = oldChannels.size(); i < l; i++) {
                    where.append("?,");
                }
                where.append("?)");
                resolver.delete(Roster.CONTENT_URI, where.toString(),
                        oldChannels.keySet().toArray(new String[oldChannels.size()]));
            }

            time += System.currentTimeMillis();

            resolver.notifyChange(Roster.CONTENT_URI, null);

            Log.d("BC", "updated channels in " + time + "ms");

            time = -System.currentTimeMillis();

            for (ContentValues v : newEntries) {
                service.updateChannel(v.getAsString(Roster.JID));
            }

        } catch (Throwable t) {
            Log.e("BC", "Error during channel sync", t);
        }
    }

    private void updateUser(String user) {
        if (!user.endsWith("/channel")) {
            service.updateChannel(user);
            return;
        }
        user = user.substring(0, user.indexOf("/channel"));
        Log.d("BC", "update user " + user);
        Cursor cursor = resolver.query(
                Roster.CONTENT_URI,
                new String[]{Roster.LAST_UPDATED},
                "jid=?",
                new String[]{user + "/channel"},
                null
        );
        if (cursor.getCount() != 1) {
            Log.e("BC", "uodate channel " + user + " canceled");
            cursor.close();
            return;
        }
        while (cursor.isBeforeFirst()) { cursor.moveToNext(); }
        long l = cursor.getLong(cursor.getColumnIndex(Roster.LAST_UPDATED));
        cursor.close();
        for (String subChannel: new String[]{
                "/channel",
                "/geo/current",
                "/geo/previous",
                "/geo/next"
        }) {
            IQ iq = new ChannelFetch(user + subChannel, l);
            client.sendPacket(iq);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) { }
        }
    }

}
