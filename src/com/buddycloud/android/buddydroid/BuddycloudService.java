package com.buddycloud.android.buddydroid;

import java.util.LinkedList;
import java.util.List;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.pubsub.Node;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;

import com.buddycloud.android.buddydroid.collector.CellListener;
import com.buddycloud.android.buddydroid.collector.NetworkListener;
import com.buddycloud.android.buddydroid.provider.BuddyCloud.Roster;
import com.buddycloud.android.buddydroid.sync.ChannelSync;
import com.buddycloud.android.buddydroid.sync.RoasterSync;
import com.buddycloud.jbuddycloud.BuddycloudClient;
import com.buddycloud.jbuddycloud.packet.BeaconLog;
import com.buddycloud.jbuddycloud.packet.ChannelFetch;
import com.buddycloud.jbuddycloud.packet.PlainPacket;
import com.buddycloud.jbuddycloud.packet.channeldiscovery.QueryItem;
import com.buddycloud.jbuddycloud.provider.BCPubSubManager;

public class BuddycloudService extends Service {

    static final String TAG = "BCService";
    private BuddycloudClient mConnection;
    private ActivityPacketListener activityListener;
    private BuddycloudService service = this;

    private CellListener cellListener = null;
    private NetworkListener networkListener = null;

    private TaskQueueThread taskQueue;

    private volatile ConnectionThread connectionThread = null;

    private LinkedList<IBuddycloudServiceListener> listeners =
        new LinkedList<IBuddycloudServiceListener>();

