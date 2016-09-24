package com.brohkahn.pingserver;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private final int PERMISSION_REQUEST_INTERNET = 0;

    private EditText serverEditText;
    private NumberPicker delayNumberPicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Resources resources = getResources();
        String serverKey = resources.getString(R.string.server_key);
        String delayKey = resources.getString(R.string.delay_key);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String server = preferences.getString(serverKey, "192.168.1.1");
        int delay = preferences.getInt(delayKey, 5);

        serverEditText = (EditText) findViewById(R.id.server_edit_text);
        serverEditText.setText(server);

        delayNumberPicker = (NumberPicker) findViewById(R.id.delay_number_picker);
        delayNumberPicker.setMinValue(1);
        delayNumberPicker.setMaxValue(60);
        delayNumberPicker.setValue(delay);
    }

    @Override
    protected void onResume() {
        super.onResume();

        refreshResults();
    }

    public void refreshResultsButtonClick(View view) {
        refreshResults();
    }

    public void refreshResults() {
        TextView serviceRunning = (TextView) findViewById(R.id.service_running_label);
        if (MyService.isRunning) {
            PingResultsDbHelper helper = new PingResultsDbHelper(this);
            Ping lastPing = helper.getLastPing();
            helper.close();

            int stringId;
            int colorId;
            if (lastPing.result == Ping.PING_SUCCESS) {
                stringId = R.string.service_running_label_success;
                colorId = R.color.success;
            } else {
                stringId = R.string.service_running_label_fail;
                colorId = R.color.fail;
            }
            serviceRunning.setText(String.format(getResources().getString(stringId), lastPing.server, lastPing.date));
            serviceRunning.setTextColor(getResources().getColor(colorId));
        } else {
            serviceRunning.setText(getResources().getText(R.string.service_running_label_off));
            serviceRunning.setTextColor(getResources().getColor(R.color.inactive));
        }
    }

    public void startServiceButtonClick(View view) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET}, PERMISSION_REQUEST_INTERNET);
        } else {
            startPingService();
        }
    }

    public void stopServiceButtonClick(View view) {
        Intent intent = new Intent(this, MyService.class);
        stopService(intent);
    }

    public void startPingService() {
        // save preferences
        final Resources resources = getResources();
        String serverKey = resources.getString(R.string.server_key);
        String delayKey = resources.getString(R.string.delay_key);

        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putString(serverKey, serverEditText.getText().toString());
        editor.putInt(delayKey, delayNumberPicker.getValue());
        editor.apply();

        Intent intent = new Intent(this, MyService.class);
        stopService(intent);
        startService(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_INTERNET: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startPingService();
                } else {
                    Toast.makeText(this, "Cannot ping a server without internet permission.", Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        }
    }

}
