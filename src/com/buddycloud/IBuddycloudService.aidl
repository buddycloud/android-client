package com.buddycloud;

import com.googlecode.asmack.Stanza;
import com.googlecode.asmack.XmppIdentity;

interface IBuddycloudService  {

    boolean follow(String channel);
    void updateChannel(String channel);

    /**
     * Try to login with a given jid/password pair. Returns true on success.
     * @param jid The user jid.
     * @param password The user password.
     */
    boolean tryLogin(String jid, String password);

    /**
     * Send a stanza via this service, return true on successfull delivery to
     * the network buffer (plus flush). Please note that some phones ignore
     * flush request, thus "true" doesn't mean "on the wire".
     * @param stanza The stanza to send.
     */
    boolean send(in Stanza stanza);

    /**
     * Send a stanza via this service, through all resource jids.
     * @param stanza The stanza to send.
     */
    void sendFromAllResources(in Stanza stanza);

    /**
     * Send a stanza via this service, through all account jids.
     * @param stanza The stanza to send.
     */
    void sendFromAllAccounts(in Stanza stanza);

    /**
     * Scan all connections for the current connection of the given jid and
     * return the full resource jid for the user.
     * @return The full user jid (including resource).
     */
    String getFullJidByBare(String bare);

    /**
     * Enable a new feature for a given jid only. The new feature will be
     * announced during the next tick.
     * @param jid The jid of the account that should announce the feature.
     * @param feature The feature to enable.
     */
    void enableFeatureForJid(String jid, String feature);

    /**
     * Enable a feature service wide. This means that all connection will take
     * advantage of the new feature. This may have unexpected side effects on
     * other applications, so use with care.
     * @param feature The feature to enable.
     */
    void enableFeature(String feature);

    /**
     * Add a new identity to the xmpp account specified by the jid.
     * @param jid The jid to enable.
     * @param identity The identity to add.
     */
    void addIdentityForJid(String jid, in XmppIdentity identity);

    /**
     * Add a new identity to all xmpp accounts. This affects all connections
     * and should thus be handled with care.
     * @param identity The xmpp identity to add.
     */
    void addIdentity(in XmppIdentity identity);

    /**
     * Retrieve all current account jids.
     * @param connected True if you only jids of connected acocunts should be
     *                  returned.
     * @return List of account jids.
     */
    String[] getAllAccountJids(boolean connected);

    /**
     * Retrieve all resource jids (where available).
     * @param connected True if you only jids of connected acocunts should be
     *                  returned.
     * @return List of account jids.
     */
    String[] getAllResourceJids(boolean connected);

}
