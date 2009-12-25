/**
 * 
 */
package com.buddycloud.android.buddydroid.provider;

import java.util.HashMap;
import java.util.Map;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import com.buddycloud.android.buddydroid.provider.BuddyCloud.CacheColumns;
import com.buddycloud.android.buddydroid.provider.BuddyCloud.ChannelData;
import com.buddycloud.android.buddydroid.provider.BuddyCloud.Places;
import com.buddycloud.android.buddydroid.provider.BuddyCloud.Roster;

/**
 * @author zero
 * 
 */

public class BuddycloudProvider extends ContentProvider {

    private static UriMatcher URI_MATCHER;

    private static final int CHANNEL_DATA = 103;
    private static final int CHANNEL_DATA_ID = 104;

    private static final int ROSTER = 201;
    private static final int ROSTER_ID = 202;

    private static final int PLACES = 301;
    private static final int PLACES_ID = 302;

    private static final String TABLE_CHANNEL_DATA = "channeldata";

    private static final String TABLE_ROSTER = "roster";
    private static final String TABLE_PLACES = "places";

    // private SQLiteOpenHelper mOpenHelper;
    private DatabaseHelper mOpenHelper;

    private static final String TAG = "Provider";
    private static final String DATABASE_NAME = "buddycloud.db";

    /**
     * Version of database.
     * 
     * The various versions were introduced in the following releases:
     * 
     * 1: Release 0.0.1
     */
    private static final int DATABASE_VERSION = 1;

    private static final Map<String, String> CHANNELS_DATA_PROJECTION_MAP = new HashMap<String, String>();

    private static class DatabaseHelper extends SQLiteOpenHelper {
        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + TABLE_CHANNEL_DATA + " ( "

                    + ChannelData._ID + " INTEGER PRIMARY KEY,"
                    + ChannelData.ITEM_ID + " INTEGER,"

                    + ChannelData.PARENT + " VARCHAR,"
                    + ChannelData.LAST_UPDATED + " LONG,"
                    + ChannelData.PUBLISHED + " LONG,"

                    + ChannelData.AUTHOR + " VARCHAR,"
                    + ChannelData.AUTHOR_JID + " VARCHAR,"
                    + ChannelData.AUTHOR_AFFILIATION + " VARCHAR,"

                    + ChannelData.CONTENT + " VARHCHAR,"
                    + ChannelData.CONTENT_TYPE + " VARCHAR,"

                    + ChannelData.NODE_NAME + " VARCHAR,"

                    + ChannelData.GEOLOC_LAT + " FLOAT,"
                    + ChannelData.GEOLOC_LON + " FLOAT,"
                    + ChannelData.GEOLOC_ACCURACY + " FLOAT,"

                    + ChannelData.GEOLOC_AREA + " VARCHAR,"
                    + ChannelData.GEOLOC_COUNTRY + " VARCHAR,"
                    + ChannelData.GEOLOC_REGION + " VARCHAR,"
                    + ChannelData.GEOLOC_LOCALITY + " VARCHAR,"

                    + ChannelData.GEOLOC_TEXT + " VARCHAR,"
                    + ChannelData.GEOLOC_TYPE + " INTEGER,"

                    + ChannelData.CACHE_UPDATE_TIMESTAMP + " LONG,"
                    + ChannelData._COUNT + " LONG,"
                    + "UNIQUE(" // CONSTRAIN
                        + ChannelData.ITEM_ID + ","
                        + ChannelData.NODE_NAME
                    + "),"
                    + "UNIQUE(" // INDEX
                        + ChannelData.NODE_NAME + ","
                        + ChannelData.LAST_UPDATED + ","
                        + ChannelData.PUBLISHED
                    + ")"
                    + ");"

            );

            db.execSQL("CREATE TABLE " + TABLE_ROSTER
                    + " (_id INTEGER PRIMARY KEY,"
                    + Roster.JID + " VARCHAR,"
                    + Roster.NAME + " VARCHAR,"
                    + Roster.STATUS + " VARCHAR,"
                    + Roster.GEOLOC + " VARCHAR,"
                    + Roster.GEOLOC_NEXT + " VARCHAR,"
                    + Roster.GEOLOC_PREV + " VARCHAR,"
                    + Roster._COUNT + " LONG,"
                    + Roster.CACHE_UPDATE_TIMESTAMP + " LONG"
                    + ");");

