package com.buddycloud.jbuddycloud.provider;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smackx.pubsub.Subscription;
import org.jivesoftware.smackx.pubsub.provider.SubscriptionProvider;
import org.xmlpull.v1.XmlPullParser;

import com.buddycloud.jbuddycloud.packet.BCSubscription;

public class BCSubscriptionProvider extends SubscriptionProvider {

    public PacketExtension parseExtension(XmlPullParser parser)
            throws Exception {
        String jid = parser.getAttributeValue(null, "jid");
        String nodeId = parser.getAttributeValue(null, "node");
        String subId = parser.getAttributeValue(null, "subid");
        String state = parser.getAttributeValue(null, "subscription");
        String affiliation = parser.getAttributeValue(null, "affiliation");
        boolean isRequired = false;

        int tag = parser.next();

        if ((tag == XmlPullParser.START_TAG)
                && parser.getName().equals("subscribe-options")) {
            tag = parser.next();

            if ((tag == XmlPullParser.START_TAG)
                    && parser.getName().equals("required"))
                isRequired = true;

            while (parser.next() != XmlPullParser.END_TAG
                    && parser.getName() != "subscribe-options")
                ;
        }
        while (parser.getEventType() != XmlPullParser.END_TAG)
            parser.next();
        return new BCSubscription(jid, nodeId, subId, (state == null ? null
                : Subscription.State.valueOf(state)), isRequired, affiliation);
    }

}
