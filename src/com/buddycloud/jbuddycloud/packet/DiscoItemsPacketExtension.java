package com.buddycloud.jbuddycloud.packet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.xmlpull.v1.XmlPullParser;

public class DiscoItemsPacketExtension
implements PacketExtension, PacketExtensionProvider {

    public static class Item {
        public String jid;
        public String subscription;
        public String affiliation;
        public String node;
        public String toString() {
            return "Item(" + jid + "," + affiliation + "," + subscription + "," + node;
        }
    }

    protected List<Item> items;

    @Override
    public PacketExtension parseExtension(XmlPullParser parser) throws Exception {
        DiscoItemsPacketExtension result = new DiscoItemsPacketExtension();
        int balance = 1;
        ArrayList<Item> items = new ArrayList<Item>();
        while (balance > 0) {
            parser.next();
            switch (parser.getEventType()) {
            case XmlPullParser.END_TAG:
                balance--;
                break;
            case XmlPullParser.START_TAG:
                balance++;
                if ("item".equals(parser.getName())) {
                    Item item = new Item();
                    item.jid = parser.getAttributeValue(null, "jid");
                    item.affiliation = parser.getAttributeValue(null, "affiliation");
                    item.subscription = parser.getAttributeValue(null, "subscription");
                    item.node = parser.getAttributeValue(null, "node");
                    items.add(item);
                    System.err.println("??? " + balance + " " + item);
                }
            }
        }
        result.setItems(items);
        return result;
    }

    public List<Item> getItems() {
        return (items == null) ? Arrays.asList(new Item[]{}) : items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    @Override
    public String getElementName() {
        return "query";
    }

    @Override
    public String getNamespace() {
        return "http://jabber.org/protocol/disco#items";
    }

    @Override
    public String toXML() {
        return "";
    }

}
