package com.buddycloud.jbuddycloud.packet;

import org.jivesoftware.smack.packet.IQ;

public class PlacesIQ extends IQ {

	private static String GET_PLACES = "<query xmlns='http://buddycloud.com/protocol/place#myplaces'>" +
										"<options>" +
										"<feature var='id' />" +
										"<feature var='name' />" +
										"<feature var='shared' />" +
										"</options>" +
										"</query>";
	
    @Override
    public String getChildElementXML() {
    	return GET_PLACES;
    }

}
