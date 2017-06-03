package com.brohkahn.pingserver;


import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatPreferenceActivity {
	public static final String TAG = "SettingsActivity";

	/**
	 * A preference value change listener that updates the preference's summary
	 * to reflect its new value.
	 */
	private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange(Preference preference, Object value) {
			String stringValue = value.toString();

			if (preference instanceof ListPreference) {
				// For list preferences, look up the correct display value in
				// the preference's 'entries' list.
				ListPreference listPreference = (ListPreference) preference;
				int index = listPreference.findIndexOfValue(stringValue);

				// Set the summary to reflect the new value.
				preference.setSummary(
						index >= 0
								? listPreference.getEntries()[index]
								: null);

			} else {
				// For all other preferences, set the summary to the value's
				// simple string representation.
				preference.setSummary(stringValue);
			}
			return true;
		}
	};


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setupActionBar();

		getFragmentManager().beginTransaction()
				.replace(android.R.id.content, new SettingsFragment())
				.commit();

	}

	/**
	 * Set up the {@link android.app.ActionBar}, if the API is available.
	 */
	private void setupActionBar() {
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			// Show the Up button in the action bar.
			actionBar.setDisplayHomeAsUpEnabled(true);
		}
	}


	public static class SettingsFragment extends PreferenceFragment {

		@Override
		public void onStop() {
			Intent intent = new Intent(getActivity(), PingServerService.class);
			intent.setAction(Constants.ACTION_PING);
			intent.putExtra(Constants.KEY_INTENT_SOURCE, TAG);
			getActivity().startService(intent);

			super.onStop();
		}

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.settings);
			setHasOptionsMenu(true);

			Resources resources = getResources();

			bindPreferenceSummaryToValue(resources.getString(R.string.key_delay));
			bindPreferenceSummaryToValue(resources.getString(R.string.key_timeout));
			bindPreferenceSummaryToValue(resources.getString(R.string.key_retries));
			bindPreferenceSummaryToValue(resources.getString(R.string.key_retries_delay));
			bindPreferenceSummaryToValue(resources.getString(R.string.key_date_format));
			bindPreferenceSummaryToValue(resources.getString(R.string.key_schedule_end_time));
			bindPreferenceSummaryToValue(resources.getString(R.string.key_schedule_start_time));

			Preference useStrictAlarmsPreference = findPreference(resources.getString(R.string.key_strict_alarm));
			if (useStrictAlarmsPreference != null) {
				useStrictAlarmsPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
					@Override
					public boolean onPreferenceChange(Preference preference, Object newValue) {
						if ((boolean) newValue) {

							checkForDozePermission();
						}
						return true;
					}
				});
			}
		}

		private void checkForDozePermission() {
			if (Build.VERSION.SDK_INT >= 23) {
				PowerManager pm = (PowerManager) getContext().getSystemService(Context.POWER_SERVICE);
				String packageName = getContext().getPackageName();

				if (!pm.isIgnoringBatteryOptimizations(packageName)) {
					Intent whiteListIntent = new Intent();
					whiteListIntent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
					whiteListIntent.setData(Uri.parse("package:" + packageName));
					startActivity(whiteListIntent);
				}
			}
		}

		private void bindPreferenceSummaryToValue(String key) {
			Preference preference = findPreference(key);
			if (preference != null) {
				preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

				// Trigger the listener immediately with the preference's
				// current value.
				sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
						PreferenceManager
								.getDefaultSharedPreferences(preference.getContext())
								.getString(preference.getKey(), ""));
			}
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == android.R.id.home) {
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
