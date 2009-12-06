package com.buddycloud.jbuddycloud.provider;

import java.util.HashMap;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.xmlpull.v1.XmlPullParser;

import android.util.Log;

import com.buddycloud.jbuddycloud.packet.LocationEvent;


public class PubSubLocationEventProvider implements PacketExtensionProvider  {

	/*
	 * No argument constructor as required by PacketExtensionProvider
	 */
	public PubSubLocationEventProvider(){
		
	}
	
	public static String getNS() { return "http://jabber.org/protocol/pubsub#event"; }
	
	
	
	 public PacketExtension parseExtension(XmlPullParser parser) throws Exception
	 {
//		 PubSubLocationEvent pubSubLocEvent = new PubSubLocationEvent();
//		 LocationHierarchy locH = null;
//		 String token = null;
//		 Location loc = null;
		 boolean done = false;
		 boolean insideLocation = false;
		 LocationEvent loc = new LocationEvent();
		 HashMap<String, String> tmp = new HashMap<String, String>();
		 String name = parser.getName();
		 int eventType;
		 StringBuffer sb = null;
		 while(!done) {
			 switch (parser.next()) {
			 
			 case XmlPullParser.START_TAG:
				 name = parser.getName();
				 if (name.equals("items")) {
					 String node = parser.getAttributeValue("", "node");
					 if (node.equals("http://jabber.org/protocol/geoloc"))
						 loc.type = LocationEvent.CURRENT;
					 else if (node.equals("http://jabber.org/protocol/geoloc-prev"))
						 loc.type = LocationEvent.PREV;
					 else if (node.equals("http://jabber.org/protocol/geoloc-next"))
						 loc.type = LocationEvent.NEXT;
				 } else
					 sb = new StringBuffer();
				 break;
			 case XmlPullParser.TEXT:
				 sb.append(parser.getText());
				 break;
			 case XmlPullParser.END_TAG:
				 if (parser.getName().equals("geoloc"))
					 done = true;
				 tmp.put(name, sb.toString());
				 break;
			 }
		}

		 loc.text = tmp.get("text");
		 loc.street = tmp.get("street");
		 loc.postalCode = tmp.get("postalcode");
		 loc.area = tmp.get("area");
		 loc.locality = tmp.get("locality");
		 loc.region = tmp.get("region");
		 loc.country = tmp.get("country");
		 String lat = tmp.get("lat");
		 if (lat != null)
			 loc.lat = (int)(Double.parseDouble(lat)*1000000);
		 String lng = tmp.get("lng");
		 if (lng != null)
			 loc.lng = (int)(Double.parseDouble(lat)*1000000);
//		 loc.accuracy = Integer.parseInt(tmp.get("accuracy"));
		 Log.e("BuddycloudService", "LOC update" + loc.toXML());
		 return loc;
	 }
	
}
