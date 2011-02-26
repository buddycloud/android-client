package com.buddycloud.jbuddycloud.packet;

import org.jivesoftware.smack.packet.IQ;

public class ChannelFetch extends IQ {

    private final long since;
    private final String node;

    public ChannelFetch(String node, long since) {
        this.node = node;
        this.since = since;
        setTo("pubsub-bridge@broadcaster.buddycloud.com");
        setType(Type.GET);
    }

    @Override
    public String getChildElementXML() {
        StringBuilder sb = new StringBuilder();
        sb.append("<pubsub xmlns='http://jabber.org/protocol/pubsub'>");
        sb.append("<items node='");
        sb.append(node);
        sb.append("'><set xmlns='http://jabber.org/protocol/rsm'>");
        if (since != 0l) {
            sb.append("<after>");
            sb.append(Long.toString(since));
            sb.append("</after>");
        }
        sb.append("</set></items></pubsub>");
        return sb.toString();
    }

}
