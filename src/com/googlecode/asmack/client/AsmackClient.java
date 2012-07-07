/*
 * Licensed under Apache License, Version 2.0 or LGPL 2.1, at your option.
 * --
 *
 * Copyright 2010 Rene Treffer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * --
 *
 * Copyright (C) 2010 Rene Treffer
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301 USA
 */

package com.googlecode.asmack.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;

import android.content.Context;
import android.os.RemoteException;
import android.util.Log;

import com.googlecode.asmack.Attribute;
import com.googlecode.asmack.Stanza;
import com.googlecode.asmack.XmppIdentity;
import com.googlecode.asmack.connection.XmppTransportService;
import com.googlecode.asmack.parser.SmackParser;

/**
 * Basic transport service client.
 */
public class AsmackClient implements PacketListener {

    /**
     * Cache of imported resources.
     */
    private static HashSet<Integer> parsed = new HashSet<Integer>();

    /**
     * The logging tag, AsmackClient.
     */
    private static final String TAG = AsmackClient.class.getSimpleName();

    /**
     * Internal hashmap for reply id to callback mapping.
     */
    private HashMap<String, Callback> replyMap =
                                    new HashMap<String, Callback>();

    /**
     * SortedSet for time-to-live tracking and callback pruning.
     */
    private TreeSet<Callback> replyTtl = new TreeSet<Callback>();

    /**
     * Lock for reply callback changes, to avoid concurrent changes.
     */
    private ReentrantLock replyLock = new ReentrantLock();

    /**
     * Current atomic id counter.
     */
    private final AtomicInteger idStatus = new AtomicInteger();

    /**
     * ID prefix for id generation.
     */
    private final String idPrefix;

    /**
     * List of packet listeners.
     */
    private CopyOnWriteArrayList<PacketListener> listeners =
                                new CopyOnWriteArrayList<PacketListener>();

    private AsmackBroadcastReceiver stanzaReceiver;

    private XmppTransportService service;

    /**
     * <p>Create a new asmack service client.</p>
     * <p>The arguments are used to generate the packet ids.</p>
     * @param idPrefix The id prefix for stanza sending.
     */
    public AsmackClient(String idPrefix) {
        this.idPrefix = idPrefix;
        idStatus.set(new Random().nextInt());
        Log.d(TAG, "instanciate");
    }

    /**
     * Open and start the transport service client.
     * @param context The application context used for service binding.
     */
    public void open(Context context, int id, XmppTransportService service) {
        if (!parsed.contains(id)) {
            parsed.add(id);
            SmackParser.getInstance().registerProviders(context, id);
        }

        stanzaReceiver = new AsmackBroadcastReceiver(this);
        service.addListener(stanzaReceiver);
        this.service = service;
    }

    /**
     * Add a packet listener to the list of permanent packet receivers.
     * @param listener The smack packet listener.
     */
    public synchronized void registerListener(PacketListener listener) {
        listeners.add(listener);
    }

    /**
     * Register a conditional packet listener.
     * @param filter The packet filter.
     * @param listener The actual listener.
     */
    public void registerListener(
        PacketFilter filter,
        PacketListener listener
    ) {
        if (filter != null) {
            registerListener(new PacketFilterListener(filter, listener));
        } else {
            registerListener(listener);
        }
    }

    /**
     * Remove a listener from the listener chain. A NullPointerException will
     * be thrown if the listener is null.
     * @param filter The packet filter. May be null.
     * @param listener The actual listener.
     * @return True on success.
     */
    public synchronized boolean removeListener(
        PacketFilter filter,
        PacketListener listener
    ) {
        if (listener == null) {
            throw new NullPointerException("can't remove listener 'null'");
        }
        if (filter == null && listeners.remove(listener)) {
            return true;
        }
        if (filter == null) {
            return false;
        }
        for (PacketListener l: listeners) {
            if (!(l instanceof PacketFilterListener)) {
                continue;
            }
            PacketFilterListener filterListener = (PacketFilterListener) l;
            if (filterListener.getFilter() == filter &&
                filterListener.getListener() == listener) {
                listeners.remove(filterListener);
                return true;
            }
        }
        return false;
    }

    /**
     * Removes a listener from the global listener list.
     * @param listener The listener to be removed.
     * @return True on success.
     */
    public boolean removeListener(PacketListener listener) {
        return removeListener(null, listener);
    }

    /**
     * Send a single stanza via the remote service.
     * @param stanza The stanza that should be send.
     * @return The id String.
     * @throws RemoteException In case of a service breakdown.
     */
    public String send(Stanza stanza) {
        return sendWithCallback(stanza, null, 0);
    }

