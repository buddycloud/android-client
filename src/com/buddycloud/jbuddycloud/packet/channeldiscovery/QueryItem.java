package com.buddycloud.jbuddycloud.packet.channeldiscovery;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.xmlpull.v1.XmlPullParser;

public class QueryItem implements PacketExtension,PacketExtensionProvider {

    final static QueryItem instance = new QueryItem();

    protected String id;
    protected String title;
    protected String description;

    public String getElementName() {
        return "item";
    }

    public String getNamespace() {
        return "http://buddycloud.com/protocol/channels";
    }

    public String toXML() {
        return null;
    }

    public PacketExtension parseExtension(XmlPullParser parser)
            throws Exception {
        QueryItem item = new QueryItem();
        while (true) {
            switch (parser.next()) {
                case XmlPullParser.START_TAG:
                    if (parser.getName().equals("id")) {
                        item.id = parser.nextText();
                        break;
                    }
                    if (parser.getName().equals("title")) {
                        item.title = parser.nextText();
                        break;
                    }
                    if (parser.getName().equals("description")) {
                        item.description = parser.nextText();
                        break;
                    }
                    break;
                case XmlPullParser.END_TAG:
                    if (parser.getName().equals("item")) {
                        return item;
                    }
                    break;
            }
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}
