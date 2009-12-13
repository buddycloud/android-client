/**
 * 
 */
package com.buddycloud.android.buddydroid.provider;

import java.util.HashMap;
import java.util.Map;

import org.openintents.intents.ProviderIntents;

import com.buddycloud.android.buddydroid.provider.BuddyCloud.CacheColumns;
import com.buddycloud.android.buddydroid.provider.BuddyCloud.ChannelData;
import com.buddycloud.android.buddydroid.provider.BuddyCloud.Channels;
import com.buddycloud.android.buddydroid.provider.BuddyCloud.Places;
import com.buddycloud.android.buddydroid.provider.BuddyCloud.Roster;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.util.Log;

/**
 * @author zero
 * 
 */

public class BuddycloudProvider extends ContentProvider {

    private static UriMatcher URI_MATCHER;

    private static final int CHANNELS = 101;
    private static final int CHANNELS_ID = 102;
    private static final int CHANNEL_DATA = 103;
    private static final int CHANNEL_DATA_ID = 104;

    private static final int ROSTER = 201;
    private static final int ROSTER_ID = 202;

    private static final int PLACES = 301;
    private static final int PLACES_ID = 302;

    private static final String TABLE_CHANNELS = "channels";
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
            db.execSQL("CREATE TABLE " + TABLE_CHANNELS
                    + " (_id INTEGER PRIMARY KEY," + Channels.NODE_NAME
                    + " VARCHAR," + Channels.TITLE + " VARCHAR,"
                    + Channels.DESCRIPTION + " VARCHAR,"
                    + Channels.CACHE_UPDATE_TIMESTAMP + " LONG,"
                    + ChannelData._COUNT + " LONG" + ");");

            db.execSQL("CREATE TABLE " + TABLE_CHANNEL_DATA
                    + " (_id INTEGER PRIMARY KEY," + ChannelData.ITEM_ID
                    + " VARCHAR," + ChannelData.ITEM_AUTHOR + " VARCHAR,"
                    + ChannelData.CONTENT_TYPE + " VARCHAR,"
                    + ChannelData.CONTENT + " VARHCHAR,"
                    + ChannelData.PUBLISHED + " VARCHAR,"
                    + ChannelData.NODE_NAME + " VARCHAR,"
                    + ChannelData.GEOLOC_ACCURACY + " FLOAT,"
                    + ChannelData.GEOLOC_AREA + " VARCHAR,"
                    + ChannelData.GEOLOC_COUNTRY + " VARCHAR,"
                    + ChannelData.GEOLOC_LAT + " VARCHAR,"
                    + ChannelData.GEOLOC_LON + " VARCHAR,"
                    + ChannelData.GEOLOC_REGION + " VARCHAR,"
                    + ChannelData.GEOLOC_TEXT + " VARCHAR,"
                    + ChannelData.GEOLOC_TIMESTAMP + " VARCHAR,"
                    + ChannelData.CACHE_UPDATE_TIMESTAMP + " LONG"
                    + ChannelData._COUNT + " LONG" + ");"

            );

            db.execSQL("CREATE TABLE " + TABLE_ROSTER
                    + " (_id INTEGER PRIMARY KEY," + Roster.JID + " VARCHAR,"
                    + Roster.NAME + " VARCHAR," + Roster.STATUS + " VARCHAR,"
                    + Roster.GEOLOC + " VARCHAR," + Roster.GEOLOC_NEXT
                    + " VARCHAR," + Roster.GEOLOC_PREV + " VARCHAR,"
                    + Roster._COUNT + " LONG," + Roster.CACHE_UPDATE_TIMESTAMP
                    + " LONG" + ");");

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

        case CHANNELS:

        case CHANNELS_ID:

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

        case CHANNELS:
            rowID = db.insert(TABLE_CHANNELS, Channels.NODE_NAME, values);
            uri = ContentUris.withAppendedId(Channels.CONTENT_URI, rowID);
            break;

        case CHANNEL_DATA:
            rowID = db.insert(TABLE_CHANNEL_DATA, ChannelData.ITEM_ID, values);
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

        case CHANNELS:
            qb.setTables(TABLE_CHANNELS);
//            qb.setProjectionMap(CHANNELS_PROJECTION_MAP);

            dummyCursor = new MatrixCursor(BuddyCloud.Channels.PROJECTION_MAP);

            dummyCursor.addRow(new String[] { "DUMMYCHANNEL", "DUMMY TITLE",
                    "SOME DESCRIPTION", "1" });
            break;
        case CHANNELS_ID:
            qb.setTables(TABLE_CHANNELS);
            qb.appendWhere("_id=" + uri.getPathSegments().get(1));

            dummyCursor = new MatrixCursor(BuddyCloud.Channels.PROJECTION_MAP);

            dummyCursor.addRow(new String[] { "DUMMYCHANNEL", "DUMMY TITLE",
                    "SOME DESCRIPTION", "1" });
            break;
        case CHANNEL_DATA:
            qb.setTables(TABLE_CHANNEL_DATA);
            qb.setProjectionMap(CHANNELS_DATA_PROJECTION_MAP);

            dummyCursor = new MatrixCursor(
                    BuddyCloud.ChannelData.PROJECTION_MAP);

