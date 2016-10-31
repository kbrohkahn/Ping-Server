package com.brohkahn.pingserver;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

class PingDbHelper extends SQLiteOpenHelper {
	// If you change the database schema, you must increment the database version.

	private static final int DATABASE_VERSION = 1;
	private static final String DATABASE_NAME = "pings.db";

	private static final String SQL_PING_RESULT_CREATE_ENTRIES =
			"CREATE TABLE " + PingResultColumns.TABLE_NAME + " (" +
					PingResultColumns._ID + " INTEGER PRIMARY KEY, " +
					PingResultColumns.COLUMN_NAME_RELATED_SERVER + " INTEGER, " +
					PingResultColumns.COLUMN_NAME_DATE + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
					PingResultColumns.COLUMN_NAME_RESULT + " INTEGER)";

	private static final String SQL_SERVER_CREATE_ENTRIES =
			"CREATE TABLE " + ServerColumns.TABLE_NAME + " (" +
					ServerColumns._ID + " INTEGER PRIMARY KEY, " +
					ServerColumns.COLUMN_NAME_ACTIVE + " INTEGER, " +
					ServerColumns.COLUMN_NAME_SERVER + " TEXT UNIQUE)";

	private static final String SQL_DELETE_PING_RESULT_ENTRIES =
			"DROP TABLE IF EXISTS " + PingResultColumns.TABLE_NAME;

	private static final String SQL_DELETE_SERVER_ENTRIES =
			"DROP TABLE IF EXISTS " + ServerColumns.TABLE_NAME;

	private static PingDbHelper mInstance = null;

	static PingDbHelper getHelper(Context context) {
		if (mInstance == null) {
			mInstance = new PingDbHelper(context.getApplicationContext());
		}
		return mInstance;
	}

	private PingDbHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	public void onCreate(SQLiteDatabase db) {
		db.execSQL(SQL_PING_RESULT_CREATE_ENTRIES);
		db.execSQL(SQL_SERVER_CREATE_ENTRIES);
	}

	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL(SQL_DELETE_PING_RESULT_ENTRIES);
		db.execSQL(SQL_DELETE_SERVER_ENTRIES);
		onCreate(db);
	}

	public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		onUpgrade(db, oldVersion, newVersion);
	}

	long savePingResults(List<Server> servers) {
		SQLiteDatabase db = getWritableDatabase();

		long inserts = 0;
		for (Server server : servers) {
			ContentValues values = new ContentValues();
			values.put(PingResultColumns.COLUMN_NAME_RELATED_SERVER, server.id);
			values.put(PingResultColumns.COLUMN_NAME_RESULT, server.lastResult);
			values.put(PingResultColumns.COLUMN_NAME_DATE, new Date().getTime());

			inserts += db.insert(PingResultColumns.TABLE_NAME, null, values);
		}

		db.close();
		return inserts;
	}

	String getServerSelect() {
//		return String.format(Locale.US,
//				"select %s.%s, %s, %s from %s where %s=1 order by %s asc",
//				ServerColumns.TABLE_NAME,
//				ServerColumns._ID,
//				ServerColumns.COLUMN_NAME_SERVER,
//				ServerColumns.COLUMN_NAME_ACTIVE,
//				ServerColumns.TABLE_NAME,
//				ServerColumns.COLUMN_NAME_ACTIVE,
//				ServerColumns.COLUMN_NAME_SERVER);

		return String.format(Locale.US,
				"select s.%s, s.%s, s.%s, p.%s, p.%s from %s s left join %s p on s.%s=p.%s where s.%s=1 " +
						"group by s.%s order by p.%s desc, s.%s asc",
				ServerColumns._ID,
				ServerColumns.COLUMN_NAME_SERVER,
				ServerColumns.COLUMN_NAME_ACTIVE,
				PingResultColumns.COLUMN_NAME_RESULT,
				PingResultColumns.COLUMN_NAME_DATE,
				ServerColumns.TABLE_NAME,
				PingResultColumns.TABLE_NAME,
				ServerColumns._ID,
				PingResultColumns.COLUMN_NAME_RELATED_SERVER,
				ServerColumns.COLUMN_NAME_ACTIVE,
				ServerColumns._ID,
				PingResultColumns.COLUMN_NAME_DATE,
				ServerColumns.COLUMN_NAME_SERVER);
	}

	List<Server> getActiveServers() {
		SQLiteDatabase db = getReadableDatabase();

		String temp = getServerSelect();

		Cursor cursor = db.rawQuery(temp, null);

		List<Server> servers = new ArrayList<>();
		boolean valuesInCursor = cursor.moveToFirst();
		while (valuesInCursor) {
			Server server = new Server(
					cursor.getInt(cursor.getColumnIndexOrThrow(ServerColumns._ID)),
					cursor.getString(cursor.getColumnIndexOrThrow(ServerColumns.COLUMN_NAME_SERVER)),
//					cursor.getInt(cursor.getColumnIndexOrThrow(PingResultColumns.COLUMN_NAME_RESULT)),
					0,
					cursor.getInt(cursor.getColumnIndexOrThrow(ServerColumns.COLUMN_NAME_ACTIVE)) == 1
			);

			servers.add(server);

			valuesInCursor = cursor.moveToNext();
		}

		cursor.close();
		db.close();

		return servers;
	}

	String getPingSelect(int serverId) {
		String serverSelect;
		if (serverId > -1) {
			serverSelect = String.format(Locale.US, "where %s.%s=%d",
					ServerColumns.TABLE_NAME,
					ServerColumns._ID,
					serverId);
		} else {
			serverSelect = "";
		}
		return String.format(Locale.US,
				"select * from %s left join %s on %s.%s=%s.%s %s order by %s asc, %s desc",
				PingResultColumns.TABLE_NAME,
				ServerColumns.TABLE_NAME,
				PingResultColumns.TABLE_NAME,
				PingResultColumns.COLUMN_NAME_RELATED_SERVER,
				ServerColumns.TABLE_NAME,
				ServerColumns._ID,
				serverSelect,
				ServerColumns.COLUMN_NAME_SERVER,
				PingResultColumns.COLUMN_NAME_DATE);
	}

