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

package com.googlecode.asmack.contacts;

/**
 * XmppMetadata represents the metadata as displayed in the contact overview.
 */
public class XmppMetadata extends Metadata {

    /**
     * The xmpp profile mimetype, as specified in the android manifest.
     */
    public final static String MIMETYPE =
        "vnd.android.cursor.item/vnd.xmpp.profile";

    /**
     * Create a new XmppMetadate object with default values.
     */
    public XmppMetadata() {
        mimetype = MIMETYPE;
        setSummary("Xmpp-Profil");
        setDetail("Show Profile");
    }

    /**
     * Set the jid of the target contact.
     * @param jid The target contact jid.
     */
    public void setJid(String jid) {
        setDetail("Show " + jid);
        setData(0, jid);
    }

    /**
     * Retrieve the target user jid.
     * @return The target user jid.
     */
    public String getJid() {
        return getData(0);
    }

    /**
     * Set the contact summary.
     * @param summary The contact summary.
     */
    public void setSummary(String summary) {
        setData(1, summary);
    }

    /**
     * Retrieve the contact summary.
     * @return The contact summary.
     */
    public String getSummary() {
        return getData(1);
    }

    /**
     * Set the contacts details.
     * @param detail The contacts details.
     */
    public void setDetail(String detail) {
        setData(2, detail);
    }

    /**
     * Retrieve the contacts details.
     * @return The contacts details.
     */
    public String getDetail() {
        return getData(2);
    }

}
