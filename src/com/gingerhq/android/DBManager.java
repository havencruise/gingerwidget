package com.gingerhq.android;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBManager {

	private static final String TAG = DBManager.class.getSimpleName();
	
	private static Map<Integer, Message> messages = new HashMap<Integer, Message>(); 
	
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
	void save(List<Unread> unreadList) {

		SQLiteDatabase db = this.dbHelper.getWritableDatabase();
		ContentValues values = new ContentValues();
		
		for (Unread unread : unreadList) {
			values.clear();
			
			values.put("id", unread.id);
			values.put("title", unread.title);
			values.put("resource_uri", unread.resource_uri);
			values.put("slug", unread.slug);
			values.put("team", unread.team);
			values.put("intro", unread.intro);
			values.put("reply_count", unread.reply_count);
			values.put("unread_count", unread.unread_count);
			values.put("latest_message", unread.latest_message.id);		// FK to message
			
			try {
				db.insertOrThrow("unread", null, values);
			} catch (SQLException e) {
				// Maybe row already exists
			}
			
			this.saveMessage(unread.latest_message, db);
		}
		
		db.close();		
	}
	
	private void saveMessage(Message msg, SQLiteDatabase db) {
		
		ContentValues values = new ContentValues();
		values.put("id", msg.id);
		values.put("permalink", msg.permalink);
		values.put("date_latest_activity", msg.date_latest_activity.getTime());
		values.put("user", msg.user);
		
		try {
			db.insertOrThrow("message", null, values);
		} catch (SQLException e) {
			// Maybe message already exists
		}
	}


	public static Message getMessage(int msg_id) {
		
		return DBManager.messages.get(Integer.valueOf(msg_id));
	}
	
	/* Inner classes */
	
	static final class DBHelper extends SQLiteOpenHelper {

		private static final String DB_NAME = "ginger.db";
		private static final int DB_VERSION = 2;
		Context context;
		
		DBHelper(Context context) {
			super(context, DB_NAME, null, DB_VERSION);
			this.context = context;
		}
		
		@Override
		public void onCreate(SQLiteDatabase db) {
			this.createTableUnread(db);
			this.createTableMessage(db);
		}
		
		private void createTableUnread(SQLiteDatabase db) {
			Log.d(TAG, "creating table 'unread'");
			String sql = "CREATE TABLE unread (" +
					"id int primary key, "+
					"title text, " +
					"reply_count int, " +
					"resource_uri text, " +
					"slug text, " +
					"team text, " +
					"unread_count int, " +
					"intro text, " +
					"latest_message int " +
					");";
					
			db.execSQL(sql);
		}
		
		private void createTableMessage(SQLiteDatabase db) {
			Log.d(TAG, "creating table 'message'");
			String sql = "CREATE TABLE message (" +
					"id int primary key, " +
					"permalink text, " +
					"user text, " +
					"date_latest_activity text " +
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
