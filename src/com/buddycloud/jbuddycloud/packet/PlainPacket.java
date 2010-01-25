/**
 * 
 */
package com.buddycloud.jbuddycloud.packet;

import org.jivesoftware.smack.packet.Packet;

/**
 * This packet implementation can be used to send any content through your
 * xmpp conenction. This may cause the server to close your connection, send
 * valid xml only.
 */
public final class PlainPacket extends Packet {

    private String text;

    public PlainPacket(String text) {
        this.text = text;
    }

    public String toXML() {
        return text;
    }

}