package com.buddycloud.android.buddydroid;

import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.widget.ExpandableListView;

public class NearbyActivity extends BCActivity {

    private NearbyExpandableListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nearby);
        adapter = new NearbyExpandableListAdapter(this);
        ((ExpandableListView)findViewById(R.id.list)).setAdapter(adapter);
        loadDirectories();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
    }

    void loadDirectory(final String id) {
        new Thread(new Runnable() {
            public void run() {
                int i = 0;
                while (connection == null || service == null ||
                   !service.asBinder().isBinderAlive()) {
                    if (i++ == 10) {
                        Log.d(getClass().getName(), "couldn't load dir " + id);
                        return;
                    }
                    bindBCService();
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        // non-critical
                    }
                }
                try {
                    String[] entries = service.getDirectoryEntries(id);
                    String[][] dir = new String[entries.length / 3][3];
                    for (i = 0; i < dir.length; i++) {
                        dir[i] = new String[]{
                            entries[i*3],
                            entries[i*3 + 1],
                            entries[i*3 + 2]
                        };
                    }
                    if (id == null) {
                        adapter.updateDirectories(dir);
                    } else {
                        adapter.updateDirectory(id, dir);
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    void loadDirectories() {
        loadDirectory(null);
    }
}