//	List<Ping> getPings(int serverId) {
//		String select = String.format(Locale.US,
//				"select * from %s left join %s on %s=%s where %s=%d order by %s, %s desc",
//				PingResultColumns.TABLE_NAME,
//				ServerColumns.TABLE_NAME,
//				PingResultColumns.COLUMN_NAME_RELATED_SERVER,
//				ServerColumns._ID,
//				ServerColumns._ID,
//				serverId,
//				ServerColumns.COLUMN_NAME_SERVER,
//				PingResultColumns.COLUMN_NAME_DATE);
//
//		SQLiteDatabase db = getReadableDatabase();
//
//		Cursor cursor = db.rawQuery(select, null);
//
//		List<Ping> pings = new ArrayList<>();
//		boolean valuesInCursor = cursor.moveToFirst();
//		while (valuesInCursor) {
//			Ping ping = new Ping(
//					cursor.getString(cursor.getColumnIndexOrThrow(PingResultColumns.COLUMN_NAME_DATE)),
//					cursor.getString(cursor.getColumnIndexOrThrow(ServerColumns.COLUMN_NAME_SERVER)),
//					cursor.getInt(cursor.getColumnIndexOrThrow(PingResultColumns.COLUMN_NAME_RESULT))
//			);
//
//			pings.add(ping);
//
//			valuesInCursor = cursor.moveToNext();
//		}
//		cursor.close();
//		db.close();
//
//		return pings;
//	}

	long saveServer(String server) {
		SQLiteDatabase db = getWritableDatabase();

		ContentValues values = new ContentValues();
		values.put(ServerColumns.COLUMN_NAME_ACTIVE, "1");

		// try to update first
		String where = String.format(Locale.US, "%s='%s'", ServerColumns.COLUMN_NAME_SERVER, server);
		long rows = db.update(ServerColumns.TABLE_NAME, values, where, null);

		if (rows == 0) {
			values.put(ServerColumns.COLUMN_NAME_SERVER, server);
			rows = db.insert(ServerColumns.TABLE_NAME, null, values);
		}

		db.close();
		return rows;
	}

	boolean deactivateServer(int id) {
		String select = String.format(Locale.US, "%s=%d", ServerColumns._ID, id);

		SQLiteDatabase db = getReadableDatabase();

		ContentValues values = new ContentValues();
		values.put(ServerColumns.COLUMN_NAME_ACTIVE, 0);

		return db.update(ServerColumns.TABLE_NAME, values, select, null) == 1;

	}

	static class PingResultColumns implements BaseColumns {
		static final String TABLE_NAME = "PingResultEntry";
		static final String COLUMN_NAME_RELATED_SERVER = "server_id";
		static final String COLUMN_NAME_RESULT = "result";
		static final String COLUMN_NAME_DATE = "date";

	}

	static class ServerColumns implements BaseColumns {
		static final String TABLE_NAME = "ServerEntry";
		static final String COLUMN_NAME_SERVER = "server";
		static final String COLUMN_NAME_ACTIVE = "active";

	}

}