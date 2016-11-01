package com.brohkahn.pingserver;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.brohkahn.loggerlibrary.LogDBHelper;
import com.brohkahn.loggerlibrary.LogEntry;

import java.util.List;
import java.util.Locale;

public class MyBroadcastReceiver extends BroadcastReceiver {
	private static final String TAG = "MyBroadcastReceiver";

	public MyBroadcastReceiver() {
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		PingDbHelper pingDbHelper = PingDbHelper.getHelper(context);
		List<Server> servers = pingDbHelper.getActiveServers();
		pingDbHelper.close();

		if (servers.size() == 0) {
			logEvent(context, "No servers found, pings not scheduled.");
		} else {
			String delayKey = context.getResources().getString(R.string.key_delay);
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
			int delay = Integer.parseInt(preferences.getString(delayKey, "5")) * 60 * 1000;
//           delay = 1000;

			// create intent and pending intent for DownloadRSSService
			Intent pingIntent = new Intent(context, MyService.class);
			pingIntent.setAction(Constants.ACTION_RESCHEDULE_PINGS);

			// create pending intent, cancel (if already running), and reschedule
			PendingIntent schedulePingIntent = PendingIntent.getService(context, 0, pingIntent, 0);

			AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
			alarmManager.cancel(schedulePingIntent);
			alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, 0, delay, schedulePingIntent);

			logEvent(context, String.format(Locale.US, "Scheduled pingIntent every %d minutes", delay));
		}
	}

	public void logEvent(Context context, String message) {
		Log.d(TAG, "onReceive(Context context, Intent intent): " + message);

		LogDBHelper helper = LogDBHelper.getHelper(context);
		helper.saveLogEntry(message, null, TAG, "onReceive(Context context, Intent intent)", LogEntry.LogLevel.Trace);
		helper.close();
	}
}
