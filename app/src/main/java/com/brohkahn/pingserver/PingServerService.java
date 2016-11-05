package com.brohkahn.pingserver;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;

import com.brohkahn.loggerlibrary.LogDBHelper;
import com.brohkahn.loggerlibrary.LogEntry;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Locale;

public class PingServerService extends IntentService {
	public static final String TAG = "PingServerService";

	private static final int NOTIFICATION_ID = 12341435;

	public PingServerService() {
		super("PingServerService");
	}


	@Override
	protected void onHandleIntent(Intent intent) {
		if (intent != null && intent.getAction().equals(Constants.ACTION_PING)) {
			// get servers
			PingDbHelper pingDbHelper = PingDbHelper.getHelper(this);
			List<Server> servers = pingDbHelper.getActiveServers();
			pingDbHelper.close();

			// log start
			String logMessage = String.format(Locale.US, "Starting to ping %d servers", servers.size());
			logEvent(logMessage, "PingServerTask", LogEntry.LogLevel.Trace);

			// get settings
			String retriesKey = getResources().getString(R.string.key_retries);
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
			int retries = Integer.parseInt(preferences.getString(retriesKey, "5"));
			int timeout = Integer.parseInt(preferences.getString(retriesKey, "1")) * 1000;

			// instantiate variables for notification
			int worstPing = Constants.PING_SUCCESS;
			String failMessage = "Failed to ping servers: ";

			for (Server server : servers) {
				int result = Constants.PING_FAIL;

				for (int tryCount = 0; tryCount < retries; tryCount++) {
					try {
						boolean reachable = InetAddress.getByName(server.name).isReachable(timeout);

						if (reachable) {
							result = Constants.PING_SUCCESS;
							break;
						} else {
							result = Constants.PING_FAIL;
						}
					} catch (UnknownHostException e) {
						result = Constants.PING_ERROR_HOST;

						logMessage = String.format(Locale.US, "UnknownHostException when pinging %s.", server.name);
						logEvent(logMessage, "PingServerTask", LogEntry.LogLevel.Trace);
					} catch (IOException e) {
						result = Constants.PING_ERROR_IO;

						logMessage = String.format(Locale.US, "IOException when pinging %s.", server.name);
						logEvent(logMessage, "PingServerTask", LogEntry.LogLevel.Trace);
					}
				}


				if (result != Constants.PING_SUCCESS) {
					logMessage = String.format(Locale.US, "Failed to ping %s.", server.name);
					logEvent(logMessage, "PingServerTask", LogEntry.LogLevel.Trace);

					worstPing = server.lastResult;
					failMessage += server.name + ", ";
				} else {
					logMessage = String.format(Locale.US, "Successfully pinged %s.", server);
					logEvent(logMessage, "PingServerTask", LogEntry.LogLevel.Trace);
				}

				// store result in server object for batch saving
				server.lastResult = result;
			}

			// save results
			PingDbHelper helper = PingDbHelper.getHelper(this);
			helper.savePingResults(servers);
			helper.close();

			// see if any pings failed
			if (worstPing != Constants.PING_SUCCESS) {
				sentFailNotification(failMessage.substring(0, failMessage.length() - 2));
			}

			// log end
			logEvent("Finished pinging servers", "PingServerTask", LogEntry.LogLevel.Trace);
		}
	}

	private void sentFailNotification(String message) {
		Intent intent = new Intent(this, MainActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

		Resources resources = getResources();
		NotificationCompat.Builder mBuilder =
				new NotificationCompat.Builder(this)
						.setSmallIcon(R.drawable.ic_notification)
						.setContentTitle(resources.getString(R.string.notification_title))
						.setVibrate(new long[]{1000, 1000})
						.setLights(Color.BLUE, 1000, 3000)
						.setContentText(message)
						.setContentIntent(pendingIntent);

		mBuilder.setSound(Settings.System.DEFAULT_NOTIFICATION_URI);

		NotificationManager mNotificationManager =
				(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());

	}

	private void logEvent(String message, String function, LogEntry.LogLevel level) {
		LogDBHelper helper = LogDBHelper.getHelper(this);
		helper.saveLogEntry(message, null, TAG, function, level);
		helper.close();
	}
}
