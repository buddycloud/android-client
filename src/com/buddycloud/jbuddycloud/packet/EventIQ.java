package com.buddycloud.jbuddycloud.packet;

import org.jivesoftware.smack.packet.IQ;

public class EventIQ extends IQ {

    @Override
    public String getChildElementXML() {
        StringBuilder sb = new StringBuilder();
        sb.append("<event xmlns='http://jabber.org/protocol/pubsub#event'>");
        sb.append(getExtensionsXML());
        sb.append("</event>");
        return sb.toString();
    }

}
