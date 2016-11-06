package com.brohkahn.pingserver;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

public class BootCompletedReceiver extends WakefulBroadcastReceiver {

	public BootCompletedReceiver() {
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent != null && intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
			Intent newIntent = new Intent(context, StartTimerService.class);
			newIntent.setAction(Constants.ACTION_RESCHEDULE_PINGS);
			startWakefulService(context, newIntent);
		}
	}
}
