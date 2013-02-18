package com.gingerhq.android;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.util.Log;

public class Unread {

	private static final String TAG = Unread.class.getSimpleName();
		
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
		
	public static List<Unread> fromJSON(String dataS) {

		List<Unread> result = new ArrayList<Unread>();
		
		try {
			JSONTokener tok = new JSONTokener(dataS);
			JSONArray arr = (JSONArray) tok.nextValue();
			
			for (int i = 0; i < arr.length(); i++) {
			
				JSONObject jobj = arr.getJSONObject(i);
				result.add(new Unread(jobj));
			}
			
		} catch (JSONException exc) {
			Log.e(TAG, "JSONException:", exc);
		}
		
		return result;
	}
	
	public Unread(JSONObject obj) throws JSONException {
		
		this.id = obj.getInt("id");
		this.title = obj.getString("title");

		this.resource_uri = obj.getString("resource_uri");
		this.slug = obj.getString("slug");
		this.team = obj.getString("team");
		this.intro = obj.getString("intro");
		
		try {
			this.reply_count = obj.getInt("reply_count");
		} catch (JSONException exc) {
			this.reply_count = -1;
		}
		
		try {
			this.unread_count = obj.getInt("unread_count");
		} catch (JSONException exc) {
			this.unread_count = -1;
		}
		
		this.message = new Message(obj.getJSONObject("message"));
		this.latest_message = new Message(obj.getJSONObject("latest_message"));
		
	}
	
	public String toString() {
		return this.title;
	}
	
	/*
	public Map<String, Object> asMap() {
		
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("id", Integer.valueOf(this.id));
		result.put("title", this.title);
		result.put("reply_count", Integer.valueOf(this.reply_count));
		result.put("resource_uri", this.resource_uri);
		result.put("slug", this.slug);
		result.put("team", this.team);
		result.put("unread_count", Integer.valueOf(this.unread_count));
		result.put("intro", this.intro);

		return result;
	}
	*/
}