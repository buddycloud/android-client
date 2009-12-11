package com.buddycloud.android.buddydroid;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;
import android.util.Log;
import android.widget.Toast;

public class SettingsActivity extends PreferenceActivity {

	private SharedPreferences prefs;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.settings);
		
		prefs = PreferenceManager.getDefaultSharedPreferences(this);

		// test account to start right away
		// TODO replace by some random generated test account or google
		// account..
		findPreference("jid").setSummary(prefs.getString("jid", ""));
		findPreference("password").setSummary("Your password");

		findPreference("jid").setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				preference.setSummary((String) newValue);
				String[] jid = ((String) newValue).split("\\@");
				if (jid.length == 2) {
					Toast.makeText(SettingsActivity.this, "saved.", Toast.LENGTH_LONG).show();
					prefs.edit().putString("username", jid[0]).commit();
					prefs.edit().putString("host", jid[1]).commit();
					
					Log.i("Settings changed", "restarting background service");
					Intent intent = new Intent(SettingsActivity.this, BuddycloudService.class);
					stopService(intent);
					startService(intent);
				} else {
					Toast.makeText(SettingsActivity.this, "invalid Jabber Id!", Toast.LENGTH_LONG).show();
				}
				
				return false;
			}
		});
		findPreference("password").setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				preference.setSummary((String) newValue);
				prefs.edit().putString("password", (String) newValue).commit();
				
				Log.i("Settings changed", "restarting background service");
				Intent intent = new Intent(SettingsActivity.this, BuddycloudService.class);
				stopService(intent);
				startService(intent);
				return false;
			}
		});

		
	}
	
	
	
}
