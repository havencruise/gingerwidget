package com.gingerhq.android;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.RemoteViews;

public class Widget extends AppWidgetProvider {

	private static final String TAG = Widget.class.getSimpleName();
	
	private static final String DISCS = "https://gingerhq.com/api/v1/discussion/?username=graham%40gkgk.org&api_key=ae9d3a85527b2772e57734072f91e83e0b25370e&format=json&limit=10&offset=0&unread=1";
    private static final String TEAMS = "https://gingerhq.com/api/v1/team/?username=graham%40gkgk.org&api_key=ae9d3a85527b2772e57734072f91e83e0b25370e&limit=100&offset=0&format=json";
	    
	@Override
	public void onReceive(Context context, Intent intent) {
		super.onReceive(context, intent);
		Log.d(TAG, "Widget.onReceive");
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		super.onUpdate(context, appWidgetManager, appWidgetIds);
		Log.d(TAG, "Widget.onUpdate:" + appWidgetIds.length);
		
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
		
		Fetch(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
			Log.d(TAG, "Fetch.<constructor>");
			this.context = context;
			this.appWidgetManager = appWidgetManager;
			this.appWidgetIds = appWidgetIds;
		}
		
		@Override
		protected String doInBackground(String... params) {
			Log.d(TAG, "Fetch.doInBackground");
			
			publishProgress("Fetching unread messages...");
			String jsonMsgs = this.fetchURL(DISCS);
			
			publishProgress("Parsing messages...");
			String jsonUnread = this.extractObjs(jsonMsgs);
			
			publishProgress("Fetching teams...");
			String jsonTeams = this.fetchURL(TEAMS);
			
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
		 * Fetch contents of a url
		 * @param urlStr Like, a URL, duh.
		 */
		private String fetchURL(String urlStr) {
			
			URL url = null;
	        try {
	            url = new URL(urlStr);
	        } catch (MalformedURLException exc) {
	            Log.e(TAG, "MalformedURLException on: "+ urlStr, exc);
	            return "";
	        }

	        StringBuffer dataRead = new StringBuffer();
	        byte[] buf = new byte[2048];
	        int num_read = 0;
	        
	        try {
	            URLConnection conn = url.openConnection();
	            InputStream stream = conn.getInputStream();

	            num_read = stream.read(buf);
	            while (num_read != -1) {	            	
	            	Log.d(TAG, "Read: "+ num_read);
	            	dataRead.append(new String(buf, 0, num_read, "UTF-8"));
	            	num_read = stream.read(buf);
	            }
	            
	            stream.close();
	        } catch (IOException exc) {
	            Log.e(TAG, "IOException fetching remote data. ", exc);
	        }
	        
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
			
			for (int i = 0; i < appWidgetIds.length; ++i) {		// In case we have multiple widgets
				
				Intent intent = new Intent(context, WidgeRemoteService.class);
				intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetIds[i]);
				intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
				
				intent.putExtra("com.gingerhq.android.Unread", result);

				RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget);
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