package com.brohkahn.pingserver;

import android.Manifest;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import com.brohkahn.loggerlibrary.LogDBHelper;
import com.brohkahn.loggerlibrary.LogEntry;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MyService extends Service {
	public static final String TAG = "MyService";

	public static boolean isRunning = false;

	private Timer timer = new Timer();
	private PingServerTask pingServerTask;

	//	private static String server;
	private static int retries;


	public MyService() {
	}

	@Override
	public void onCreate() {
		super.onCreate();

		isRunning = true;

		Resources resources = getResources();
		String delayKey = resources.getString(R.string.key_delay);
		String retriesKey = resources.getString(R.string.key_retries);

		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		retries = Integer.parseInt(preferences.getString(retriesKey, "3"));
		int delay = Integer.parseInt(preferences.getString(delayKey, "5")) * 60 * 1000;
//           delay = 1000;

		logEvent(String.format(Locale.US, "Service starting, will ping every %d ms.", delay),
				"PingServerTask",
				LogEntry.LogLevel.Warning);
		sendToast("Sending first ping");

		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				pingServer();
			}
		}, 0, delay);

	}

	public void sendToast(String message) {
		Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onDestroy() {
		String message = "Stopping pings.";
		sendToast(message);
		logEvent(message, "PingServerTask", LogEntry.LogLevel.Warning);

		isRunning = false;

		if (pingServerTask != null) {
			pingServerTask.cancel(true);
			pingServerTask = null;
		}

		timer.cancel();
		timer.purge();
		timer = null;

		super.onDestroy();
	}

	private void pingServer() {
		int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET);
		if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
			pingServerTask = new PingServerTask(getApplicationContext());
			pingServerTask.execute(null, null);
		}
	}

	private static class PingServerTask extends AsyncTask<Void, Void, Void> {
		private Context context;

		private final int TIMEOUT = 5000;
		private final int NOTIFICATION_ID = 412;

		private List<Server> servers;

		private PingServerTask(Context context) {
			this.context = context;

			PingDbHelper pingDbHelper = PingDbHelper.getHelper(context);
			servers = pingDbHelper.getActiveServers();
			pingDbHelper.close();
		}

		protected Void doInBackground(Void... params) {
			for (Server server : servers) {
				int result = Constants.PING_FAIL;
				int count = 0;
				while (count < retries && result != Constants.PING_SUCCESS) {
					try {
						result = InetAddress.getByName(server.name).isReachable(TIMEOUT) ? Constants.PING_SUCCESS :
								Constants.PING_FAIL;
					} catch (UnknownHostException e) {
						result = Constants.PING_ERROR_HOST;
						logEvent(String.format(Locale.US, "UnknownHostException when pinging %s.", server.name),
								"PingServerTask",
								LogEntry.LogLevel.Warning);
					} catch (IOException e) {
						result = Constants.PING_ERROR_IO;
						logEvent(String.format(Locale.US, "IOException when pinging %s.", server.name),
								"PingServerTask",
								LogEntry.LogLevel.Warning);
					}

					if (server.lastResult != Constants.PING_SUCCESS) {
						logEvent(String.format(Locale.US, "Failed to ping %s.", server.name),
								"PingServerTask",
								LogEntry.LogLevel.Message);
					} else {
						logEvent(String.format(Locale.US, "Successfully pinged %s.", server),
								"PingServerTask",
								LogEntry.LogLevel.Message);
					}
				}

				server.lastResult = result;
			}

			PingDbHelper helper = PingDbHelper.getHelper(context);
			helper.savePingResults(servers);
			helper.close();

			return null;
		}

		protected void onPostExecute(Void result) {
			int worstPing = Constants.PING_SUCCESS;
			String failMessage = "Failed to ping servers: ";

			for (Server server : servers) {
				if (server.lastResult != Constants.PING_SUCCESS) {
					worstPing = server.lastResult;
					failMessage += server.name + ", ";
				}
			}

			if (worstPing != Constants.PING_SUCCESS) {
				sentFailNotification(failMessage.substring(0, failMessage.length() - 2));
			}
		}

		private void logEvent(String message, String function, LogEntry.LogLevel level) {
			LogDBHelper helper = LogDBHelper.getHelper(context);
			helper.saveLogEntry(message, null, TAG, function, level);
			helper.close();
		}


		private void sentFailNotification(String message) {
			Resources resources = context.getResources();
			NotificationCompat.Builder mBuilder =
					new NotificationCompat.Builder(context)
							.setSmallIcon(R.drawable.ic_notification)
							.setContentTitle(resources.getString(R.string.notification_title))
							.setVibrate(new long[]{1000, 1000})
							.setLights(Color.BLUE, 1000, 3000)
							.setContentText(message);
			mBuilder.setSound(Settings.System.DEFAULT_NOTIFICATION_URI);

			NotificationManager mNotificationManager =
					(NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
			mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());

		}
	}


	@Override
	public IBinder onBind(Intent intent) {
		// TODO: Return the communication channel to the service.
		throw new UnsupportedOperationException("Not yet implemented");
	}

	private void logEvent(String message, String function, LogEntry.LogLevel level) {
		LogDBHelper helper = LogDBHelper.getHelper(this);
		helper.saveLogEntry(message, null, TAG, function, level);
		helper.close();
	}
}
