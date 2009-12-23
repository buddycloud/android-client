package com.buddycloud.jbuddycloud.packet;

import org.jivesoftware.smackx.pubsub.Subscription;

public class BCSubscription extends Subscription {

    protected String affiliation;

    public BCSubscription(String jid, String nodeId, String subscriptionId,
            State state, boolean configRequired, String affiliation) {
        super(jid, nodeId, subscriptionId, state, configRequired);
        this.affiliation = affiliation;
    }

    public BCSubscription(String jid, String nodeId, String subscriptionId,
            State state, boolean configRequired) {
        super(jid, nodeId, subscriptionId, state, configRequired);
    }

    public BCSubscription(String jid, String nodeId, String subscriptionId,
            State state) {
        super(jid, nodeId, subscriptionId, state);
    }

    public BCSubscription(String subscriptionJid, String nodeId) {
        super(subscriptionJid, nodeId);
    }

    public BCSubscription(String subscriptionJid) {
        super(subscriptionJid);
    }

    public String toXML()
    {
        StringBuilder builder = new StringBuilder("<subscription");
        appendAttribute(builder, "jid", jid);
        
        if (getNode() != null)
            appendAttribute(builder, "node", getNode());
        
        if (id != null)
            appendAttribute(builder, "subid", id);
        
        if (state != null)
            appendAttribute(builder, "subscription", state.toString());

        if (affiliation != null)
            appendAttribute(builder, "affiliation", affiliation);

        builder.append("/>");
        return builder.toString();
    }

    private void appendAttribute(StringBuilder builder, String att, String value)
    {
        builder.append(" ");
        builder.append(att);
        builder.append("='");
        builder.append(value);
        builder.append("'");
    }

}
