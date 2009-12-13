package com.buddycloud.jbuddycloud;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smackx.NodeInformationProvider;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.packet.DiscoverInfo;
import org.jivesoftware.smackx.packet.DiscoverInfo.Identity;
import org.jivesoftware.smackx.packet.DiscoverItems.Item;

import com.buddycloud.jbuddycloud.provider.PubSubLocationEventProvider;

public class BuddycloudClient extends XMPPConnection {

    public static final String VERSION = "0.0.1";

    public static final Pattern JID_PATTERN =
        Pattern.compile("..*@[^.].*\\.[^.][^.][^.]*");

    static {
        ProviderManager.getInstance().addIQProvider("query",
                "http://jabber.org/protocol/disco#items",
                new org.jivesoftware.smackx.provider.DiscoverItemsProvider());
        ProviderManager.getInstance().addIQProvider("query",
                "http://jabber.org/protocol/disco#info",
                new org.jivesoftware.smackx.provider.DiscoverInfoProvider());
        ProviderManager.getInstance().addIQProvider("pubsub",
                "http://jabber.org/protocol/pubsub",
                new org.jivesoftware.smackx.pubsub.provider.PubSubProvider());
        ProviderManager
                .getInstance()
                .addExtensionProvider(
                        "create",
                        "http://jabber.org/protocol/pubsub",
                        new org.jivesoftware.smackx.pubsub.provider.SimpleNodeProvider());
        ProviderManager.getInstance().addExtensionProvider("items",
                "http://jabber.org/protocol/pubsub",
                new org.jivesoftware.smackx.pubsub.provider.ItemsProvider());
        ProviderManager.getInstance().addExtensionProvider("item",
                "http://jabber.org/protocol/pubsub",
                new org.jivesoftware.smackx.pubsub.provider.ItemProvider());
        ProviderManager
                .getInstance()
                .addExtensionProvider(
                        "subscriptions",
                        "http://jabber.org/protocol/pubsub",
                        new org.jivesoftware.smackx.pubsub.provider.SubscriptionsProvider());
        ProviderManager
                .getInstance()
                .addExtensionProvider(
                        "subscription",
                        "http://jabber.org/protocol/pubsub",
                        new org.jivesoftware.smackx.pubsub.provider.SubscriptionProvider());
        ProviderManager
                .getInstance()
                .addExtensionProvider(
                        "affiliations",
                        "http://jabber.org/protocol/pubsub",
                        new org.jivesoftware.smackx.pubsub.provider.AffiliationsProvider());
        ProviderManager
                .getInstance()
                .addExtensionProvider(
                        "affiliation",
                        "http://jabber.org/protocol/pubsub",
                        new org.jivesoftware.smackx.pubsub.provider.AffiliationProvider());
        ProviderManager.getInstance().addExtensionProvider("options",
                "http://jabber.org/protocol/pubsub",
                new org.jivesoftware.smackx.pubsub.provider.FormNodeProvider());
        ProviderManager.getInstance().addIQProvider("pubsub",
                "http://jabber.org/protocol/pubsub#owner",
                new org.jivesoftware.smackx.pubsub.provider.PubSubProvider());
        ProviderManager.getInstance().addExtensionProvider("configure",
                "http://jabber.org/protocol/pubsub#owner",
                new org.jivesoftware.smackx.pubsub.provider.FormNodeProvider());
        ProviderManager.getInstance().addExtensionProvider("default",
                "http://jabber.org/protocol/pubsub#owner",
                new org.jivesoftware.smackx.pubsub.provider.FormNodeProvider());
        // ProviderManager.getInstance().addExtensionProvider("event","http://jabber.org/protocol/pubsub#event",new
        // org.jivesoftware.smackx.pubsub.provider.EventProvider());
        ProviderManager
                .getInstance()
                .addExtensionProvider(
                        "configuration",
                        "http://jabber.org/protocol/pubsub#event",
                        new org.jivesoftware.smackx.pubsub.provider.ConfigEventProvider());
        ProviderManager
                .getInstance()
                .addExtensionProvider(
                        "delete",
                        "http://jabber.org/protocol/pubsub#event",
                        new org.jivesoftware.smackx.pubsub.provider.SimpleNodeProvider());
        ProviderManager.getInstance().addExtensionProvider("options",
                "http://jabber.org/protocol/pubsub#event",
                new org.jivesoftware.smackx.pubsub.provider.FormNodeProvider());
        ProviderManager.getInstance().addExtensionProvider("items",
                "http://jabber.org/protocol/pubsub#event",
                new org.jivesoftware.smackx.pubsub.provider.ItemsProvider());
        ProviderManager.getInstance().addExtensionProvider("item",
                "http://jabber.org/protocol/pubsub#event",
                new org.jivesoftware.smackx.pubsub.provider.ItemProvider());
        ProviderManager
                .getInstance()
                .addExtensionProvider(
                        "retract",
                        "http://jabber.org/protocol/pubsub#event",
                        new org.jivesoftware.smackx.pubsub.provider.RetractEventProvider());
        ProviderManager
                .getInstance()
                .addExtensionProvider(
                        "purge",
                        "http://jabber.org/protocol/pubsub#event",
                        new org.jivesoftware.smackx.pubsub.provider.SimpleNodeProvider());
        ProviderManager.getInstance().addExtensionProvider("event",
                PubSubLocationEventProvider.getNS(),
                new PubSubLocationEventProvider());
    }

