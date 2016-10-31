package com.brohkahn.pingserver;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PingListView extends AppCompatActivity {
	public static String KEY_SERVER_ID = "serverId";

	private int serverId;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_view_pings);

		setSupportActionBar((Toolbar) findViewById(R.id.ping_list_view_toolbar));

		if (getSupportActionBar() != null) {
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		}

		serverId = getIntent().getIntExtra(KEY_SERVER_ID, -1);

		CursorLoader cursorLoader = new CursorLoader(this, Uri.EMPTY, null, null, null, null) {
			@Override
			public Cursor loadInBackground() {
				PingDbHelper dbHelper = PingDbHelper.getHelper(getApplicationContext());
				SQLiteDatabase db = dbHelper.getReadableDatabase();
				return db.rawQuery(dbHelper.getPingSelect(serverId), null);
			}
		};

		ListView listView = (ListView) findViewById(R.id.ping_list_view);
		listView.setAdapter(new PingListAdapter(getApplicationContext(), cursorLoader.loadInBackground(), 0));

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.ping_list_view, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
			case android.R.id.home:
				finish();
				return true;
			case R.id.action_deactivate:
				PingDbHelper pingDbHelper = PingDbHelper.getHelper(this);
				pingDbHelper.deactivateServer(serverId);
				pingDbHelper.close();
				finish();
				return true;

			default:
				return super.onOptionsItemSelected(item);
		}
	}

	private static class PingListAdapter extends CursorAdapter {
		private SimpleDateFormat dateFormat;
		private int successColor;
		private int failColor;

		private PingListAdapter(Context context, Cursor cursor, int flags) {
			super(context, cursor, flags);

			dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US);

			successColor = ContextCompat.getColor(context, R.color.success);
			failColor = ContextCompat.getColor(context, R.color.fail);
		}

		public void bindView(View view, Context context, Cursor cursor) {
			final int result = cursor.getInt(cursor.getColumnIndexOrThrow(PingDbHelper.PingResultColumns.COLUMN_NAME_RESULT));

			if (result == Constants.PING_SUCCESS) {
				view.setBackgroundColor(successColor);
			} else {
				view.setBackgroundColor(failColor);
			}

			TextView dateTextView = (TextView) view.findViewById(R.id.ping_date);
			Date date = new Date();
			date.setTime(cursor.getLong(cursor.getColumnIndexOrThrow(PingDbHelper.PingResultColumns.COLUMN_NAME_DATE)));
			dateTextView.setText(dateFormat.format(date));

			TextView serverTextView = (TextView) view.findViewById(R.id.ping_server);
			serverTextView.setText(cursor.getString(cursor.getColumnIndexOrThrow(PingDbHelper.ServerColumns.COLUMN_NAME_SERVER)));
		}

		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			return LayoutInflater.from(context).inflate(R.layout.ping_list_item, parent, false);
		}
	}
}