    private long pingTime = 3 * 60 * 1000;
    private long deadTime = 6 * 60 * 1000;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, " onCreate");
        cellListener = new CellListener(this);
        networkListener = new NetworkListener(this);
        activityListener = new ActivityPacketListener();
        final BuddycloudService service = this;
        final BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "Received " + intent);
                synchronized(TAG) {
                    if (connectionThread != null &&
                        connectionThread.isAlive()) {
                        // connection in progress
                        Log.d(TAG, "Connect running");
                        return;
                    }
                    if (mConnection != null && mConnection.isConnected()) {
                        // connected
                        long time = System.currentTimeMillis()
                                  - activityListener.getLastActivity();
                        if (time > pingTime) {
                            if (time < deadTime) {
                                Log.w(TAG, "silence on the line, ping");
                                mConnection.ping();
                            } else {
                                Log.w(TAG, "silence on the line, drop");
                                mConnection.disconnect();
                            }
                            mConnection = null;
                        } else {
                            Log.d(TAG, "test connection");
                            if (mConnection.testConnection()) {
                                Log.d(TAG, "Connection ok");
                                return;
                            }
                        }
                    }
                    SharedPreferences preferences =
                        PreferenceManager.getDefaultSharedPreferences(service);
                    String jid = preferences.getString("jid", null);
                    String username = preferences.getString("username", null);
                    String password = preferences.getString("password", null);
                    if (jid != null && password != null) {
                        Log.d(TAG, "triggering reconnect...");
                        connectionThread = new ConnectionThread(
                            jid, username, password, null, null, false, service
                        );
                    } else {
                        Log.d(TAG, "missing settings");
                    }
                }
            }
        };
        registerReceiver(receiver,
                new IntentFilter("android.intent.action.TIME_TICK"));
        registerReceiver(receiver,
                new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
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
        new ChannelSync(this, mConnection, getContentResolver());
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

    public void updateChannel(String channel) {
        Log.d("BC", "update channel " + channel);
        Cursor cursor = getContentResolver().query(
                Roster.CONTENT_URI,
                new String[]{Roster.LAST_UPDATED},
                "jid=?",
                new String[]{channel},
                null
        );
        if (cursor.getCount() != 1) {
            Log.e("BC", "update channel " + channel + " canceled");
            cursor.close();
            return;
        }
        while (cursor.isBeforeFirst()) { cursor.moveToNext(); }
        long l = cursor.getLong(cursor.getColumnIndex(Roster.LAST_UPDATED));
        cursor.close();
        IQ iq = new ChannelFetch(channel, l);
        try {
            send(iq);
        } catch (InterruptedException e) {
            e.printStackTrace(System.err);
        }
    }

    @Override
    public void onStart(Intent intent, int startId) {
        Log.d(TAG, " onStart");
        super.onStart(intent, startId);

        if (taskQueue == null) {
            taskQueue = new TaskQueueThread();
            taskQueue.start();
        }

        cellListener.start();

        synchronized (TAG) {
            if (connectionThread != null && connectionThread.isAlive()) {
                return;
            }
            if (mConnection != null && mConnection.isConnected()) {
                return;
            }

            // check for cached credentials. If available, use it!
            SharedPreferences preferences =
                PreferenceManager.getDefaultSharedPreferences(this);
            String jid = preferences.getString("jid", null);
            String username = preferences.getString("username", null);
            String password = preferences.getString("password", null);
            if (jid != null && password != null) {
                connectionThread = new ConnectionThread(
                    jid, username, password, null, null, false, service
                );
            }
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

            public boolean follow(String channel) throws RemoteException {
                if (!isConnected()) {
                    return false;
                }
                String user = mConnection.getUser();
                if (user == null) {
                    return false;
                }
                if (user.indexOf('/') != -1) {
                    user = user.substring(0, user.indexOf('/'));
                }
                BCPubSubManager pubSubManager = mConnection.getPubSubManager();
                Node node;
                String title;
                try {
                    node = pubSubManager.getNode(channel);
                    node.subscribe(user);
                    title = pubSubManager.fetchChannelTitle(node);
                } catch (XMPPException e) {
                    e.printStackTrace();
                    throw new RemoteException();
                }

                // basically subscribed, add to content provider

                ContentValues channelEntry = new ContentValues();
                channelEntry.put(Roster.JID, channel);
                channelEntry.put(Roster.NAME, title);
                getContentResolver().insert(Roster.CONTENT_URI, channelEntry);
                updateChannel(channel);

                return true;
            }

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
                synchronized (TAG) {
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
                synchronized (TAG) {
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
                synchronized (TAG) {
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
                synchronized (TAG) {
                    if (listener.asBinder().isBinderAlive()) {
                        listeners.add(listener);
                    }
                }
            }

            public String[] getDirectories() throws RemoteException {
                return getDirectoryEntries(null);
            }

            public String[] getDirectoryEntries(String id)
                    throws RemoteException {
                List<QueryItem> directory = null;
                try {
                    directory = mConnection.getDirectory(id);
                } catch (XMPPException e) {
                    Log.e(TAG,
                            "could not fetch directory list (" + id + ")",
                            e);
                }
                if (directory == null) {
                    return null;
                }
                String v[] = new String[directory.size() * 3];
                int i = 0;
                for (QueryItem item : directory) {
                    v[i++] = item.getId();
                    v[i++] = item.getTitle();
                    v[i++] = item.getDescription();
                }
                return v;
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

                Log.d(TAG, "cache credentials");

                SharedPreferences preferences =
                    PreferenceManager.getDefaultSharedPreferences(this);
                String jid = client.getUser();
                if (jid.indexOf('/') != -1) {
                    jid = jid.substring(0, jid.indexOf('/'));
                }
                preferences.edit()
                    .putString("jid", jid)
                    .putString("username", client.getLoginUsername())
                    .putString("password", password)
                    .commit();
            }

            activityListener.bump();
            mConnection.addPacketListener(activityListener, null);

            mConnection.addGeoLocListener(
                new ConnectionBCGeolocListener(getContentResolver())
            );
            mConnection.addAtomListener(
                new BCConnectionAtomListener(getContentResolver(), mConnection)
            );

            Log.d(TAG, "notify handler about new connection");

            LinkedList<IBuddycloudServiceListener> remove = null;
            for (IBuddycloudServiceListener listener: listeners) {
                if (listener.asBinder().isBinderAlive()) {
                    try {
                        listener.onBCConnected();
                    } catch (RemoteException e) {
                        if (remove == null) {
                            remove =
                                new LinkedList<IBuddycloudServiceListener>();
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
        synchronized (TAG) {
            if (mConnection != null && mConnection.isConnected()) {
                // multithreading? We may be connected before all
                // attempts come back...
                // So we ignore the problem if we have a working connection.
                return;
            }
            e.printStackTrace(System.err);
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
