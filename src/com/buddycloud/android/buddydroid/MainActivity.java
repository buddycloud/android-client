package com.buddycloud.android.buddydroid;

import org.jivesoftware.smack.XMPPConnection;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.widget.TabHost;

public class MainActivity extends TabActivity {
    private TabHost mTabHost;
    private Intent backgroungService;

    static {
        System.setProperty("smack.debugEnabled", "true");
        XMPPConnection.DEBUG_ENABLED = true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        mTabHost = getTabHost();
        mTabHost.addTab(mTabHost.newTabSpec("roster").setIndicator("Roster", 
                getResources().getDrawable(R.drawable.ic_menu_allfriends))
                .setContent(new Intent(this, RosterActivity.class)));
        mTabHost.addTab(mTabHost.newTabSpec("chnls").setIndicator("Channels",
                getResources().getDrawable(R.drawable.ic_menu_friendslist))
                .setContent(new Intent(this, ChannelActivity.class)));
        mTabHost.addTab(mTabHost.newTabSpec("nearby").setIndicator("NearBy", 
               getResources().getDrawable(android.R.drawable.ic_menu_myplaces))
                .setContent(new Intent(this, NearbyActivity.class)));

        backgroungService = new Intent(this, BuddycloudService.class);
        startService(backgroungService);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

        case R.id.stop:
            stopService(backgroungService);
            break;

        case R.id.settings:
            startActivity(new Intent(this, SettingsActivity.class));
            break;
        case R.id.feedback:
            LogCollector.giveFeedback(this, "blah blah blah");
            break;
        }
        return super.onOptionsItemSelected(item);
    }
}
