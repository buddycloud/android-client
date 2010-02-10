package com.buddycloud.android.buddydroid;

import java.util.LinkedList;

import org.jivesoftware.smack.packet.IQ;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;

import com.buddycloud.android.buddydroid.collector.CellListener;
import com.buddycloud.android.buddydroid.collector.NetworkListener;
import com.buddycloud.android.buddydroid.sync.ChannelSync;
import com.buddycloud.android.buddydroid.sync.RoasterSync;
import com.buddycloud.jbuddycloud.BuddycloudClient;
import com.buddycloud.jbuddycloud.packet.BeaconLog;
import com.buddycloud.jbuddycloud.packet.PlainPacket;

public class BuddycloudService extends Service {

    static final String TAG = "BCService";
    private BuddycloudClient mConnection;
    private BuddycloudService service = this;

    private CellListener cellListener = null;
    private NetworkListener networkListener = null;

    private TaskQueueThread taskQueue;

    private volatile ConnectionThread connectionThread = null;

    private LinkedList<IBuddycloudServiceListener> listeners =
        new LinkedList<IBuddycloudServiceListener>();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, " onCreate");
        cellListener = new CellListener(this);
        networkListener = new NetworkListener(this);

        if (connectionThread != null || mConnection != null) {
            return;
        }

        // check for cached credentials. If available, use it!
        SharedPreferences preferences =
            PreferenceManager.getDefaultSharedPreferences(this);
        String jid = preferences.getString("jid", null);
        String username = preferences.getString("username", null);
        String password = preferences.getString("password", null);
        String host = preferences.getString("host", null);
        Integer port = preferences.getInt("port", -1);
        if (port == -1) { port = null; }
        if (jid != null && password != null) {
            connectionThread = new ConnectionThread(
                jid, username, password, host, port, false, service
            );
        }
    }

    public void updateRoaster() {
        if (mConnection == null ||
           !mConnection.isConnected() ||
           !mConnection.isAuthenticated()
        ) {
            return;
        }
        new RoasterSync(mConnection, getContentResolver());
    }

    public void updateChannels() {
        if (mConnection == null ||
           !mConnection.isConnected() ||
           !mConnection.isAuthenticated()
        ) {
            return;
        }
        new ChannelSync(mConnection, getContentResolver());
    }

    private long beaconLogTimer;
    private int internalPriority;

    /**
     * Send a beacon log. Priority is 0..10, with 0 meaning instant and
     * 10 meaning "if 3 min. have passed".
     * @param prio
     * @throws InterruptedException 
     */
    public boolean sendBeaconLog(final int prio) throws InterruptedException {
        final int priority = Math.min(internalPriority, prio);
        return taskQueue.add(new Runnable() {
            public void run() {
                if (mConnection == null ||
                    !mConnection.isAuthenticated()
                ) {
                    return;
                }
                long now = System.currentTimeMillis();
                long delta = now - beaconLogTimer;
                if (delta < 30000 * priority) {
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
                networkListener.appendTo(log);
                mConnection.sendPacket(log);
            }
        });
    }

    public boolean send(final IQ iq) throws InterruptedException {
        return taskQueue.add(new Runnable() {
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

        if (taskQueue == null) {
            taskQueue = new TaskQueueThread();
            taskQueue.start();
        }

        try {
            if (!taskQueue.add(new Runnable() {

                public void run() {
                    if (mConnection == null ||
                            !mConnection.isConnected() ||
                            !mConnection.isAuthenticated()
                        ) {
                            cellListener.start();

                        }
                }
            })) {
                Log.d(TAG, "Failed to start service");
            }
        } catch (InterruptedException e) {
            Log.d(TAG, e.getMessage(), e);
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, " onDestroy");
        super.onDestroy();

        cellListener.stop();

        try {
            taskQueue.add(new Runnable() {
                public void run() {
                    synchronized (TAG) {
                        if (mConnection != null && mConnection.isConnected()) {
                            mConnection.disconnect();
                            mConnection = null;
                        }
                    }
                }
            });
        } catch (InterruptedException e) {
            // Doesn't matter
        }

        taskQueue.stopQueue();
        taskQueue = null;

        synchronized (TAG) {
            if (mConnection != null && mConnection.isConnected()) {
                mConnection.disconnect();
                mConnection = null;
            }
        }

        Log.i(TAG, "disonnected.");
    }

    private final IBuddycloudService.Stub binder =
        new IBuddycloudService.Stub() {

            public boolean isConnected() throws RemoteException {
                return (mConnection != null) && mConnection.isConnected();
            }

            public void send(String rawXml)
                    throws RemoteException {
                mConnection.sendPacket(new PlainPacket(rawXml));
            }

            public String getJidWithResource() throws RemoteException {
                return mConnection.getUser();
            }

            public void createAccount(String username, String password)
                throws RemoteException {
                synchronized (service) {
                    if (connectionThread != null) {
                        connectionThread.setStop(true);
                        connectionThread.interrupt();
                    }
                    connectionThread = new ConnectionThread(
                        username, null, password, null, null, true, service
                    );
                }
            }


            public void login(String username, String password)
                    throws RemoteException {
                synchronized (service) {
                    if (connectionThread != null) {
                        connectionThread.setStop(true);
                        connectionThread.interrupt();
                    }
                    connectionThread = new ConnectionThread(
                        username, null, password, null, null, false, service
                    );
                }
            }

            public void loginAnonymously() throws RemoteException {
                synchronized (service) {
                    if (connectionThread != null) {
                        connectionThread.setStop(true);
                        connectionThread.interrupt();
                    }
                    connectionThread = new ConnectionThread(
                        null, null, null, null, null, false, service
                    );
                }
            }

            public boolean isAnonymous() throws RemoteException {
                return mConnection != null && mConnection.isConnected() &&
                    mConnection.isAnonymous();
            }

            public boolean isAuthenticated() throws RemoteException {
                return mConnection != null && mConnection.isConnected() &&
                    mConnection.isAuthenticated();
            }

            public void addListener(IBuddycloudServiceListener listener)
                    throws RemoteException {
                synchronized (this) {
                    if (listener.asBinder().isBinderAlive()) {
                        listeners.add(listener);
                    }
                }
            }
        };

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void setClient(BuddycloudClient client, String password) {
        synchronized (this) {
            if (mConnection != null) {
                try {
                    mConnection.disconnect();
                } catch (Exception e) {
                    // we just throw the connection away
                }
            }
            mConnection = client;
            if (!mConnection.isAnonymous()) {
                SharedPreferences preferences =
                    PreferenceManager.getDefaultSharedPreferences(this);
                String jid = client.getUser();
                if (jid.indexOf('/') != -1) {
                    jid = jid.substring(0, jid.indexOf('/'));
                }
                preferences.edit().putString("jid", jid);
                preferences.edit().putString(
                        "username", client.getLoginUsername()
                );
                preferences.edit().putString("password", password);
                preferences.edit().putString("host", client.getHost());
                preferences.edit().putInt("port", client.getPort());
            }
            mConnection.addGeoLocListener(
                new ConnectionBCGeolocListener(getContentResolver())
            );
            mConnection.addAtomListener(
                new BCConnectionAtomListener(getContentResolver())
            );

            LinkedList<IBuddycloudServiceListener> remove = null;
            for (IBuddycloudServiceListener listener: listeners) {
                if (listener.asBinder().isBinderAlive()) {
                    try {
                        listener.onBCConnected();
                    } catch (RemoteException e) {
                        if (remove == null) {
                            remove = new LinkedList<IBuddycloudServiceListener>();
                        }
                        remove.add(listener);
                    }
                } else {
                    // remove listener
                    if (remove == null) {
                        remove = new LinkedList<IBuddycloudServiceListener>();
                    }
                    remove.add(listener);
                }
            }
            if (remove != null) {
                listeners.removeAll(remove);
            }

            updateRoaster();
            updateChannels();
        }
    }

    public void connectionFailed(Exception e) {
        synchronized (this) {
            if (mConnection != null && mConnection.isConnected()) {
                // multithreading? We may be connected before all
                // attempts come back...
                // So we ignore the problem if we have a working connection.
                return;
            }
            LinkedList<IBuddycloudServiceListener> remove = null;
            for (IBuddycloudServiceListener listener: listeners) {
                if (listener.asBinder().isBinderAlive()) {
                    try {
                        listener.onBCLoginFailed();
                    } catch (RemoteException ex) {
                        if (remove == null) {
                            remove = new LinkedList<IBuddycloudServiceListener>();
                        }
                        remove.add(listener);
                    }
                } else {
                    // remove listener
                    if (remove == null) {
                        remove = new LinkedList<IBuddycloudServiceListener>();
                    }
                    remove.add(listener);
                }
            }
            if (remove != null) {
                listeners.removeAll(remove);
            }
        }
    }

}
