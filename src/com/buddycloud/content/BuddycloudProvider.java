/**
 * 
 */
package com.buddycloud.content;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

import com.buddycloud.content.BuddyCloud.ChannelData;
import com.buddycloud.content.BuddyCloud.Places;
import com.buddycloud.content.BuddyCloud.Roster;

/**
 * @author zero
 * 
 */

public class BuddycloudProvider extends ContentProvider {

    private static UriMatcher URI_MATCHER;

    private static final int CHANNEL_DATA = 103;

    private static final int ROSTER = 201;
    private static final int ROSTER_VIEW = 202;

    private static final int PLACES = 301;

    public static final String TABLE_CHANNEL_DATA = "channeldata";
    public static final String TABLE_ROSTER = "roster";
    public static final String TABLE_PLACES = "places";

    // private SQLiteOpenHelper mOpenHelper;
    DatabaseHelper mOpenHelper;

    public static final String TAG = "Provider";
    private static final String DATABASE_NAME = "buddycloud.db";

    /**
     * Version of database.
     * 
     * The various versions were introduced in the following releases:
     * 
     * 1: Release 0.0.1
     */
    private static final int DATABASE_VERSION = 11;

    static class DatabaseHelper extends SQLiteOpenHelper {
        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + TABLE_CHANNEL_DATA + " ( "

                    + ChannelData._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
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
                    + ChannelData.UNREAD + " BOOLEAN DEFAULT 0,"

                    + ChannelData.CACHE_UPDATE_TIMESTAMP + " LONG,"
                    + ChannelData._COUNT + " LONG,"
                    + "UNIQUE(" // CONSTRAIN
                        + ChannelData.ITEM_ID + ","
                        + ChannelData.NODE_NAME
                    + ")"
                    + ");"

            );

            db.execSQL("CREATE TABLE " + TABLE_ROSTER
                    + " ("
                    + Roster._ID + " INTEGER PRIMARY KEY,"
                    + Roster.JID + " VARCHAR,"
                    + Roster.SELF + " INTEGER DEFAULT 0,"
                    + Roster.NAME + " VARCHAR,"
                    + Roster.ENTRYTYPE + " VARCHAR,"
                    + Roster.STATUS + " VARCHAR,"
                    + Roster.GEOLOC + " VARCHAR,"
                    + Roster.GEOLOC_NEXT + " VARCHAR,"
                    + Roster.GEOLOC_PREV + " VARCHAR,"
                    + Roster.LAST_UPDATED + " LONG,"
                    + Roster.UNREAD_MESSAGES +  " INTEGER DEFAULT 0,"
                    + Roster.UNREAD_REPLIES + " INTEGER DEFAULT 0,"
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
        public void onUpgrade(
            SQLiteDatabase db,
            int oldVersion,
            int newVersion
        ) {

            if (newVersion == oldVersion) {
                return;
            }

            // bc data is channel data, easy to refetch, so drop is the
            // easiest atm

            db.execSQL("DROP TABLE " + TABLE_ROSTER + ";");
            db.execSQL("DROP TABLE " + TABLE_CHANNEL_DATA + ";");
            db.execSQL("DROP TABLE " + TABLE_PLACES + ";");
            onCreate(db);
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

        default:
            Log.w(TAG, "no type handler for " + uri);

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
        switch (URI_MATCHER.match(uri)) {

        case CHANNEL_DATA:
            return ChannelDataHelper.insert(uri, values, this);

        case ROSTER:
            return RosterHelper.insert(values, this);

        default:
            Log.d(TAG, "no insert handler for " + uri);

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

        switch (what) {

        case CHANNEL_DATA:
            return ChannelDataHelper.queryChannelData(uri, projection,
                    selection, selectionArgs, sortOrder, this);

        case ROSTER_VIEW:
            return RosterHelper.queryRosterView(this);

        case ROSTER:
            return RosterHelper.queryRoster(projection,
                    selection, selectionArgs, sortOrder, this);
        }
        return null;
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
        int what = URI_MATCHER.match(uri);

        switch (what) {
        case ROSTER:
            return RosterHelper.update(values, selection, selectionArgs, this);

        case CHANNEL_DATA:
            return ChannelDataHelper
                        .update(values, selection, selectionArgs, this);

        default:
            Log.w(TAG, "no update handler for " + uri);

        }

        return -1;
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.content.ContentProvider#delete(android.net.Uri,
     * java.lang.String, java.lang.String[])
     */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int what = URI_MATCHER.match(uri);
        switch (what) {
        case ROSTER:
            return RosterHelper.delete(selection, selectionArgs, this);
        default:
            Log.w(TAG, "no delete handler for " + uri);
        }
        return 0;
    }

    static {

        URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
        URI_MATCHER.addURI("com.buddycloud", "channeldata", CHANNEL_DATA);

        URI_MATCHER.addURI("com.buddycloud", "roster", ROSTER);
        URI_MATCHER.addURI("com.buddycloud", "roster_view", ROSTER_VIEW);

        URI_MATCHER.addURI("com.buddycloud", "places", PLACES);

    }

}
