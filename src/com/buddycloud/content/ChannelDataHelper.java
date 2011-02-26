package com.buddycloud.content;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

import com.buddycloud.android.buddydroid.BCNotifications;
import com.buddycloud.content.BuddyCloud.CacheColumns;
import com.buddycloud.content.BuddyCloud.ChannelData;
import com.buddycloud.content.BuddyCloud.Roster;

public class ChannelDataHelper {

    private final static String rosterJidQuery =
        " SELECT " + Roster.LAST_UPDATED +
        " FROM " + BuddycloudProvider.TABLE_ROSTER +
        " WHERE " + Roster.JID + "=?";

    public static Uri insert(Uri uri, ContentValues values,
            BuddycloudProvider provider) {

        values.put(CacheColumns.CACHE_UPDATE_TIMESTAMP, System
                .currentTimeMillis());
        values.remove(BaseColumns._ID);

        boolean rosterChanged = false;

        SQLiteDatabase database = provider.getDatabase();

        synchronized (database) {

            try {
                database.beginTransaction();

                String node = values.getAsString(ChannelData.NODE_NAME);
                long id = values.getAsLong(ChannelData.ITEM_ID);
                long parent = 0l;
                if (values.getAsLong(ChannelData.PARENT) != null) {
                    parent = values.getAsLong(ChannelData.PARENT);
                }

                Cursor c = database
                    .rawQuery(rosterJidQuery, new String[] { node });

                if (c.getCount() == 1) {
                    c.moveToFirst();
                    long last_updated = c.getLong(
                            c.getColumnIndex(Roster.LAST_UPDATED)
                    );

                    if (id > last_updated) {

                        rosterChanged = true;

                        ContentValues lu = new ContentValues();
                        lu.put(Roster.LAST_UPDATED, id);
                        lu.put(CacheColumns.CACHE_UPDATE_TIMESTAMP,
                                System.currentTimeMillis());
                        if (parent == 0l) {
                            lu.put(
                                Roster.LAST_MESSAGE,
                                values.getAsString(ChannelData.CONTENT)
                            );
                        }

                        database.update(
                                BuddycloudProvider.TABLE_ROSTER,
                                lu,
                                Roster.JID + "=?",
                                new String[] { node }
                        );

                        if (parent == 0) {
                            lu.remove(Roster.LAST_MESSAGE);

                            // update possible childs
                            database.update(
                                BuddycloudProvider.TABLE_CHANNEL_DATA,
                                lu,
                                ChannelData.NODE_NAME + "=? AND " +
                                ChannelData.PARENT + "=" + id,
                                new String[] { node }
                            );

                        } else {

                            // update whole thread
                            database.update(
                                    BuddycloudProvider.TABLE_CHANNEL_DATA,
                                    lu,
                                    ChannelData.NODE_NAME + "=? AND ("
                                    + ChannelData.PARENT + "=" + parent + " OR "
                                    + ChannelData.ITEM_ID + "=" + parent
                                    + ")",
                                    new String[] { node }
                            );

                        }
                    } else {

                        values.put(ChannelData.LAST_UPDATED, last_updated);

                    }
                } else {
                    Log.e("BC", "Couldn't update last_updated");
                }
                c.close();

                if (values.containsKey(ChannelData._ID)) {
                    values.remove(ChannelData._ID);
                }

                // Actually store the new message
                database.insert(
                        BuddycloudProvider.TABLE_CHANNEL_DATA, null, values);

                // try to update unread counts
                if (values.containsKey(ChannelData.UNREAD)
                 && values.getAsBoolean(ChannelData.UNREAD)
                ) {
                    RosterHelper.recomputeUnread(
                        values.getAsString(ChannelData.NODE_NAME),
                        provider,
                        database
                    );
                }

                database.setTransactionSuccessful();

                notifyChange(provider);

                if (rosterChanged) {
                    RosterHelper.notifyChange(provider);
                }

            } catch (Throwable t) {
                t.printStackTrace(System.err);
            } finally {
                database.endTransaction();
            }

        }

        if (rosterChanged) {
            BCNotifications.updateNotification(provider);
        }

        return null;
    }

