package com.buddycloud.jbuddycloud.packet;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.util.StringUtils;

public class TextLocation extends IQ {

    private final String text;

    public TextLocation(String text) {
        this.text = text;
    }

    @Override
    public Type getType() {
        return Type.SET;
    }

    @Override
    public String getChildElementXML() {
        StringBuilder sb = new StringBuilder();
        sb.append("<pubsub xmlns='http://jabber.org/protocol/pubsub'><publish node='http://jabber.org/protocol/geoloc'><item><geoloc xml:lang='en' xmlns='http://jabber.org/protocol/geoloc'><text>")
          .append(StringUtils.escapeForXML(text))
          .append("</text><accuracy>1000000.0</accuracy></geoloc></item></publish></pubsub>");
        return sb.toString();
    }

}
