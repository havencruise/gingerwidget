package com.gingerhq.android;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBManager {

	private static final String TAG = DBManager.class.getSimpleName();
	
	private DBHelper dbHelper;
	
	public DBManager(Context context) {
		this.dbHelper = new DBHelper(context);
	}
	
	/**
	 * Load unread messages.
	 */
	List<Unread> load() {
		SQLiteDatabase db = this.dbHelper.getReadableDatabase();
		Cursor cursor = db.query("unread", null, null, null, null, null, null); //"created_dt desc");
		
		List<Unread> result = new ArrayList<Unread>();
		
		while (cursor.moveToNext()) {			
			result.add(new Unread(cursor));
		}
		
		cursor.close();
		db.close();
		
		return result;
	}
	
	/**
	 * Write the new messages to the database.
	 */
	void save(List<Unread> msgs) {

		SQLiteDatabase db = this.dbHelper.getWritableDatabase();
		ContentValues values = new ContentValues();
		
		for (Unread unread : msgs) {
			values.clear();
			
			values.put("id", unread.id);
			values.put("title", unread.title);
			values.put("resource_uri", unread.resource_uri);
			values.put("slug", unread.slug);
			values.put("team", unread.team);
			values.put("intro", unread.intro);
			values.put("reply_count", unread.reply_count);
			values.put("unread_count", unread.unread_count);
			
			try {
				db.insertOrThrow("unread", null, values);
			} catch (SQLException e) {
				// Maybe row already exists
			}
		}
		
		db.close();		
	}
	
	/* Inner classes */
	
	static final class DBHelper extends SQLiteOpenHelper {

		private static final String DB_NAME = "ginger.db";
		private static final int DB_VERSION = 1;
		Context context;
		
		DBHelper(Context context) {
			super(context, DB_NAME, null, DB_VERSION);
			this.context = context;
		}
		
		@Override
		public void onCreate(SQLiteDatabase db) {
			Log.d(TAG, "onCreate - creating 'unread' table");
			String sql = "CREATE TABLE unread (" +
					"id int primary key, "+
					"title text, " +
					"reply_count int, " +
					"resource_uri text, " +
					"slug text, " +
					"team text, " +
					"unread_count int, " +
					"intro text" +
					");";
					
			db.execSQL(sql);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.d(TAG, "onUpgrade");
			
			db.execSQL("DROP TABLE IF EXISTS "+ DB_NAME);
			this.onCreate(db);
		}

	}	
}
