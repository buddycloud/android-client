package com.buddycloud.android.buddydroid;

import org.jivesoftware.smack.BOSHConnection;
import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.XMPPConnection;

import com.buddycloud.jbuddycloud.BuddycloudClient;

public class ConnectionThread extends Thread {

    private final String jid;
    private final String password;
    private final boolean create;
    private final BuddycloudService service;
    private final String username;
    private boolean stop = false;
    private final String host;
    private final Integer port;

    public ConnectionThread(
            String jid,
            String username,
            String password,
            String host,
            Integer port,
            boolean create,
            BuddycloudService service
    ) {
        Connection.DEBUG_ENABLED = true;
        BOSHConnection.DEBUG_ENABLED = true;
        BuddycloudClient.DEBUG_ENABLED = true;
        XMPPConnection.DEBUG_ENABLED = true;

        this.jid = jid;
        this.username = username;
        this.password = password;
        this.host = host;
        this.port = port;
        this.create = create;
        this.service = service;
        start();
    }

    public void run() {
        try {
            BuddycloudClient client = null;
            if (create) {
                int at = jid.indexOf('@');
                final String name;
                if (at == -1) {
                    name = jid;
                } else {
                    name = jid.substring(0, at);
                }
                client = BuddycloudClient
                    .registerBuddycloudClient(name, password);
            } else {
                if (jid == null) {
                    client = BuddycloudClient.createAnonymousBuddycloudClient();
                } else {
                    client = BuddycloudClient
                        .createBuddycloudClient(
                                jid, password, host, port, username
                        );
                }
            }
            if (!this.stop && client != null && client.isConnected()
                    && client.isAuthenticated()) {
                service.setClient(client, password);
            }
        } catch (Exception e) {
            if (!this.stop) {
                service.connectionFailed(e);
            }
        }
    }

    public void setStop(boolean stop) {
        this.stop = stop;
    }

}
