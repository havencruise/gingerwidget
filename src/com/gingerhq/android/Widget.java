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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.Browser;
import android.util.Log;
import android.widget.RemoteViews;

public class Widget extends AppWidgetProvider {

	private static final String TAG = Widget.class.getSimpleName();
	
	private static final String DISCS = "https://gingerhq.com/api/v1/discussion/?username=graham%40gkgk.org&api_key=ae9d3a85527b2772e57734072f91e83e0b25370e&format=json&limit=10&offset=0&unread=1";
    private static final String TEAMS = "https://gingerhq.com/api/v1/team/?username=graham%40gkgk.org&api_key=ae9d3a85527b2772e57734072f91e83e0b25370e&limit=100&offset=0&format=json";
	    
    private static final String NO_NETWORK = "NO_NETWORK";
    private static final String FETCH_ERROR = "FETCH_ERROR";
    
	@Override
	public void onReceive(Context context, Intent intent) {
		super.onReceive(context, intent);
		Log.d(TAG, "Widget.onReceive");
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		super.onUpdate(context, appWidgetManager, appWidgetIds);
		Log.d(TAG, "Widget.onUpdate:" + appWidgetIds.length);
				
		// Without this, the second HTTPS connection will hang until timeout. Not sure why, something about Android's connection pooling.
		// Yes, even on > FROYO, On JELLY_BEAN in fact.
		System.setProperty("http.keepAlive", "false");
		
		new Fetch(context, appWidgetManager, appWidgetIds).execute();
		
		//context.startService(new Intent(context, Updater.class));
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
		
		Fetch(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
			Log.d(TAG, "Fetch.<constructor>");
			this.context = context;
			this.appWidgetManager = appWidgetManager;
			this.appWidgetIds = appWidgetIds;
			this.random = new Random();
		}
		
		@Override
		protected String doInBackground(String... params) {
			Log.d(TAG, "Fetch.doInBackground");
			
			if (!this.isOnline()) {
				Log.d(TAG, "No network, skipping");
				return NO_NETWORK;
			}

			publishProgress("Fetching unread messages...");
			String jsonMsgs = null;
			try {
				jsonMsgs = this.fetchURL(DISCS);
			} catch (IOException exc) {
	            Log.e(TAG, "IOException fetching discussions.", exc);
	            return FETCH_ERROR;
			}
			
			publishProgress("Parsing messages...");
			String jsonUnread = this.extractObjs(jsonMsgs);
			
			publishProgress("Fetching teams...");
			String jsonTeams = null;
			try {
				jsonTeams = this.fetchURL(TEAMS);
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
		 * Is the device connected to the network?
		 */
		private boolean isOnline() {
			ConnectivityManager connMgr = (ConnectivityManager) this.context.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
			return (networkInfo != null && networkInfo.isConnected());
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
					context.getPackageName(), 
					R.layout.widget);
			
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
			
			if (result == NO_NETWORK) {
				rv.setTextViewText(R.id.title, context.getString(R.string.offline));
				appWidgetManager.updateAppWidget(appWidgetIds[0], rv);
				return;
			} else if (result == FETCH_ERROR) {
				rv.setTextViewText(
						R.id.title, 
						context.getString(R.string.fetch_error));
				appWidgetManager.updateAppWidget(appWidgetIds[0], rv);
				return;
			}
			
			// In case we have multiple widgets
			for (int i = 0; i < appWidgetIds.length; ++i) {	
				
				Intent intent = new Intent(context, WidgeRemoteService.class);
				intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetIds[i]);
				intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
				
				intent.putExtra("com.gingerhq.android.Unread", result);
	
				rv.setRemoteAdapter(R.id.widgetList, intent);
				rv.setEmptyView(R.id.widgetList, R.id.widgetEmpty);
				rv.setTextViewText(R.id.title, context.getString(R.string.unread));
				
				appWidgetManager.updateAppWidget(appWidgetIds[i], rv);
			}
		}

		@Override
		protected void onProgressUpdate(String... values) {
			super.onProgressUpdate(values);
			
			RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget);
			rv.setTextViewText(R.id.title, values[0]);
			appWidgetManager.updateAppWidget(appWidgetIds[0], rv);
			
			Log.d(TAG, "Fetch.onProgressUpdate: " + Arrays.toString(values));
		}
		
	}
	
}