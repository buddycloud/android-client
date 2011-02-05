package com.buddycloud.component;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;

import android.os.RemoteException;

import com.googlecode.asmack.client.AsmackClient;
import com.googlecode.asmack.client.TransportServiceBindListener;
import com.googlecode.asmack.connection.IXmppTransportService;

/**
 * Ensure that the basic buddycloud components are part of the roster.
 */
public class ComponentAdd
    implements PacketListener, Runnable, TransportServiceBindListener
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
        client.addTransportServiceBindListener(this);
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
                try {
                    client.sendFromAllAccounts(subscribed);
                } catch (RemoteException e) {
                    // not critical
                }
            }
        }
    }

    /**
     * Send initial subscription presence to all components.
     */
    @Override
    public void run() {
        for (String jid: COMPONENTS) {
            Presence subscribe = new Presence(Presence.Type.subscribe);
            subscribe.setTo(jid);
            try {
                client.sendFromAllAccounts(subscribe);
            } catch (RemoteException e) {
                // not critical
            }
        }
    }

    /**
     * Start a new thread when the xmpp service comes online.
     * @param service Ignored.
     */
    @Override
    public void onTrasportServiceConnect(IXmppTransportService service) {
        new Thread(this).start();
    }

    /**
     * Ignored. The XMPP service got offlien.
     * @param service Ignored.
     */
    @Override
    public void onTrasportServiceDisconnect(IXmppTransportService service) {
    }

}
