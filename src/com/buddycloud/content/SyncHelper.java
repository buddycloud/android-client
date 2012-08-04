package com.buddycloud.content;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

import com.buddycloud.content.BuddyCloud.Sync;

public class SyncHelper {

    public static int delete(String selection, String[] selectionArgs,
            BuddycloudProvider buddycloudProvider) {
        return 0;
    }

    public static Uri insert(ContentValues values, BuddycloudProvider provider) {
        values.remove(BaseColumns._ID);
        SQLiteDatabase database = provider.getDatabase();

        synchronized (database) {
            if (database.insert(BuddycloudProvider.TABLE_SYNC, null, values) != -1) {
                notifyChange(provider);
            }
        }
        return null;
    }

    public static int update(ContentValues values, String selection,
            String[] selectionArgs, BuddycloudProvider buddycloudProvider) {
        return 0;
    }

    public static Cursor query(String[] projection, String selection,
            String[] selectionArgs, String sortOrder,
            BuddycloudProvider provider) {

        SQLiteDatabase database = provider.getDatabase();

        synchronized (database) {
            Cursor c = database.query(
                    BuddycloudProvider.TABLE_SYNC,
                    projection,
                    selection,
                    selectionArgs,
                    null,
                    null,
                    sortOrder
            );
            c.setNotificationUri(
                provider.getContext().getContentResolver(),
                Sync.CONTENT_URI
            );
            return c;
        }
    }

    public static void notifyChange(BuddycloudProvider provider) {
        Log.d(BuddycloudProvider.TAG, "notify ui about channel udpates");
        provider.getContext().getContentResolver().notifyChange(
                Sync.CONTENT_URI, null);
    }

}
