package com.buddycloud.component;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;

import android.os.RemoteException;

import com.googlecode.asmack.client.AsmackClient;

/**
 * Ensure that the basic buddycloud components are part of the roster.
 */
public class ComponentAdd
    implements PacketListener, Runnable
{

    /**
     * Basic buddycloud components.
     */
    public final static String[] COMPONENTS = new String[] {
        "maitred.buddycloud.com", "broadcaster.buddycloud.com",
        "butler.buddycloud.com"
    };

    /**
     * The internal asmack client.
     */
    private final AsmackClient client;

    /**
     * Create a new component adding client for a given asmack client.
     * @param client The asmack client.
     */
    public ComponentAdd(AsmackClient client) {
        super();
        this.client = client;
        client.registerListener(this);
        new Thread(this).start();
    }

    /**
     * Check a package for a subscription from one of the components and
     * acknowledge the subscription.
     */
    @Override
    public void processPacket(Packet packet) {
        if (!(packet instanceof Presence)) {
            return;
        }
        Presence presence = (Presence) packet;
        if (presence.getType() != Presence.Type.subscribe) {
            return;
        }
        for (String jid: COMPONENTS) {
            if (presence.getFrom().equals(jid)) {
                Presence subscribed = new Presence(Presence.Type.subscribed);
                subscribed.setTo(jid);
                client.sendFromAllAccounts(subscribed);
            }
        }
    }

    /**
     * Send initial subscription presence to all components.
     */
    @Override
    public void run() {
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e1) {
        }
        for (String jid: COMPONENTS) {
            Presence subscribe = new Presence(Presence.Type.subscribe);
            subscribe.setTo(jid);
            client.sendFromAllAccounts(subscribe);
        }
    }

}
