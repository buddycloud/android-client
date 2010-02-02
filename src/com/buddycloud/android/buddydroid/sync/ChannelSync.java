package com.buddycloud.android.buddydroid.sync;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smackx.FormField;
import org.jivesoftware.smackx.packet.DataForm;
import org.jivesoftware.smackx.packet.DiscoverInfo;
import org.jivesoftware.smackx.pubsub.Node;
import org.jivesoftware.smackx.pubsub.Subscription;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

import com.buddycloud.android.buddydroid.provider.BuddyCloud.Roster;
import com.buddycloud.jbuddycloud.BuddycloudClient;
import com.buddycloud.jbuddycloud.packet.ChannelFetch;

public class ChannelSync extends Thread {

    private final BuddycloudClient client;
    private final ContentResolver resolver;

    public ChannelSync(BuddycloudClient client, ContentResolver resolver) {
        this.client = client;
        this.resolver = resolver;
        this.start();
    }

    @Override
    public void run() {
        try {
            Log.d("BC", "read channels");
            long time = -System.currentTimeMillis();

            ArrayList<ContentValues> newEntries = new ArrayList<ContentValues>();

            HashMap<String, String> oldChannels = new HashMap<String, String>();
            Cursor query = resolver.query(
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
                    if (!jid.startsWith("/user/")) {
                        oldChannels.put(jid, name);
                    }
                    query.moveToNext();
                }
            }
            query.close();

            Log.d("BC", "fetch subscription");

            List<Subscription> subscriptions =
                client.getPubSubManager().getSubscriptions();
            for (Subscription subscription : subscriptions) {
                String channel = subscription.getNode();
                if (channel.startsWith("/user/")) {
                    updateUser(channel);
                    continue;
                }
                if (oldChannels.containsKey(channel)) {
                    updateChannel(channel);
                    oldChannels.remove(channel);
                    continue;
                }
                Log.d("BC", "add channel " + channel);
                ContentValues values = new ContentValues();
                try {
                    values.put(Roster.JID, channel);
                    values.put(Roster.NAME, fetchChannelTitle(channel));
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
                client.sendPacket(new ChannelFetch(
                    v.getAsString(Roster.JID),
                    0l
                )); // Trigger a full fetch for every new channel
            }

        } catch (Throwable t) {
            Log.e("BC", "Error during channel sync", t);
        }
    }

    private void updateUser(String user) {
        if (!user.endsWith("/channel")) {
            updateChannel(user);
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
                Thread.sleep(100);
            } catch (InterruptedException e) { }
        }
    }

    private void updateChannel(String channel) {
        Log.d("BC", "update channel " + channel);
        Cursor cursor = resolver.query(
                Roster.CONTENT_URI,
                new String[]{Roster.LAST_UPDATED},
                "jid=?",
                new String[]{channel},
                null
        );
        if (cursor.getCount() != 1) {
            Log.e("BC", "uodate channel " + channel + " canceled");
            cursor.close();
            return;
        }
        while (cursor.isBeforeFirst()) { cursor.moveToNext(); }
        long l = cursor.getLong(cursor.getColumnIndex(Roster.LAST_UPDATED));
        cursor.close();
        IQ iq = new ChannelFetch(channel, l);
        client.sendPacket(iq);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) { }
    }

    private String fetchChannelTitle(String nodeName) throws XMPPException {
        Node node = client.getPubSubManager().getNode(nodeName);
        DiscoverInfo info = node.discoverInfo();
        for (PacketExtension packetExtension : info.getExtensions()) {
            if (!(packetExtension instanceof DataForm)) {
                continue;
            }
            DataForm dataForm = (DataForm) packetExtension;
            Iterator<FormField> fields = dataForm.getFields();
            while (fields.hasNext()) {
                FormField field = fields.next();
                if (field.getVariable().equals("pubsub#title")) {
                    Log.d("BC", "ChannelName[" + nodeName +"]=" + field.getValues().next().trim());
                    return field.getValues().next().trim();
                }
            }
            Log.d("BC", "PE: " + packetExtension.getClass().toString());
        }
        return nodeName;
    }

}
