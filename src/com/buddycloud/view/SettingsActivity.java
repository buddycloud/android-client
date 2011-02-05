package com.buddycloud.view;

import java.util.regex.Pattern;

import com.buddycloud.BuddycloudService;
import com.buddycloud.R;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;
import android.util.Log;
import android.widget.Toast;

public class SettingsActivity extends PreferenceActivity {

	private SharedPreferences prefs;
	private Pattern jidPattern = Pattern.compile("..*@..*\\....*");

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.settings);

		prefs = PreferenceManager.getDefaultSharedPreferences(this);

		findPreference("jid").setSummary(prefs.getString("jid", ""));
		findPreference("password").setSummary("Your password");

		findPreference("jid").setOnPreferenceChangeListener(
				new OnPreferenceChangeListener() {
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {
						String newJid = newValue.toString();
						if (jidPattern.matcher(newJid).matches()) {
							prefs.edit().putString("jid", newJid).commit();
							preference.setSummary(newJid);

							Log.i("Settings changed",
									"restarting background service");
							Intent intent = new Intent(SettingsActivity.this,
									BuddycloudService.class);
							stopService(intent);
							startService(intent);
						} else {
							Toast.makeText(SettingsActivity.this,
									"Invalid Jabber Id!", Toast.LENGTH_LONG)
									.show();
						}

						return false;
					}
				});
		findPreference("password").setOnPreferenceChangeListener(
				new OnPreferenceChangeListener() {
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {
						prefs.edit()
							.putString("password", newValue.toString())
							.commit();
						Log.i("Settings changed",
								"restarting background service");
						Intent intent = new Intent(SettingsActivity.this,
								BuddycloudService.class);
						stopService(intent);
						startService(intent);
						return false;
					}
				});

	}

}
