package com.buddycloud.jbuddycloud.provider;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.pubsub.CollectionNode;
import org.jivesoftware.smackx.pubsub.LeafNode;
import org.jivesoftware.smackx.pubsub.Node;
import org.jivesoftware.smackx.pubsub.PubSubManager;

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

}
