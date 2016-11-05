package com.brohkahn.pingserver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class PingServerReceiver extends BroadcastReceiver {
	public PingServerReceiver() {
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		Intent newIntent = new Intent(context, PingServerService.class);
		newIntent.setAction(Constants.ACTION_PING);
		context.startService(newIntent);

	}
}
