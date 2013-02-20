package com.gingerhq.android;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.util.Log;

public 	class Fetch {

	private static final String TAG = Fetch.class.getSimpleName();

	private static final String ROOT = "https://gingerhq.com";
	private static final String API_ROOT = ROOT + "/api/v1";
	private static final String DISCS = API_ROOT + 
		"/discussion/?username=EMAIL&api_key=API_KEY&format=json&limit=10&offset=0&unread=1";
    private static final String TEAMS = API_ROOT + 
    	"/team/?username=EMAIL&api_key=API_KEY&limit=100&offset=0&format=json";
    
    //private static final String FETCH_ERROR = "FETCH_ERROR";

	Random random;
	String email;
	String apiKey;
	
	Fetch(String email,	String apiKey) {
		
		Log.d(TAG, "Fetch.<constructor>");
		
		this.email = email;
		this.apiKey = apiKey;
		
		this.random = new Random();
	}
	
	protected List<Unread> fetch() {
		Log.d(TAG, "Fetch.fetch");

		//publishProgress("Fetching unread messages...");
		String jsonMsgs = null;
		try {
			jsonMsgs = this.fetchURL(this.insertPrefs(DISCS));
		} catch (IOException exc) {
            Log.e(TAG, "IOException fetching discussions.", exc);
            return null;
		}
		
		//publishProgress("Parsing messages...");
		String jsonUnread = this.extractObjs(jsonMsgs);
		
		//publishProgress("Fetching teams...");
		String jsonTeams = null;
		try {
			jsonTeams = this.fetchURL(this.insertPrefs(TEAMS));
		} catch (IOException exc) {
			Log.e(TAG, "IOException fetching teams.", exc);
			return null;
		}
		
		//publishProgress("Parsing teams...");
		Map<String, String> teamNames = null;
		try {
			teamNames = this.extractTeamNames(jsonTeams);
		} catch (JSONException exc) {
			Log.e(TAG, "JSONException parsing team names: ", exc);
			return null;
		}
		
		//publishProgress("Tweaking teams...");
		try {
			jsonUnread = this.setTeamNames(jsonUnread, teamNames);
		} catch (JSONException exc) {
			Log.e(TAG, "JSONException setting team names: ", exc);
		}
		
		//publishProgress("Displaying...");
		return  Unread.fromJSON(jsonUnread);
	}

	/**
	 * Replace EMAIL and API_KEY in given string with preferences.
	 * @return The replaces string
	 */
	private String insertPrefs(String original) {
		
		return original
			.replaceFirst("EMAIL", this.email)
			.replaceFirst("API_KEY", this.apiKey);
	}
	
	private String setTeamNames(String jsonUnread, Map<String, String> teamNames) throws JSONException {
		
		JSONArray array = new JSONArray(jsonUnread);
		JSONArray output = new JSONArray();
		
		for (int i = 0; i < array.length(); i++) {
			JSONObject unreadJsonObj = (JSONObject) array.get(i);
			String teamURI = unreadJsonObj.getString("team");
			unreadJsonObj.put("team", teamNames.get(teamURI));
			output.put(unreadJsonObj);
		}
		
		return output.toString();
	}

	private Map<String, String> extractTeamNames(String jsonTeams) throws JSONException {

		Map<String, String> result = new HashMap<String, String>();
		
		JSONObject obj = (JSONObject) new JSONTokener(jsonTeams).nextValue();
		JSONArray array = obj.getJSONArray("objects");
		for (int i = 0; i < array.length(); i++) {
			JSONObject teamJsonObj = (JSONObject) array.get(i);
			String resource_uri = teamJsonObj.getString("resource_uri");
			String name = teamJsonObj.getString("name");
			result.put(resource_uri, name);
		}
		
		return result;
	}

	/**
	 * Fetch contents of a URL.
	 * @param urlStr A URL to fetch.
	 */
	private String fetchURL(String urlStr) throws IOException {

		URL url = null;
		String cacheBuster = "&rnd=" + Math.abs(random.nextInt());
        try {
            url = new URL(urlStr + cacheBuster);
        } catch (MalformedURLException exc) {
            Log.e(TAG, "MalformedURLException on: "+ urlStr, exc);
            return "";
        }
        
        StringBuffer dataRead = new StringBuffer();
        byte[] buf = new byte[2048];
        int num_read = 0;

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.connect();
                    
        InputStream stream = conn.getInputStream();
        
        num_read = stream.read(buf);
        while (num_read != -1) {
        	//Log.d(TAG, "fetchURL: Read: "+ num_read);
        	dataRead.append(new String(buf, 0, num_read, "UTF-8"));
        	num_read = stream.read(buf);
        }
        
        stream.close();
        conn.disconnect();
        
        return dataRead.toString();
	}
	
	/**
	 * Extract unread messages JSON from incoming JSON.
	 */
	private String extractObjs(String jsonMsgs) {
		if (jsonMsgs == null || jsonMsgs.length() == 0) {
			return null;
		}
		
		try {
			JSONObject obj = (JSONObject) new JSONTokener(jsonMsgs).nextValue();
			JSONArray array = obj.getJSONArray("objects");
			return array.toString();
			
		} catch (JSONException exc) {
			Log.e(TAG, "JSONException parsing unread msgs from Ginger.", exc);
		}
		
		return "";
	}
}
