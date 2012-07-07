package com.buddycloud;

import java.util.ArrayList;
import java.util.Date;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Type;
import org.jivesoftware.smackx.packet.DiscoverInfo;

import android.content.ContentValues;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.buddycloud.android.buddydroid.BCConnectionAtomListener;
import com.buddycloud.asmack.BuddycloudChannelMetadataListener;
import com.buddycloud.asmack.BuddycloudLocationChannelListener;
import com.buddycloud.asmack.ChannelSync;
import com.buddycloud.collect.CellListener;
import com.buddycloud.collect.NetworkListener;
import com.buddycloud.component.ComponentAdd;
import com.buddycloud.content.BuddyCloud.ChannelData;
import com.buddycloud.content.BuddyCloud.Roster;
import com.buddycloud.jbuddycloud.packet.BeaconLog;
import com.buddycloud.jbuddycloud.packet.ChannelFetch;
import com.buddycloud.jbuddycloud.packet.RSMSet;
import com.googlecode.asmack.Stanza;
import com.googlecode.asmack.StanzaSink;
import com.googlecode.asmack.XMPPUtils;
import com.googlecode.asmack.XmppAccount;
import com.googlecode.asmack.XmppException;
import com.googlecode.asmack.XmppIdentity;
import com.googlecode.asmack.client.AsmackClientService;
import com.googlecode.asmack.connection.AccountConnection;
import com.googlecode.asmack.connection.Connection;
import com.googlecode.asmack.connection.ConnectionFactory;
import com.googlecode.asmack.connection.IXmppTransportService;
import com.googlecode.asmack.connection.XmppTransportService;
import com.googlecode.asmack.disco.Database;

/**
 * Buddycloud service for interacting with the transport layer and the local
 * database.
 */
public class BuddycloudService extends AsmackClientService {

    /**
     * Create a new buddycloud service with the smack config from
     * res/xml/smackproviders.xml
     */
    public BuddycloudService() {
        super(R.xml.smackproviders);
    }

    /**
     * Logging tag for BuddycloudService.
     */
    static final String TAG = BuddycloudService.class.getSimpleName();

    /**
     * The cell change listener. The cell listener recognizes cell changes and
     * informs the service about new scan results.
     */
    private CellListener cellListener = null;

    /**
     * The network listener. The network listener is responsible for detecting
     * wifi cell changes.
     */
    private NetworkListener networkListener = null;

    /**
     * A short task queue for internal command needs.
     */
    private TaskQueueThread taskQueue;

    /**
     * The internal time tick counter.
     */
    private int tick = 0;

    /**
     * Create a new buddycloud service, initialize the required platform
     * listeners.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        cellListener = new CellListener(this);
        networkListener = new NetworkListener(this);
        cellListener.start();
        getApplicationContext().registerReceiver(
            new TimeBroadcastReceiver(this),
            new IntentFilter("android.intent.action.TIME_TICK")
        );
    }

    /**
     * The last time we send a beacon log.
     */
    private long beaconLogTimer;

    /**
     * Internal priority tracks the minimum priority since the last beacon
     * log send.
     */
    private int internalPriority;

    /**
     * Send a beacon log. Priority is 0..10, with 0 meaning instant and
     * 10 meaning "if 3 min. have passed".
     * @param prio The priority of sending the colleciton.
     * @throws InterruptedException 
     */
    public boolean sendBeaconLog(final int prio)
        throws InterruptedException
    {
        final int priority = Math.min(internalPriority, prio);
        if (taskQueue == null) {
            return false;
        }
        return taskQueue.add(new Runnable() {
            public void run() {
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
                cellListener.appendTo(log);
                networkListener.appendTo(log);
                try {
                    client.sendFromAllResources(log);
                } catch (RemoteException e) {
                    Log.w(TAG, "Could not send beacon log", e);
                }
            }
        });
    }

    /**
     * Send a single packet.
     * @param packet The packet to send.
     * @return True on success.
     * @throws InterruptedException
     * @throws RemoteException
     */
    public void send(final Packet packet)
        throws InterruptedException, RemoteException
    {
        if (packet.getFrom() == null || packet.getFrom().length() == 0) {
            client.sendFromAllResources(packet);
        } else {
            client.send(packet, packet.getFrom());
        }
    }

