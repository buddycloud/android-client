package com.buddycloud.android.buddydroid;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.Window;
import android.widget.TabHost;

public class MainActivity extends TabActivity {

    private Intent backgroungService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        backgroungService = new Intent(this, BuddycloudService.class);

        TabHost tabHost = getTabHost();
        tabHost.addTab(tabHost.newTabSpec("roster").setIndicator("Roster", 
                getResources().getDrawable(R.drawable.ic_menu_allfriends))
                .setContent(new Intent(this, RosterActivity.class)));
        tabHost.addTab(tabHost.newTabSpec("chnls").setIndicator("Channels",
                getResources().getDrawable(R.drawable.ic_menu_friendslist))
                .setContent(new Intent(this, ChannelActivity.class)));
        tabHost.addTab(tabHost.newTabSpec("nearby").setIndicator("NearBy", 
               getResources().getDrawable(android.R.drawable.ic_menu_myplaces))
                .setContent(new Intent(this, NearbyActivity.class)));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

        case R.id.stop:
            stopService(backgroungService);
            finish();
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
