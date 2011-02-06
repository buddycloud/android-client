package com.buddycloud.asmack;

import java.util.HashMap;

import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smackx.pubsub.Subscription;
import org.jivesoftware.smackx.pubsub.SubscriptionsExtension;
import org.jivesoftware.smackx.pubsub.packet.PubSub;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.RemoteException;
import android.util.Log;

import com.buddycloud.StateSequenceWorkflow;
import com.buddycloud.content.BuddyCloud.Roster;
import com.buddycloud.jbuddycloud.packet.ChannelFetch;
import com.googlecode.asmack.client.AsmackClient;

/**
 * Sync all channels of the user.
 */
public final class ChannelSync extends StateSequenceWorkflow {

    /**
     * The logging tag of this class.
     */
    private static final String TAG = ChannelSync.class.getSimpleName();

    /**
     * A content resolver to fetch the current roster.
     */
    private final ContentResolver resolver;

    /**
     * A hashmap of roster and fetch times.
     */
    private final HashMap<String, Long> roster;

    /**
     * Create a new channel sync thread and start it.
     * @param client The asmack client.
     * @param via The jid used for sending requests.
     * @param resolver The content resolver used for the roster lookup.
     */
    public ChannelSync(
        AsmackClient client,
        String via,
        ContentResolver resolver
    ) {
        super(client, via);
        this.resolver = resolver;
        roster = new HashMap<String, Long>();
        start();
    }

    /**
     * Start the channel sync process by sending out fetch requests.
     */
    @Override
    public void start() {
        // step 1: get all roster entries
        Cursor cursor = resolver.query(
            Roster.CONTENT_URI,
            Roster.PROJECTION_MAP,
            null, null, null
        );
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                String node = cursor.getString(cursor.getColumnIndex(
                                    Roster.JID));
                Long lastUpdate = cursor.getLong(cursor.getColumnIndex(
                                    Roster.LAST_UPDATED));
                roster.put(node, lastUpdate == null ? 0l : lastUpdate);
                cursor.moveToNext();
            }
        }
        cursor.close();

        // Step 2: get all subscriptions
        PubSub pubsub = new PubSub();
        pubsub.setTo("broadcaster.buddycloud.com");
        SubscriptionsExtension subscription = new SubscriptionsExtension(null);
        pubsub.addExtension(subscription);
        try {
            String fullJid = client.getFullJidByBare(via);
            pubsub.setFrom(fullJid);
            send(pubsub, 1);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void state1(Packet packet) {
        if (packet == null) {
            return;
        }
        if (!(packet instanceof PubSub)) {
            return; 
        }
        Log.d("BC", "got pubsub");
        PubSub pubsub = (PubSub) packet;
        for (PacketExtension extension:  pubsub.getExtensions()) {
            if (!(extension instanceof SubscriptionsExtension)) {
                continue;
            }
            SubscriptionsExtension subscriptions =
                                        (SubscriptionsExtension) extension;
            for (Subscription subscription: subscriptions.getSubscriptions()) {
                String node = subscription.getNode();
                if (!roster.containsKey(node)) {
                    // new roster entry
                    ContentValues values = new ContentValues();
                    values.put(Roster.JID, node);
                    values.put(Roster.ENTRYTYPE, getType(node));
                    resolver.insert(Roster.CONTENT_URI, values);
                    roster.put(node, -1l);
                }
                ChannelFetch fetch = new ChannelFetch(node, roster.get(node)
                                        +1l);
                fetch.setTo("broadcaster.buddycloud.com");
                try {
                    String fullJid = client.getFullJidByBare(via);
                    fetch.setFrom(fullJid);
                    send(fetch, 2);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void state2(Packet packet) {
        // we don't care about atoms, there is another listener for that.
    }

    /**
     * Retrieve the type of the channel.
     * @param node The channel node name.
     * @return The channel node type.
     */
    protected String getType(String node) {
        String fragment[] = node.split("/");
        if (fragment.length < 2) {
            return "unknown";
        }
        if ("channel".equals(fragment[1])) {
            return "channel";
        }
        if ("user".equals(fragment[1])) {
            if (fragment.length > 1 &&
                "geo".equals(fragment[fragment.length - 2])
            ) {
                if ("future".equals(fragment[fragment.length - 1])) {
                    return "geo-future";
                }
                if ("previous".equals(fragment[fragment.length - 1])) {
                    return "geo-previous";
                }
                if ("current".equals(fragment[fragment.length - 1])) {
                    return "geo-current";
                }
                return "geo-unknown";
            }
            if ("mood".equals(fragment[fragment.length - 1])) {
                return "mood";
            }
            if ("channel".equals(fragment[fragment.length - 1])) {
                return "channel";
            }
        }
        return "unknown";
    }
}
