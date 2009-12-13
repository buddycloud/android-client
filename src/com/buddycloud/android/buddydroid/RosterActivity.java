
package com.buddycloud.android.buddydroid;

import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SmackConfiguration;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
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
import android.widget.CursorAdapter;
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
    
//    	Cursor buddies = managedQuery(Roster.CONTENT_URI, Roster.PROJECTION_MAP, null, null, null);
    	Cursor buddies = getContentResolver().query(Roster.CONTENT_URI, Roster.PROJECTION_MAP, null, null, null);
    	Log.d("provider", "cursor is: " + buddies);
    	setListAdapter(new RosterAdapter(this, buddies));

    	getListView().setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long arg3) {
				((RosterAdapter)getListAdapter()).toggle(position);
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {} 
		});
    	// getListView().setTextFilterEnabled(true);
    }
    

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {    
       ((RosterAdapter)getListAdapter()).toggle(position);
    }

	private class RosterAdapter extends CursorAdapter {

	    private int mExpandedPosition;
	    
    	public RosterAdapter(Context context, Cursor c) {
            super(context, c);
    	}

    	public void toggle(int position) {
    	    if (mExpandedPosition != position)
    	        mExpandedPosition = position;
    	    else
    	        mExpandedPosition = -1;
    	    notifyDataSetChanged();
    	}

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return getLayoutInflater().inflate(R.layout.roster_row, null);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            
            boolean hasNextLocation = (cursor.getString(cursor.getColumnIndex(Roster.GEOLOC_NEXT)) != null
                    && !cursor.getString(cursor.getColumnIndex(Roster.GEOLOC_NEXT)).equals("null"));

            String jid = cursor.getString(cursor.getColumnIndex(Roster.JID));
            jid = jid.split("[@]")[0];

            TextView tv = (TextView) view.findViewById(R.id.title);
            tv.setText(cursor.getString(cursor.getColumnIndex(Roster.NAME)) + " (" + jid + ")");

            if (cursor.getPosition() == mExpandedPosition) {
                tv = (TextView) view.findViewById(R.id.desc);
                tv.setText(cursor.getString(cursor.getColumnIndex(Roster.STATUS)));

                tv = (TextView) view.findViewById(R.id.loc_prev);
                tv.setText(cursor.getString(cursor.getColumnIndex(Roster.GEOLOC_PREV)));
                tv.setPaintFlags(tv.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                tv = (TextView) view.findViewById(R.id.loc_current);
                tv.setText(cursor.getString(cursor.getColumnIndex(Roster.GEOLOC)));
                tv = (TextView) view.findViewById(R.id.loc_next);
                if (hasNextLocation) tv.setText(cursor.getString(cursor.getColumnIndex(Roster.GEOLOC_NEXT)));
                else tv.setText("");

                view.findViewById(R.id.desc).setVisibility(View.VISIBLE);
                view.findViewById(R.id.arrow).setVisibility(View.VISIBLE);
                ImageView iv = (ImageView) view.findViewById(R.id.arrow);
                iv.setImageResource((hasNextLocation ? R.drawable.history2 : R.drawable.history1));

                view.findViewById(R.id.loc_prev).setVisibility(View.VISIBLE);
                view.findViewById(R.id.loc_current).setVisibility(View.VISIBLE);
                view.findViewById(R.id.loc_next).setVisibility(hasNextLocation ? View.VISIBLE : View.GONE);
            } else {
                tv = (TextView) view.findViewById(R.id.desc);
                tv.setText(cursor.getString(cursor.getColumnIndex(Roster.GEOLOC)));

                view.findViewById(R.id.arrow).setVisibility(View.GONE);
                view.findViewById(R.id.loc_prev).setVisibility(View.GONE);
                view.findViewById(R.id.loc_current).setVisibility(View.GONE);
                view.findViewById(R.id.loc_next).setVisibility(View.GONE);
            }
            return;
        }
    }
}