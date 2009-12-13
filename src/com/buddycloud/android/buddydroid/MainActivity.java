package com.buddycloud.android.buddydroid;

import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TabActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.TabHost;

public class MainActivity extends TabActivity {
    private TabHost mTabHost;
    private Intent backgroungService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(getString(R.string.app_name));

        mTabHost = getTabHost();
        mTabHost.addTab(mTabHost.newTabSpec("roster").setIndicator("Roster")
                .setContent(new Intent(this, RosterActivity.class)));
        mTabHost.addTab(mTabHost.newTabSpec("channels")
                .setIndicator("Channels").setContent(
                        new Intent(this, ChannelActivity.class)));
        mTabHost.addTab(mTabHost.newTabSpec("nearby").setIndicator("NearBy")
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
