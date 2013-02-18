package com.gingerhq.android;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

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

		this.id = jsonObj.getInt("id");
		
		this.user = jsonObj.getString("user");
		
		this.permalink = jsonObj.getString("permalink");
		
		String latest_dt = jsonObj.getString("date_latest_activity");
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.US);
		df.setTimeZone(TimeZone.getTimeZone("UTC"));
		try {
			this.date_latest_activity = df.parse(latest_dt);
		} catch (ParseException exc) {
			Log.e(TAG, "ParseException: " + exc);
		}

		/*
		this.uuid = jsonObj.getString("uuid");
		this.root = jsonObj.getString("root");
		this.discussion = jsonObj.getString("discussion");
		this.is_read = jsonObj.getBoolean("read");
		*/
	}
}
