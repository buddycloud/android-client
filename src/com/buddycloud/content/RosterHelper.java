package com.buddycloud.content;

import com.buddycloud.content.BuddyCloud.CacheColumns;
import com.buddycloud.content.BuddyCloud.ChannelData;
import com.buddycloud.content.BuddyCloud.Roster;

import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.util.Log;

/**
 * All sql related roster handling.
 */
public class RosterHelper {

    /**
     * Query the full roster and bind the query to the roster uri.
     * @param uri The roster uri
     * @param projection The fields to fetch
     * @param selection The where clause arguments
     * @param selectionArgs String arguments for the where clause
     * @param sortOrder Ordering
     * @param provider The BuddycloudProvider
     * @return A bound cursor
     */
    public static Cursor queryRoster(
            String[] projection,
            String selection,
            String[] selectionArgs,
            String sortOrder,
            BuddycloudProvider provider) {

        SQLiteDatabase database = provider.getDatabase();

        synchronized (database) {
            Cursor c = database.query(
                    BuddycloudProvider.TABLE_ROSTER,
                    projection,
                    selection,
                    selectionArgs,
                    null,
                    null,
                    sortOrder
            );
            c.setNotificationUri(
                provider.getContext().getContentResolver(),
                Roster.CONTENT_URI
            );
            return c;
        }
    }

    /**
     * Query for the roster view hack.
     */
    private final static String rosterViewQuery =
        "SELECT *," +
            Roster.UNREAD_REPLIES + ">0 as hasReplies," +
            Roster.UNREAD_MESSAGES + ">0 as hasUnread " +
        " FROM " + BuddycloudProvider.TABLE_ROSTER +
        " WHERE type='channel' " +
        " ORDER BY " + 
            "self DESC," +
            "hasReplies DESC," +
            "hasUnread DESC," +
            "last_updated DESC," +
            "cache_update_timestamp DESC";

    /**
     * Create a cursor for the roster view, bound to the roster uri.
     * The main difference to the rosterQuery is that your own jid is always on
     * top.
     * 
     * @param provider The BuddycloudProvider
     * @return A bound cursor
     */
    public static Cursor queryRosterView(BuddycloudProvider provider) {
        SQLiteDatabase database = provider.getDatabase();
        synchronized (database) {
            Cursor c = database
                    .rawQuery(rosterViewQuery, new String[]{});

            c.setNotificationUri(
                provider.getContext().getContentResolver(),
                Roster.CONTENT_URI
            );
            return c;
        }
    }

