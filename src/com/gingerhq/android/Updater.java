package com.gingerhq.android;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

public class Updater extends IntentService {

	private static final String TAG = Updater.class.getSimpleName();
    private static final String DISCS = "https://gingerhq.com/api/v1/discussion/?username=graham%40gkgk.org&api_key=ae9d3a85527b2772e57734072f91e83e0b25370e&format=json&limit=10&offset=0&unread=1";
    private static final String TEAMS = "https://gingerhq.com/api/v1/team/?username=%@&api_key=%@&limit=1000&offset=0&format=json";
    
    private DBManager dbManager;
    
    public Updater() {
    	super(TAG);
    	Log.d(TAG, "Updater.<constructor>");
    	
    	this.dbManager = new DBManager(this);
    }
    
	@Override
	protected void onHandleIntent(Intent intent) {
		Log.d(TAG, "UpdaterThread onHandleIntent start");
			
		String jsonMsgs = this.fetch();
		List<Unread> newUnread = this.parse(jsonMsgs);
		dbManager.save(newUnread);
		
		Log.d(TAG, "UpdaterThread onHandleIntent stop");
	}

	/**
	 * Fetch unread messages as JSON.
	 * @return JSON string
	 */
	private String fetch() {
		
		URL url = null;
        try {
            url = new URL(DISCS);
        } catch (MalformedURLException exc) {
            Log.e(TAG, "MalformedURLException on: "+ DISCS, exc);
            return "";
        }

        String line = "";
        try {
            URLConnection conn = url.openConnection();
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            line = reader.readLine();
            reader.close();
        } catch (IOException exc) {
            Log.e(TAG, "IOException fetch unread discussions. ", exc);
        }
        
        return line;
	}
	
	/**
	 * Parse JSON of incoming messages.
	 */
	private List<Unread> parse(String jsonMsgs) {
		if (jsonMsgs == null || jsonMsgs.length() == 0) {
			return null;
		}
		
		List<Unread> result = new ArrayList<Unread>();
		
		try {
			JSONObject obj = (JSONObject) new JSONTokener(jsonMsgs).nextValue();
			
			JSONObject meta = obj.getJSONObject("meta");
			Log.d(TAG, "New messages: " + meta.get("total_count"));
			
			JSONArray array = obj.getJSONArray("objects");
			
			for (int i = 0; i < array.length(); i++) {
				JSONObject unreadJsonObj = (JSONObject) array.get(i);
			
				Log.d(TAG, "----");
				Iterator<String> iter = (Iterator<String>) unreadJsonObj.keys();
				while (iter.hasNext()) {
					String key = iter.next();
					Log.d(TAG, key + " = " + unreadJsonObj.get(key));
				}
				
				Unread unread = new Unread(unreadJsonObj);			
				result.add(unread);
			}
			
		} catch (JSONException exc) {
			Log.e(TAG, "JSONException parsing unread msgs from Ginger.", exc);
		}
		
		Log.d(TAG, "result: " + result);
		return result;
	}
	
}
