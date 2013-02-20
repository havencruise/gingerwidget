package com.gingerhq.android;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.Browser;
import android.util.Log;
import android.widget.RemoteViews;

public class Widget extends AppWidgetProvider {

	private static final String TAG = Widget.class.getSimpleName();
    
    Context context;
    AppWidgetManager appWidgetManager;
    int[] appWidgetIds;
    RemoteViews remoteViews;
    
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
		
		this.remoteViews = this.createRemoteViews();
		
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
		
		this.appWidgetManager.notifyAppWidgetViewDataChanged(this.appWidgetIds, R.id.widgetList);
	}
	
	private RemoteViews createRemoteViews() {
		
		RemoteViews rv = new RemoteViews(
				this.context.getPackageName(), 
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
		
		// In case we have multiple widgets
		for (int i = 0; i < this.appWidgetIds.length; ++i) {	
			
			Intent intent = new Intent(context, WidgeRemoteService.class);
			intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, this.appWidgetIds[i]);
			intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
			
			rv.setRemoteAdapter(R.id.widgetList, intent);
			rv.setEmptyView(R.id.widgetList, R.id.widgetEmpty);
			rv.setTextViewText(R.id.title, this.context.getString(R.string.unread));
		}
				
		return rv;		
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
		this.remoteViews.setTextViewText(R.id.title, context.getString(stringId));
		this.appWidgetManager.updateAppWidget(this.appWidgetIds, this.remoteViews);
	}
	
	/**
	 * Clicking the refresh icon updates the widget
	 */
	private void attachRefresh() {
		
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
		this.remoteViews.setOnClickPendingIntent(R.id.refresh, pendingIntent);
		
		this.appWidgetManager.updateAppWidget(this.appWidgetIds, this.remoteViews);
	}
	
	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		super.onDeleted(context, appWidgetIds);
		Log.d(TAG, "Widget.onDeleted");
	}
	
}