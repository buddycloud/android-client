package com.buddycloud.android.buddydroid;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.database.DataSetObserver;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListAdapter;
import android.widget.TextView;

/**
 * DataModel for the nearby tab. Triggers requeries and informs the UI about
 * updates.
 */
public class NearbyExpandableListAdapter implements ExpandableListAdapter {

    private String[][] groups = new String[][]{{null, "Loading...", null}};

    private HashMap<String, String[][]> content =
        new HashMap<String, String[][]>();

    private LayoutInflater inflater;

    private final NearbyActivity activity;

    public NearbyExpandableListAdapter(NearbyActivity activity) {
        this.activity = activity;
        inflater = (LayoutInflater)
                    activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public boolean areAllItemsEnabled() {
        return true;
    }

    public Object getChild(int groupPosition, int childPosition) {
        return null;
    }

    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    public View getChildView(int groupPosition, int childPosition,
            boolean isLastChild, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.nearby_row, null);
        }

        TextView titleView =
            (TextView)convertView.findViewById(R.id.title);

        TextView descriptionView =
            (TextView)convertView.findViewById(R.id.description);

        String[] entry = content.get(groups[groupPosition][0])[childPosition];

        titleView.setText(entry[1]);

        if (entry[2] != null) {
            descriptionView.setText(entry[2]);
            descriptionView.setVisibility(View.VISIBLE);
        } else {
            descriptionView.setVisibility(View.GONE);
        }

        return convertView;
    }

    public int getChildrenCount(int groupPosition) {
        String[][] dir = content.get(groups[groupPosition][0]);
        if (dir == null) {
            return 0;
        } else {
            return dir.length;
        }
    }

    public long getCombinedChildId(long groupId, long childId) {
        // we have bigger problems if we get a child-group of >64k entries
        return groupId * 65536 + (childId + 1);
    }

    public long getCombinedGroupId(long groupId) {
        return groupId * 65536;
    }

    public Object getGroup(int groupPosition) {
        return groups.length;
    }

    public int getGroupCount() {
        return groups.length;
    }

    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    public View getGroupView(int groupPosition, boolean isExpanded,
            View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.nearby_row, null);
        }

        TextView titleView =
            (TextView)convertView.findViewById(R.id.title);

        TextView descriptionView =
            (TextView)convertView.findViewById(R.id.description);

        titleView.setText(groups[groupPosition][1]);

        if (groups[groupPosition][2] != null) {
            descriptionView.setText(groups[groupPosition][2]);
            descriptionView.setVisibility(View.VISIBLE);
        } else {
            descriptionView.setVisibility(View.GONE);
        }

        return convertView;
    }

    public boolean hasStableIds() {
        return false;
    }

    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return false;
    }

    public boolean isEmpty() {
        return false;
    }

    public void onGroupCollapsed(int groupPosition) {
    }

    public void onGroupExpanded(int groupPosition) {
    }

    private ArrayList<DataSetObserver> observers =
        new ArrayList<DataSetObserver>(2);

    public void registerDataSetObserver(DataSetObserver observer) {
        synchronized (observers) {
            observers.add(observer);
        }
    }

    public void unregisterDataSetObserver(DataSetObserver observer) {
        synchronized (observers) {
            observers.remove(observer);
        }
    }

    public void updateDirectories(String[][] dir) {
        groups = dir;
        Log.d(getClass().getName(), "Got " + dir.length + " entries!");
        fireUpdate();
        for(String[] entry : dir) {
            activity.loadDirectory(entry[0]);
        }
    }

    public void updateDirectory(String id, String[][] dir) {
        content.put(id, dir);
        fireUpdate();
    }

    private void fireUpdate() {
        synchronized (observers) {
            for (final DataSetObserver observer: observers) {
                activity.runOnUiThread(new Runnable(){
                    public void run() {
                        observer.onChanged();
                    }
                });
            }
        }
    }
}
