package com.gingerhq.android;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.provider.Browser;
import android.util.Log;
import android.widget.RemoteViews;

public class Widget extends AppWidgetProvider {

	private static final String TAG = Widget.class.getSimpleName();
	
	private static final String ROOT = "https://gingerhq.com";
	private static final String API_ROOT = ROOT + "/api/v1";
	private static final String DISCS = API_ROOT + 
		"/discussion/?username=EMAIL&api_key=API_KEY&format=json&limit=10&offset=0&unread=1";
    private static final String TEAMS = API_ROOT + 
    	"/team/?username=EMAIL&api_key=API_KEY&limit=100&offset=0&format=json";
    
    private static final String FETCH_ERROR = "FETCH_ERROR";
    
    Context context;
    AppWidgetManager appWidgetManager;
    int[] appWidgetIds;
    
	@Override
	public void onReceive(Context context, Intent intent) {
		super.onReceive(context, intent);
		Log.d(TAG, "Widget.onReceive");
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		super.onUpdate(context, appWidgetManager, appWidgetIds);
		Log.d(TAG, "Widget.onUpdate");
		
		this.context = context;
		this.appWidgetManager = appWidgetManager;
		this.appWidgetIds = appWidgetIds;
		
		if (!this.isOnline()) {
			Log.d(TAG, "No network, skipping");
			this.updateTitle(R.string.offline);
			return;
		}
		
		String email = this.getEmail();
		String apiKey = this.getAPIKey();
		if (email == null || apiKey == null) {
			Log.d(TAG, "No preferences yet");
			this.updateTitle(R.string.waitingPrefs);
			return;
		}
		
		this.attachRefresh();
		
		// Without this, the second HTTPS connection will hang until timeout. 
		// Not sure why, something about Android's connection pooling.
		// Yes, even on > FROYO, On JELLY_BEAN in fact.
		System.setProperty("http.keepAlive", "false");
		
		new Fetch(context, appWidgetManager, appWidgetIds, email, apiKey)
			.execute();
	}
	
	private String getEmail() {
		SharedPreferences prefs = 
				PreferenceManager.getDefaultSharedPreferences(context);
		return prefs.getString("email", null);
	}
	
	private String getAPIKey() {
		SharedPreferences prefs = 
				PreferenceManager.getDefaultSharedPreferences(context);
		return prefs.getString("api_key", null);
	}
	
	/**
	 * Is the device connected to the network?
	 */
	private boolean isOnline() {
		ConnectivityManager connMgr = (ConnectivityManager) this.context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
		return (networkInfo != null && networkInfo.isConnected());
	}
	
	/**
	 * Change the widget title to reflect current status
	 */
	private void updateTitle(int stringId) {
		RemoteViews rv = new RemoteViews(
				this.context.getPackageName(), 
				R.layout.widget);
		rv.setTextViewText(R.id.title, context.getString(stringId));
		this.appWidgetManager.updateAppWidget(this.appWidgetIds[0], rv);
	}
	
