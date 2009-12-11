package com.buddycloud.android.buddydroid;

import java.util.Collection;
import java.util.Iterator;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.provider.ProviderManager;

import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.buddycloud.android.buddydroid.provider.BuddyCloud;
import com.buddycloud.android.buddydroid.provider.BuddyCloud.Roster;
import com.buddycloud.jbuddycloud.BuddycloudClient;
import com.buddycloud.jbuddycloud.packet.LocationEvent;
import com.buddycloud.jbuddycloud.provider.PubSubLocationEventProvider;

public class BuddycloudService extends Service {

        
        private static final String TAG = "Service";
        private BuddycloudClient mConnection;

        @Override
        public void onCreate() {
                Log.d(TAG, " onCreate");

                SharedPreferences pm = PreferenceManager.getDefaultSharedPreferences(this);
                String password = pm.getString("password", "testaccount");
                String username = pm.getString("username", "orangeman");
                String host = pm.getString("host", "buddycloud.com");
                
                // Create a connection
//              connConfig.setReconnectionAllowed(true);
                mConnection = new BuddycloudClient(host);
                
                try {
                	//                      SmackConfiguration.setPacketReplyTimeout(12000);
                	mConnection.connect();
                	mConnection.login(username, password);
                	Log.i(TAG, "connected to " + mConnection.getHost() + ":" + mConnection.getPort());
                } catch (Exception ex) {
                	Log.e("XMPPClient", "Failed to connect to " + mConnection.getHost());
                	Log.e("XMPPClient", ex.toString());
                }
                
                if (mConnection.isAuthenticated()) {
                	Toast.makeText(this, "Login successful :)", Toast.LENGTH_SHORT).show();
                	
                	// retrieve roster
                	getContentResolver().delete(Roster.CONTENT_URI, null, null);
                	for (Iterator iterator = mConnection.getRoster().getEntries().iterator(); iterator.hasNext();) {
                		RosterEntry buddy = ((RosterEntry) iterator.next());
//                		Log.d(TAG, buddy.getUser()+" "+buddy.getName()+" "+buddy.getStatus());
                		ContentValues values = new ContentValues();
                		values.put(Roster.JID, buddy.getUser());
                		values.put(Roster.NAME, buddy.getUser().split("\\@")[0]);
                		getContentResolver().insert(Roster.CONTENT_URI, values);
                	}
                	getContentResolver().notifyChange(Roster.CONTENT_URI, null);

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
                			//                      Log.d(TAG, "Packet: "+packet.toXML());
                			if (packet instanceof Message) {
                				LocationEvent loc = (LocationEvent) packet.getExtension(PubSubLocationEventProvider.getNS());
                				if (loc != null) {
                					ContentValues values = new ContentValues();
                					switch (loc.type) {
                					case LocationEvent.CURRENT:
                						Log.d(TAG, "GEOLOC CURR received: "+packet.getFrom()+" -> "+loc.text);
                                		values.put(Roster.GEOLOC, loc.text);
                						break;
                					case LocationEvent.PREV:
                						Log.d(TAG, "GEOLOC PREV received: "+packet.getFrom()+" -> "+loc.text);
                						values.put(Roster.GEOLOC_PREV, loc.text);
                						break;
                					case LocationEvent.NEXT:
                						Log.d(TAG, "GEOLOC NEXT received: "+packet.getFrom()+" -> "+loc.text);
                						values.put(Roster.GEOLOC_NEXT, loc.text);
                						break;
                					}
                					getContentResolver().update(Roster.CONTENT_URI, values, Roster.JID+"='"+packet.getFrom()+"'", null);
                				}
                			}

                		}}, null);
                } else {
                	Toast.makeText(this, "Login failed :(", Toast.LENGTH_LONG).show();
                }
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
