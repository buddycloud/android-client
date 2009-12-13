package com.buddycloud.android.buddydroid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;

import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.buddycloud.android.buddydroid.provider.BuddyCloud.Roster;
import com.buddycloud.jbuddycloud.BuddycloudClient;
import com.buddycloud.jbuddycloud.packet.BeaconLog;
import com.buddycloud.jbuddycloud.packet.LocationEvent;
import com.buddycloud.jbuddycloud.provider.PubSubLocationEventProvider;

public class BuddycloudService extends Service {

    private static final String TAG = "Service";
    private BuddycloudClient mConnection;
    private BuddycloudService service = this;
    private CellListener cellListener = null;

    private ArrayBlockingQueue<Runnable> taskQueue;

    private Thread bgExecutor = new Thread() {
        public void run() {
            while (taskQueue != null) {
                try {
                    Runnable runnable = taskQueue.poll(60, TimeUnit.SECONDS);
                    if (runnable != null) {
                        runnable.run();
                    }
                } catch (Exception e) {
                    Log.d(TAG, e.toString());
                }
            }
        }
    };

    private Handler toastHandler = new Handler() {

        @Override
        public void handleMessage(android.os.Message msg) {
            super.handleMessage(msg);
            Toast.makeText(service, msg.getData().get("msg").toString(),
                    Toast.LENGTH_LONG).show();
        }

    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, " onCreate");
        System.setProperty("smack.debugEnabled", "true");
        taskQueue = new ArrayBlockingQueue<Runnable>(5);
        bgExecutor.start();
        TelephonyManager telephonyManager =
            (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        cellListener = new CellListener(this);
        telephonyManager.listen(
            cellListener,
            PhoneStateListener.LISTEN_CELL_LOCATION |
            PhoneStateListener.LISTEN_SIGNAL_STRENGTH
        );
    }

    public void createConnection() {
        if (mConnection != null && mConnection.isConnected()) {
            return;
        }

        SharedPreferences pm =
            PreferenceManager.getDefaultSharedPreferences(this);

        String jid = pm.getString("jid", null);

        if (jid != null && jid.indexOf('@') == -1) {
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
                        getContentResolver().update(Roster.CONTENT_URI, values,
                                Roster.JID + "='" + packet.getFrom() + "'",
                                null);
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

        Log.d("Roster", "read roaster");
        long time = -System.currentTimeMillis();

        ArrayList<ContentValues> newEntries = new ArrayList<ContentValues>();

        HashMap<String, String> oldRoster = new HashMap<String, String>();
        Cursor query = getContentResolver().query(
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

        String jid = PreferenceManager.getDefaultSharedPreferences(this)
                     .getString("jid", "");
        ContentValues values = new ContentValues();
        if (oldRoster.containsKey(jid)) {
            oldRoster.remove(jid);
        } else {
            values.put(Roster.JID, jid);
            values.put(Roster.NAME, jid.substring(0, jid.lastIndexOf('@')));
            newEntries.add(values);
        }

        Log.d("Roster", "fetch new roster");

        Iterator iterator = mConnection.getRoster().getEntries().iterator();
        while (iterator.hasNext()) {
            RosterEntry buddy = ((RosterEntry) iterator.next());
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
                    getContentResolver().update(
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
        getContentResolver().bulkInsert(
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
            getContentResolver().delete(Roster.CONTENT_URI, where.toString(),
                    oldRoster.keySet().toArray(new String[oldRoster.size()]));
        }

        time += System.currentTimeMillis();

        getContentResolver().notifyChange(Roster.CONTENT_URI, null);
        Log.d(TAG, "updated roster in " + time + "ms");
    }

    private long beaconLogTimer;
    private int internalPriority;

    /**
     * Send a beacon log. Priority is 0..10, with 0 meaning instant and
     * 10 meaning "if 3 min. have passed".
     * @param prio
     */
    public void sendBeaconLog(final int prio) {
        final int priority = Math.min(internalPriority, prio);
        taskQueue.add(new Runnable() {
            @Override
            public void run() {
                if (mConnection == null ||
                    !mConnection.isAuthenticated()
                ) {
                    return;
                }
                long now = System.currentTimeMillis();
                long delta = now - beaconLogTimer;
                if (delta < 18000 * priority) {
                    return;
                }
                if (delta < 10000) {
                    internalPriority = priority;
                    return;
                }
                internalPriority = 10;
                beaconLogTimer = now;
                BeaconLog log = new BeaconLog();
                log.setTo("butler.buddycloud.com");
                log.setFrom(mConnection.getUser());
                cellListener.appendTo(log);
                mConnection.sendPacket(log);
            }
        });
    }

    public void send(final IQ iq) {
        taskQueue.add(new Runnable() {
            @Override
            public void run() {
                if (mConnection == null ||
                    !mConnection.isAuthenticated()
                ) {
                    return;
                }
                mConnection.sendPacket(iq);
            }
        });
    }

    @Override
    public void onStart(Intent intent, int startId) {
        Log.d(TAG, " onStart");
        super.onStart(intent, startId);

        taskQueue.add(new Runnable() {

            @Override
            public void run() {
                if (mConnection == null ||
                        !mConnection.isConnected() ||
                        !mConnection.isAuthenticated()
                    ) {
                        createConnection();
                        android.os.Message msg = new android.os.Message();
                        if (mConnection != null && mConnection.isAuthenticated()) {
                            msg.getData().putString("msg", "You are online!");
                            toastHandler.sendMessage(msg);
                        } else {
                            msg.getData().putString("msg", "Login failed :-(");
                            toastHandler.sendMessage(msg);
                            return;
                        }
                        sendBeaconLog(0);
                        configureConnection();
                        updateRoaster();
                    }
            }
        });
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, " onDestroy");
        super.onDestroy();

        TelephonyManager telephonyManager =
            (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        telephonyManager.listen(cellListener, 0);

        taskQueue.add(new Runnable() {
            @Override
            public void run() {
                if (mConnection == null || !mConnection.isConnected()) {
                    return;
                }
                mConnection.disconnect();
            }
        });

        taskQueue.clear();
        taskQueue = null;

        Log.i(TAG, "disonnected.");
    }

        @Override
        public IBinder onBind(Intent intent) {
                // TODO Auto-generated method stub
                return null;
        }

}
