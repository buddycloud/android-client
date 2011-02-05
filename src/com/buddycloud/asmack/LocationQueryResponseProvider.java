package com.buddycloud.asmack;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.xmlpull.v1.XmlPullParser;

import android.util.Log;

import com.buddycloud.jbuddycloud.packet.LocationQueryResponse;

public class LocationQueryResponseProvider implements IQProvider {
    
    public static String getNS() { return "http://buddycloud.com/protocol/location"; }

    public IQ parseIQ(XmlPullParser parser) throws Exception {
        Log.d("Service", "Juhu "+parser.getName());
        LocationQueryResponse loc = new LocationQueryResponse();
        loc.label = parser.getAttributeValue("", "label");
        loc.quality = Integer.valueOf(parser.getAttributeValue("", "cellpatternquality"));
        loc.place_id = Integer.valueOf(parser.getAttributeValue("", "placeid"));
        loc.state = parser.getAttributeValue("", "state");
        return loc;
    }

}