            db.execSQL("CREATE TABLE " + TABLE_PLACES
                    + " (_id INTEGER PRIMARY KEY," + Places.PLACE_ID
                    + " VARCHAR," + Places.NAME + " VARCHAR," + Places.LAT
                    + " VARCHAR," + Places.LON + " VARCHAR,"
                    + Places.DESCRIPTION + " VARCHAR," + Places.AREA
                    + " VARCHAR," + Places.COUNTRY + " VARCHAR,"
                    + Places.POSTALCODE + " VARCHAR," + Places.REGION
                    + " VARCHAR," + Places.STREET + " VARCHAR,"
                    + Places.POPULATION + " VARCHAR," + Places.SHARED
                    + " BOOLEAN," + Places._COUNT + " LONG,"
                    + Places.CACHE_UPDATE_TIMESTAMP + " LONG" + ");");

        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // TODO Auto-generated method stub

        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.content.ContentProvider#onCreate()
     */
    @Override
    public boolean onCreate() {
        mOpenHelper = new DatabaseHelper(getContext());
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.content.ContentProvider#getType(android.net.Uri)
     */
    @Override
    public String getType(Uri uri) {
        int what = URI_MATCHER.match(uri);

        switch (what) {

        case CHANNEL_DATA:

        case CHANNEL_DATA_ID:

        case ROSTER:

        case ROSTER_ID:

        }
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.content.ContentProvider#insert(android.net.Uri,
     * android.content.ContentValues)
     */
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        int what = URI_MATCHER.match(uri);
        long rowID = 0;
        values.put(CacheColumns.CACHE_UPDATE_TIMESTAMP, System
                .currentTimeMillis());
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        switch (what) {

        case CHANNEL_DATA:
            rowID = db.insert(TABLE_CHANNEL_DATA, ChannelData._ID, values);
            uri = ContentUris.withAppendedId(ChannelData.CONTENT_URI, rowID);
            break;

        case ROSTER:
            rowID = db.insert(TABLE_ROSTER, Roster.JID, values);
            // uri = ContentUris.withAppendedId(Roster.CONTENT_URI, rowID);
            Log.d(TAG, "inserted " + values.getAsString(Roster.JID));
            break;
        }
        if (rowID > 0) {
            // Intent intent = new Intent(ProviderIntents.ACTION_INSERTED);
            // intent.setData(uri);
            // getContext().sendBroadcast(intent);
            return uri;
        }

        getContext().getContentResolver().notifyChange(uri, null);

        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.content.ContentProvider#query(android.net.Uri,
     * java.lang.String[], java.lang.String, java.lang.String[],
     * java.lang.String)
     */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {

        int what = URI_MATCHER.match(uri);
        MatrixCursor dummyCursor = null;

        // Log.d("provider", "what: " + what);
        // Log.d("provider", "uri: " + uri);
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        switch (what) {

        case CHANNEL_DATA:
            String sql = "SELECT " + ChannelData._ID + ","
                                   + ChannelData.ITEM_ID + ","
                                   + ChannelData.PARENT + ","
                                   + ChannelData.LAST_UPDATED + ","
                                   + ChannelData.PUBLISHED + ","
                                   + ChannelData.AUTHOR + ","
                                   + ChannelData.AUTHOR_JID + ","
                                   + ChannelData.AUTHOR_AFFILIATION + ","
                                   + ChannelData.CONTENT + ","
                                   + ChannelData.CONTENT_TYPE + ","
                                   + ChannelData.NODE_NAME + ","
                                   + ChannelData.GEOLOC_LAT + ","
                                   + ChannelData.GEOLOC_LON + ","
                                   + ChannelData.GEOLOC_ACCURACY + ","
                                   + ChannelData.GEOLOC_AREA + ","
                                   + ChannelData.GEOLOC_COUNTRY + ","
                                   + ChannelData.GEOLOC_REGION + ","
                                   + ChannelData.GEOLOC_LOCALITY + ","
                                   + ChannelData.GEOLOC_TEXT + ","
                                   + ChannelData.GEOLOC_TYPE + ","
                                   + ChannelData.CACHE_UPDATE_TIMESTAMP
                      + " FROM "   + TABLE_CHANNEL_DATA;
            if (selection != null) {
                sql  += " WHERE "  + selection;
            }
            if (sortOrder != null) {
                sql += " ORDER BY " + sortOrder;
            }

            Cursor c = mOpenHelper.getReadableDatabase().rawQuery(
                    sql, selectionArgs
            );
            c.setNotificationUri(getContext().getContentResolver(), uri);
            return c;
        case CHANNEL_DATA_ID:
            sql = "SELECT " + ChannelData._ID + ","
                            + ChannelData.ITEM_ID + ","
                            + ChannelData.PARENT + ","
                            + ChannelData.LAST_UPDATED + ","
                            + ChannelData.PUBLISHED + ","
                            + ChannelData.AUTHOR + ","
                            + ChannelData.AUTHOR_JID + ","
                            + ChannelData.AUTHOR_AFFILIATION + ","
                            + ChannelData.CONTENT + ","
                            + ChannelData.CONTENT_TYPE + ","
                            + ChannelData.NODE_NAME + ","
                            + ChannelData.GEOLOC_LAT + ","
                            + ChannelData.GEOLOC_LON + ","
                            + ChannelData.GEOLOC_ACCURACY + ","
                            + ChannelData.GEOLOC_AREA + ","
                            + ChannelData.GEOLOC_COUNTRY + ","
                            + ChannelData.GEOLOC_REGION + ","
                            + ChannelData.GEOLOC_LOCALITY + ","
                            + ChannelData.GEOLOC_TEXT + ","
                            + ChannelData.GEOLOC_TYPE + ","
                            + ChannelData.CACHE_UPDATE_TIMESTAMP
                      + " FROM "  + TABLE_CHANNEL_DATA;
            if (selection != null) {
                sql += " WHERE " + selection;
            }
            if (sortOrder != null) {
                sql += " ORDER BY " + sortOrder;
            }

            c = mOpenHelper.getReadableDatabase().rawQuery(
                    sql, selectionArgs
            );
            c.setNotificationUri(getContext().getContentResolver(), uri);
            return c;
        case ROSTER:
//            qb.setTables(TABLE_ROSTER);
//            qb.setProjectionMap(ROSTER_PROJECTION_MAP);
            String jid = "/user/" + PreferenceManager
                .getDefaultSharedPreferences(getContext()).getString("jid","")
                + "/channel";
            c = mOpenHelper.getReadableDatabase().rawQuery("SELECT " +
                    "_id, jid, name, status, geoloc_prev, geoloc, geoloc_next, " +
                    "jid='" + jid + "' AS itsMe " +
                    "FROM roster ORDER BY itsMe DESC, cache_update_timestamp DESC",
                    new String[]{}
            );
            c.setNotificationUri(getContext().getContentResolver(), uri);
            return c;
        case ROSTER_ID:
            qb.setTables(TABLE_ROSTER);
            qb.appendWhere("_id=" + uri.getPathSegments().get(1));

            dummyCursor = new MatrixCursor(BuddyCloud.Roster.PROJECTION_MAP);
            dummyCursor.addRow(new String[] { "1", "dummy@buddycloud.com",
                    "DUMMY", "STATUS", "HERE", "There", "Nowhere" });
            break;
        case PLACES:
            qb.setTables(TABLE_PLACES);
            qb.setProjectionMap(PLACES_PROJECTION_MAP);

            dummyCursor = new MatrixCursor(BuddyCloud.Places.PROJECTION_MAP);
            dummyCursor.addRow(new String[] { "1", "Places.NAME",
                    "Places.DESCRIPTION", "Places.LAT", "Places.LON",
                    "Places.STREET", "Places.AREA", "Places.REGION",
                    "Places.COUNTRY", "Places.POSTALCODE", "Places.POPULATION",
                    "Places.REVISION", "FALSE" });
            break;
        case PLACES_ID:
            qb.setTables(TABLE_PLACES);
            qb.appendWhere("_id=" + uri.getPathSegments().get(1));

            dummyCursor = new MatrixCursor(BuddyCloud.Places.PROJECTION_MAP);
            dummyCursor.addRow(new String[] { "1", "Places.NAME",
                    "Places.DESCRIPTION", "Places.LAT", "Places.LON",
                    "Places.STREET", "Places.AREA", "Places.REGION",
                    "Places.COUNTRY", "Places.POSTALCODE", "Places.POPULATION",
                    "Places.REVISION", "FALSE" });
            break;
        }

        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor c = qb.query(db, projection, selection, selectionArgs, null,
                null, sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.content.ContentProvider#update(android.net.Uri,
     * android.content.ContentValues, java.lang.String, java.lang.String[])
     */
    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        int count = 0;
        int what = URI_MATCHER.match(uri);
         values.put(CacheColumns.CACHE_UPDATE_TIMESTAMP,System.currentTimeMillis());
        switch (what) {

        case CHANNEL_DATA:

        case CHANNEL_DATA_ID:

        case ROSTER:
            mOpenHelper.getWritableDatabase().update(TABLE_ROSTER, values,
                    selection, selectionArgs);
            Log.d(TAG, "updated roster");
            break;
        case ROSTER_ID:

        }
        getContext().getContentResolver().notifyChange(uri, null);

        // Intent intent = new Intent(ProviderIntents.ACTION_MODIFIED);
        // intent.setData(uri);
        // getContext().sendBroadcast(intent);

        return count;
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.content.ContentProvider#delete(android.net.Uri,
     * java.lang.String, java.lang.String[])
     */
    @Override
    public int delete(Uri uri, String arg1, String[] arg2) {
        int what = URI_MATCHER.match(uri);
        switch (what) {
        case CHANNEL_DATA:
            break;
        case ROSTER:
            mOpenHelper.getWritableDatabase().delete(TABLE_ROSTER, arg1, arg2);
            break;
        }
        return 0;
    }

