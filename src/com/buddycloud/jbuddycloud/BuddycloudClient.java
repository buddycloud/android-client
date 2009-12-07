package com.buddycloud.jbuddycloud;

import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smackx.NodeInformationProvider;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.packet.DiscoverInfo;
import org.jivesoftware.smackx.packet.DiscoverInfo.Identity;
import org.jivesoftware.smackx.packet.DiscoverItems.Item;

import android.util.Log;

public class BuddycloudClient extends XMPPConnection {

	static final String version = "5.023";

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

	class InitialPresence extends Packet {

		@Override
		public void setError(XMPPError error) {
			super.setError(error);
			Log.e("SMACK", "InitialPresence ERROR " + error.getMessage());
		}

		@Override
		public String toXML() {
			Log.e("SMACK", "InitialPresence SENDING");
			return "<presence><priority>10</priority><status>available</status>"
					+ "<c xmlns=\"http://jabber.org/protocol/caps\" node=\"http://buddydroid.com/caps\" "
					+ "ver=\"" + version + "\"/></presence>";
		}

	}

	@Override
	public synchronized void login(String username, String password)
			throws XMPPException {
		super.login(username, password /* , "buddydroid", false */);
		discoveryManager.addFeature("http://jabber.org/protocol/disco#info");
		discoveryManager.addFeature("http://jabber.org/protocol/pubsub");
		discoveryManager.addFeature("http://jabber.org/protocol/geoloc");
		discoveryManager.addFeature("http://jabber.org/protocol/geoloc+notify");
		discoveryManager.addFeature("http://jabber.org/protocol/geoloc-prev");
		discoveryManager.addFeature("http://jabber.org/protocol/geoloc-prev+notify");
		discoveryManager.addFeature("http://jabber.org/protocol/geoloc-next");
		discoveryManager.addFeature("http://jabber.org/protocol/geoloc-next+notify");
		discoveryManager.setNodeInformationProvider("http://buddydroid.com/caps#"+version, new jBuddycloudFeatures());
		sendPacket(new InitialPresence());
	}

	class jBuddycloudFeatures implements NodeInformationProvider {

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
			Identity id = new DiscoverInfo.Identity("client", "buddydroid");
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
