package com.buddycloud.android.buddydroid;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import com.buddycloud.android.buddydroid.provider.BuddyCloud.Places;

public class PlacesActivity extends Activity 
		implements OnItemClickListener {

	protected ListView placesList;
	protected PlacesAdapter listAdapter;
	
	/**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.places);
        
        // list of my places
        placesList = (ListView)findViewById(R.id.placesList);
        
        Cursor places = managedQuery (
                Places.CONTENT_URI,
                Places.PROJECTION_MAP,
                null,
                null,
                null);

        Log.d("provider", "cursor is: " + places);
        
        listAdapter = new PlacesAdapter(this, places);
        
        placesList.setAdapter(listAdapter);
        placesList.setOnItemClickListener(this);
    }

    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    	// TODO implement action
    	//Cursor place = (Cursor) listAdapter.getItem(position);
	}
    
    private class PlacesAdapter extends CursorAdapter {

        public PlacesAdapter(Context context, Cursor c) {
            super(context, c);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return getLayoutInflater().inflate(R.layout.places_row, null);
        }
        
        @Override
        public void bindView(View view, Context context, Cursor cursor) {
        	
        	// Read information form cursor
            String placeName = cursor.getString(
                    cursor.getColumnIndex(Places.NAME));
            String placeStreet = cursor.getString(
            		cursor.getColumnIndex(Places.STREET));
            String placeArea = cursor.getString(
            		cursor.getColumnIndex(Places.AREA));
            
            TextView nameView = (TextView) view.findViewById(R.id.placeName);
            TextView locationView = (TextView) view.findViewById(R.id.placeStreetLocation);
           
            //  Cursor Data -> UI
            nameView.setText(placeName);
            locationView.setText(placeArea + ", " + placeStreet);
        }
    }
	
}
