package com.buddycloud.android.buddydroid.sync;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.jivesoftware.smack.XMPPException;
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

import com.buddycloud.android.buddydroid.provider.BuddyCloud.Channels;
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
                Channels.CONTENT_URI,
                new String[]{Channels.NODE_NAME, Channels.TITLE},
                null, null, null
            );
            if (query.getCount() > 0) {
                if (query.isBeforeFirst()) {
                    query.moveToNext();
                }
                while (!query.isAfterLast()) {
                    oldChannels.put(query.getString(1), query.getString(2));
                    query.moveToNext();
                }
            }
            query.close();

            Log.d("BC", "fetch subscription");

            List<Subscription> subscriptions =
                client.getPubSubManager().getSubscriptions();
            for (Subscription subscription : subscriptions) {
                String channel = subscription.getNode();
                if (oldChannels.containsKey(channel)) {
                    oldChannels.remove(channel);
                    continue;
                }
                Log.d("BC", "add channel " + channel);
                ContentValues values = new ContentValues();
                values.put(Channels.NODE_NAME, channel);
                values.put(Channels.TITLE, fetchChannelTitle(channel));
                newEntries.add(values);
            }
            resolver.bulkInsert(
                Channels.CONTENT_URI,
                newEntries.toArray(new ContentValues[newEntries.size()])
            );
            if (oldChannels.size() > 0) {
                StringBuilder where = new StringBuilder(Channels.NODE_NAME);
                where.append(" IN (");
                for (int i = 1, l = oldChannels.size(); i < l; i++) {
                    where.append("?,");
                }
                where.append("?)");
                resolver.delete(Channels.CONTENT_URI, where.toString(),
                        oldChannels.keySet().toArray(new String[oldChannels.size()]));
            }

            time += System.currentTimeMillis();

            resolver.notifyChange(Channels.CONTENT_URI, null);

            Log.d("BC", "updated channels in " + time + "ms");

            time = -System.currentTimeMillis();

            for (ContentValues v : newEntries) {
                client.sendPacket(new ChannelFetch(
                    v.getAsString(Channels.NODE_NAME),
                    0l
                )); // Trigger a full fetch for every new channel
            }

        } catch (Throwable t) {
            Log.e("BC", "Error during channel sync", t);
        }
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