    public static BuddycloudClient createBuddycloudClient(
        String jid,
        String password,
        String host,
        Integer port,
        String cachedUsername
    ) {
        ArrayList<ConnectionConfiguration> configs =
            new ArrayList<ConnectionConfiguration>(3);

        if (jid.length() == 0) {
            jid = null;
        }

        if (host != null && port != null) {
            configs.add(new ConnectionConfiguration(host, port));
        }

        if (jid == null) {
            configs.add(new ConnectionConfiguration("buddycloud.com"));
        } else
        if (JID_PATTERN.matcher(jid).matches()) {
            configs.add(
                new ConnectionConfiguration(
                    jid.substring(jid.lastIndexOf('@') + 1)
                )
            );
        }

        BuddycloudClient connection = null;
        for (ConnectionConfiguration conf: configs) {

            try {
                connection = new BuddycloudClient(conf);
                connection.connect();
            } catch (Exception e) {
                // TODO logging
            }

            if (!connection.isConnected()) {
                continue;
            }

            if (jid == null) {
                try {
                    connection.loginAnonymously();
                } catch (XMPPException e) {
                    // TODO logging
                }
            } else {
                if (cachedUsername != null) {
                    try {
                        connection.login(cachedUsername, password);
                    } catch (XMPPException e) {
                        // TODO logging
                    }
                }
                String id = jid + '@';
                while (id.indexOf('@') != -1) {
                    id = id.substring(0, id.lastIndexOf('@'));
                    if (!connection.isAuthenticated() &&
                        !jid.equals(cachedUsername)
                    ) {
                        try {
                            connection.login(id, password);
                        } catch (XMPPException e) {
                            // TODO logging
                        }
                        if (!connection.isAuthenticated()) {
                            try {
                                connection.disconnect();
                            } catch (Exception e) {
                                // Unimportant
                            }
                            try {
                                connection.disconnect();
                                connection = new BuddycloudClient(conf);
                                connection.connect();
                            } catch (Exception e) {
                                // TODO logging
                            }
                        }
                    }
                }
            }

            if (connection != null && (
                connection.isAuthenticated() || connection.isAnonymous()
            )) {
                connection.discoveryManager
                .addFeature("http://jabber.org/protocol/disco#info");
                connection.discoveryManager
                .addFeature("http://jabber.org/protocol/pubsub");
                connection.discoveryManager
                .addFeature("http://jabber.org/protocol/geoloc");
                connection.discoveryManager
                .addFeature("http://jabber.org/protocol/geoloc+notify");
                connection.discoveryManager
                .addFeature("http://jabber.org/protocol/geoloc-prev");
                connection.discoveryManager
                .addFeature("http://jabber.org/protocol/geoloc-prev+notify");
                connection.discoveryManager
                .addFeature("http://jabber.org/protocol/geoloc-next");
                connection.discoveryManager
                .addFeature("http://jabber.org/protocol/geoloc-next+notify");
                connection.discoveryManager
                .setNodeInformationProvider(
                    "http://buddydroid.com/caps#" + VERSION,
                    new JBuddycloudFeatures()
                );
                connection.sendPacket(new InitialPresence());
                return connection;
            } else {
                if (connection.isConnected()) {
                    try {
                        connection.disconnect();
                    } catch (Exception e) {
                        // TODO logging
                    }
                }
            }
        }

        return null;
    }

    private ServiceDiscoveryManager discoveryManager;

    public BuddycloudClient(ConnectionConfiguration config) {
        super(config);
    }

    @Override
    public void connect() throws XMPPException {
        super.connect();
        int i = 0;
        while (!isConnected() && i < 600) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            i += 1;
        }
        discoveryManager = new ServiceDiscoveryManager(this);
    }

    private static class InitialPresence extends Packet {

        @Override
        public void setError(XMPPError error) {
            super.setError(error);
        }

        @Override
        public String toXML() {
            return "<presence><priority>10</priority><status>available</status>"
                    + "<c xmlns=\"http://jabber.org/protocol/caps\" node=\"http://buddydroid.com/caps\" "
                    + "ver=\"" + VERSION + "\"/></presence>";
        }

    }

    @Override
    public synchronized void login(String username, String password)
            throws XMPPException {
        super.login(username, password, "buddydroid");
    }

    private static class JBuddycloudFeatures
    implements NodeInformationProvider {

        public List<String> getNodeFeatures() {
            List<String> features = new ArrayList<String>();
            features.add("http://jabber.org/protocol/disco#info");
            features.add("http://jabber.org/protocol/pubsub");
            features.add("http://jabber.org/protocol/geoloc");
            features.add("http://jabber.org/protocol/geoloc+notify");
            features.add("http://jabber.org/protocol/geoloc-prev");
            features.add("http://jabber.org/protocol/geoloc-prev+notify");
            features.add("http://jabber.org/protocol/geoloc-next");
            features.add("http://jabber.org/protocol/geoloc-next+notify");
            return features;
        }

        public List<Identity> getNodeIdentities() {
            List<Identity> r = new ArrayList<Identity>();
            Identity id = new DiscoverInfo.Identity("client", "BuddycloudClient");
            id.setType("mobile");
            r.add(id);
            return r;
        }

        @Override
        public List<Item> getNodeItems() {
            // TODO Auto-generated method stub
            return null;
        }

    }
}
