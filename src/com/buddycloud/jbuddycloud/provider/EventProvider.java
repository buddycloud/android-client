package com.buddycloud.jbuddycloud.provider;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.xmlpull.v1.XmlPullParser;

import com.buddycloud.jbuddycloud.packet.EventIQ;

public class EventProvider implements IQProvider {

    public IQ parseIQ(XmlPullParser parser) throws Exception
    {
        EventIQ event = new EventIQ();

        do
        {
            int tag = parser.next();

            if (tag == XmlPullParser.START_TAG) { 
                event.addExtension(PacketParserUtils.parsePacketExtension(
                        parser.getName(),
                        parser.getNamespace(),
                        parser
                ));
            }
        } while (!"event".equals(parser.getName()));

        return event;
    }

}
