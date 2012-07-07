package com.buddycloud.asmack;

import java.util.ArrayList;
import java.util.Collection;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smackx.pubsub.EventElement;
import org.jivesoftware.smackx.pubsub.ItemsExtension;
import org.jivesoftware.smackx.pubsub.PayloadItem;

import android.content.ContentResolver;
import android.content.ContentValues;

import com.buddycloud.content.BuddyCloud.Roster;
import com.buddycloud.jbuddycloud.packet.BCAtom;
import com.buddycloud.jbuddycloud.packet.GeoLoc;

public class BuddycloudLocationChannelListener implements PacketListener {

    private final ContentResolver resolver;

    public BuddycloudLocationChannelListener(ContentResolver resolver) {
        this.resolver = resolver;
    }

    private void processItems(String from, ItemsExtension items) {
        String node = items.getNode();
        for (PacketExtension itemsExtension : items.getExtensions()) {
            if (!(itemsExtension instanceof PayloadItem)) {
                continue;
            }
            PayloadItem<?> payload = (PayloadItem<?>) itemsExtension;
            if (payload.getPayload() instanceof BCAtom) {
                if (isBroadcaster(from)) {
                    BCAtom atom = (BCAtom) payload.getPayload();
                    atom.setId(payload.getId());
                } else {
                    System.err.println("Atom by unknown sender " + from);
                }
            } else
            if (payload .getPayload() instanceof GeoLoc) {
                GeoLoc geoLoc = (GeoLoc) payload.getPayload();
                if (node.equals(
                        "http://jabber.org/protocol/geoloc")
                ) {
                    geoLoc.setLocType(GeoLoc.Type.CURRENT);
                } else
                if (node.equals(
                    "http://jabber.org/protocol/geoloc-next")
                ) {
                    geoLoc.setLocType(GeoLoc.Type.NEXT);
                } else
                if (node.equals(
                    "http://jabber.org/protocol/geoloc-prev")
                ) {
                    geoLoc.setLocType(GeoLoc.Type.PREV);
                } else
                if (isBroadcaster(from)) {
                    if (node.endsWith("/geo/current")) {
                        geoLoc.setLocType(GeoLoc.Type.CURRENT);
                        from = node.substring(6);
                        from = from.substring(0, from.length()-12);
                    } else
                    if (node.endsWith("/geo/future")) {
                        geoLoc.setLocType(GeoLoc.Type.NEXT);
                        from = node.substring(6);
                        from = from.substring(0, from.length()-11);
                    } else
                    if (node.endsWith("/geo/previous")) {
                        geoLoc.setLocType(GeoLoc.Type.PREV);
                        from = node.substring(6);
                        from = from.substring(0, from.length()-13);
                    }
                }
                if (geoLoc.getType() == null) {
                    return;
                }
                ContentValues values = new ContentValues();
                if (geoLoc.getLocType().equals(GeoLoc.Type.CURRENT)) {
                    values.put(Roster.GEOLOC, geoLoc.getText());
                } else
                if (geoLoc.getLocType().equals(GeoLoc.Type.NEXT)) {
                    values.put(Roster.GEOLOC_NEXT, geoLoc.getText());
                } else
                if (geoLoc.getLocType().equals(GeoLoc.Type.PREV)) {
                    values.put(Roster.GEOLOC_PREV, geoLoc.getText());
                }
                resolver.update(Roster.CONTENT_URI, values,
                        Roster.JID + "='/user/" + from + "/channel'",
                        null);
            } else {
                System.err.println("Unknown item payload " +
                        payload.getPayload().getClass().toString());
            }
        }
    }

    public void processPacket(Packet packet) {
        try {
            Collection<PacketExtension> extensions = null;

            if (packet instanceof IQ) {
                extensions = ((IQ)packet).getExtensions();
            } else
            if (packet instanceof Message) {
                extensions = ((Message)packet).getExtensions();
            } else {
                return;
            }

            String from = packet.getFrom();

            for (PacketExtension packetExtension : extensions) {

                if (packetExtension instanceof EventElement) {
                    EventElement event = (EventElement) packetExtension;
                    for (PacketExtension eventExtension : event.getExtensions()) {
                        if (!(eventExtension instanceof ItemsExtension)) {
                            continue;
                        }
                        processItems(from, (ItemsExtension)eventExtension);
                    }
                } else
                if (packetExtension instanceof ItemsExtension) {
                    processItems(from, (ItemsExtension)packetExtension);
                }

            }
        } catch (Throwable t) {
            t.printStackTrace(System.err);
        }
    }

    private final static boolean isBroadcaster(String jid) {
        return jid.equals("broadcaster.buddycloud.com") ||
               jid.equals("pubsub-bridge@broadcaster.buddycloud.com");
    }

}
