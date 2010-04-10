package com.buddycloud.jbuddycloud.packet;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.xmlpull.v1.XmlPullParser;

public class Ping implements PacketExtension, PacketExtensionProvider {

    public String getElementName() {
        return "ping";
    }

    public String getNamespace() {
        return "urn:xmpp:ping";
    }

    public String toXML() {
        return "<ping xmlns='urn:xmpp:ping' />";
    }

    public PacketExtension parseExtension(XmlPullParser parser)
            throws Exception {
        return new Ping();
    }

}
