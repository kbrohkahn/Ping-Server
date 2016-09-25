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

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;

public class MyService extends Service {
    public static boolean isRunning = false;

    private final int TIMEOUT = 5000;
    private final int NOTIFICATION_ID = 132432;

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
        sendToast("Stopping pings");

        isRunning = false;
        timer.cancel();
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
            } catch (IOException e) {
                result = Ping.PING_ERROR_IO;
            }
            return result;
        }

        protected void onPostExecute(Integer result) {
            savePingResult(result);

            if (result != Ping.PING_SUCCESS) {
                sentFailNotification();
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
}
