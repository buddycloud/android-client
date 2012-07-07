package com.buddycloud.jbuddycloud.packet;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.util.StringUtils;

public class MessageArchiveManagement extends IQ {

    public String start = null;
    public String end = null;
    public int limit = -1;

    public MessageArchiveManagement() {
    }

    @Override
    public String getChildElementXML() {
        StringBuilder sb = new StringBuilder();
        sb.append("<query xmlns='urn:xmpp:archive#management'>");
        if (start != null && start.length() > 0) {
            sb.append("<start>").append(start).append("</start>");
        }
        if (end != null && end.length() > 0) {
            sb.append("<end>").append(end).append("</end>");
        }
        if (limit > 0) {
            sb.append("<limit>").append(limit).append("</limit>");
        }
        sb.append("</query>");
        return sb.toString();
    }

}
