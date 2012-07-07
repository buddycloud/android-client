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

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Packet;

import android.content.Context;
import android.os.RemoteException;

import com.buddycloud.content.ChannelSync;
import com.googlecode.asmack.XmppIdentity;
import com.googlecode.asmack.connection.AccountConnection;
import com.googlecode.asmack.connection.XmppTransportService;

/**
 * AsmackClientService provides basic client initialization code for userspace
 * services. Initializes
 */
public abstract class AsmackClientService
    extends XmppTransportService
    implements TransportServiceBindListener {

    /**
     * The internal asmack client that wrapps bind/unbind, listeners and
     * stanza receivers.
     */
    protected AsmackClient client;

    /**
     * The resource id of a smack providers xml.
     */
    private final int resourceId;

    /**
     * Create a new asmack client server for a given configuration xml. The
     * configuration XML is global for all services and should thus not be
     * assumed to be different between clients.
     * @param resourceId The smack xml resource id.
     */
    public AsmackClientService(int resourceId) {
        this.resourceId = resourceId;
    }

    /**
     * Called when the base context is attaced, used 
     */
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        createClient();
        preClientStart();
        client.open(getApplicationContext(), resourceId, this);
    }

    /**
     * Called to define the client prefix for events.
     * @return The packet id prefix.
     */
    protected String getIDPrefix() {
        return "as";
    }

    /**
     * Called after the client is created and before the client is started.
     * This can be used to register listeners.
     */
    protected void preClientStart() {
    }

    /**
     * Called after the service has been connected. This is the earliest point
     * to send stanzas.
     */
    public void onTrasportServiceConnect() {
    }

    /**
     * Called whenever the client disconnects.
     */
    public void onTrasportServiceDisconnect() {
        createClient();
        preClientStart();
        client.open(getApplicationContext(), resourceId, this);
    }

    /**
     * Helper to create a new client, killing the old client.
     */
    private void createClient() {
        if (client != null) {
            client.close(getApplicationContext());
        }
        client = new AsmackClient(getIDPrefix());
    }

    public void registerListener(PacketListener listener) {
        client.registerListener(listener);
    }

    public void registerListener(PacketFilter filter, PacketListener listener) {
        client.registerListener(filter, listener);
    }

    public boolean removeListener(PacketFilter filter, PacketListener listener) {
        return client.removeListener(filter, listener);
    }

    public boolean removeListener(PacketListener listener) {
        return client.removeListener(listener);
    }

    public String send(Packet packet, String via) {
        return client.send(packet, via);
    }

    public void sendUnsafe(Context context, Packet packet, String via) {
        client.sendUnsafe(context, packet, via);
    }

    public String sendWithCallback(Packet packet, String via,
            PacketListener callback, long ttl) {
        return client.sendWithCallback(packet, via, callback, ttl);
    }

    public String sendFromAllAccounts(Packet packet) {
        return client.sendFromAllAccounts(packet);
    }

    public String sendFromAllResources(Packet packet) {
        return client.sendFromAllResources(packet);
    }

    public void purgeCallback() {
        client.purgeCallback();
    }

    public void processPacket(Packet packet) {
        client.processPacket(packet);
    }

}
