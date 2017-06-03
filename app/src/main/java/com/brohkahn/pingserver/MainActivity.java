package com.brohkahn.pingserver;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.AlertDialog;
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
import android.widget.Toast;

import com.brohkahn.loggerlibrary.LogDBHelper;
import com.brohkahn.loggerlibrary.LogEntry;
import com.brohkahn.loggerlibrary.LogViewList;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
	private static final String TAG = "MainActivity";

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

		NotificationManager mNotificationManager =
				(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancel(Constants.NOTIFICATION_ID);
	}

	private BroadcastReceiver pingsUpdatedReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			refreshResults();
		}
	};

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

		IntentFilter intentFilter = new IntentFilter(Constants.ACTION_PINGS_UPDATED);
		registerReceiver(pingsUpdatedReceiver, intentFilter);

	}

	@Override
	protected void onPause() {
		adapter.getCursor().close();

		unregisterReceiver(pingsUpdatedReceiver);

		super.onPause();
	}


	public void refreshResults() {
		Cursor cursor = adapter.getCursor();
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}

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
			case R.id.action_refresh_pings:
				Intent intent = new Intent(this, PingServerService.class);
				intent.setAction(Constants.ACTION_PING);
				intent.putExtra(Constants.KEY_INTENT_SOURCE, TAG);
				startService(intent);

				return true;
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

	private class ServerListAdapter extends CursorAdapter {

		private SimpleDateFormat dateFormat;
		private int successColor;
		private int failColor;

		private ServerListAdapter(Context context, Cursor cursor, int flags) {
			super(context, cursor, flags);

			Resources resources = context.getResources();
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
			String dateFormatString = preferences.getString(
					resources.getString(R.string.key_date_format),
					resources.getString(R.string.default_date_format));
			dateFormat = new SimpleDateFormat(dateFormatString, Locale.US);

			successColor = ContextCompat.getColor(context, R.color.success);
			failColor = ContextCompat.getColor(context, R.color.fail);
		}

		public void bindView(View view, Context context, Cursor cursor) {
			TextView dateTextView = (TextView) view.findViewById(R.id.ping_date);

			long timeInMillis = cursor.getLong(cursor.getColumnIndexOrThrow(PingDbHelper.PingResultColumns
					.COLUMN_NAME_DATE));
			if (timeInMillis > 0) {
				Date date = new Date(timeInMillis);
				dateTextView.setText(dateFormat.format(date));
			} else {
				dateTextView.setText("");
			}

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

			view.setOnLongClickListener(new View.OnLongClickListener() {
				@Override
				public boolean onLongClick(View view) {
					showDeactivateDialog(id);
					return true;
				}
			});
		}

		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			return LayoutInflater.from(context)
					.inflate(R.layout.ping_list_item, parent, false);
		}
	}


	public void showDeactivateDialog(final int id) {
		PingDbHelper pingDbHelper = PingDbHelper.getHelper(this);
		Server server = pingDbHelper.getServer(id);
		pingDbHelper.close();

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.deactivate_title)
				.setMessage(getResources().getString(R.string.deactivate_message, server.name))
				.setPositiveButton(R.string.deactivate_positive, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						deactivateServer(id);
						dialogInterface.dismiss();
					}
				})
				.setNegativeButton(R.string.deactivate_negative, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						dialogInterface.dismiss();

					}
				});

		builder.create().show();
	}

	public void deactivateServer(int id) {
		PingDbHelper pingDbHelper = PingDbHelper.getHelper(this);
		Server server = pingDbHelper.getServer(id);
		boolean success = pingDbHelper.deactivateServer(id);
		pingDbHelper.close();

		LogEntry.LogLevel level;
		String message;
		if (success) {
			level = LogEntry.LogLevel.Message;
			message = "Server " + server.name + " successfully deactivated";
		} else {
			level = LogEntry.LogLevel.Error;
			message = "Unable to deactivate server " + server.name + ", please contact developer";
		}
		logEvent(message, "deactivateServer()", level);
		Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

		refreshResults();
	}

	private void logEvent(String message, String function, LogEntry.LogLevel level) {
		LogDBHelper helper = LogDBHelper.getHelper(this);
		helper.saveLogEntry(message, null, TAG, function, level);
		helper.close();
	}

	private void viewPings(int id) {
		Intent intent = new Intent(this, PingListView.class);
		intent.putExtra(PingListView.KEY_SERVER_ID, id);
		startActivity(intent);
	}
}
