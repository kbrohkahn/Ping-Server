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
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MyService extends Service {
    public static final String TAG = "MyService";

    public static boolean isRunning = false;

    private final int TIMEOUT = 5000;
    private final int NOTIFICATION_ID = 412;

    private Timer timer;
    private String server;

    public MyService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final Resources resources = getResources();
        String serverKey = resources.getString(R.string.server_key);
        String delayKey = resources.getString(R.string.delay_key);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        server = preferences.getString(serverKey, "192.168.1.1");
        int delay = preferences.getInt(delayKey, 5) * 60 * 1000;

        logEvent(String.format(Locale.US, "Service starting, will ping %s every %d ms.", server, delay),
                "PingServerTask",
                LogEntry.LogLevel.Warning);
        sendToast("Sending first ping");

        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                pingServer();
            }
        }, 0, delay);

        isRunning = true;

        return super.onStartCommand(intent, flags, startId);
    }

    public void sendToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy() {
        String message = String.format(Locale.US, "Stopping pings of %s.", server);
        sendToast(message);
        logEvent(message, "PingServerTask", LogEntry.LogLevel.Warning);

        isRunning = false;

        timer.cancel();
        timer.purge();
        timer = null;
        super.onDestroy();
    }

    private void pingServer() {
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            new PingServerTask().execute(null, null);
        }
    }

    private class PingServerTask extends AsyncTask<Void, Void, Integer> {
        protected Integer doInBackground(Void... params) {
            int result;
            try {
                result = InetAddress.getByName(server).isReachable(TIMEOUT) ? Ping.PING_SUCCESS : Ping.PING_FAIL;
            } catch (UnknownHostException e) {
                result = Ping.PING_ERROR_HOST;
                logEvent(String.format(Locale.US, "UnknownHostException when pinging %s.", server),
                        "PingServerTask",
                        LogEntry.LogLevel.Warning);
            } catch (IOException e) {
                result = Ping.PING_ERROR_IO;
                logEvent(String.format(Locale.US, "IOException when pinging %s.", server),
                        "PingServerTask",
                        LogEntry.LogLevel.Warning);

            }
            return result;

        }

        protected void onPostExecute(Integer result) {
            savePingResult(result);

            if (result != Ping.PING_SUCCESS) {
                logEvent(String.format(Locale.US, "Failed to ping %s.", server),
                        "PingServerTask",
                        LogEntry.LogLevel.Message);
                sentFailNotification();
            } else {
                logEvent(String.format(Locale.US, "Successfully pinged %s.", server),
                        "PingServerTask",
                        LogEntry.LogLevel.Message);
            }
        }
    }

    public void savePingResult(int result) {
        PingResultsDbHelper helper = new PingResultsDbHelper(this);
        helper.savePing(server, result);
        helper.close();
    }

    public void sentFailNotification() {
        final Resources resources = getResources();
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(resources.getString(R.string.notification_title))
                        .setVibrate(new long[]{1000, 1000})
                        .setLights(Color.BLUE, 1000, 3000)
                        .setContentText(String.format(resources.getString(R.string.notification_text), server));
        mBuilder.setSound(Settings.System.DEFAULT_NOTIFICATION_URI);

        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());

    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private void logEvent(String message, String function, LogEntry.LogLevel level) {
        LogDBHelper.saveLogEntry(this, message, null, TAG, function, level);
    }
}
