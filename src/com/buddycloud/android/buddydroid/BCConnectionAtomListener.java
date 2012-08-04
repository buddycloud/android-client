/**
 * 
 */
package com.buddycloud.android.buddydroid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smackx.pubsub.EventElement;
import org.jivesoftware.smackx.pubsub.ItemsExtension;
import org.jivesoftware.smackx.pubsub.PayloadItem;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.util.Log;

import com.buddycloud.content.BuddyCloud.ChannelData;
import com.buddycloud.content.BuddyCloud.Roster;
import com.buddycloud.jbuddycloud.packet.BCAtom;
import com.buddycloud.jbuddycloud.packet.EventIQ;
import com.buddycloud.jbuddycloud.packet.GeoLoc;
import com.buddycloud.jbuddycloud.packet.MessageForwarded;
import com.googlecode.asmack.connection.XmppTransportService;

public final class BCConnectionAtomListener
    implements PacketListener {

    private final ContentResolver resolver;
    private String[] accountJids;
    private XmppTransportService service;

    public BCConnectionAtomListener(
            ContentResolver resolver, XmppTransportService service
    ) {
        this.resolver = resolver;
        this.service = service;
        this.accountJids = service.getAllAccountJids(false);
    }

    public ContentValues receive(String node, BCAtom atom) {
        if (node.startsWith("/user/")) {
            node = node.substring(6);
            String jid = node.substring(0, node.indexOf('/'));
            node = node.substring(node.indexOf('/') + 1);
            if (node.startsWith("geo/")) {
                ContentValues values = new ContentValues();
                GeoLoc loc = atom.getGeoloc();
                if (node.equals("geo/future")) {
                    values.put(Roster.GEOLOC_NEXT, loc.getText());
                } else
                if (node.equals("geo/current")) {
                    values.put(Roster.GEOLOC, loc.getText());
                } else
                if (node.equals("geo/previous")) {
                    values.put(Roster.GEOLOC_PREV, loc.getText());
                }
                resolver.update(Roster.CONTENT_URI, values,
                        Roster.JID + "='" + jid + "'",
                        null);
                return null;
            }
            if (!node.equals("channel")) {
                // /user/jid/mood ?
                return null;
            }
            node = "/user/" + jid + "/" + node;
        }
        // Channel !
        ContentValues values = new ContentValues();
        values.put(ChannelData.NODE_NAME,
                   node);
        values.put(ChannelData.AUTHOR,
                   atom.getAuthorName());
        values.put(ChannelData.AUTHOR_JID,
                   atom.getAuthorJid());
        values.put(ChannelData.AUTHOR_AFFILIATION,
                   atom.getAffiliation());
        values.put(ChannelData.CONTENT,
                   atom.getContent());
        values.put(ChannelData.CONTENT_TYPE,
                   atom.getContentType());
        values.put(ChannelData.ITEM_ID,
                   atom.getId());
        values.put(ChannelData.LAST_UPDATED,
                   atom.getId());
        if (atom.getParent() == null) {
            values.put(ChannelData.PARENT, 0);
        } else {
            values.put(ChannelData.PARENT,
                    atom.getParent());
        }
        values.put(ChannelData.PUBLISHED,
                   atom.getPublished());
        GeoLoc loc = atom.getGeoloc();
        boolean unread = true;
        for (String me: accountJids) {
            if (me.equals(node)) {
                unread = false;
            }
        }
        if (unread) {
            values.put(ChannelData.UNREAD, unread);
        }
        if (loc != null) {
            values.put(ChannelData.GEOLOC_ACCURACY,
                       loc.getAccuracy());
            values.put(ChannelData.GEOLOC_AREA,
                       loc.getArea());
            values.put(ChannelData.GEOLOC_COUNTRY,
                       loc.getCountry());
            values.put(ChannelData.GEOLOC_LAT,
                       loc.getLat());
            values.put(ChannelData.GEOLOC_LOCALITY,
                       loc.getLocality());
            values.put(ChannelData.GEOLOC_LON,
                       loc.getLon());
            values.put(ChannelData.GEOLOC_REGION,
                       loc.getRegion());
            values.put(ChannelData.GEOLOC_TEXT,
                       loc.getText());
            if (loc.getLocType() != null) {
                values.put(ChannelData.GEOLOC_TYPE,
                           loc.getLocType().toString());
            }
        }

        return values;
    }

    public List<ContentValues> handleExtension(
        PacketExtension extension,
        List<ContentValues> result
    ) {
        if (extension instanceof ItemsExtension) {
            ItemsExtension items = (ItemsExtension) extension;
            String node = items.getNode();
            if (items.getItems().size() > 0 && result == null) {
                result = new ArrayList<ContentValues>(items.getItems().size() + 1);
            }
            for (PacketExtension itemsExtension: items.getItems()) {
                if (!(itemsExtension instanceof PayloadItem<?>)) {
                    Log.d("BC/LISTENER", "Not a Payload: " + itemsExtension.getClass());
                    continue;
                }
                Object payload = ((PayloadItem<?>)itemsExtension).getPayload();
                if (payload instanceof BCAtom) {
                    ContentValues contentValues =
                                            receive(node, (BCAtom) payload);
                    if (contentValues != null) {
                        result.add(contentValues);
                    }
                } else {
                    Log.d("BC/LISTENER", "Not a BCAtom: " + payload.getClass());
                }
            }
        } else
        if (extension instanceof EventElement) {
            EventElement event = (EventElement) extension;
            for (PacketExtension e: event.getExtensions()) {
                handleExtension(e, result);
            }
        } else
        if (extension instanceof MessageForwarded) {
            MessageForwarded msgfwd = (MessageForwarded) extension;
            for (Message msg : msgfwd.messages) {
                processMessage(msg);
            }
        } else {
            Log.d("BC/LISTENER", "Unhandled extension: " + extension.getClass());
        }
        return result;
    }

    public void processIQ(EventIQ event) {
        Collection<PacketExtension> extensions = event.getExtensions();
        List<ContentValues> values = null;
        for (PacketExtension extension : extensions) {
            values = handleExtension(extension, values);
        }
        if (values != null && values.size() > 0) {
            Log.d("BC/LISTENER", "bulk insert " + values.size());
            resolver.bulkInsert(ChannelData.CONTENT_URI, values.toArray(
                new ContentValues[values.size()]
            ));
        }
    }

    public void processMessage(Message message) {
        Collection<PacketExtension> extensions = message.getExtensions();
        List<ContentValues> values = null;
        for (PacketExtension extension : extensions) {
            values = handleExtension(extension, values);
        }
        if (values != null && values.size() > 0) {
            Log.d("BC/LISTENER", "bulk insert " + values.size());
            resolver.bulkInsert(ChannelData.CONTENT_URI, values.toArray(
                new ContentValues[values.size()]
            ));
        }
    }

    @Override
    public void processPacket(Packet packet) {
        if (packet == null) {
            return;
        }
        if (packet instanceof EventIQ) {
            processIQ((EventIQ) packet);
            return;
        }
        if (packet instanceof Message) {
            processMessage((Message) packet);
            return;
        }
        return;
    }

}