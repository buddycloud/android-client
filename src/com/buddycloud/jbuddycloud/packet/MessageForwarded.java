package com.buddycloud.jbuddycloud.packet;

import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.xmlpull.v1.XmlPullParser;

import android.util.Log;

public class MessageForwarded implements PacketExtension, PacketExtensionProvider {

    public List<Message> messages;

    @Override
    public PacketExtension parseExtension(XmlPullParser parser) throws Exception {
        MessageForwarded result = new MessageForwarded();
        ArrayList<Message> messages = new ArrayList<Message>();
        parser.nextTag();
        while (parser.getEventType() != XmlPullParser.END_TAG ||
               !"forwarded".equals(parser.getName())) {
            if (parser.getEventType() == XmlPullParser.START_TAG) {
                if ("message".equals(parser.getName())) {
                    messages.add((Message)PacketParserUtils.parseMessage(parser));
                } else {
                    PacketExtension ex = PacketParserUtils.parsePacketExtension(
                        parser.getName(),
                        parser.getNamespace(),
                        parser
                    );
                    Log.d("XXXX", "Unhandled extension: " + ex.toXML());
                }
            }
            parser.nextTag();
        }
        result.messages = messages;
        return result;
    }

    @Override
    public String getElementName() {
        return "forwarded";
    }

    @Override
    public String getNamespace() {
        return "urn:xmpp:forward:tmp";
    }

    @Override
    public String toXML() {
        return null;
    }


}
