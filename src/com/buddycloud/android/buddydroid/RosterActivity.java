
package com.buddycloud.android.buddydroid;

import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SmackConfiguration;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.database.sqlite.SQLiteCursor;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.RelativeLayout.LayoutParams;

import com.buddycloud.android.buddydroid.provider.BuddyCloud.Roster;
import com.buddycloud.jbuddycloud.BuddycloudClient;

public class RosterActivity extends ListActivity {
	
	protected static final String TAG = "RosterActivity";
	private Cursor mBuddies;
	private Intent backgroungService;
	
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    
    	mBuddies = managedQuery(Roster.CONTENT_URI, Roster.PROJECTION_MAP, null, null, null);
    	Log.d("provider", "cursor is: " + mBuddies);
    	startManagingCursor(mBuddies);
    	setListAdapter(new RosterAdapter());

    	getContentResolver().registerContentObserver(Roster.CONTENT_URI, false, 
    		new ContentObserver(new Handler()) {

			@Override
			public void onChange(boolean selfChange) {
				Log.d(TAG, "Content Changed");
				// mBuddies.requery() causes some strange error!??
				// just close and query again as workaraound so far
				mBuddies.close();
				mBuddies = managedQuery(Roster.CONTENT_URI, Roster.PROJECTION_MAP, null, null, null);
				((RosterAdapter)getListAdapter()).notifyDataSetChanged();
				super.onChange(selfChange);
			}
		});

    	getListView().setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				((RosterAdapter)getListAdapter()).toggle(arg2);
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {} 
		});
    	// getListView().setTextFilterEnabled(true);

    	backgroungService = new Intent(this, BuddycloudService.class);
    	startService(backgroungService );
    }
    

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {    
       ((RosterAdapter)getListAdapter()).toggle(position);
    }
    
    @Override
	protected void onPause() {
		stopService(backgroungService);
		super.onPause();
	}


	private class RosterAdapter extends BaseAdapter {
    	
    	private int mExpandedPosition;

		public Object getItem(int position) {
			mBuddies.moveToPosition(position);
			return mBuddies.getString(mBuddies.getColumnIndex(Roster.JID));
		}

		public long getItemId(int position) {
			mBuddies.moveToPosition(position);
			return mBuddies.getLong(mBuddies.getColumnIndex(Roster._ID));
		}
		
		public int getCount() {
			if (mBuddies == null) return 0;
			return mBuddies.getCount();
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			getItem(position);
			
			boolean hasNextLocation = (mBuddies.getString(mBuddies.getColumnIndex(Roster.GEOLOC_NEXT)) != null
					   && !mBuddies.getString(mBuddies.getColumnIndex(Roster.GEOLOC_NEXT)).equals("null"));
			
			String jid = mBuddies.getString(mBuddies.getColumnIndex(Roster.JID));
			jid = jid.split("[@]")[0];
			
			
			View v = (View) makeView(convertView);
			
			TextView tv = (TextView) v.findViewById(R.id.title);
			tv.setText(mBuddies.getString(mBuddies.getColumnIndex(Roster.NAME)) + " (" + jid + ")");
			
			if (position == mExpandedPosition) {
				tv = (TextView) v.findViewById(R.id.desc);
				tv.setText(mBuddies.getString(mBuddies.getColumnIndex(Roster.STATUS)));
				
				tv = (TextView) v.findViewById(R.id.loc_prev);
				tv.setText(mBuddies.getString(mBuddies.getColumnIndex(Roster.GEOLOC_PREV)));
				tv.setPaintFlags(tv.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
				tv = (TextView) v.findViewById(R.id.loc_current);
				tv.setText(mBuddies.getString(mBuddies.getColumnIndex(Roster.GEOLOC)));
				tv = (TextView) v.findViewById(R.id.loc_next);
				if (hasNextLocation) tv.setText(mBuddies.getString(mBuddies.getColumnIndex(Roster.GEOLOC_NEXT)));
				else tv.setText("");
				
				v.findViewById(R.id.desc).setVisibility(View.VISIBLE);
				v.findViewById(R.id.arrow).setVisibility(View.VISIBLE);
				ImageView iv = (ImageView) v.findViewById(R.id.arrow);
				iv.setImageResource((hasNextLocation ? R.drawable.history2 : R.drawable.history1));
				
				v.findViewById(R.id.loc_prev).setVisibility(View.VISIBLE);
				v.findViewById(R.id.loc_current).setVisibility(View.VISIBLE);
				v.findViewById(R.id.loc_next).setVisibility(hasNextLocation ? View.VISIBLE : View.GONE);
			} else {
				tv = (TextView) v.findViewById(R.id.desc);
				tv.setText(mBuddies.getString(mBuddies.getColumnIndex(Roster.GEOLOC)));
				
				v.findViewById(R.id.arrow).setVisibility(View.GONE);
				v.findViewById(R.id.loc_prev).setVisibility(View.GONE);
				v.findViewById(R.id.loc_current).setVisibility(View.GONE);
				v.findViewById(R.id.loc_next).setVisibility(View.GONE);
			}
			return v;
		}
		
		
		public void toggle(int position) {
			if (mExpandedPosition != position)
				mExpandedPosition = position;
			else
				mExpandedPosition = -1;
			notifyDataSetChanged();
		}
		
		private View makeView(View convertView) {
			if (convertView != null) return convertView;
			
			LayoutInflater i = (LayoutInflater) RosterActivity.this.getSystemService(LAYOUT_INFLATER_SERVICE);
			
			return i.inflate(R.layout.roster_row, null);
		}
    }
}