package com.buddycloud.android.buddydroid;

import java.util.Collection;
import java.util.Iterator;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;

import com.buddycloud.android.buddydroid.provider.BuddyCloud.Roster;
import com.buddycloud.jbuddycloud.BuddycloudClient;
import com.buddycloud.jbuddycloud.packet.LocationEvent;
import com.buddycloud.jbuddycloud.provider.PubSubLocationEventProvider;

import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

public class BuddycloudService extends Service {

	
	private static final String TAG = "BuddycloudService";
	private BuddycloudClient mConnection;

	@Override
	public void onCreate() {
		Log.d(TAG, " onCreate");
		
    	SharedPreferences pm = PreferenceManager.getDefaultSharedPreferences(this);
		String port = pm.getString("port", "5222");
		String jid = pm.getString("jid", "orangeman@buddycloud.com");
		String password = pm.getString("password", "testaccount");
		String host = jid.split("@")[1];
		String username = jid.split("@")[0];
		
		// Create a connection
		ConnectionConfiguration connConfig = new ConnectionConfiguration(host, Integer.parseInt(port));
//		connConfig.setReconnectionAllowed(true);
		mConnection = new BuddycloudClient(connConfig);
		
		try {
//			SmackConfiguration.setPacketReplyTimeout(12000);
			mConnection.connect();
			mConnection.login(username, password, "buddycloud");
			Log.i(TAG, "connected to " + mConnection.getHost());
		} catch (Exception ex) {
			ex.printStackTrace();
			Log.e("XMPPClient", "Failed to connect to " + mConnection.getHost());
			Log.e("XMPPClient", ex.toString());
		}
		
		// retrieve roster
		for (Iterator iterator = mConnection.getRoster().getEntries().iterator(); iterator.hasNext();) {
    		RosterEntry buddy = ((RosterEntry) iterator.next());
    		Log.d(TAG, buddy.getUser()+" "+buddy.getName()+" "+buddy.getStatus());
    		ContentValues values = new ContentValues();
    		values.put(Roster.JID, buddy.getUser());
    		values.put(Roster.NAME, buddy.getUser().split("\\@")[0]);
    		getContentResolver().insert(Roster.CONTENT_URI, values);
    	}
		
		mConnection.getRoster().addRosterListener(new RosterListener() {

    		@Override
    		public void entriesAdded(Collection<String> addresses) {
    			Log.d(TAG, " entry added event received");
    		}

    		@Override
    		public void entriesDeleted(Collection<String> addresses) {
    			Log.d(TAG, " entry deleted event received");
    		}

    		@Override
    		public void entriesUpdated(Collection<String> addresses) {
    			Log.d(TAG, " entry updated event received");
    		}

    		@Override
    		public void presenceChanged(Presence presence) {
    			Log.d(TAG, "presence changed: "+presence.getFrom()+" -> "+presence.getType().name());
    		}
    		
    		
    	});
		
		mConnection.addPacketListener(new PacketListener() {

    		@Override
    		public void processPacket(Packet packet) {
    			Log.d(TAG, "Packet: "+packet.toXML());
    			if (packet instanceof Message) {
    				LocationEvent loc = (LocationEvent) packet.getExtension(PubSubLocationEventProvider.getNS());
    				if (loc != null) {
    					switch (loc.type) {
    					case LocationEvent.CURRENT:
    						break;
    					case LocationEvent.PREV:
    						break;
    					case LocationEvent.NEXT:
    						break;
    					}

    				}
    			}

    		}}, null);
		super.onCreate();
	}

	@Override
	public void onStart(Intent intent, int startId) {
		Log.d(TAG, " onStart");
		
		super.onStart(intent, startId);
	}

	@Override
	public void onDestroy() {
		Log.d(TAG, " onDestroy");
		mConnection.disconnect();
		Log.i(TAG, "disonnected.");
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

}
