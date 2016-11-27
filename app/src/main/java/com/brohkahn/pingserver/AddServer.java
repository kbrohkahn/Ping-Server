package com.brohkahn.pingserver;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;

public class AddServer extends AppCompatActivity {
	public static final String TAG = "AddServer";

	private EditText serverEditText;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_add_ping);

		setSupportActionBar((Toolbar) findViewById(R.id.add_ping_toolbar));

		if (getSupportActionBar() != null) {
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		}

		serverEditText = (EditText) findViewById(R.id.server_edit_text);

	}

	@Override
	public void onBackPressed() {
		String serverName = serverEditText.getText().toString();

		if (serverName.equals("")) {
			super.onBackPressed();
		} else {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.save_title)
					.setMessage(getResources().getString(R.string.save_message, serverName))
					.setPositiveButton(R.string.save_positive, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialogInterface, int i) {
							saveServer();
							dialogInterface.dismiss();
							finish();
						}
					})
					.setNegativeButton(R.string.save_negative, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialogInterface, int i) {
							dialogInterface.dismiss();
							finish();

						}
					});

			builder.create().show();
		}
	}

	private void saveServer() {
		String serverName = serverEditText.getText().toString();
		if (!serverName.equals("")) {
			PingDbHelper helper = PingDbHelper.getHelper(getApplicationContext());
			helper.saveServer(serverName);
			helper.close();

			Intent intent = new Intent(this, StartTimerService.class);
			intent.setAction(Constants.ACTION_RESCHEDULE_PINGS);
			intent.putExtra(Constants.KEY_INTENT_SOURCE, TAG);
			startService(intent);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.add_ping, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
			case android.R.id.home:
				finish();
				return true;
			case R.id.action_save:
				saveServer();
				finish();
				return true;
			case R.id.action_cancel:
				finish();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
}
