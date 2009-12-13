package com.buddycloud.android.buddydroid;

import java.util.Iterator;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;

import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.buddycloud.android.buddydroid.provider.BuddyCloud.Roster;
import com.buddycloud.jbuddycloud.BuddycloudClient;
import com.buddycloud.jbuddycloud.packet.LocationEvent;
import com.buddycloud.jbuddycloud.provider.PubSubLocationEventProvider;

public class BuddycloudService extends Service {

    private static final String TAG = "Service";
    private BuddycloudClient mConnection;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, " onCreate");
        System.setProperty("smack.debugEnabled", "true");
    }

    public void createConnection() {
        if (mConnection != null && mConnection.isConnected()) {
           return;
        }

        SharedPreferences pm =
            PreferenceManager.getDefaultSharedPreferences(this);

        String jid = pm.getString("jid", null);
        if (jid.indexOf('@') == -1) {
            return;
        }
        String password = pm.getString("password", null);

        mConnection = BuddycloudClient.createBuddycloudClient(
            jid,
            password,
            null, null, null
        );

    }

    public void configureConnection() {
        if (mConnection == null || !mConnection.isAuthenticated()) {
            return;
        }
        mConnection.addPacketListener(new PacketListener() {

            @Override
            public void processPacket(Packet packet) {
                if (packet instanceof Message) {
                    LocationEvent loc = (LocationEvent) packet.getExtension(PubSubLocationEventProvider.getNS());
                    if (loc != null) {
                        Log.d(TAG, "GEOLOC received: "+packet.getFrom()+" -> "+loc.text);
                        ContentValues values = new ContentValues();
                        switch (loc.type) {
                        case LocationEvent.CURRENT:
                            values.put(Roster.GEOLOC, loc.text);
                            break;
                        case LocationEvent.PREV:
                            values.put(Roster.GEOLOC_PREV, loc.text);
                            break;
                        case LocationEvent.NEXT:
                            values.put(Roster.GEOLOC_NEXT, loc.text);
                            break;
                        }
                        getContentResolver().update(Roster.CONTENT_URI, values, Roster.JID+"='"+packet.getFrom()+"'", null);
                    }
                }
            }}, null);
    }

    public void updateRoaster() {
        if (mConnection == null ||
           !mConnection.isConnected() ||
           !mConnection.isAuthenticated()
        ) {
            return;
        }

        getContentResolver().delete(Roster.CONTENT_URI, null, null);
        Iterator iterator = mConnection.getRoster().getEntries().iterator();
        while (iterator.hasNext()) {
            RosterEntry buddy = ((RosterEntry) iterator.next());
            ContentValues values = new ContentValues();
            values.put(Roster.JID, buddy.getUser());
            values.put(Roster.NAME, buddy.getUser().split("\\@")[0]);
            getContentResolver().insert(Roster.CONTENT_URI, values);
        }
        getContentResolver().notifyChange(Roster.CONTENT_URI, null);
    }

        @Override
        public void onStart(Intent intent, int startId) {
            Log.d(TAG, " onStart");
            super.onStart(intent, startId);

            if (mConnection == null ||
                !mConnection.isConnected() ||
                !mConnection.isAuthenticated()
            ) {
                createConnection();
                if (mConnection != null && mConnection.isAuthenticated()) {
                    Toast.makeText(this, "You are online!", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Login failed :(", Toast.LENGTH_LONG).show();
                    return;
                }
                configureConnection();
                updateRoaster();
            }
        }

    @Override
    public void onDestroy() {
        Log.d(TAG, " onDestroy");
        super.onDestroy();
        if (mConnection == null || !mConnection.isConnected()) {
            return;
        }
        mConnection.disconnect();
        Log.i(TAG, "disonnected.");
    }

        @Override
        public IBinder onBind(Intent intent) {
                // TODO Auto-generated method stub
                return null;
        }

}
