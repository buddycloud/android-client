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
        sb.append("<pubsub xmlns='http://jabber.org/protocol/pubsub'><items node='")
          .append(node)
          .append("'><set xmlns='http://jabber.org/protocol/rsm'><after>")
          .append(Long.toString(since))
          .append("</after></set></items></pubsub>");
        return sb.toString();
    }

}
