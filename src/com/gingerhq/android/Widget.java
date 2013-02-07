package com.gingerhq.android;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.RemoteViews;

public class Widget extends AppWidgetProvider {

	private static final String TAG = Widget.class.getSimpleName();
	
	@Override
	public void onReceive(Context context, Intent intent) {
		super.onReceive(context, intent);
		Log.d(TAG, "Widget.onReceive");
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		super.onUpdate(context, appWidgetManager, appWidgetIds);
		Log.d(TAG, "Widget.onUpdate:" + appWidgetIds.length);
		
		context.startService(new Intent(context, Updater.class));
		
		for (int i = 0; i < appWidgetIds.length; ++i) {		// In case we have multiple widgets
			
			Intent intent = new Intent(context, WidgeRemoteService.class);
			intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetIds[i]);
			intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
			
			RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget);
			rv.setRemoteAdapter(R.id.widgetList, intent);
			rv.setEmptyView(R.id.widgetList, R.id.widgetEmpty);
			
			appWidgetManager.updateAppWidget(appWidgetIds[i], rv);
		}
		
	}

	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		// TODO Auto-generated method stub
		super.onDeleted(context, appWidgetIds);
		Log.d(TAG, "Widget.onDeleted");
	}

}