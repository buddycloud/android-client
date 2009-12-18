package com.buddycloud.jbuddycloud;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.Roster.SubscriptionMode;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smackx.NodeInformationProvider;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.packet.DiscoverInfo;
import org.jivesoftware.smackx.packet.DiscoverInfo.Identity;
import org.jivesoftware.smackx.packet.DiscoverItems.Item;
import org.jivesoftware.smackx.pubsub.ItemPublishEvent;
import org.jivesoftware.smackx.pubsub.PayloadItem;
import org.jivesoftware.smackx.pubsub.PubSubManager;
import org.jivesoftware.smackx.pubsub.listener.ItemEventListener;

import android.util.Log;

import com.buddycloud.jbuddycloud.packet.GeoLoc;
import com.buddycloud.jbuddycloud.provider.LocationQueryResponseProvider;

public class BuddycloudClient extends XMPPConnection implements ItemEventListener, PacketListener {

    public static final String VERSION = "0.0.1";

    public static final Pattern JID_PATTERN =
        Pattern.compile("..*@[^.].*\\.[^.][^.][^.]*");

    static {
        System.setProperty("smack.debugEnabled", "true");

        ProviderManager pm = ProviderManager.getInstance();
        pm.addIQProvider(
            "query", "http://jabber.org/protocol/disco#items",
             new org.jivesoftware.smackx.provider.DiscoverItemsProvider()
        );
        pm.addIQProvider("query",
                "http://jabber.org/protocol/disco#info",
                new org.jivesoftware.smackx.provider.DiscoverInfoProvider());
        pm.addIQProvider("pubsub",
                "http://jabber.org/protocol/pubsub",
                new org.jivesoftware.smackx.pubsub.provider.PubSubProvider());
        pm.addExtensionProvider(
                        "create",
                        "http://jabber.org/protocol/pubsub",
                        new org.jivesoftware.smackx.pubsub.provider.SimpleNodeProvider());
        pm.addExtensionProvider("items",
                "http://jabber.org/protocol/pubsub",
                new org.jivesoftware.smackx.pubsub.provider.ItemsProvider());
        pm.addExtensionProvider("item",
                "http://jabber.org/protocol/pubsub",
                new org.jivesoftware.smackx.pubsub.provider.ItemProvider());
        pm.addExtensionProvider(
                        "subscriptions",
                        "http://jabber.org/protocol/pubsub",
                        new org.jivesoftware.smackx.pubsub.provider.SubscriptionsProvider());
        pm.addExtensionProvider(
                        "subscription",
                        "http://jabber.org/protocol/pubsub",
                        new org.jivesoftware.smackx.pubsub.provider.SubscriptionProvider());
        pm.addExtensionProvider(
                        "affiliations",
                        "http://jabber.org/protocol/pubsub",
                        new org.jivesoftware.smackx.pubsub.provider.AffiliationsProvider());
        pm.addExtensionProvider(
                        "affiliation",
                        "http://jabber.org/protocol/pubsub",
                        new org.jivesoftware.smackx.pubsub.provider.AffiliationProvider());
        pm.addExtensionProvider("options",
                "http://jabber.org/protocol/pubsub",
                new org.jivesoftware.smackx.pubsub.provider.FormNodeProvider());
        pm.addIQProvider("pubsub",
                "http://jabber.org/protocol/pubsub#owner",
                new org.jivesoftware.smackx.pubsub.provider.PubSubProvider());
        pm.addExtensionProvider("configure",
                "http://jabber.org/protocol/pubsub#owner",
                new org.jivesoftware.smackx.pubsub.provider.FormNodeProvider());
        pm.addExtensionProvider("default",
                "http://jabber.org/protocol/pubsub#owner",
                new org.jivesoftware.smackx.pubsub.provider.FormNodeProvider());
        pm.addExtensionProvider("event",
                "http://jabber.org/protocol/pubsub#event",
                new org.jivesoftware.smackx.pubsub.provider.EventProvider());
        pm.addExtensionProvider(
                        "configuration",
                        "http://jabber.org/protocol/pubsub#event",
                        new org.jivesoftware.smackx.pubsub.provider.ConfigEventProvider());
        pm.addExtensionProvider(
                        "delete",
                        "http://jabber.org/protocol/pubsub#event",
                        new org.jivesoftware.smackx.pubsub.provider.SimpleNodeProvider());
        pm.addExtensionProvider("options",
                "http://jabber.org/protocol/pubsub#event",
                new org.jivesoftware.smackx.pubsub.provider.FormNodeProvider());
        pm.addExtensionProvider("items",
                "http://jabber.org/protocol/pubsub#event",
                new org.jivesoftware.smackx.pubsub.provider.ItemsProvider());
        pm.addExtensionProvider("item",
                "http://jabber.org/protocol/pubsub#event",
                new org.jivesoftware.smackx.pubsub.provider.ItemProvider());
        pm.addExtensionProvider(
                        "retract",
                        "http://jabber.org/protocol/pubsub#event",
                        new org.jivesoftware.smackx.pubsub.provider.RetractEventProvider());
        pm.addExtensionProvider(
                        "purge",
                        "http://jabber.org/protocol/pubsub#event",
                        new org.jivesoftware.smackx.pubsub.provider.SimpleNodeProvider());
        /*
        pm.addExtensionProvider("event",
                PubSubLocationEventProvider.getNS(),
                new PubSubLocationEventProvider()
        );
        */
        pm.addExtensionProvider(
                "geoloc",
                "http://jabber.org/protocol/geoloc",
                new GeoLoc()
        );
        pm.addIQProvider("location",
                LocationQueryResponseProvider.getNS(),
                new LocationQueryResponseProvider());
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

        if (jid != null && jid.length() == 0) {
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
    private PubSubManager pubSubManager;

    public PubSubManager getPubSubManager() {
        return pubSubManager;
    }

    public BuddycloudClient(ConnectionConfiguration config) {
        super(config);
    }

    @Override
    public void connect() throws XMPPException {
        super.connect();
        int i = 0;
        while (!isConnected() && i < 100) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            i += 1;
        }
        if (!isConnected()) {
            return;
        }
        addPacketListener(this, null);
        discoveryManager = new ServiceDiscoveryManager(this);
        pubSubManager = new PubSubManager(this, "broadcaster.buddycloud.com");
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

    private static class BroadcasterPresence extends Packet {

        @Override
        public void setError(XMPPError error) {
            super.setError(error);
        }

        
        @Override
        public String toXML() {
            return "<presence to='broadcaster.buddycloud.com' type='subscribe' xmlns='jabber:client'/>";
        }

    }

    @Override
    public synchronized void login(String username, String password)
            throws XMPPException {
        super.login(username, password, "buddydroid");
        String jid = getUser();
        if (jid.lastIndexOf('/') != -1) {
            jid = jid.substring(0, jid.lastIndexOf('/'));
        }

        getRoster().setSubscriptionMode(SubscriptionMode.manual);

        Presence broadcaster =
            getRoster().getPresence("broadcaster.buddycloud.com");
        if (broadcaster == null) {
            getRoster().createEntry("broadcaster.buddycloud.com",
                "broadcaster.buddycloud.com", null);
        }

        BroadcasterPresence presence = new BroadcasterPresence();
        sendPacket(presence);

        pubSubManager.getSupportedFeatures();
        pubSubManager.getSubscriptions();
        for (String channel : new String[]{
            "/user/" + jid + "/mood",
            "/user/" + jid + "/channel",
            "/user/" + jid + "/geo/current",
            "/user/" + jid + "/geo/future",
            "/user/" + jid + "/geo/previous"
        }) {
            try {
                pubSubManager.getNode(channel)
                    .addItemEventListener(this);
            } catch (Throwable t) {
                try {
                    pubSubManager.getNode(channel).subscribe(jid);
                    pubSubManager.getNode(channel)
                    .addItemEventListener(this);
                } catch (Throwable t2) {
                    Log.e("SMACK", t.getMessage(), t);
                    Log.e("SMACK", t2.getMessage(), t2);
                }
            }
        }
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

    @Override
    public void handlePublishedItems(ItemPublishEvent items) {
        for (Object item : items.getItems()) {
            Log.d("SMACK", item.getClass().toString());
            Log.d("SMACK", item.toString());
            if (item instanceof PayloadItem) {
                PayloadItem payload = (PayloadItem) item;
                Log.d("SMACK", payload.getClass().toString());
                Log.d("SMACK", payload.toString());
                PacketExtension p = payload.getPayload();
                Log.d("SMACK", p.getClass().toString());
                Log.d("SMACK", p.toString());
                if (p instanceof GeoLoc) {
                    Log.d("SMACK", " ::::::::::::: JACKPOT !");
                }
            }
        }
    }

    @Override
    public void processPacket(Packet packet) {
        Log.d("SMACK", packet.getClass().toString());
    }
}
