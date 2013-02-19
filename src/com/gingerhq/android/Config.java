package com.gingerhq.android;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;

public class Config extends PreferenceActivity {
	
	private static final String TAG = PreferenceActivity.class.getSimpleName();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Log.d(TAG, "PreferenceActivity.onCreate");
		
		FragmentManager fragManager = getFragmentManager();
		FragmentTransaction tx = fragManager.beginTransaction();
		tx = tx.replace(android.R.id.content, new ConfigFrag());
		tx.commit();
		
		Intent inIntent = getIntent();
		Bundle extra = inIntent.getExtras();
		int appWidgetId = extra.getInt(
				AppWidgetManager.EXTRA_APPWIDGET_ID,
				AppWidgetManager.INVALID_APPWIDGET_ID);
		Log.d(TAG, "appWidgetId: "+ appWidgetId);

		// Docs say the UPDATE doesn't get sent, but actually it does,
		// so don't need to do this
		/*
		Intent updateIntent = new Intent(getApplicationContext(), Widget.class);
		updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
		updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		sendBroadcast(updateIntent);
		*/
		
		Log.d(TAG, "PreferenceActivity: UPDATE sent");
		
		Intent resultIntent = new Intent();
		resultIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		setResult(RESULT_OK, resultIntent);
		Log.d(TAG, "PreferenceActivity: Result set");
	}
	
}


