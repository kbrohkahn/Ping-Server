package com.brohkahn.pingserver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.brohkahn.loggerlibrary.LogDBHelper;
import com.brohkahn.loggerlibrary.LogEntry;

public class MyReceiver extends BroadcastReceiver {
    public MyReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        LogDBHelper.saveLogEntry(context,
                "Received device start up broadcast",
                null,
                "MyReceiver",
                "onReceive(Context context, Intent intent)",
                LogEntry.LogLevel.Trace);

        Intent serviceIntent = new Intent(context, MyService.class);
        context.startService(serviceIntent);
    }
}
