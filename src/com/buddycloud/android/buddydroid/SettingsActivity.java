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

public class SettingsActivity extends PreferenceActivity {

	private SharedPreferences prefs;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.settings);
		
		prefs = PreferenceManager.getDefaultSharedPreferences(this);

		// test account to start right away
		// TODO replace by some random generated test account or google account..
		findPreference("jid").setSummary(prefs.getString("jid", "orangeman@buddycloud.com"));
		findPreference("password").setSummary(prefs.getString("password", "xxxxxx"));

		findPreference("jid").setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				preference.setSummary((String) newValue);
				prefs.edit().putString("jid", (String) newValue).commit();
				// reconnect xmpp
				Intent intent = new Intent(SettingsActivity.this, BuddycloudService.class);
				stopService(intent);
				startService(intent);
				return false;
			}
		});
		findPreference("password").setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				preference.setSummary((String) newValue);
				prefs.edit().putString("password", (String) newValue).commit();
				// reconnect xmpp
				Intent intent = new Intent(SettingsActivity.this, BuddycloudService.class);
				stopService(intent);
				startService(intent);
				return false;
			}
		});

		
	}
	
	
	
}