    private static HashMap<String, String> PLACES_PROJECTION_MAP = new HashMap<String, String>();

    private static HashMap<String, String> ROSTER_PROJECTION_MAP = new HashMap<String, String>();
    private static HashMap<String, String> CHANNELS_PROJECTION_MAP = new HashMap<String, String>();
    static {

        URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
        URI_MATCHER.addURI("com.buddycloud", "channeldata", CHANNEL_DATA);
        URI_MATCHER.addURI("com.buddycloud", "channeldata/#", CHANNEL_DATA_ID);

        URI_MATCHER.addURI("com.buddycloud", "roster", ROSTER);
        URI_MATCHER.addURI("com.buddycloud", "roster/#", ROSTER_ID);

        URI_MATCHER.addURI("com.buddycloud", "places", PLACES);
        URI_MATCHER.addURI("com.buddycloud", "places/#", PLACES_ID);

        CHANNELS_DATA_PROJECTION_MAP.put(ChannelData._ID, ChannelData._ID);
        CHANNELS_DATA_PROJECTION_MAP.put(ChannelData.NODE_NAME,
                ChannelData.NODE_NAME);
        CHANNELS_DATA_PROJECTION_MAP.put(ChannelData.PARENT,
                ChannelData.PARENT);
        CHANNELS_DATA_PROJECTION_MAP.put(ChannelData.LAST_UPDATED,
                ChannelData.LAST_UPDATED);

        CHANNELS_DATA_PROJECTION_MAP.put(ChannelData.ITEM_ID,
                ChannelData.ITEM_ID);

        CHANNELS_DATA_PROJECTION_MAP.put(ChannelData.CONTENT,
                ChannelData.CONTENT);
        CHANNELS_DATA_PROJECTION_MAP.put(ChannelData.CONTENT_TYPE,
                ChannelData.CONTENT_TYPE);

        CHANNELS_DATA_PROJECTION_MAP.put(ChannelData.AUTHOR,
                ChannelData.AUTHOR);
        CHANNELS_DATA_PROJECTION_MAP.put(ChannelData.AUTHOR_JID,
                ChannelData.AUTHOR_JID);
        CHANNELS_DATA_PROJECTION_MAP.put(ChannelData.AUTHOR_AFFILIATION,
                ChannelData.AUTHOR_AFFILIATION);

        CHANNELS_DATA_PROJECTION_MAP.put(ChannelData.GEOLOC_LAT,
                ChannelData.GEOLOC_LAT);
        CHANNELS_DATA_PROJECTION_MAP.put(ChannelData.GEOLOC_LON,
                ChannelData.GEOLOC_LON);
        CHANNELS_DATA_PROJECTION_MAP.put(ChannelData.GEOLOC_ACCURACY,
                ChannelData.GEOLOC_ACCURACY);

        CHANNELS_DATA_PROJECTION_MAP.put(ChannelData.GEOLOC_LOCALITY,
                ChannelData.GEOLOC_LOCALITY);
        CHANNELS_DATA_PROJECTION_MAP.put(ChannelData.GEOLOC_REGION,
                ChannelData.GEOLOC_REGION);
        CHANNELS_DATA_PROJECTION_MAP.put(ChannelData.GEOLOC_AREA,
                ChannelData.GEOLOC_AREA);
        CHANNELS_DATA_PROJECTION_MAP.put(ChannelData.GEOLOC_COUNTRY,
                ChannelData.GEOLOC_COUNTRY);

        CHANNELS_DATA_PROJECTION_MAP.put(ChannelData.GEOLOC_TEXT,
                ChannelData.GEOLOC_TEXT);
        CHANNELS_DATA_PROJECTION_MAP.put(ChannelData.GEOLOC_TYPE,
                ChannelData.GEOLOC_TYPE);

        CHANNELS_DATA_PROJECTION_MAP.put(ChannelData.PUBLISHED,
                ChannelData.PUBLISHED);

        CHANNELS_DATA_PROJECTION_MAP.put(ChannelData.CACHE_UPDATE_TIMESTAMP,
                ChannelData.CACHE_UPDATE_TIMESTAMP);

        ROSTER_PROJECTION_MAP.put(Roster._ID, Roster._ID);
        ROSTER_PROJECTION_MAP.put(Roster._COUNT, Roster._COUNT);
        ROSTER_PROJECTION_MAP.put(Roster.JID, Roster.JID);
        ROSTER_PROJECTION_MAP.put(Roster.STATUS, Roster.STATUS);
        ROSTER_PROJECTION_MAP.put(Roster.NAME, Roster.NAME);
        ROSTER_PROJECTION_MAP.put(Roster.GEOLOC, Roster.GEOLOC);
        ROSTER_PROJECTION_MAP.put(Roster.GEOLOC_NEXT, Roster.GEOLOC_NEXT);
        ROSTER_PROJECTION_MAP.put(Roster.GEOLOC_PREV, Roster.GEOLOC_PREV);
        ROSTER_PROJECTION_MAP.put(Roster.CACHE_UPDATE_TIMESTAMP,
                Roster.CACHE_UPDATE_TIMESTAMP);
        

        PLACES_PROJECTION_MAP.put(Places._ID, Places._ID);
        PLACES_PROJECTION_MAP.put(Places._COUNT, Places._COUNT);
        PLACES_PROJECTION_MAP.put(Places.AREA, Places.AREA);
        PLACES_PROJECTION_MAP.put(Places.COUNTRY, Places.COUNTRY);
        PLACES_PROJECTION_MAP.put(Places.DESCRIPTION, Places.DESCRIPTION);
        PLACES_PROJECTION_MAP.put(Places.LAT, Places.LAT);
        PLACES_PROJECTION_MAP.put(Places.LON, Places.LON);
        PLACES_PROJECTION_MAP.put(Places.NAME, Places.NAME);
        PLACES_PROJECTION_MAP.put(Places.PLACE_ID, Places.PLACE_ID);
        PLACES_PROJECTION_MAP.put(Places.POPULATION, Places.POPULATION);
        PLACES_PROJECTION_MAP.put(Places.POSTALCODE, Places.POPULATION);
        PLACES_PROJECTION_MAP.put(Places.REGION, Places.REGION);
        PLACES_PROJECTION_MAP.put(Places.REVISION, Places.REVISION);
        PLACES_PROJECTION_MAP.put(Places.SHARED, Places.SHARED);
        PLACES_PROJECTION_MAP.put(Places.CACHE_UPDATE_TIMESTAMP,
                Places.CACHE_UPDATE_TIMESTAMP);

    }
}
