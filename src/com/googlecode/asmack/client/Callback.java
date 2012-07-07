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

/**
 * Callback for reply packages. The natural ordering of the callbacks is based
 * on the time to live.
 */
class Callback implements Comparable<Callback> {

    /**
     * Time To Live for the callback.
     */
    private final long ttl;

    /**
     * The reply id.
     */
    private final String id;

    /**
     * The internal listener callback.
     */
    private final PacketListener callback;

    /**
     * Create a new collback instance with a given listener sink, reply id
     * and time to live.
     * @param ttl The time to live.
     * @param id The required reply id.
     * @param callback The actual callback.
     */
    public Callback(long ttl, String id, PacketListener callback) {
        this.ttl = ttl;
        this.id = id;
        this.callback = callback;
    }

    /**
     * Compares this callback to another callback. The natural ordering is
     * based on the ttl, thus callbacks that that expire earlier.
     * @param another The other callback.
     * @return -1, 0 or 1 if the other callback expires earlier, at the same
     *         time or later.
     */
    @Override
    public int compareTo(Callback another) {
        if (another.ttl != ttl) {
            if (ttl < another.ttl) {
                return -1;
            } else {
                return 1;
            }
        }
        return id.compareTo(another.id);
    }

    /**
     * Retrieve the time to live of the callback.
     * @return The expire time.
     */
    public long getTTL() {
        return ttl;
    }

    /**
     * Get the reply id.
     * @return The reply
     */
    public String getId() {
        return id;
    }

    /**
     * The final callback listener.
     * @return The callback listener.
     */
    public PacketListener getCallback() {
        return callback;
    }

}
