package com.buddycloud.jbuddycloud.packet;

import org.jivesoftware.smack.packet.PacketExtension;

public class RSMSet implements PacketExtension {

    private final long after;

    public RSMSet(long after) {
        this.after = after;
    }

    @Override
    public String getElementName() {
        return "set";
    }

    @Override
    public String getNamespace() {
        return "http://jabber.org/protocol/rsm";
    }

    @Override
    public String toXML() {
        StringBuilder sb = new StringBuilder(80);
        sb.append("<set xmlns='http://jabber.org/protocol/rsm'><after>");
        sb.append(Long.toString(after));
        sb.append("</after></set>");
        return sb.toString();
    }

}
