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
import org.jivesoftware.smack.packet.Packet;

import android.util.Log;

import com.googlecode.asmack.Stanza;
import com.googlecode.asmack.connection.StanzaListener;
import com.googlecode.asmack.parser.SmackParser;

/**
 * Asmack broadcast receiver will parse stanza intents and handes out parsed
 * smack packages.
 */
public class AsmackBroadcastReceiver implements StanzaListener {

    /**
     * The tag name for debugging ("AsmackBroadcastReceiver").
     */
    private static final String TAG =
                            AsmackBroadcastReceiver.class.getSimpleName();

    /**
     * The packet listener that will handled the incoming packages.
     */
    private final PacketListener listener;

    /**
     * Create a new broadcast receiver for a given xml config resource,
     * delegating all parsed packages to the listener. 
     * @param id The id of the xml config.
     * @param listener A packet listener parsed stanzas.
     */
    public AsmackBroadcastReceiver(PacketListener listener) {
        this.listener = listener;
    }

    /**
     * Called on every new intent, will parse stanzas and call the packet
     * listener with smack objects.
     * @param context The current context.
     * @param intent The intent.
     */
    @Override
    public void receive(Stanza stanza) {
        Packet packet = null;
        try {
            packet = SmackParser.getInstance().parse(stanza);
        } catch (Exception e) {
            Log.e(TAG, "Couldn't parse stanza", e);
        }
        if (packet == null) {
            return;
        }
        listener.processPacket(packet);
    }

}
