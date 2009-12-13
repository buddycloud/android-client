package com.buddycloud.android.buddydroid;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
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
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.telephony.CellLocation;
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

        getContentResolver().delete(Roster.CONTENT_URI, null, null);
        // insert yourself into roster ;) dirty ugly stupid for now :P
        ContentValues values = new ContentValues();
        String jid = PreferenceManager.getDefaultSharedPreferences(this)
                     .getString("jid", "");
        values.put(Roster.JID, jid);
        values.put(Roster.NAME, jid.substring(0, jid.indexOf('@')));
        getContentResolver().insert(Roster.CONTENT_URI, values);

        Iterator iterator = mConnection.getRoster().getEntries().iterator();
        while (iterator.hasNext()) {
            RosterEntry buddy = ((RosterEntry) iterator.next());
            values.clear();
            values.put(Roster.JID, buddy.getUser());
            values.put(Roster.NAME, buddy.getUser().split("\\@")[0]);
            getContentResolver().insert(Roster.CONTENT_URI, values);
        }
        getContentResolver().notifyChange(Roster.CONTENT_URI, null);
        Log.d(TAG, "inserted roster");
    }

    private long beaconLogTimer;

    public void sendBeaconLog(final boolean force) {
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
                if (delta < 180000 && !force) {
                    return;
                }
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
                        sendBeaconLog(true);
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

        taskQueue = null;
        taskQueue.clear();

        Log.i(TAG, "disonnected.");
    }

        @Override
        public IBinder onBind(Intent intent) {
                // TODO Auto-generated method stub
                return null;
        }

}