    /**
     * Adds a new entry to the roster, notifying all queries about the update.
     * @param uri The roster uri.
     * @param values The entry values.
     * @param provider The BuddycloudProvider.
     * @return null
     */
    public static Uri insert(
            ContentValues values,
            BuddycloudProvider provider
    ) {
        values.put(
            CacheColumns.CACHE_UPDATE_TIMESTAMP,
            System.currentTimeMillis()
        );
        values.remove(BaseColumns._ID);

        SQLiteDatabase database = provider.getDatabase();
        synchronized (database) {
            if (database.insert(
                    BuddycloudProvider.TABLE_ROSTER, Roster.JID, values) == -1)
            {
                return null;
            }
            notifyChange(provider);
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

        int count = -1;
        SQLiteDatabase database = provider.getDatabase();
        synchronized (database) {
            count = database.update(
                    BuddycloudProvider.TABLE_ROSTER,
                    values,
                    selection,
                    selectionArgs
            );
            if (count > 0) {
                notifyChange(provider);
            }
        }
        return count;
    }

    private final static String totalUnreadQuery =
        "SELECT sum(" + Roster.UNREAD_MESSAGES + ") " +
        " FROM " + BuddycloudProvider.TABLE_ROSTER;

    private final static String totalUnreadRepliesQuery =
        "SELECT sum(" + Roster.UNREAD_REPLIES + ") " +
        " FROM " + BuddycloudProvider.TABLE_ROSTER;

    public final static long[] getUnreadCounts(BuddycloudProvider provider) {
        long unread = 0l;
        long unreadReplies = 0l;

        SQLiteDatabase database = provider.getDatabase();
        synchronized (database) {
            {
                Cursor cursor = database.rawQuery(totalUnreadQuery, null);
                if (cursor.moveToFirst()) {
                    unread = cursor.getLong(0);
                }
                cursor.close();
            }

            {
                Cursor cursor =
                            database.rawQuery(totalUnreadRepliesQuery, null);
                if (cursor.moveToFirst()) {
                    unreadReplies = cursor.getLong(0);
                }
                cursor.close();
            }
        }

        return new long[]{unreadReplies, unread};
    }

    private final static String unreadCountQuery =
        "SELECT count(*) " +
        " FROM " + BuddycloudProvider.TABLE_CHANNEL_DATA +
        " WHERE " + ChannelData.NODE_NAME + "=?" +
        " AND " + ChannelData.UNREAD + "=1";

    private final static String unreadReplyCountQuery =
        "SELECT count(*) " +
        " FROM "+ BuddycloudProvider.TABLE_CHANNEL_DATA + " AS c1 " +
        " WHERE " + ChannelData.NODE_NAME + "=? " +
        " AND " + ChannelData.UNREAD + "=1 " +"" +
        " AND NOT " + ChannelData.PARENT + "=0 " +
        " AND EXISTS (" +
            "SELECT * " +
            "  FROM " + BuddycloudProvider.TABLE_CHANNEL_DATA + " AS c2 " +
            " WHERE c2." + ChannelData.ITEM_ID + "=" + 
                   "c1." + ChannelData.PARENT +
            " AND c2." + ChannelData.AUTHOR_JID + "=? " + 
            " AND c2." + ChannelData.NODE_NAME + "=" + 
                 "c1." + ChannelData.NODE_NAME +
        ")";

    static void recomputeUnread(
            String channel,
            BuddycloudProvider provider,
            SQLiteDatabase db
    ) {

        int unread = 0;
        int unreadReplies = 0;

        {   // get unread messages
            Cursor c = db.rawQuery(
                        unreadCountQuery,
                        new String[]{channel}
            );
            if (c.moveToFirst()) {
                unread = c.getInt(0);
            }
            c.close();
        }

        {   // get unread replies
            SharedPreferences preferences =
                PreferenceManager.getDefaultSharedPreferences(
                    provider.getContext());
            final String jid = preferences.getString("jid", "");

            Cursor c = db.rawQuery(
                    unreadReplyCountQuery,
                    new String[]{channel, jid}
            );
            if (c.moveToFirst()) {
                unreadReplies = c.getInt(0);
            }
            c.close();
        }

        // set the values

        ContentValues values = new ContentValues();

        values.put(Roster.UNREAD_MESSAGES, unread);
        values.put(Roster.UNREAD_REPLIES, unreadReplies);

        db.update(
            BuddycloudProvider.TABLE_ROSTER,
            values,
            Roster.JID + "=?",
            new String[]{channel}
        );

    }

    /**
     * Delete a Roster entry.
     * @param selection
     * @param selectionArgs
     * @param provider
     * @return
     */
    public static int delete(
            String selection,
            String[] selectionArgs,
            BuddycloudProvider provider
    ) {

        int count = -1;

        SQLiteDatabase database = provider.getDatabase();
        synchronized (database) {

            count = database.delete(
                    BuddycloudProvider.TABLE_ROSTER,
                    selection,
                    selectionArgs
            );
            if (count > 0) {
                notifyChange(provider);
            }
        }

        return count;
    }

    /**
     * Notify all bound cursors about new content.
     * @param provider
     */
    public static void notifyChange(BuddycloudProvider provider) {
        Log.d(BuddycloudProvider.TAG, "notify ui about roster udpates");
        provider.getContext().getContentResolver()
            .notifyChange(Roster.CONTENT_URI, null);
    }

}
