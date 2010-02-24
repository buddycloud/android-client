/**
 * 
 */
package com.buddycloud.android.buddydroid;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.util.Log;

import com.buddycloud.android.buddydroid.provider.BuddyCloud.ChannelData;
import com.buddycloud.android.buddydroid.provider.BuddyCloud.Roster;
import com.buddycloud.jbuddycloud.BCAtomListener;
import com.buddycloud.jbuddycloud.BuddycloudClient;
import com.buddycloud.jbuddycloud.packet.BCAtom;
import com.buddycloud.jbuddycloud.packet.GeoLoc;

final class BCConnectionAtomListener
    implements BCAtomListener {

    private final ContentResolver resolver;
    private final BuddycloudClient connection;

    public BCConnectionAtomListener(
            ContentResolver resolver,
            BuddycloudClient connection
    ) {
        this.resolver = resolver;
        this.connection = connection;
    }

    public void receive(String node, BCAtom atom) {
        if (node.startsWith("/user/")) {
            node = node.substring(6);
            String jid = node.substring(0, node.indexOf('/'));
            node = node.substring(node.indexOf('/') + 1);
            if (node.startsWith("geo/")) {
                ContentValues values = new ContentValues();
                GeoLoc loc = atom.getGeoloc();
                if (node.equals("geo/future")) {
                    values.put(Roster.GEOLOC_NEXT, loc.getText());
                } else
                if (node.equals("geo/current")) {
                    values.put(Roster.GEOLOC, loc.getText());
                } else
                if (node.equals("geo/previous")) {
                    values.put(Roster.GEOLOC_PREV, loc.getText());
                }
                resolver.update(Roster.CONTENT_URI, values,
                        Roster.JID + "='" + jid + "'",
                        null);
                return;
            }
            if (!node.equals("channel")) {
                // /user/jid/mood ?
                return;
            }
            node = "/user/" + jid + "/" + node;
        }
        // Channel !
        ContentValues values = new ContentValues();
        values.put(ChannelData.NODE_NAME,
                   node);
        values.put(ChannelData.AUTHOR,
                   atom.getAuthorName());
        values.put(ChannelData.AUTHOR_JID,
                   atom.getAuthorJid());
        values.put(ChannelData.AUTHOR_AFFILIATION,
                   atom.getAffiliation());
        values.put(ChannelData.CONTENT,
                   atom.getContent());
        values.put(ChannelData.CONTENT_TYPE,
                   atom.getContentType());
        values.put(ChannelData.ITEM_ID,
                   atom.getId());
        values.put(ChannelData.LAST_UPDATED,
                   atom.getId());
        values.put(ChannelData.PARENT,
                   atom.getParentId());
        values.put(ChannelData.PUBLISHED,
                   atom.getPublished());
        GeoLoc loc = atom.getGeoloc();
        if (loc != null) {
            values.put(ChannelData.GEOLOC_ACCURACY,
                       loc.getAccuracy());
            values.put(ChannelData.GEOLOC_AREA,
                       loc.getArea());
            values.put(ChannelData.GEOLOC_COUNTRY,
                       loc.getCountry());
            values.put(ChannelData.GEOLOC_LAT,
                       loc.getLat());
            values.put(ChannelData.GEOLOC_LOCALITY,
                       loc.getLocality());
            values.put(ChannelData.GEOLOC_LON,
                       loc.getLon());
            values.put(ChannelData.GEOLOC_REGION,
                       loc.getRegion());
            values.put(ChannelData.GEOLOC_TEXT,
                       loc.getText());
            if (loc.getLocType() != null) {
                values.put(ChannelData.GEOLOC_TYPE,
                           loc.getLocType().toString());
            }
        }

        if (!connection.getUser().startsWith(atom.getAuthorJid())) {
            values.put(ChannelData.UNREAD, true);
        }

        resolver.insert(ChannelData.CONTENT_URI, values);
        Log.d(BuddycloudService.TAG, "stored " + atom.getId() + "@" + node);
    }
}