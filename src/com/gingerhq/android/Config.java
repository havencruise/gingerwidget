package com.gingerhq.android;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class Config extends PreferenceActivity {
	
	//private static final String TAG = PreferenceActivity.class.getSimpleName();
	
	ChangeListener changeListener;
	int appWidgetId;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
				
		this.showPrefs();
		
		this.appWidgetId = this.getAppWidgetId();
		
		this.attachChangeListener();
		this.sendResult();
		
	}
	
	/**
	 * Show the ConfigFrag preferences screen.
	 */
	private void showPrefs() {
		FragmentManager fragManager = getFragmentManager();
		FragmentTransaction tx = fragManager.beginTransaction();
		tx = tx.replace(android.R.id.content, new ConfigFrag());
		tx.commit();
	}

	private int getAppWidgetId() {
		Intent inIntent = getIntent();
		Bundle extra = inIntent.getExtras();
		int appWidgetId = extra.getInt(
				AppWidgetManager.EXTRA_APPWIDGET_ID,
				AppWidgetManager.INVALID_APPWIDGET_ID);
		return appWidgetId;
	}
	
	/**
	 * Send the widget a message requesting it run it's onUpdate method.
	 */
	private void updateWidget() {
		Intent updateIntent = new Intent(getApplicationContext(), Widget.class);
		updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
		int[] ids = {this.appWidgetId};
		updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
		sendBroadcast(updateIntent);
	}
	
	/**
	 * Watch the preferences and update the widget when they change.
	 */
	private void attachChangeListener() {
		Context context = getApplicationContext();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		this.changeListener = new ChangeListener();
		prefs.registerOnSharedPreferenceChangeListener(this.changeListener);
	}
	
	/**
	 * Set positive result, to ensure this widget gets attached to home screen.
	 */
	private void sendResult() {
		Intent resultIntent = new Intent();
		resultIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, this.appWidgetId);
		setResult(RESULT_OK, resultIntent);
	}
	
	/* Inner classes */
	
	class ChangeListener implements SharedPreferences.OnSharedPreferenceChangeListener {

		@Override
		public void onSharedPreferenceChanged(
				SharedPreferences sharedPreferences, 
				String key) {

			updateWidget();
		}
		
	}

}