    /**
     * Sync a single channel.
     * @param channel The full channel path to sync.
     */
    public void updateChannel(String channel) {
        Log.d("BC", "update channel " + channel);
        long startTime = 0l;
        long endTime = 0l;
        Cursor cursor = getContentResolver().query(
                ChannelData.BROKEN_CONTENT_URI,
                null,
                null,
                new String[]{channel},
                null
        );
        if (cursor.getCount() > 0) {
            if (cursor.moveToFirst()) {
                startTime = cursor.getLong(cursor.getColumnIndex(
                    ChannelData.PARENT
                ));
                endTime = cursor.getLong(cursor.getColumnIndex(
                        ChannelData.ITEM_ID
                ));
            }
            Log.w(TAG, "Found broken threads in " + channel + ", "
                     + new Date(startTime) + " / "+ new Date(endTime));
        }
        cursor.close();
        cursor = getContentResolver().query(
                Roster.CONTENT_URI,
                new String[]{Roster.LAST_UPDATED},
                "jid=?",
                new String[]{channel},
                null
        );
        if (cursor.getCount() == 1) {
            cursor.moveToFirst();
            startTime = cursor.getLong(
                            cursor.getColumnIndex(Roster.LAST_UPDATED));
            IQ iq = new ChannelFetch(channel, startTime);
            try {
                send(iq);
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        cursor.close();
        DiscoverInfo info = new DiscoverInfo();
        info.setNode(channel);
        info.setType(org.jivesoftware.smack.packet.IQ.Type.GET);
        info.setTo("broadcaster.buddycloud.com");
        try {
            send(info);
        } catch (InterruptedException e) {
            e.printStackTrace(System.err);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * Start the buddycloud service and the internal task queue.
     * @param intent The start intent.
     * @param startId The start id.
     */
    @Override
    public void onStart(Intent intent, int startId) {
        Log.d(TAG, " onStart");
        super.onStart(intent, startId);

        if (taskQueue == null) {
            taskQueue = new TaskQueueThread();
            taskQueue.start();
        }

        cellListener.start();
    }

    /**
     * Destroy the current buddycloud service, freeing the listeners and
     * stopping the task queue.
     */
    @Override
    public void onDestroy() {
        Log.d(TAG, " onDestroy");
        super.onDestroy();

        cellListener.stop();

        taskQueue.stopQueue();
        taskQueue = null;
    }

    /**
     * Service binding for UI related methods.
     */
    private final IBuddycloudService.Stub binder =
        new IBuddycloudService.Stub() {

            public boolean follow(String channel) throws RemoteException {
                // TODO implement
                return false;
            }

            @Override
            public void updateChannel(String channel) throws RemoteException {
                BuddycloudService.this.updateChannel(channel);
            }

            /**
             * Run a login try, without interfering with the real core.
             * @param jid The user jid.
             * @param password The user password.
             * @return True on success.
             */
            @Override
            public boolean tryLogin(String jid, String password) throws RemoteException {
                XmppAccount account = new XmppAccount();
                account.setJid(jid);
                account.setPassword(password);
                account.setConnection("xmpp:" + XMPPUtils.getDomain(account.getJid()));
                account.setResource("asmack-testlogin-" + ID);
                Connection connection =
                    ConnectionFactory.createConnection(account);
                try {
                    connection.connect(new StanzaSink() {
                        @Override
                        public void receive(Stanza stanza) {}

                        @Override
                        public void connectionFailed(Connection connection,
                                XmppException exception) {
                        }

                    });
                    connection.close();
                    return true;
                } catch (XmppException e) {
                    return false;
                }
            }

            /**
             * Send a single stanza via an appropriate connection.
             * @param stanza The stanza to send.
             */
            @Override
            public boolean send(Stanza stanza) throws RemoteException {
                return BuddycloudService.this.send(stanza);
            }

            /**
             * Send a single stanza through all connections, altering from
             * to be the resource address.
             * @param stanza The stanza to send.
             */
            @Override
            public void sendFromAllResources(Stanza stanza)
                throws RemoteException
            {
                BuddycloudService.this.sendFromAllResources(stanza);
            }

            /**
             * Send a single stanza through all connections, altering from
             * to be the account address.
             * @param stanza The stanza to send.
             */
            @Override
            public void sendFromAllAccounts(Stanza stanza)
                throws RemoteException
            {
                BuddycloudService.this.sendFromAllAccounts(stanza);
            }

            /**
             * Retrieve the full resource jid by bare jid.
             * @param bare The bare user jid.
             * @return The full resource jid.
             */
            @Override
            public String getFullJidByBare(String bare) throws RemoteException {
                return BuddycloudService.this.getFullJidByBare(bare);
            }

            /**
             * Enable a new feature for a given jid. A new presence will be
             * send with the next tick (max. 60s).
             * @param jid The jid to enhance
             * @param feature The new feature.
             */
            @Override
            public void enableFeatureForJid(String jid, String feature)
                    throws RemoteException {
                Database.enableFeature(
                    getApplicationContext(),
                    jid,
                    feature,
                    null
                );
                JID_VERIFICATION_CACHE.remove(jid);
            }

            /**
             * Enable a feature for all xmpp connections. New features will
             * be announced with the next time tick.
             * @param feature The feature to be announced.
             */
            @Override
            public void enableFeature(String feature) throws RemoteException {
                Database.enableFeature(
                    getApplicationContext(),
                    feature,
                    null
                );
                JID_VERIFICATION_CACHE.clear();
            }

            /**
             * Add an identity to a given xmpp connection. The identity will
             * be announced with the next time tick.
             * @param jid The user jid.
             * @param identity The new xmpp identity.
             */
            @Override
            public void addIdentityForJid(String jid, XmppIdentity identity)
                    throws RemoteException {
                Database.addIdentity(
                    getApplicationContext(),
                    jid,
                    identity,
                    null
                );
                JID_VERIFICATION_CACHE.remove(jid);
            }

            /**
             * Add a new identity to all xmpp accounts. The identity will be
             * announced during the next time tick.
             * @param identity The new identity.
             */
            @Override
            public void addIdentity(XmppIdentity identity)
                    throws RemoteException {
                Database.addIdentity(
                    getApplicationContext(),
                    identity,
                    null
                );
                JID_VERIFICATION_CACHE.clear();
            }

            /**
             * Retrieve all current account jids.
             * @param connected True if you only jids of connected acocunts should be
             *                  returned.
             * @return List of account jids.
             */
            @Override
            public String[] getAllAccountJids(
                boolean connected
            ) throws RemoteException {
                ArrayList<String> jids = new ArrayList<String>();
                for (AccountConnection state: connections.values()) {
                    if (connected && state.getCurrentState() !=
                        AccountConnection.State.Connected) {
                        continue;
                    }
                    jids.add(state.getAccount().getJid());
                }
                return jids.toArray(new String[jids.size()]);
            }

            /**
             * Retrieve all resource jids (where available).
             * @param connected True if you only jids of connected acocunts should be
             *                  returned.
             * @return List of account jids.
             */
            @Override
            public String[] getAllResourceJids(
                boolean connected
            ) throws RemoteException {
                ArrayList<String> resources = new ArrayList<String>();
                for (AccountConnection state: connections.values()) {
                    if (state.getCurrentState() !=
                        AccountConnection.State.Connected) {
                        continue;
                    }
                    String jid = state.getConnection().getResourceJid();
                    if (jid != null) {
                        resources.add(jid);
                    }
                }
                return resources.toArray(new String[resources.size()]);
            }

        };

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onTrasportServiceConnect(IXmppTransportService service) {
        try {
            service.enableFeature(
                        "http://jabber.org/protocol/geoloc");
            service.enableFeature(
                        "http://jabber.org/protocol/geoloc-next");
            service.enableFeature(
                        "http://jabber.org/protocol/geoloc-prev");
            service.enableFeature(
                        "http://jabber.org/protocol/geoloc+notify");
            service.enableFeature(
                        "http://jabber.org/protocol/geoloc-next+notify");
            service.enableFeature(
                        "http://jabber.org/protocol/geoloc-prev+notify");

            SharedPreferences preferences =
                getApplicationContext().getSharedPreferences("buddycloud", 0);

            for (String jid: client.getAllAccountJids(true)) {
                if (preferences.getString("main_jid", "").length() == 0) {
                    preferences.edit().putString("main_jid", jid).commit();
                }
                Log.d("BC", "SYNC " + jid);
                new ChannelSync(client, jid,
                    getApplicationContext().getContentResolver()
                );
            }

            String jids[] = client.getAllAccountJids(false);
            for (String jid: jids) {
                ContentValues values = new ContentValues();
                values.put(Roster.SELF, 1);
                getApplicationContext().getContentResolver().update(
                    Roster.CONTENT_URI,
                    values,
                    "jid=?",
                    new String[]{"/user/" + jid + "/channel"}
                );
            }
            sendDirectedPresence();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onTrasportServiceDisconnect(IXmppTransportService service) {
    }

    /**
     * All ids are prefixed with bc if possible, thus return bc as prefix.
     */
    @Override
    protected String getIDPrefix() {
        return "bc";
    }

    /**
     * Called just before the client starts up, used to bind the component
     * adding tool.
     */
    @Override
    protected void preClientStart() {
        new ComponentAdd(client);
        client.registerListener(new BuddycloudLocationChannelListener(
            getContentResolver()
        ));
        client.registerListener(new BuddycloudChannelMetadataListener(
                getContentResolver()
            ));
        BCConnectionAtomListener atomListener = new BCConnectionAtomListener(
                                                    getContentResolver(), this);
        client.registerListener(atomListener);
    }

    /**
     * Send a direct presence to the channel server.
     */
    public void sendDirectedPresence() {
        Cursor cursor = getContentResolver().query(
            Roster.CONTENT_URI,
            new String[]{Roster.LAST_UPDATED},
            null, null, "last_updated desc"
        );
        if (cursor.moveToFirst()) {
            long after = cursor.getLong(
                    cursor.getColumnIndex(Roster.LAST_UPDATED));
            Presence presence = new Presence(Type.available);
            presence.setTo("broadcaster.buddycloud.com");
            presence.addExtension(new RSMSet(after));
            try {
                client.sendFromAllResources(presence);
            } catch (RemoteException e) {
            }
        }
        cursor.close();
    }

    /**
     * Time Tick callback to send beacon logs and directet presence.
     */
    public void onTimeTick() {
        tick++;
        try {
            sendBeaconLog(10);
        } catch (InterruptedException e) {
        }
        if (tick % 10 == 0) {
            sendDirectedPresence();
        }
    }

}