            dummyCursor.addRow(new String[] { "ChannelData.NODE_NAME",
                    "ChannelData._ID", "ChannelData.CACHE_UPDATE_TIMESTAMP",
                    "ChannelData.CONTENT", "ChannelData.CONTENT_TYPE",
                    "ChannelData.GEOLOC_ACCURACY", "ChannelData.GEOLOC_AREA",
                    "ChannelData.GEOLOC_COUNTRY", "ChannelData.GEOLOC_LAT",
                    "ChannelData.GEOLOC_LON", "ChannelData.GEOLOC_LOCALITY",
                    "ChannelData.GEOLOC_REGION", "ChannelData.GEOLOC_TEXT",
                    "ChannelData.GEOLOC_TIMESTAMP", "ChannelData.ITEM_AUTHOR",
                    "ChannelData.ITEM_ID", "ChannelData.PUBLISHED"

            });
            break;
        case CHANNEL_DATA_ID:
            qb.setTables(TABLE_CHANNEL_DATA);
            qb.appendWhere("_id=" + uri.getPathSegments().get(1));

            dummyCursor = new MatrixCursor(
                    BuddyCloud.ChannelData.PROJECTION_MAP);

            dummyCursor.addRow(new String[] { "ChannelData.NODE_NAME",
                    "ChannelData._ID", "ChannelData.CACHE_UPDATE_TIMESTAMP",
                    "ChannelData.CONTENT", "ChannelData.CONTENT_TYPE",
                    "ChannelData.GEOLOC_ACCURACY", "ChannelData.GEOLOC_AREA",
                    "ChannelData.GEOLOC_COUNTRY", "ChannelData.GEOLOC_LAT",
                    "ChannelData.GEOLOC_LON", "ChannelData.GEOLOC_LOCALITY",
                    "ChannelData.GEOLOC_REGION", "ChannelData.GEOLOC_TEXT",
                    "ChannelData.GEOLOC_TIMESTAMP", "ChannelData.ITEM_AUTHOR",
                    "ChannelData.ITEM_ID", "ChannelData.PUBLISHED"

            });

            break;
        case ROSTER:
//            qb.setTables(TABLE_ROSTER);
//            qb.setProjectionMap(ROSTER_PROJECTION_MAP);
            Cursor c = mOpenHelper.getReadableDatabase().rawQuery("SELECT " +
            	"_id, jid, name, status, geoloc_prev, geoloc, geoloc_next, " +
            	"jid='"+PreferenceManager.getDefaultSharedPreferences(
            	        getContext()).getString("jid","")+"' AS itsMe " +
            	"FROM roster ORDER BY itsMe DESC, cache_update_timestamp DESC"
            	, null);
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

        case CHANNELS:

        case CHANNELS_ID:

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
        case CHANNELS:
            break;
        case CHANNEL_DATA:
            break;
        case ROSTER:
            mOpenHelper.getWritableDatabase().execSQL(
                    "DELETE FROM " + TABLE_ROSTER);
            Log.d(TAG, "deleted roster");
            break;
        }
        return 0;
    }

    private static HashMap<String, String> PLACES_PROJECTION_MAP = new HashMap<String, String>();

    private static HashMap<String, String> ROSTER_PROJECTION_MAP = new HashMap<String, String>();
    private static HashMap<String, String> CHANNELS_PROJECTION_MAP = new HashMap<String, String>();
    static {

        URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
        URI_MATCHER.addURI("com.buddycloud", "channels", CHANNELS);
        URI_MATCHER.addURI("com.buddycloud", "channels/#", CHANNELS_ID);
        URI_MATCHER.addURI("com.buddycloud", "channeldata", CHANNEL_DATA);
        URI_MATCHER.addURI("com.buddycloud", "channeldata/#", CHANNEL_DATA_ID);

        URI_MATCHER.addURI("com.buddycloud", "roster", ROSTER);
        URI_MATCHER.addURI("com.buddycloud", "roster/#", ROSTER_ID);

        URI_MATCHER.addURI("com.buddycloud", "places", PLACES);
        URI_MATCHER.addURI("com.buddycloud", "places/#", PLACES_ID);

        CHANNELS_PROJECTION_MAP.put(Channels._ID, Channels._ID);
        CHANNELS_PROJECTION_MAP.put(Channels._COUNT, Channels._COUNT);
        CHANNELS_PROJECTION_MAP.put(Channels.NODE_NAME, Channels.NODE_NAME);
        CHANNELS_PROJECTION_MAP.put(Channels.TITLE, Channels.TITLE);
        CHANNELS_PROJECTION_MAP.put(Channels.DESCRIPTION, Channels.DESCRIPTION);
        CHANNELS_PROJECTION_MAP.put(Channels.CACHE_UPDATE_TIMESTAMP,
                Channels.CACHE_UPDATE_TIMESTAMP);

        CHANNELS_DATA_PROJECTION_MAP.put(ChannelData._ID, ChannelData._ID);
        CHANNELS_DATA_PROJECTION_MAP.put(ChannelData.NODE_NAME,
                ChannelData.NODE_NAME);
        CHANNELS_DATA_PROJECTION_MAP.put(ChannelData.CONTENT,
                ChannelData.CONTENT);
        CHANNELS_DATA_PROJECTION_MAP.put(ChannelData.CONTENT_TYPE,
                ChannelData.CONTENT_TYPE);
        CHANNELS_DATA_PROJECTION_MAP.put(ChannelData.ITEM_AUTHOR,
                ChannelData.ITEM_AUTHOR);
        CHANNELS_DATA_PROJECTION_MAP.put(ChannelData.ITEM_ID,
                ChannelData.ITEM_ID);
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
        CHANNELS_DATA_PROJECTION_MAP.put(ChannelData.GEOLOC_TEXT,
                ChannelData.GEOLOC_TEXT);
        CHANNELS_DATA_PROJECTION_MAP.put(ChannelData.GEOLOC_COUNTRY,
                ChannelData.GEOLOC_COUNTRY);
        CHANNELS_DATA_PROJECTION_MAP.put(ChannelData.GEOLOC_TIMESTAMP,
                ChannelData.GEOLOC_TIMESTAMP);
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
