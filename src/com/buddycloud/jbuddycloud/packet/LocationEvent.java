package com.buddycloud.jbuddycloud.packet;

import org.jivesoftware.smack.packet.PacketExtension;

public class LocationEvent implements PacketExtension {
	
	public static final int PREV = 1;
	public static final int CURRENT = 2;
	public static final int NEXT = 3;
	public String text;
	public String street;
	public String postalCode;
	public String area;
	public String locality;
	public String region;
	public String country;
	public int lat;
	public int lng;
	public int accuracy;
	public int type;

	public String getElementName() {
		 return "event";
	}

	public String getNamespace() {
		return "http://jabber.org/protocol/pubsub#event";
	}

	public String toXML() {
		StringBuffer sb = new StringBuffer();
		sb.append("<lat>").append(lat).append("</lat>")
		.append("<lon>").append(lng).append("</lon>");
		return sb.toString();
	}

}