    /**
     * Sends a packet over the wire, generating and setting a new id.
     * @param packet The smack packet to send.
     * @return The id String.
     * @throws RemoteException In case of a service breakdown.
     */
    public String send(Packet packet, String via) {
        return sendWithCallback(packet, via, null, 0l);
    }

    /**
     * Directly send a single stanza via a given connection. This method is
     * unsafe as it doesn't guarnatee the message to be received by the service.
     * @param context The context to use for sending.
     * @param stanza The XMPP stanza.
     * @param via The account jid.
     */
    public void sendUnsafe(
        Context context,
        Stanza stanza,
        String via
    ) {
        service.send(stanza);
    }

    /**
     * Send a smack packet via a given connection. This method is unsafe as it
     * doesn't even guarantee the packet to be delivered to the service.
     * @param context The context to use for intent broadcasts.
     * @param packet The packet to send.
     * @param via The account jid.
     */
    public void sendUnsafe(
        Context context,
        Packet packet,
        String via
    ) {
        sendUnsafe(context, toStanza(packet, null), via);
    }

    /**
     * Convert a packet to it's stanza form, adding a new ID if referenced.
     * @param packet The smack packet to send.
     * @param id A new id, or null.
     * @return The stanza instance.
     */
    public final static Stanza toStanza(Packet packet, String id) {
        String name = "message";
        if (packet instanceof IQ) {
            name = "iq";
        }
        if (packet instanceof Presence) {
            name = "presence";
        }
        if (id == null || id.length() == 0) {
            id = packet.getPacketID();
        }
        String via = null;
        if (packet.getFrom() != null && packet.getFrom().length() > 0) {
            via = packet.getFrom();
        }
        ArrayList<Attribute> attributes = new ArrayList<Attribute>();
        if (id != null) {
            attributes.add(new Attribute("id", "", id));
        }
        return new Stanza(
            name,
            packet.getXmlns(),
            via,
            packet.toXML(),
            attributes
        );
    }

    /**
     * Sends a packet over the wire, generating and setting a new id.
     * @param packet The smack packet to send.
     * @param ttl Time to live.
     * @return The id String.
     * @throws RemoteException In case of a service breakdown.
     */
    public String sendWithCallback(
        Packet packet,
        String via,
        PacketListener callback,
        long ttl
    ) {
        String id = generateId();
        Stanza stanza = toStanza(packet, id);
        if (stanza.getVia() == null && via != null) {
            stanza.setVia(via);
        }
        sendWithCallback(stanza, callback, ttl);
        return id;
    }

    /**
     * Send a single packet from all currently connected accounts.
     * @param packet The smack packet to send.
     * @return The id String.
     * @throws RemoteException In case of a remote service breakdown.
     */
    public String sendFromAllAccounts(Packet packet) {
        String id = generateId();
        Stanza stanza = toStanza(packet, id);
        service.sendFromAllAccounts(stanza);
        return id;
    }

    /**
     * Send a single stanza from all currently connected accounts.
     * @param stanza The stanza to send.
     * @throws RemoteException In case of a remote service breakdown.
     */
    public void sendFromAllAccounts(Stanza stanza) {
        service.sendFromAllAccounts(stanza);
    }

    /**
     * Send a single packet from all currently connected accounts, using
     * the resource as the source.
     * @param packet The smack packet to send.
     * @return The id String.
     * @throws RemoteException In case of a remote service breakdown.
     */
    public String sendFromAllResources(Packet packet) {
        String id = generateId();
        Stanza stanza = toStanza(packet, id);
        service.sendFromAllResources(stanza);
        return id;
    }

    /**
     * Send a single stanza from all currently connected accounts, using
     * the resource jid as from value.
     * @param stanza The stanza to send.
     * @throws RemoteException In case of a remote service breakdown.
     */
    public void sendFromAllResources(Stanza stanza) {
        service.sendFromAllResources(stanza);
    }

    /**
     * Send a single stanza, registering a callback for the auto-generated id.
     * The time to live is the guaranteed minimum time that the callback will
     * survive.
     * @param stanza The stanza to send.
     * @param callback The callback on rply.
     * @param ttl The time to live of the callback, in milliseconds.
     * @return The stanza id.
     * @throws RemoteException In case of a remote service breakdown.
     */
    public String sendWithCallback(
        Stanza stanza,
        PacketListener callback,
        long ttl
    ) {
        String id = getStanzaId(stanza);
        if (callback != null && ttl > 0) {
            try {
                replyLock.lock();
                Callback cb = new Callback(
                        System.currentTimeMillis() + ttl,
                        id,
                        callback
                );
                replyMap.put(id, cb);
                this.replyTtl.add(cb);
            } finally {
                replyLock.unlock();
            }
        }
        if (service.send(stanza)) {
            return id;
        }
        return null;
    }

