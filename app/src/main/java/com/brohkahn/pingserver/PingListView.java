package com.brohkahn.pingserver;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PingListView extends AppCompatActivity {
    private Cursor pingCursor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.ping_list_view);

        PingResultsDbHelper helper = new PingResultsDbHelper(this);

        SQLiteDatabase db = helper.getReadableDatabase();

        String query = String.format(Locale.US, "SELECT * FROM %s ORDER BY %s DESC",
                PingResultsDbHelper.PingResult.TABLE_NAME,
                PingResultsDbHelper.PingResult.COLUMN_NAME_DATE);
        pingCursor = db.rawQuery(query, null);

        PingListAdaper adapter = new PingListAdaper(this, pingCursor, 0);

        ListView listView = (ListView) findViewById(R.id.ping_list_view);
        listView.setAdapter(adapter);

        helper.close();
    }

    @Override
    protected void onDestroy() {
        pingCursor.close();
        super.onDestroy();
    }

    public class PingListAdaper extends CursorAdapter {
        private SimpleDateFormat dateFormat;

        public PingListAdaper(Context context, Cursor cursor, int flags) {
            super(context, cursor, flags);

            dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US);
        }

        public void bindView(View view, Context context, Cursor cursor) {
            final int result = cursor.getInt(cursor.getColumnIndexOrThrow(PingResultsDbHelper.PingResult.COLUMN_NAME_RESULT));
            int colorResID;
            if (result == Ping.PING_SUCCESS) {
                colorResID = R.color.success;
            } else {
                colorResID = R.color.error;
            }
            view.setBackgroundColor(getResources().getColor(colorResID));

            TextView dateTextView = (TextView) view.findViewById(R.id.ping_date);
            Date date = new Date();
            date.setTime(cursor.getLong(cursor.getColumnIndexOrThrow(PingResultsDbHelper.PingResult.COLUMN_NAME_DATE)));
            dateTextView.setText(dateFormat.format(date));

            TextView serverTextView = (TextView) view.findViewById(R.id.ping_server);
            serverTextView.setText(cursor.getString(cursor.getColumnIndexOrThrow(PingResultsDbHelper.PingResult.COLUMN_NAME_SERVER)));

        }

        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return LayoutInflater.from(context).inflate(R.layout.ping_list_item, parent, false);
        }
    }
}
