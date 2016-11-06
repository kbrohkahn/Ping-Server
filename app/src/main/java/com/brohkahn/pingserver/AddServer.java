package com.brohkahn.pingserver;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;

public class AddServer extends AppCompatActivity {

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

	private void savePing() {
		String server = serverEditText.getText().toString();
		PingDbHelper helper = PingDbHelper.getHelper(getApplicationContext());
		helper.saveServer(server);
		helper.close();

		Intent intent = new Intent(this, StartTimerService.class);
		intent.setAction(Constants.ACTION_RESCHEDULE_PINGS);
		startService(intent);
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
				savePing();
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
