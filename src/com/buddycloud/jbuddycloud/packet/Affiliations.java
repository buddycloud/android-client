package com.buddycloud.jbuddycloud.packet;

import java.util.ArrayList;

import org.jivesoftware.smack.packet.IQ;

public class Affiliations extends IQ {

    private ArrayList<Affiliation> affs = new ArrayList<Affiliation>();
    private String node;

    public Affiliations(String node) {
        this.node = node;
        setType(IQ.Type.SET);
    }

    public void add(Affiliation aff) {
        affs.add(aff);
    }

    @Override
    public String getChildElementXML() {
        StringBuilder sb = new StringBuilder();
        sb.append("<pubsub xmlns='http://jabber.org/protocol/pubsub#owner'><affiliations");
        if (node != null) {
            sb.append(" node='");
            sb.append(node);
            sb.append("'>");
        } else {
            sb.append(">");
        }
        for (Affiliation aff : affs) {
            sb.append(aff.toXML());
        }
        sb.append("</affiliations></pubsub>");
        return sb.toString();
    }

}
