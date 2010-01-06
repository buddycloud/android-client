package com.buddycloud.jbuddycloud.provider;

import java.util.List;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smackx.Form;
import org.jivesoftware.smackx.packet.DiscoverInfo;
import org.jivesoftware.smackx.pubsub.CollectionNode;
import org.jivesoftware.smackx.pubsub.ConfigureForm;
import org.jivesoftware.smackx.pubsub.Node;
import org.jivesoftware.smackx.pubsub.NodeExtension;
import org.jivesoftware.smackx.pubsub.PubSubElementType;
import org.jivesoftware.smackx.pubsub.SubscribeForm;
import org.jivesoftware.smackx.pubsub.Subscription;
import org.jivesoftware.smackx.pubsub.SubscriptionsExtension;
import org.jivesoftware.smackx.pubsub.listener.ItemDeleteListener;
import org.jivesoftware.smackx.pubsub.listener.ItemEventListener;
import org.jivesoftware.smackx.pubsub.listener.NodeConfigListener;
import org.jivesoftware.smackx.pubsub.packet.PubSub;
import org.jivesoftware.smackx.pubsub.packet.PubSubNamespace;

public class BCCollectionNode extends Node {

    private final CollectionNode node;

    public BCCollectionNode(XMPPConnection connection, CollectionNode node) {
        super(connection, node.getId());
        this.node = node;
        this.to = "broadcaster.buddycloud.com";
    }

    public void addConfigurationListener(NodeConfigListener listener) {
        node.addConfigurationListener(listener);
    }

    public void addItemDeleteListener(ItemDeleteListener listener) {
        node.addItemDeleteListener(listener);
    }

    public void addItemEventListener(ItemEventListener listener) {
        node.addItemEventListener(listener);
    }

    public DiscoverInfo discoverInfo() throws XMPPException {
        return node.discoverInfo();
    }

    public boolean equals(Object o) {
        return node.equals(o);
    }

    public List<Subscription> getAllSubscriptions() throws XMPPException {
        return node.getAllSubscriptions();
    }

    public String getId() {
        return node.getId();
    }

    public ConfigureForm getNodeConfiguration() throws XMPPException {
        return node.getNodeConfiguration();
    }

    public SubscribeForm getSubscriptionOptions(String jid,
            String subscriptionId) throws XMPPException {
        return node.getSubscriptionOptions(jid, subscriptionId);
    }

    public SubscribeForm getSubscriptionOptions(String jid)
            throws XMPPException {
        return node.getSubscriptionOptions(jid);
    }

    public List<Subscription> getSubscriptions() throws XMPPException {
        return node.getSubscriptions();
    }

    public void removeConfigurationListener(NodeConfigListener listener) {
        node.removeConfigurationListener(listener);
    }

    public void removeItemDeleteListener(ItemDeleteListener listener) {
        node.removeItemDeleteListener(listener);
    }

    public void removeItemEventListener(ItemEventListener listener) {
        node.removeItemEventListener(listener);
    }

    public void sendConfigurationForm(Form submitForm) throws XMPPException {
        node.sendConfigurationForm(submitForm);
    }

    public Subscription subscribe(String jid, SubscribeForm subForm)
            throws XMPPException {
        return node.subscribe(jid, subForm);
    }

    public Subscription subscribe(String jid) throws XMPPException {
        return node.subscribe(jid);
    }

    public String toString() {
        return node.toString();
    }

    public void unsubscribe(String jid, String subscriptionId)
            throws XMPPException {
        node.unsubscribe(jid, subscriptionId);
    }

    public void unsubscribe(String jid) throws XMPPException {
        node.unsubscribe(jid);
    }

    public List<Subscription> getBCSubscriptions()
        throws XMPPException
    {
        PubSub reply = (PubSub)sendPubsubPacket(Type.GET, new NodeExtension(
                PubSubElementType.SUBSCRIPTIONS_OWNER,
                getId()), PubSubNamespace.OWNER);
        SubscriptionsExtension subElem = (SubscriptionsExtension)
                reply.getExtension(PubSubElementType.SUBSCRIPTIONS);
        if (subElem == null) {
            subElem = (SubscriptionsExtension)
                reply.getExtension(PubSubElementType.SUBSCRIPTIONS_OWNER);
        }
        return subElem.getSubscriptions();
    }

}
