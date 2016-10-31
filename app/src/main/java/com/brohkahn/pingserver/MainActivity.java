package com.brohkahn.pingserver;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.brohkahn.loggerlibrary.LogViewList;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

	public ServerListAdapter adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		Toolbar toolbar = (Toolbar) findViewById(R.id.activity_main_toolbar);
		setSupportActionBar(toolbar);

		FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.activity_main_fab);
		fab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				startAddPingActivity();
			}
		});

		adapter = new ServerListAdapter(this, getCursorLoader().loadInBackground(), 0);
		((ListView) findViewById(R.id.main_list_view)).setAdapter(adapter);

		if (!MyService.isRunning) {
			startService(new Intent(getApplicationContext(), MyService.class));
		}
	}

	private CursorLoader getCursorLoader() {
		return new CursorLoader(this, Uri.EMPTY, null, null, null, null) {
			@Override
			public Cursor loadInBackground() {
				PingDbHelper dbHelper = PingDbHelper.getHelper(getApplicationContext());
				SQLiteDatabase db = dbHelper.getReadableDatabase();

				return db.rawQuery(dbHelper.getServerSelect(), null);
			}
		};
	}

	private void startAddPingActivity() {
		startActivity(new Intent(this, AddServer.class));
	}

	@Override
	protected void onResume() {
		super.onResume();

		refreshResults();
	}


	@Override
	protected void onPause() {
		adapter.getCursor().close();

		super.onPause();
	}


	public void refreshResults() {
		adapter.swapCursor(getCursorLoader().loadInBackground());
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
			case R.id.action_view_pings:
				viewPings(-1);
				return true;
			case R.id.action_view_logs:
				startActivity(new Intent(this, LogViewList.class));
				return true;
			case R.id.action_about:
				startActivity(new Intent(this, AboutActivity.class));
				return true;
			case R.id.action_settings:
				startActivity(new Intent(this, SettingsActivity.class));
				return true;

			default:
				return super.onOptionsItemSelected(item);
		}
	}


	public class ServerListAdapter extends CursorAdapter {

		private SimpleDateFormat dateFormat;
		private int successColor;
		private int failColor;

		private ServerListAdapter(Context context, Cursor cursor, int flags) {
			super(context, cursor, flags);

			dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US);

			successColor = ContextCompat.getColor(context, R.color.success);
			failColor = ContextCompat.getColor(context, R.color.fail);
		}

		public void bindView(View view, Context context, Cursor cursor) {
			TextView dateTextView = (TextView) view.findViewById(R.id.ping_date);
			Date date = new Date();
			date.setTime(cursor.getLong(cursor.getColumnIndexOrThrow(PingDbHelper.PingResultColumns.COLUMN_NAME_DATE)));
			dateTextView.setText(dateFormat.format(date));

			TextView serverTextView = (TextView) view.findViewById(R.id.ping_server);
			serverTextView.setText(cursor.getString(cursor.getColumnIndexOrThrow(PingDbHelper.ServerColumns.COLUMN_NAME_SERVER)));

			int result = cursor.getInt(cursor.getColumnIndexOrThrow(PingDbHelper.PingResultColumns.COLUMN_NAME_RESULT));
			if (result == Constants.PING_SUCCESS) {
				view.setBackgroundColor(successColor);
			} else {
				view.setBackgroundColor(failColor);
			}

			final int id = cursor.getInt(cursor.getColumnIndexOrThrow(PingDbHelper.ServerColumns._ID));
			view.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					viewPings(id);
				}
			});
		}

		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			return LayoutInflater.from(context)
					.inflate(R.layout.ping_list_item, parent, false);
		}
	}

	private void viewPings(int id) {
		Intent intent = new Intent(this, PingListView.class);
		intent.putExtra(PingListView.KEY_SERVER_ID, id);
		startActivity(intent);
	}
}