	/**
	 * Clicking the refresh icon updates the widget
	 */
	private void attachRefresh() {
		
		RemoteViews rv = new RemoteViews(
				this.context.getPackageName(), 
				R.layout.widget);
		
		Intent updateIntent = new Intent(this.context, Widget.class);
		updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
		updateIntent.putExtra(
				AppWidgetManager.EXTRA_APPWIDGET_IDS, 
				this.appWidgetIds);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(
				context,
				0,
				updateIntent,
				0);
		rv.setOnClickPendingIntent(R.id.refresh, pendingIntent);
		
		this.appWidgetManager.updateAppWidget(this.appWidgetIds, rv);
	}
	
	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		super.onDeleted(context, appWidgetIds);
		Log.d(TAG, "Widget.onDeleted");
	}

	class Fetch extends AsyncTask<String, String, String> {

		Context context;
		AppWidgetManager appWidgetManager;
		int[] appWidgetIds;
		Random random;
		String email;
		String apiKey;
		
		Fetch(
				Context context, 
				AppWidgetManager appWidgetManager, 
				int[] appWidgetIds,
				String email,
				String apiKey) {
			
			Log.d(TAG, "Fetch.<constructor>");
			this.context = context;
			this.appWidgetManager = appWidgetManager;
			this.appWidgetIds = appWidgetIds;
			this.email = email;
			this.apiKey = apiKey;
			
			this.random = new Random();
		}
		
		@Override
		protected String doInBackground(String... params) {
			Log.d(TAG, "Fetch.doInBackground");

			publishProgress("Fetching unread messages...");
			String jsonMsgs = null;
			try {
				jsonMsgs = this.fetchURL(this.insertPrefs(DISCS));
			} catch (IOException exc) {
	            Log.e(TAG, "IOException fetching discussions.", exc);
	            return FETCH_ERROR;
			}
			
			publishProgress("Parsing messages...");
			String jsonUnread = this.extractObjs(jsonMsgs);
			
			publishProgress("Fetching teams...");
			String jsonTeams = null;
			try {
				jsonTeams = this.fetchURL(this.insertPrefs(TEAMS));
			} catch (IOException exc) {
				Log.e(TAG, "IOException fetching teams.", exc);
				return FETCH_ERROR;
			}
			
			publishProgress("Parsing teams...");
			Map<String, String> teamNames = null;
			try {
				teamNames = this.extractTeamNames(jsonTeams);
			} catch (JSONException exc) {
				Log.e(TAG, "JSONException parsing team names: ", exc);
				return null;
			}
			
			publishProgress("Tweaking teams...");
			try {
				jsonUnread = this.setTeamNames(jsonUnread, teamNames);
			} catch (JSONException exc) {
				Log.e(TAG, "JSONException setting team names: ", exc);
			}
			
			publishProgress("Displaying...");
			return jsonUnread;
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
		
		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);

			RemoteViews rv = new RemoteViews(
					this.context.getPackageName(), 
					R.layout.widget);
			
			if (result == FETCH_ERROR) {
				rv.setTextViewText(
						R.id.title, 
						context.getString(R.string.fetch_error));
				this.appWidgetManager.updateAppWidget(this.appWidgetIds[0], rv);
				return;
			}
			
			// The URL (second parameter) gets filled in in WidgetRemoteFactory
			Intent browserIntent = new Intent(Intent.ACTION_VIEW, null);						
			browserIntent.putExtra(
					Browser.EXTRA_APPLICATION_ID, 
					this.context.getPackageName());	// Re-use tab
			
			// The Intent is filled in by WidgetRemoteFactory.getViewAt
			PendingIntent pendingIntent = PendingIntent.getActivity(
					this.context, 
					0, 
					browserIntent, 
					PendingIntent.FLAG_UPDATE_CURRENT);
			rv.setPendingIntentTemplate(R.id.widgetList, pendingIntent);
			
			// In case we have multiple widgets
			for (int i = 0; i < this.appWidgetIds.length; ++i) {	
				
				Intent intent = new Intent(context, WidgeRemoteService.class);
				intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, this.appWidgetIds[i]);
				intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
				
				intent.putExtra("com.gingerhq.android.Unread", result);
	
				rv.setRemoteAdapter(R.id.widgetList, intent);
				rv.setEmptyView(R.id.widgetList, R.id.widgetEmpty);
				rv.setTextViewText(R.id.title, this.context.getString(R.string.unread));
				
				this.appWidgetManager.updateAppWidget(this.appWidgetIds[i], rv);
			}
			
			//this.appWidgetManager.notifyAppWidgetViewDataChanged(this.appWidgetIds, R.id.widgetLayout);
			
			Log.d(TAG, "Finished onPostExecute");
		}

		@Override
		protected void onProgressUpdate(String... values) {
			super.onProgressUpdate(values);
			
			RemoteViews rv = new RemoteViews(this.context.getPackageName(), R.layout.widget);
			rv.setTextViewText(R.id.title, values[0]);
			this.appWidgetManager.updateAppWidget(this.appWidgetIds[0], rv);
			
			Log.d(TAG, "Fetch.onProgressUpdate: " + Arrays.toString(values));
		}
		
	}
	
}