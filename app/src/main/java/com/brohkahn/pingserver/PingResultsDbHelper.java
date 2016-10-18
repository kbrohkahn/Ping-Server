package com.brohkahn.pingserver;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import java.util.Date;

public class PingResultsDbHelper extends SQLiteOpenHelper {
    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "FeedReader.db";

    public PingResultsDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(PingResultsTable.SQL_CREATE_ENTRIES);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(PingResultsTable.SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public long savePing(String server, int result) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(PingResult.COLUMN_NAME_SERVER, server);
        values.put(PingResult.COLUMN_NAME_RESULT, result);
        values.put(PingResult.COLUMN_NAME_DATE, new Date().getTime());

        long rows = db.insert(PingResult.TABLE_NAME, null, values);
        db.close();
        return rows;
    }

    public Ping getLastPing() {
        SQLiteDatabase db = getReadableDatabase();

        String[] projection = {
                PingResult._ID,
                PingResult.COLUMN_NAME_DATE,
                PingResult.COLUMN_NAME_SERVER,
                PingResult.COLUMN_NAME_RESULT
        };

        String sortOrder = PingResult.COLUMN_NAME_DATE + " DESC";

        Cursor c = db.query(PingResult.TABLE_NAME, projection, null, null, null, null, sortOrder, "1");

        Ping lastPing = null;
        if (c.moveToFirst()) {
            lastPing = new Ping();
            lastPing.date = c.getString(c.getColumnIndexOrThrow(PingResult.COLUMN_NAME_DATE));
            lastPing.server = c.getString(c.getColumnIndexOrThrow(PingResult.COLUMN_NAME_SERVER));
            lastPing.result = c.getInt(c.getColumnIndexOrThrow(PingResult.COLUMN_NAME_RESULT));
        }
        c.close();
        db.close();

        return lastPing;
    }

    public static final class PingResultsTable {
        private static final String SQL_CREATE_ENTRIES =
                "CREATE TABLE " + PingResult.TABLE_NAME + " (" +
                        PingResult._ID + " INTEGER PRIMARY KEY," +
                        PingResult.COLUMN_NAME_DATE + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                        PingResult.COLUMN_NAME_SERVER + " TEXT, " +
                        PingResult.COLUMN_NAME_RESULT + " INTEGER )";

        private static final String SQL_DELETE_ENTRIES =
                "DROP TABLE IF EXISTS " + PingResult.TABLE_NAME;

        private PingResultsTable() {
        }
    }

    public static class PingResult implements BaseColumns {
        public static final String TABLE_NAME = "PingEntries";
        public static final String COLUMN_NAME_SERVER = "server";
        public static final String COLUMN_NAME_RESULT = "result";
        public static final String COLUMN_NAME_DATE = "date";
    }

}