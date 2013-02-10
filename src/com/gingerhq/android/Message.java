package com.gingerhq.android;

import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

public class Message {

	private static final String TAG = Message.class.getSimpleName();
	
	int id;
	String uuid;
	
	String body;
	String raw_body;
	
	String root;
	String discussion;
	String votes;
	
	String date_created;
	String date_edited;
	Date date_latest_activity;
	
	String collapsed;
	String permalink;
	String resource_uri;
	
	boolean is_read;
	String team;
	String user;
	
	String attachments;
	
	public Message(JSONObject jsonObj) throws JSONException {
		/*
		Log.d(TAG, "----");
		Iterator<String> iter = (Iterator<String>) jsonObj.keys();
		while (iter.hasNext()) {
			String key = iter.next();
			Log.d(TAG, key + " = " + jsonObj.get(key));
		}
		*/
		this.id = jsonObj.getInt("id");
		
		this.user = jsonObj.getString("user");
		
		this.permalink = jsonObj.getString("permalink");
		this.date_latest_activity = new Date( jsonObj.getLong("date_latest_activity") );

		/*
		this.uuid = jsonObj.getString("uuid");
		this.root = jsonObj.getString("root");
		this.discussion = jsonObj.getString("discussion");
		this.is_read = jsonObj.getBoolean("read");
		*/
	}
}
