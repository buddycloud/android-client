
package com.buddycloud.android.buddydroid;

import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SmackConfiguration;

import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Paint;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.RelativeLayout.LayoutParams;

import com.buddycloud.android.buddydroid.provider.BuddyCloud.Roster;
import com.buddycloud.jbuddycloud.BuddycloudClient;

public class RosterActivity extends ListActivity {
	
	private Cursor c;
	private Intent backgroungService;
	
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	
    	backgroungService = new Intent(this, BuddycloudService.class);
		startService(backgroungService );
    	

    
    	c = getContentResolver().query(Roster.CONTENT_URI, Roster.PROJECTION_MAP, null, null, null);
    	
    	Log.d("provider", "cursor is: " + c);
    	
    	setListAdapter(new RosterAdapter());
    	getListView().setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				
				((RosterAdapter)getListAdapter()).toggle(arg2);
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				((RosterAdapter)getListAdapter()).untoggleAll();
				
			} });
    	// getListView().setTextFilterEnabled(true);
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

    	private List<Boolean> expanded = new ArrayList<Boolean>();

    	
    	public RosterAdapter() {
    		for (int i = 0, n = getCount(); i < n; i++) {
    			expanded.add(false);
    		}
    	}
    	
		
		public Object getItem(int position) {
			c.moveToPosition(position);
			
			return c.getString(c.getColumnIndex(Roster.JID));
		}

		public long getItemId(int position) {
			c.moveToPosition(position);
			
			return c.getLong(c.getColumnIndex(Roster._ID));
		}
		
		public int getCount() {
			if (c == null) return 0;
			
			return c.getCount();
		}
		

		public View getView(int position, View convertView, ViewGroup parent) {
			getItem(position);
			
			boolean isExpanded = expanded.get(position);
			boolean hasNextLocation = (c.getString(c.getColumnIndex(Roster.GEOLOC_NEXT)) != null
					   && !c.getString(c.getColumnIndex(Roster.GEOLOC_NEXT)).equals("null"));
			
			String jid = c.getString(c.getColumnIndex(Roster.JID));
			jid = jid.split("[@]")[0];
			
			
			View v = (View) makeView(convertView);
			
			TextView tv = (TextView) v.findViewById(R.id.title);
			tv.setText(c.getString(c.getColumnIndex(Roster.NAME)) + " (" + jid + ")");
			
			if (isExpanded) {
				tv = (TextView) v.findViewById(R.id.desc);
				tv.setText(c.getString(c.getColumnIndex(Roster.STATUS)));
				
				tv = (TextView) v.findViewById(R.id.loc_prev);
				tv.setText(c.getString(c.getColumnIndex(Roster.GEOLOC_PREV)));
				tv.setPaintFlags(tv.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
				tv = (TextView) v.findViewById(R.id.loc_current);
				tv.setText(c.getString(c.getColumnIndex(Roster.GEOLOC)));
				tv = (TextView) v.findViewById(R.id.loc_next);
				if (hasNextLocation) tv.setText(c.getString(c.getColumnIndex(Roster.GEOLOC_NEXT)));
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
				tv.setText(c.getString(c.getColumnIndex(Roster.GEOLOC)));
				
				v.findViewById(R.id.arrow).setVisibility(View.GONE);
				v.findViewById(R.id.loc_prev).setVisibility(View.GONE);
				v.findViewById(R.id.loc_current).setVisibility(View.GONE);
				v.findViewById(R.id.loc_next).setVisibility(View.GONE);
			}
			
			return v;
		}
		
		
		public void toggle(int position) {
			untoggleAll(false);
			expanded.set(position, !expanded.get(position));
			notifyDataSetChanged();
		}
		
		public void untoggleAll() {
			untoggleAll(true);
		}
		
		private void untoggleAll(boolean notify) {
			for (int i = 0; i < expanded.size(); i++) expanded.set(i, false);
			if (notify) notifyDataSetChanged();
		}
		
		
		private View makeView(View convertView) {
			if (convertView != null) return convertView;
			
			LayoutInflater i = (LayoutInflater) RosterActivity.this.getSystemService(LAYOUT_INFLATER_SERVICE);
			
			return i.inflate(R.layout.roster_row, null);
		}
    }
}