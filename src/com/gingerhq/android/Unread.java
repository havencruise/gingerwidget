package com.gingerhq.android;

import org.json.JSONException;
import org.json.JSONObject;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class Unread implements Parcelable {

	private static final String TAG = Unread.class.getSimpleName();
	
	static final Parcelable.Creator<Unread> CREATOR = new UnreadCreator();
	
	public int id;
	public String title;
	public int reply_count;
	public String resource_uri;
	public String slug;
	public String team;
	public int unread_count; 
	public String intro;
	
	public Message message;
	public Message latest_message;
	
	public Unread() {
	}
	
	private Unread(Parcel source) {
		Log.d(TAG, "Unread<constructor> with Parcel");
		this.id = source.readInt();
		/*
		this.title = source.readString();
		this.reply_count = source.readInt();
		this.resource_uri = source.readString();
		this.slug = source.readString();
		this.team = source.readString();
		this.unread_count = source.readInt();
		this.intro = source.readString();
		*/
	}
	
	public Unread(JSONObject obj) throws JSONException {
		
		this.id = obj.getInt("id");
		this.title = obj.getString("title");
		this.resource_uri = obj.getString("resource_uri");
		this.slug = obj.getString("slug");
		this.team = obj.getString("team");
		this.intro = obj.getString("intro");
		
		String reply_count_str = obj.getString("reply_count");
		if (reply_count_str != "null") {
			this.reply_count = Integer.parseInt(reply_count_str);
		}
		
		String unread_count_str = obj.getString("unread_count");
		if (unread_count_str != "null") {
			this.unread_count = Integer.parseInt(unread_count_str);
		}
		
		//this.message = new Message(obj.getJSONObject("message"));
		//this.latest_message = new Message(obj.getJSONObject("latest_message"));
	}
	
	public Unread(Cursor cursor) {

		this.id = cursor.getInt(cursor.getColumnIndex("id"));
		this.title = cursor.getString(cursor.getColumnIndex("title"));
		this.resource_uri = cursor.getString(cursor.getColumnIndex("resource_uri"));
		this.slug = cursor.getString(cursor.getColumnIndex("slug"));
		this.team = cursor.getString(cursor.getColumnIndex("team"));
		this.intro = cursor.getString(cursor.getColumnIndex("intro"));
		this.unread_count = cursor.getInt(cursor.getColumnIndex("unread_count"));
		this.reply_count = cursor.getInt(cursor.getColumnIndex("reply_count"));
		
		int latest_id = cursor.getInt(cursor.getColumnIndex("latest_message"));
		this.latest_message = DBManager.getMessage(latest_id);
	}
	
	public String toString() {
		return this.title;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		Log.d(TAG, "Unread.writeToParcel: "+ this.id);
		dest.writeInt(this.id);
		/*
		dest.writeString(this.title);
		dest.writeInt(this.reply_count);
		dest.writeString(this.resource_uri);
		dest.writeString(this.slug);
		dest.writeString(this.team);
		dest.writeInt(this.unread_count);
		dest.writeString(this.intro);
		*/
	}
	
	/* Inner classes */
	
	static class UnreadCreator implements Parcelable.Creator<Unread> {

		@Override
		public Unread createFromParcel(Parcel source) {
			Log.d(TAG, "UnreadCreator.createFromParcel");
			return new Unread(source);
		}

		@Override
		public Unread[] newArray(int size) {
			Log.d(TAG, "UnreadCreator.newArray");
			return new Unread[size];
		}
		
	}
}
