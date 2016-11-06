package com.brohkahn.pingserver;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

public class PingServerReceiver extends WakefulBroadcastReceiver {
	public PingServerReceiver() {
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent != null && intent.getAction().equals(Constants.ACTION_PING)) {
			Intent newIntent = new Intent(context, PingServerService.class);
			newIntent.setAction(Constants.ACTION_PING);
			startWakefulService(context, newIntent);
		}
	}
}
