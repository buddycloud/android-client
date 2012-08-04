package com.buddycloud.jbuddycloud.packet;

import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.util.StringUtils;

public class RSMSet implements PacketExtension {

    public String after = null;
    public String before = null;
    public long max = -1;

    public RSMSet() {
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
        sb.append("<set xmlns='http://jabber.org/protocol/rsm'>");
        if (after != null) {
            sb.append("<after>").append(StringUtils.escapeForXML(after)).append("</after>");
        }
        if (before != null) {
            sb.append("<before>").append(StringUtils.escapeForXML(before)).append("</before>");
        }
        if (max > 0) {
            sb.append("<max>").append(max).append("</max>");
        }
        sb.append("</set>");
        return sb.toString();
    }

}