    /**
     * Close the client.
     * @param context The context that should be used for unbinding.
     */
    public void close(Context context) {
    }

    /**
     * Helper to geot or generate a stanza id.
     * @param stanza The stanza.
     * @return The stanza id.
     */
    private String getStanzaId(Stanza stanza) {
        Attribute idAttribute = stanza.getAttribute("id");
        if (idAttribute == null) {
            idAttribute = new Attribute("id", generateId(), null);
            stanza.addAttribute(idAttribute);
        }
        return idAttribute.getValue();
    }

    /**
     * Generate a new stanza id.
     * @return A new stanza id.
     */
    private String generateId() {
        return idPrefix + "-" +
            Integer.toHexString(idStatus.getAndIncrement());
    }

    /**
     * Check if the service is still alive.
     * @param context The context used for the check.
     * @return True on success.
     */
    public boolean checkService(Context context) {
        return true;
    }

    /**
     * Purge stalled callbacks based on time to live constrains.
     */
    public void purgeCallback() {
        if (replyTtl.size() == 0) {
            return;
        }
        long time = System.currentTimeMillis();
        Callback first = replyTtl.first();
        while (first.getTTL() < time) {
            replyTtl.remove(first);
            replyMap.remove(first.getId());
            if (replyTtl.size() == 0) {
                return;
            }
            first = replyTtl.first();
        }
    }

    /**
     * Process a packet, call all listeners and remove packet specific
     * callbacks.
     * @param packet The smack packet.
     */
    @Override
    public void processPacket(Packet packet) {
        String id = packet.getPacketID();
        if (id != null) {
            Callback callback = null;
            try {
                replyLock.lock();
                callback = replyMap.remove(id);
                purgeCallback();
                if (callback != null) {
                    replyTtl.remove(callback);
                }
            } finally {
                replyLock.unlock();
            }
            if (callback != null) {
                try {
                    callback.getCallback().processPacket(packet);
                } catch (Exception e) {
                    Log.e(TAG,
                        "Callback throws an exception. "
                         + callback.getCallback(),
                         e);
                }
            }
        }
        for (PacketListener listener: listeners) {
            try {
                listener.processPacket(packet);
            } catch (Exception e) {
                Log.e(TAG,
                    "PacketListener throws an exception. "
                     + listener,
                     e);
            }
        }
    }

    /**
     * Scan all connections for the current connection of the given jid and
     * return the full resource jid for the user.
     * @return The full user jid (including resource).
     */
    public String getFullJidByBare(String bare) throws RemoteException {
        return service.getFullJidByBare(bare);
    }

    /**
     * Retrieve all current account jids.
     * @param connected True if you only jids of connected acocunts should be
     *                  returned.
     * @return List of account jids.
     */
    public String[] getAllAccountJids(boolean connected) throws RemoteException {
        return service.getAllAccountJids(connected);
    }

    /**
     * Retrieve all resource jids (where available).
     * @param connected True if you only jids of connected acocunts should be
     *                  returned.
     * @return List of account jids.
     */
    public String[] getAllResourceJids(boolean connected)
            throws RemoteException {
        return service.getAllResourceJids(connected);
    }

    /**
     * Enable a new feature for a given jid only. The new feature will be
     * announced during the next tick.
     * @param jid The jid of the account that should announce the feature.
     * @param feature The feature to enable.
     */
    public void enableFeatureForJid(String jid, String feature)
            throws RemoteException {
        service.enableFeatureForJid(jid, feature);
    }

    /**
     * Enable a feature service wide. This means that all connection will take
     * advantage of the new feature. This may have unexpected side effects on
     * other applications, so use with care.
     * @param feature The feature to enable.
     */
    public void enableFeature(String feature) throws RemoteException {
        service.enableFeature(feature);
    }

    /**
     * Add a new identity to the xmpp account specified by the jid.
     * @param jid The jid to enable.
     * @param identity The identity to add.
     */
    public void addIdentityForJid(String jid, XmppIdentity identity)
            throws RemoteException {
        service.addIdentityForJid(jid, identity);
    }

    /**
     * Add a new identity to all xmpp accounts. This affects all connections
     * and should thus be handled with care.
     * @param identity The xmpp identity to add.
     */
    public void addIdentity(XmppIdentity identity) throws RemoteException {
        service.addIdentity(identity);
    }

}
