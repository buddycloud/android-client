package com.buddycloud.jbuddycloud.packet;

import org.jivesoftware.smack.packet.IQ;

public class MessageArchiveManagement extends IQ {

    public String start = null;
    public String end = null;

    public MessageArchiveManagement() {
    }

    @Override
    public String getChildElementXML() {
        StringBuilder sb = new StringBuilder();
        sb.append("<query xmlns='urn:xmpp:mam:tmp'>");
        if (start != null && start.length() > 0) {
            sb.append("<start>").append(start).append("</start>");
        }
        if (end != null && end.length() > 0) {
            sb.append("<end>").append(end).append("</end>");
        }
        sb.append("</query>");
        return sb.toString();
    }

}
