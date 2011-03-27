/*
 * This file is part of ApnSwitch. - (C) 2011 Adrian Ulrich
 *
 * ApnSwitch is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ApnSwitch is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ApnSwitch. If not, see <http://www.gnu.org/licenses/>.
 */

package ch.blinkenlights.android.apnswitch;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;


public class ApnSwitch extends AppWidgetProvider {
	static public final String CLICK = "ch.blinkenlights.android.apnswitch.CLICK";
	
	@Override
	public void onUpdate(Context ctx, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
//		log("onUpdate -> updating widget");
		updateWidget(ctx);
	}
	@Override
	public void onEnabled(Context ctx) {
//		log("** ON ENABLE is ignored **");
	}
	@Override
	public void onDisabled(Context ctx) {
//		log("** ON DISABLE is ignored **");
	}
	@Override
	public void onReceive(Context ctx, Intent intent) {
		final String action = intent.getAction();
		
		if(action.equals(Intent.ACTION_TIME_TICK)) {
			// log("onReceive: Ignored ACTION_TIME_TICK");
		}
		else if(action.equals(CLICK)) {
			log("onReceive: Widget was clicked -> updating");
			toggleApnStatus(ctx);
			updateWidget(ctx);
		}
		else {
			log("onReceive: calling super due to other_event: "+action);
			super.onReceive(ctx,intent);
		}
	}
	
	private void log(String lmsg) {
		android.util.Log.v("ApnSwitch INFO: ", lmsg);
	}
	
	/*
	 * Refresh widget
	*/
	private void updateWidget(Context ctx) {
		RemoteViews      rview = new RemoteViews(ctx.getPackageName(), R.layout.widget);
		ComponentName    cname = new ComponentName(ctx, ApnSwitch.class);
		AppWidgetManager amgr  = AppWidgetManager.getInstance(ctx);
		rview.setImageViewResource(R.id.Icon, getApnStatus(ctx) ? R.drawable.apn_on : R.drawable.apn_off);
		makeClickable(ctx,rview);
		amgr.updateAppWidget(cname,rview);
		log("--> widget refreshed <--");
	}
	
	private void makeClickable(Context ctx, RemoteViews rview) {
		Intent xint = new Intent(CLICK);
		PendingIntent pint = PendingIntent.getBroadcast(ctx, 0, xint, 0);
		rview.setOnClickPendingIntent(R.id.WidgetLayout, pint);
	}
	
	/*
	 * Returns current APN-Status : true == apn is enabled
	*/
	private boolean getApnStatus(Context ctx) {
		ApnDao apn = new ApnDao(ctx.getContentResolver());
		return ( apn.getApnState() == ApplicationConstants.State.ON ? true : false );
	}
	
	/*
	 * Toggle APN on/off
	*/
	private void toggleApnStatus(Context ctx) {
		setApnStatus(ctx, getApnStatus(ctx) ? false : true);
		log("toggleApnStatus: APN-enabled: " + new Boolean(getApnStatus(ctx)).toString());
	}
	
	/*
	 * Enables APN if 'on' is TRUE
	*/
	private void setApnStatus(Context ctx, boolean on) {
		ApnDao apn = new ApnDao(ctx.getContentResolver());
		apn.switchApnState( on ? ApplicationConstants.State.ON : ApplicationConstants.State.OFF );
	}
}
