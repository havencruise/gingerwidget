package com.gingerhq.android;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

public class WidgeRemoteService extends RemoteViewsService {

	@Override
	public RemoteViewsFactory onGetViewFactory(Intent intent) {
		return new WidgetRemoteFactory(this.getApplicationContext(), intent);
	}

}

class WidgetRemoteFactory implements RemoteViewsService.RemoteViewsFactory {

	private static final String TAG = WidgetRemoteFactory.class.getSimpleName();
	
	List<Unread> data;
	Context context;
	
	public WidgetRemoteFactory(Context applicationContext, Intent intent) {
		Log.d(TAG, "WidgetRemoteFactory constructor");
		this.context = applicationContext;
				
		String jsonData = intent.getStringExtra("com.gingerhq.android.Unread");
		this.data = Unread.fromJSON(jsonData);
	}

	@Override
	public int getCount() {
		Log.d(TAG, "WidgetRemoteFactory.getCount: " + this.data.size());
		return this.data.size();
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public RemoteViews getLoadingView() {
		// We don't have a loading view
		return null;
	}

	@Override
	public RemoteViews getViewAt(int position) {
		
		Unread unread = this.data.get(position);
		
		RemoteViews rv = new RemoteViews(
				this.context.getPackageName(), 
				R.layout.row);
		rv.setTextViewText(R.id.rowTitle, unread.title);
		rv.setTextViewText(
				R.id.rowUnreadCount, 
				String.valueOf(unread.unread_count));
		rv.setTextViewText(R.id.rowUser, "In " + unread.team);
		
		rv.setTextViewText(R.id.rowWhen, 
				DateUtils.getRelativeDateTimeString(
						this.context, 
						unread.latest_message.date_latest_activity.getTime(), 
						DateUtils.MINUTE_IN_MILLIS, 
						DateUtils.WEEK_IN_MILLIS, 
						0) );

		// Open browser on click
		
		String msgURL = "https://gingerhq.com" + unread.message.permalink;
		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(msgURL));
		rv.setOnClickFillInIntent(R.id.widgetRow, intent);

		return rv;
	}

	@Override
	public int getViewTypeCount() {
		return 1;
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public void onCreate() {
		Log.d(TAG, "WidgetRemoteFactory.onCreate");
		// Maybe we need to do something here?
	}

	@Override
	public void onDataSetChanged() {
		Log.d(TAG, "WidgetRemoteFactory.onDataSetChanged");
		// Refresh the cursor (?)
	}

	@Override
	public void onDestroy() {
		Log.d(TAG, "WidgetRemoteFactory.onDestroy");
		// pass
	}
	
}