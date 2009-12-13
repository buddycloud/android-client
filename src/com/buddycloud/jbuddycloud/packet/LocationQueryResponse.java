package com.buddycloud.jbuddycloud.packet;

import org.jivesoftware.smack.packet.IQ;

public class LocationQueryResponse extends IQ {
    
    public String label;
    public int quality;
    public int place_id;
    public String state;

    @Override
    public String getChildElementXML() {
        // TODO Auto-generated method stub
        return null;
    }

}
