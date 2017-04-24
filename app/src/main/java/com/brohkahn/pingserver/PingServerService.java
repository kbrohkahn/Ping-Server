package com.brohkahn.pingserver;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import com.brohkahn.loggerlibrary.LogDBHelper;
import com.brohkahn.loggerlibrary.LogEntry;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class PingServerService extends IntentService {
	public static final String TAG = "PingServerService";


	public PingServerService() {
		super("PingServerService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		String intentSource = intent.getStringExtra(Constants.KEY_INTENT_SOURCE);
		boolean isFromReceiver = intentSource != null && !intentSource.equals(PingServerReceiver.TAG);

		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		if (cm.getActiveNetworkInfo() == null) {
			Toast.makeText(this, "Not connected to network, unable to ping servers.", Toast.LENGTH_SHORT).show();
		} else if (intent.getAction().equals(Constants.ACTION_PING)) {
			Resources resources = getResources();
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

			boolean useSchedule = preferences.getBoolean(resources.getString(R.string.key_use_schedule), false);
			int startTime = Integer.parseInt(preferences.getString(resources.getString(R.string.key_schedule_start_time)
					, resources.getString(R.string.default_schedule_start_time)));
			int endTime = Integer.parseInt(preferences.getString(resources.getString(R.string.key_schedule_end_time)
					, resources.getString(R.string.default_schedule_end_time)));
			int currentTime = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) * 100
					+ Calendar.getInstance().get(Calendar.MINUTE);
			if (!isFromReceiver || !useSchedule || (startTime < currentTime && currentTime < endTime)) {
				// get servers
				PingDbHelper pingDbHelper = PingDbHelper.getHelper(this);
				List<Server> servers = pingDbHelper.getActiveServers();
				pingDbHelper.close();

				// log start
				String logMessage = String.format(Locale.US, "Starting to ping %d servers", servers.size());
				logEvent(logMessage, "PingServerTask", LogEntry.LogLevel.Trace);

				// get settings
				int timeout = Integer.parseInt(preferences.getString(resources.getString(R.string.key_timeout), "1")) *
						1000;
				int retries = Integer.parseInt(preferences.getString(resources.getString(R.string.key_retries), "5"));
				int retryDelay = Integer.parseInt(preferences.getString(resources.getString(R.string.key_retries_delay),
						"1"));


				// instantiate variables for notification
				int worstPing = Constants.PING_SUCCESS;
				String failMessage = "Failed to ping servers: ";

				for (Server server : servers) {
					int result = Constants.PING_FAIL;

					String urlWithHttp;
					String urlWithoutHttp;
					if (server.name.indexOf("http") == 0) {
						urlWithHttp = server.name;
						urlWithoutHttp = server.name.replace("http://", "").replace("https://", "");
					} else {
						urlWithHttp = "http://" + server.name;
						urlWithoutHttp = server.name;
					}


					for (int tryCount = 0; tryCount < retries; tryCount++) {
						try {
							boolean reachable = InetAddress.getByName(urlWithoutHttp).isReachable(timeout);
							if (reachable) {
								result = Constants.PING_SUCCESS;
								break;
							}
						} catch (UnknownHostException e) {
							logMessage = "UnknownHostException when pinging " + urlWithoutHttp;
							logEvent(logMessage, "PingServerTask", LogEntry.LogLevel.Message);
						} catch (IOException e) {
							logMessage = "IOException when pinging " + urlWithoutHttp;
							logEvent(logMessage, "PingServerTask", LogEntry.LogLevel.Message);
						}

						try {
							// try various java methods
							URL url = new URL(urlWithHttp);

							HttpURLConnection connection = (HttpURLConnection) url.openConnection();
							connection.setConnectTimeout(timeout);
							connection.setRequestMethod("GET");
							connection.setRequestProperty("Connection", "close");
							connection.setReadTimeout(timeout + 5000);
							connection.connect();

							if (connection.getResponseCode() == 200) {
								result = Constants.PING_SUCCESS;
								break;
							} else {
								result = Constants.PING_FAIL;
							}
						} catch (MalformedURLException e) {
							result = Constants.PING_ERROR_HOST;
							logMessage = "MalformedURLException when pinging " + urlWithHttp;
							logEvent(logMessage, "PingServerTask", LogEntry.LogLevel.Message);
						} catch (IOException e) {
							result = Constants.PING_ERROR_IO;
							logMessage = "IOException when pinging " + server.name;
							logEvent(logMessage, "PingServerTask", LogEntry.LogLevel.Message);
						}


						try {
							Thread.sleep(1000 * retryDelay);
						} catch (InterruptedException e) {
							logEvent("InterruptedException when trying to sleep thread to delay retry",
									"PingServerTask",
									LogEntry.LogLevel.Warning);
						}

					}


					if (result != Constants.PING_SUCCESS) {
						logMessage = String.format(Locale.US, "Failed to ping %s.", server.name);
						logEvent(logMessage, "PingServerTask", LogEntry.LogLevel.Message);

						worstPing = server.lastResult;
						failMessage += server.name + ", ";
					} else {
						logMessage = String.format(Locale.US, "Successfully pinged %s.", server.name);
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
				} else {
					NotificationManager mNotificationManager =
							(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
					mNotificationManager.cancel(Constants.NOTIFICATION_ID);
				}

				// log end
				logEvent("Finished pinging servers", "PingServerTask", LogEntry.LogLevel.Trace);

				sendBroadcast(new Intent(Constants.ACTION_PINGS_UPDATED));
			}
		}

		if (isFromReceiver) {
			PingServerReceiver.completeWakefulIntent(intent);
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
						.setVibrate(new long[]{250, 250})
						.setLights(Color.BLUE, 1000, 3000)
						.setContentText(message)
						.setContentIntent(pendingIntent);

		mBuilder.setSound(Settings.System.DEFAULT_NOTIFICATION_URI);

		NotificationManager mNotificationManager =
				(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancel(Constants.NOTIFICATION_ID);
		mNotificationManager.notify(Constants.NOTIFICATION_ID, mBuilder.build());
	}

	private void logEvent(String message, String function, LogEntry.LogLevel level) {
		LogDBHelper helper = LogDBHelper.getHelper(this);
		helper.saveLogEntry(message, null, TAG, function, level);
		helper.close();
	}
}
