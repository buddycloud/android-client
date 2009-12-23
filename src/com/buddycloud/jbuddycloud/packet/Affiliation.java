package com.buddycloud.jbuddycloud.packet;

import org.jivesoftware.smack.packet.PacketExtension;

public class Affiliation implements
        PacketExtension {

    private String jid;
    private String affiliation;

    public Affiliation(String jid, String affiliation) {
        super();
        this.jid = jid;
        this.affiliation = affiliation;
    }

    public String getJid() {
        return jid;
    }

    public void setJid(String jid) {
        this.jid = jid;
    }

    public String getAffiliation() {
        return affiliation;
    }

    public void setAffiliation(String affiliation) {
        this.affiliation = affiliation;
    }

    @Override
    public String getElementName() {
        return "affiliation";
    }

    @Override
    public String getNamespace() {
        return "http://jabber.org/protocol/pubsub#owner";
    }

    @Override
    public String toXML() {
        StringBuilder sb = new StringBuilder();
        sb.append("<affiliation");
        if (jid != null) {
            sb.append(" jid='")
              .append(jid)
              .append("'");
        }
        if (affiliation != null) {
            sb.append(" affiliation='")
              .append(affiliation)
              .append("'");
        }
        sb.append("/>");
        return sb.toString();
    }

}
