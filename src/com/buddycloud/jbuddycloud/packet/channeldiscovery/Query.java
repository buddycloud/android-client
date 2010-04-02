package com.buddycloud.jbuddycloud.packet.channeldiscovery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.xmlpull.v1.XmlPullParser;

public class Query extends IQ implements IQProvider {

    protected String id;
    protected ArrayList<QueryItem> items = new ArrayList<QueryItem>();

    public IQ parseIQ(XmlPullParser parser) throws Exception {
        Query query = new Query();
        while (true) {
            switch (parser.next()) {
                case XmlPullParser.START_TAG:
                    if (parser.getName().equals("item")) {
                        query.items.add((QueryItem)
                                QueryItem.instance.parseExtension(parser));
                        break;
                    }
                    if (parser.getName().equals("items")) {
                        for (int i = 0; i < parser.getAttributeCount(); i++) {
                            if (parser.getAttributeName(i).equals("id")) {
                                id = parser.getAttributeValue(i);
                            }
                        }
                        break;
                    }
                case XmlPullParser.END_TAG:
                    if (parser.getName().equals("query")) {
                        return query;
                    }
                    break;
            }
        }
    }

    @Override
    public String getChildElementXML() {
        StringBuilder builder = new StringBuilder();
        builder.append("<query ");
        builder.append("xmlns='http://buddycloud.com/protocol/channels'>");
        builder.append("<items var='directory'");
        if (id != null) {
           builder.append(" id='");
           builder.append(id);
           builder.append("'");
        }
        if (items.size() == 0) {
            builder.append(" />");
        } else {
            builder.append(">");
            for (QueryItem item: items) {
                builder.append(item.toXML());
            }
            builder.append("</items>");
        }
        builder.append("</query>");
        return builder.toString();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<QueryItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public void setItems(ArrayList<QueryItem> items) {
        this.items = items;
    }

}
