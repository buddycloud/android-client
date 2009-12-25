package com.buddycloud.android.buddydroid.sync;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.jivesoftware.smack.RosterEntry;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

import com.buddycloud.android.buddydroid.provider.BuddyCloud.Roster;
import com.buddycloud.jbuddycloud.BuddycloudClient;

public class RoasterSync extends Thread {

    private final BuddycloudClient client;
    private final ContentResolver resolver;

    public RoasterSync(BuddycloudClient client, ContentResolver resolver) {
        this.client = client;
        this.resolver = resolver;
        this.start();
    }

    @Override
    public void run() {
        try {
            Log.d("Roster", "read roaster");
            long time = -System.currentTimeMillis();

            ArrayList<ContentValues> newEntries = new ArrayList<ContentValues>();

            HashMap<String, String> oldRoster = new HashMap<String, String>();
            Cursor query = resolver.query(
                Roster.CONTENT_URI,
                new String[]{Roster.JID, Roster.NAME},
                null, null, null
            );
            if (query.getCount() > 0) {
                if (query.isBeforeFirst()) {
                    query.moveToNext();
                }
                while (!query.isAfterLast()) {
                    oldRoster.put(query.getString(1), query.getString(2));
                    query.moveToNext();
                }
            }
            query.close();

            String jid = client.getUser();
            if (jid.indexOf('/') != -1) {
                jid = jid.substring(0, jid.indexOf('/'));
            }

            ContentValues values = new ContentValues();
            if (oldRoster.containsKey(jid)) {
                oldRoster.remove(jid);
            } else {
                values.put(Roster.JID, jid);
                values.put(Roster.NAME, jid.substring(0, jid.lastIndexOf('@')));
                newEntries.add(values);
            }

            Log.d("Roster", "fetch new roster");

            Iterator<RosterEntry> iterator =
                client.getRoster().getEntries().iterator();
            while (iterator.hasNext()) {
                RosterEntry buddy = iterator.next();
                String newName = buddy.getName();
                String newUser = buddy.getUser();
                if (newName == null) {
                    if (newUser.indexOf('@') != -1) {
                        newName = newUser.substring(0, newUser.lastIndexOf('@'));
                    } else {
                        newName = newUser;
                    }
                }
                if (oldRoster.containsKey(buddy.getUser())) {
                    String name = oldRoster.get(newUser);
                    if (!name.equals(newName)) {
                        // Update name
                        Log.d("Roaster", "update " + buddy.getUser());
                        values = new ContentValues();
                        values.put(Roster.JID, newUser);
                        values.put(Roster.NAME, newName);
                        resolver.update(
                            Roster.CONTENT_URI,
                            values,
                            Roster.JID + " = ? AND " + Roster.NAME + " = ?",
                            new String[]{newUser, name}
                        );
                    }
                    oldRoster.remove(newUser);
                    continue;
                }
                Log.d("Roster", "add " + newUser);
                values = new ContentValues();
                values.put(Roster.JID, newUser);
                values.put(Roster.NAME, newName);
                newEntries.add(values);
            }
            resolver.bulkInsert(
                Roster.CONTENT_URI,
                newEntries.toArray(new ContentValues[newEntries.size()])
            );
            if (oldRoster.size() > 0) {
                StringBuilder where = new StringBuilder(Roster.JID);
                where.append(" IN (");
                for (int i = 1, l = oldRoster.size(); i < l; i++) {
                    where.append("?,");
                }
                where.append("?)");
                resolver.delete(Roster.CONTENT_URI, where.toString(),
                        oldRoster.keySet().toArray(new String[oldRoster.size()]));
            }

            time += System.currentTimeMillis();

            resolver.notifyChange(Roster.CONTENT_URI, null);
            Log.d("BC", "updated roster in " + time + "ms");
        } catch (Throwable t) {
            Log.e("BC", "Error during Roster sync", t);
        }
    }

}
