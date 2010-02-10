/**
 * 
 */
package com.buddycloud.android.buddydroid;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.util.Log;

import com.buddycloud.android.buddydroid.provider.BuddyCloud.Roster;
import com.buddycloud.jbuddycloud.BCGeoLocListener;
import com.buddycloud.jbuddycloud.packet.GeoLoc;

final class ConnectionBCGeolocListener implements BCGeoLocListener {

    private final ContentResolver resolver;

    public ConnectionBCGeolocListener(ContentResolver resolver) {
        this.resolver = resolver;
    }

    public void receive(String from, GeoLoc loc) {
        if (loc.getType() == null) {
            return;
        }
        ContentValues values = new ContentValues();
        if (loc.getLocType().equals(GeoLoc.Type.CURRENT)) {
            values.put(Roster.GEOLOC, loc.getText());
        } else
        if (loc.getLocType().equals(GeoLoc.Type.NEXT)) {
            values.put(Roster.GEOLOC_NEXT, loc.getText());
        } else
        if (loc.getLocType().equals(GeoLoc.Type.PREV)) {
            values.put(Roster.GEOLOC_PREV, loc.getText());
        }
        Log.d(BuddycloudService.TAG, "Update '/user/" + from + "/channel'");
        resolver.update(Roster.CONTENT_URI, values,
                Roster.JID + "='/user/" + from + "/channel'",
                null);
    }
}