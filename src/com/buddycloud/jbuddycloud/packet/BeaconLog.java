package com.buddycloud.jbuddycloud.packet;

import java.util.ArrayList;

import org.jivesoftware.smack.packet.IQ;

public class BeaconLog extends IQ {
	
	public LocationEvent location;
	
	public ArrayList<Beacon> beacons = new ArrayList<Beacon>();

	public static class Beacon {
		
		public String id;
		public String type;
		public int signal = Integer.MIN_VALUE;

		public Object toXML() {
			StringBuffer sb = new StringBuffer();
			sb.append("<reference>")
			.append("<id>").append(id).append("</id>")
			.append("<type>").append(type).append("</type>");
			if (signal != Integer.MIN_VALUE) {
			    sb.append("<signalstrength>").append(signal).append("</signalstrength>");
			}
			sb.append("</reference>");
			return sb.toString();
		}
	}
	

    public void add(String type, String id, int signal) {
        Beacon beacon = new Beacon();
        beacon.type = type;
        beacon.id = id;
        beacon.signal = signal;
        beacons.add(beacon);
    }

    public void add(String type, String id) {
        Beacon beacon = new Beacon();
        beacon.type = type;
        beacon.id = id;
        beacons.add(beacon);
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
