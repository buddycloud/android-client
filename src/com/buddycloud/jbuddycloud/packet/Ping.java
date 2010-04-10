package com.buddycloud.jbuddycloud.packet;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.xmlpull.v1.XmlPullParser;

public class Ping extends IQ implements IQProvider {

    public String getElementName() {
        return "ping";
    }

    public String getNamespace() {
        return "urn:xmpp:ping";
    }

    @Override
    public String getChildElementXML() {
        return "<ping xmlns='urn:xmpp:ping' />";
    }

    public IQ parseIQ(XmlPullParser parser) throws Exception {
        parser.nextTag();
        return new Ping();
    }

}
