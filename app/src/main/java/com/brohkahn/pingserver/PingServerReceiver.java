package com.brohkahn.pingserver;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

import com.brohkahn.loggerlibrary.LogDBHelper;
import com.brohkahn.loggerlibrary.LogEntry;

public class PingServerReceiver extends WakefulBroadcastReceiver {
	public static final String TAG = "PingServerReceiver";
	
	public PingServerReceiver() {
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent != null && intent.getAction().equals(Constants.ACTION_PING)) {
			LogDBHelper helper = LogDBHelper.getHelper(context);
			helper.saveLogEntry(TAG + " received broadcast, starting service", null, TAG, "onReceive", LogEntry.LogLevel.Trace);
			helper.close();

			Intent newIntent = new Intent(context, PingServerService.class);
			newIntent.setAction(Constants.ACTION_PING);
			newIntent.putExtra(Constants.KEY_INTENT_SOURCE, TAG);
			startWakefulService(context, newIntent);
		}
	}
}
