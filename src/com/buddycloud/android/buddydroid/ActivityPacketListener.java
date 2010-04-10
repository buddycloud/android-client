package com.buddycloud.android.buddydroid;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.Packet;

public class ActivityPacketListener implements PacketListener {

    private long lastActivity = System.currentTimeMillis();

    public void processPacket(Packet packet) {
        bump();
    }

    public long getLastActivity() {
        return lastActivity;
    }

    public void bump() {
        lastActivity = System.currentTimeMillis();
    }
}
