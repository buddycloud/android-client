package com.buddycloud.jbuddycloud.provider;

import java.util.Iterator;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smackx.FormField;
import org.jivesoftware.smackx.packet.DataForm;
import org.jivesoftware.smackx.packet.DiscoverInfo;
import org.jivesoftware.smackx.pubsub.CollectionNode;
import org.jivesoftware.smackx.pubsub.LeafNode;
import org.jivesoftware.smackx.pubsub.Node;
import org.jivesoftware.smackx.pubsub.PubSubManager;

import android.util.Log;

public class BCPubSubManager extends PubSubManager {

    private final Connection connection;

    public BCPubSubManager(Connection connection) {
        super(connection);
        this.connection = connection;
    }

    public BCPubSubManager(Connection connection, String toAddress) {
        super(connection, toAddress);
        this.connection = connection;
    }

    @Override
    public Node getNode(String id) throws XMPPException {
        Node node = super.getNode(id);
        if (node instanceof CollectionNode) {
            return new BCCollectionNode(connection, (CollectionNode) node);
        }
        if (node instanceof LeafNode) {
            return new BCLeafNode(connection, (LeafNode) node);
        }
        return node;
    }

    public String fetchChannelTitle(Node node) throws XMPPException {
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
                    Log.d("BC", "ChannelName[" + node.getId() +"]=" + field.getValues().next().trim());
                    return field.getValues().next().trim();
                }
            }
            Log.d("BC", "PE: " + packetExtension.getClass().toString());
        }
        return node.getId();
    }

}
