package com.buddycloud.android.buddydroid.provider;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

import com.buddycloud.android.buddydroid.provider.BuddyCloud.CacheColumns;
import com.buddycloud.android.buddydroid.provider.BuddyCloud.ChannelData;
import com.buddycloud.android.buddydroid.provider.BuddyCloud.Roster;

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

        synchronized (provider.mOpenHelper) {

            SQLiteDatabase db = provider.mOpenHelper.getWritableDatabase();

            try {
                db.beginTransaction();

                String node = values.getAsString(ChannelData.NODE_NAME);
                long id = values.getAsLong(ChannelData.ITEM_ID);
                long parent = 0l;
                if (values.getAsLong(ChannelData.PARENT) != null) {
                    parent = values.getAsLong(ChannelData.PARENT);
                }

                Cursor c = db.rawQuery(rosterJidQuery, new String[] { node });

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

                        db.update(
                                BuddycloudProvider.TABLE_ROSTER,
                                lu,
                                Roster.JID + "=?",
                                new String[] { node }
                        );

                        if (parent == 0) {

                            // update possible childs
                            db.update(
                                BuddycloudProvider.TABLE_CHANNEL_DATA,
                                lu,
                                ChannelData.NODE_NAME + "=? AND " +
                                ChannelData.PARENT + "=" + id,
                                new String[] { node }
                            );

                        } else {

                            // update whole thread
                            db.update(
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
                db.insert(BuddycloudProvider.TABLE_CHANNEL_DATA, null, values);

                // try to update unread counts
                if (values.containsKey(ChannelData.UNREAD)
                 && values.getAsBoolean(ChannelData.UNREAD)
                ) {
                    RosterHelper.recomputeUnread(
                        values.getAsString(ChannelData.NODE_NAME),
                        provider,
                        db
                    );
                }

                db.setTransactionSuccessful();

                notifyChange(provider);

                if (rosterChanged) {
                    RosterHelper.notifyChange(provider);
                }

            } catch (Throwable t) {
                t.printStackTrace(System.err);
            } finally {
                db.endTransaction();
            }

        }

        return null;
    }

    public static Cursor queryChannelData(
            Uri uri,
            String[] projection,
            String selection,
            String[] selectionArgs,
            String sortOrder,
            BuddycloudProvider provider
    ) {

        synchronized (provider.mOpenHelper) {
            Cursor c = provider.mOpenHelper.getReadableDatabase().query(
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
        provider.getContext().getContentResolver().notifyChange(
                ChannelData.CONTENT_URI, null);
    }

}
