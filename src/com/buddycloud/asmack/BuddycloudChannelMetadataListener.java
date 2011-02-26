package com.buddycloud.asmack;

import java.util.Iterator;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smackx.FormField;
import org.jivesoftware.smackx.packet.DataForm;
import org.jivesoftware.smackx.packet.DiscoverInfo;

import android.content.ContentResolver;
import android.content.ContentValues;

import com.buddycloud.content.BuddyCloud.Roster;

public class BuddycloudChannelMetadataListener implements PacketListener {

    private final ContentResolver resolver;

    public BuddycloudChannelMetadataListener(ContentResolver resolver) {
        this.resolver = resolver;
    }

    public void processPacket(Packet packet) {
        if (!(packet instanceof DiscoverInfo)) {
            return;
        }
        if (!isBroadcaster(packet.getFrom())) {
            return;
        }
        DiscoverInfo info = (DiscoverInfo) packet;
        if (info.getType() != Type.RESULT) {
            return;
        }
        if (info.getNode() == null || info.getNode().length() == 0) {
            return;
        }
        for (PacketExtension extension: info.getExtensions()) {
            if (!(extension instanceof DataForm)) {
                continue;
            }
            DataForm form = (DataForm) extension;
            Iterator<FormField> iterator = form.getFields();
            while (iterator.hasNext()) {
                FormField field = iterator.next();
                if ("pubsub#title".equals(field.getVariable())) {
                    if (!field.getValues().hasNext()) {
                        continue;
                    }
                    String title = field.getValues().next();
                    ContentValues values = new ContentValues();
                    values.put(Roster.NAME, title);
                    resolver.update(Roster.CONTENT_URI, values, "jid=?",
                                    new String[]{info.getNode()});
                    continue;
                }
            }
        }
    }

    private final static boolean isBroadcaster(String jid) {
        return jid.equals("broadcaster.buddycloud.com") ||
               jid.equals("pubsub-bridge@broadcaster.buddycloud.com");
    }

}
