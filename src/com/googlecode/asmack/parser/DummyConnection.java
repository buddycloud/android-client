package com.googlecode.asmack.parser;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;

/**
 * A dummy smack connection object that does not implement any method.
 */
public class DummyConnection extends Connection {

    /**
     * Create a new dummy connection.
     */
    protected DummyConnection() {
        super(null);
    }

    /**
     * Connect. Intentional not implemented.
     */
    @Override
    public void connect() throws XMPPException {
    }

    /**
     * Disconnect. Intentional not implemented.
     */
    @Override
    public void disconnect(Presence arg0) {
    }

    /**
     * Retrieve the connection id. Intentional not implemented.
     * @return null.
     */
    @Override
    public String getConnectionID() {
        return null;
    }

    /**
     * Retrieve the current roster. Intentional not implemented.
     * @return null.
     */
    @Override
    public Roster getRoster() {
        return null;
    }

    /**
     * Retrive the user jid. Intentional not implemented.
     * @return null.
     */
    public String getUser() {
        return null;
    }

    /**
     * Check if the connection is anonymous. Intentional not implemented.
     * @return false.
     */
    @Override
    public boolean isAnonymous() {
        return false;
    }

    /**
     * Check if the user is authenticated. Intentional not implemented.
     * @return false;
     */
    public boolean isAuthenticated() {
        return false;
    }

    /**
     * Check if the connection is established. Intentional not implemented.
     * @return false.
     */
    @Override
    public boolean isConnected() {
        return false;
    }

    /**
     * Check if the connection is secure. Intentional not implemented.
     * @return false.
     */
    @Override
    public boolean isSecureConnection() {
        return false;
    }

    /**
     * Check if the connection is compressed. Intentional not implemented.
     * @return false.
     */
    @Override
    public boolean isUsingCompression() {
        return false;
    }

    /**
     * Perform a login. Intentional not implemented.
     * @param arg0 Ignored.
     * @param arg1 Ignored.
     * @param arg2 Ignored.
     */
    @Override
    public void login(String arg0, String arg1, String arg2)
            throws XMPPException {
    }

    /**
     * Login anonymously. Intentional not implemented.
     */
    @Override
    public void loginAnonymously() throws XMPPException {
    }

    /**
     * Send a packet. Intentional not implemented.
     * @param arg0 Ignored.
     */
    @Override
    public void sendPacket(Packet arg0) {
    }

}
