package com.buddycloud.jbuddycloud;

import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smackx.NodeInformationProvider;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.packet.DiscoverInfo;
import org.jivesoftware.smackx.packet.DiscoverInfo.Identity;
import org.jivesoftware.smackx.packet.DiscoverItems.Item;

import android.graphics.BitmapFactory;
import android.util.Log;

import com.buddycloud.jbuddycloud.packet.LocationEvent;
import com.buddycloud.jbuddycloud.provider.PubSubLocationEventProvider;



public class BuddycloudClient extends XMPPConnection {

	static final String version = "4.12345";

	public BuddycloudClient(ConnectionConfiguration config) {
		super(config);
		ProviderManager.getInstance().addExtensionProvider("event", PubSubLocationEventProvider.getNS(), new PubSubLocationEventProvider());
		
	}
	
	

	@Override
	public void connect() throws XMPPException {
		super.connect();
		
		
		ServiceDiscoveryManager dm = new ServiceDiscoveryManager(this);
		ServiceDiscoveryManager.setIdentityType("mobile");
		ServiceDiscoveryManager.setIdentityName("client");
		dm.setNodeInformationProvider("http://buddydroid.com/caps#"+version, new jBuddycloudFeatures());
	}



	@Override
	public synchronized void login(String username, String password) throws XMPPException {
		super.login(username, password /*, "buddydroid", false*/ );
		sendPacket(new InitialPresence());
//		addPacketListener(new PacketListener() {
//        	
//        	public void processPacket(Packet packet) {
//        		if (packet instanceof DiscoverInfo) {
////        		System.out.println("boops: "+packet.toXML());
//        			
//        		}
//        		if (packet instanceof IQ && packet.getXmlns().equals("http://jabber.org/protocol/disco#info")) {
//        			System.out.println("boops: "+packet.toXML());
//        			Features f = new Features();
//        			f.setPacketID(packet.getPacketID());
//        			f.setFrom(packet.getTo());
//        			f.setTo(packet.getFrom());
//        			sendPacket(f);
//        		}
//        	}
//        }, null);
	}

//	class Features extends IQ {
//
//		@Override
//		public String getChildElementXML() {
//			StringBuffer sb = new StringBuffer();
//			sb.append("<feature var='http://jabber.org/protocol/disco#info'/>");
//		    sb.append("<feature var=\"http://jabber.org/protocol/geoloc\"/>");
//		    sb.append("<feature var=\"http://jabber.org/protocol/geoloc+notify\"/>");
//		    sb.append("<feature var=\"http://jabber.org/protocol/geoloc-prev\"/>");
//		    sb.append("<feature var=\"http://jabber.org/protocol/geoloc-prev+notify\"/>");
//		    sb.append("<feature var=\"http://jabber.org/protocol/geoloc-next\"/>");
//		    sb.append("<feature var=\"http://jabber.org/protocol/geoloc-next+notify\"/>");
//			return sb.toString();
//		}
//
//		@Override
//		public String getXmlns() {
//			return "http://jabber.org/protocol/disco#info";
//		}
//
//	}
	 class InitialPresence extends Packet {

		@Override
		public String toXML() {
			return "<presence><priority>10</priority><status>available</status>" +
					"<c xmlns=\"http://jabber.org/protocol/caps\" node=\"http://buddydroid.com/caps\" " +
					"ver=\""+version+"\"/></presence>";
		}

	}
	 
	 

	class jBuddycloudFeatures implements NodeInformationProvider {
		
		public List<String> getNodeFeatures() {
			Log.d("ServiceDiscovery", "features are beeing discovered");
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
		
		public List<Item> getNodeItems() {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
	


}