    public static int update(
        ContentValues values,
        String selection,
        String[] selectionArgs,
        BuddycloudProvider provider
    ) {
        values.put(
            CacheColumns.CACHE_UPDATE_TIMESTAMP,
            System.currentTimeMillis()
        );
        values.remove(BaseColumns._ID);

        boolean notifyRoster = false;
        boolean notify = false;

        int count = -1;
        SQLiteDatabase database = provider.getDatabase();
        synchronized (database) {

            try {
                database.beginTransaction();
                count = database.update(
                        BuddycloudProvider.TABLE_CHANNEL_DATA,
                        values,
                        selection,
                        selectionArgs
                );

                if (
                    count >= 1
                    && values.containsKey(ChannelData.UNREAD)
                    && !values.getAsBoolean(ChannelData.UNREAD)
                    && values.size() == 2
                ) { // we've just updated unread
                    Cursor c = database.query(
                            BuddycloudProvider.TABLE_CHANNEL_DATA,
                            ChannelData.PROJECTION_MAP,
                            selection,
                            selectionArgs,
                            null,
                            null,
                            null
                    );
                    if (c.moveToFirst()) {
                        String channel = c.getString(
                            c.getColumnIndex(ChannelData.NODE_NAME)
                        );
                        RosterHelper.recomputeUnread(channel, provider,
                                database);
                        notifyRoster = true;
                    }
                    c.close();
                }
                notify = true;

                database.setTransactionSuccessful();
            } finally {
                try {
                    database.endTransaction();
                } catch (Exception e) {
                    // irrelevant
                }
            }

            if (notify) {
                notifyChange(provider);
            }
            if (notifyRoster) {
                RosterHelper.notifyChange(provider);
                BCNotifications.updateNotification(provider);
            }
        }

        return count;
    }

    public static Cursor queryChannelData(
            Uri uri,
            String[] projection,
            String selection,
            String[] selectionArgs,
            String sortOrder,
            BuddycloudProvider provider
    ) {

        SQLiteDatabase database = provider.getDatabase();

        synchronized (database) {
            Cursor c = database.query(
                    BuddycloudProvider.TABLE_CHANNEL_DATA,
                    projection,
                    selection,
                    selectionArgs,
                    null,
                    null,
                    sortOrder
            );
            c.setNotificationUri(
                provider.getContext().getContentResolver(),
                ChannelData.CONTENT_URI
            );
            return c;
        }

    }

    public static void notifyChange(BuddycloudProvider provider) {
        Log.d(BuddycloudProvider.TAG, "notify ui about channel udpates");
        provider.getContext().getContentResolver().notifyChange(
                ChannelData.CONTENT_URI, null);
    }

    private final static String BROKEN_CHANNEL_QUERY =
        "SELECT DISTINCT " + ChannelData.PARENT + ", " + ChannelData.ITEM_ID
            + " FROM "
            + BuddycloudProvider.TABLE_CHANNEL_DATA + " WHERE "
            + ChannelData.NODE_NAME + " = ? AND "
            + ChannelData.PARENT + " <> 0 AND "
            + ChannelData.PARENT + " NOT IN ("
                 + "SELECT DISTINCT " + ChannelData.ITEM_ID + " FROM "
                 + BuddycloudProvider.TABLE_CHANNEL_DATA + " WHERE "
                 + ChannelData.PARENT + " = 0 AND "
                 + ChannelData.NODE_NAME + " = ?"
            + ") ORDER BY "
            + ChannelData.PARENT + " ASC, "
            + ChannelData.ITEM_ID + " ASC";

    public static Cursor queryBrokenChannelData(String channel,
            BuddycloudProvider provider) {
        SQLiteDatabase database = provider.getDatabase();
        synchronized (database) {
            Cursor c = database.rawQuery(
                BROKEN_CHANNEL_QUERY, new String[]{
                    channel, channel
                }
            );
            return c;
        }
    }

}
