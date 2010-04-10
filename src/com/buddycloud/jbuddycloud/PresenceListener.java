package com.buddycloud.jbuddycloud;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;

import android.util.Log;

public class PresenceListener implements PacketListener {

    private static final String TAG = PresenceListener.class.getSimpleName();;

    private String account;
    private BuddycloudClient client;

    public PresenceListener(BuddycloudClient client) {
        this.client = client;
        account = client.getUser();
        if (account.indexOf('/') != -1) {
            account = account.substring(0, account.indexOf('/'));
        }
    }

    public void processPacket(Packet packet) {
        if (!(packet instanceof Presence)) {
            return;
        }
        String from = packet.getFrom();
        if (from.indexOf('/') != -1) {
            from = from.substring(0, from.indexOf('/'));
        }
        if (!from.equals(account)) {
            return;
        }
        Presence presence = (Presence)packet;
        if (presence.getMode() == null) {
            return;
        }
        if (presence.getMode() == Presence.Mode.available) {
            return;
        }
        Log.w(TAG, "non-available presence on myself " + presence.getMode());
        client.sendInitialPresence();
    }

}
