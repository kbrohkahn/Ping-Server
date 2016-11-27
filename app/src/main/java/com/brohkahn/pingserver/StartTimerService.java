package com.brohkahn.pingserver;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.brohkahn.loggerlibrary.LogDBHelper;
import com.brohkahn.loggerlibrary.LogEntry;

import java.util.List;
import java.util.Locale;

public class StartTimerService extends IntentService {
	private static final String TAG = "BootCompletedReceiver";

	private static final int MS_MINUTE = 60 * 1000;

	public StartTimerService() {
		super("StartTimerService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		if (intent != null && intent.getAction().equals(Constants.ACTION_RESCHEDULE_PINGS)) {
			PingDbHelper pingDbHelper = PingDbHelper.getHelper(this);
			List<Server> servers = pingDbHelper.getActiveServers();
			pingDbHelper.close();

			if (servers.size() == 0) {
				logEvent("No servers found, pings not scheduled");
			} else {
				String delayKey = getResources().getString(R.string.key_delay);
				SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
				int delay = Integer.parseInt(preferences.getString(delayKey, "5"));

				// create intent and pending intent for DownloadRSSService
				Intent pingIntent = new Intent(this, PingServerReceiver.class);
				pingIntent.setAction(Constants.ACTION_PING);

				// create pending intent, cancel (if already running), and reschedule
				PendingIntent schedulePingIntent = PendingIntent.getBroadcast(this, Constants.BROADCAST_PING_CODE, pingIntent, PendingIntent.FLAG_UPDATE_CURRENT);

				AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
				alarmManager.cancel(schedulePingIntent);
				alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, 0, MS_MINUTE * delay, schedulePingIntent);

				logEvent(String.format(Locale.US, "Scheduled pingIntent every %d minutes", delay));
			}

			String intentSource = intent.getStringExtra(Constants.KEY_INTENT_SOURCE);
			if (intentSource != null && intentSource.equals(BootCompletedReceiver.TAG)) {
				BootCompletedReceiver.completeWakefulIntent(intent);
			}
		}
	}


	public void logEvent(String message) {
		Log.d(TAG, "onReceive(Context context, Intent intent): " + message);

		LogDBHelper helper = LogDBHelper.getHelper(this);
		helper.saveLogEntry(message, null, TAG, "onReceive(Context context, Intent intent)", LogEntry.LogLevel.Trace);
		helper.close();
	}
}
