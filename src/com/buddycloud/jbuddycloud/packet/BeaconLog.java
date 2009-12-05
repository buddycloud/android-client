package com.buddycloud.jbuddycloud.packet;

import java.util.ArrayList;

import org.jivesoftware.smack.packet.IQ;

public class BeaconLog extends IQ {
	
	public LocationEvent location;
	
	public ArrayList<Beacon> beacons = new ArrayList<Beacon>();
	public static class Beacon {
		
		public String id;
		public String type;
		public int signal;
		
		public Object toXML() {
			StringBuffer sb = new StringBuffer();
			sb.append("<reference>")
			.append("<id>").append(id).append("</id>")
			.append("<type>").append(type).append("</type>")
			.append("<signalstrength>").append(signal).append("</signalstrength>")
			.append("</reference>");
			return sb.toString();
		}
	}
	

	@Override
	public String getChildElementXML() {
		StringBuffer sb = new StringBuffer();
		sb.append("<locationquery xmlns=\"urn:xmpp:locationquery:0\">\n");
		if (location != null) sb.append(location.toXML());
		sb.append("<publish>true</publish>");
		for (int i = 0; i < beacons.size(); i++) {
			sb.append(beacons.get(i).toXML());
		}
		sb.append("</locationquery>");
		return sb.toString();
	}

}
