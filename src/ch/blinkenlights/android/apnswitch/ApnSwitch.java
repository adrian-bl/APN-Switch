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
		updateWidget(ctx);
	}
	
	@Override
	public void onEnabled(Context ctx) {
	}
	
	@Override
	public void onDisabled(Context ctx) {
	}
	
	@Override
	public void onReceive(Context ctx, Intent intent) {
		final String action = intent.getAction();
		
		if(action.equals(Intent.ACTION_TIME_TICK)) {
			// log("onReceive: Ignored ACTION_TIME_TICK");
		}
		else if(action.equals(CLICK)) {
			toggleApnStatus(ctx);
			updateWidget(ctx);
		}
		else {
			super.onReceive(ctx,intent);
		}
	}
	
	private void log(String lmsg) {
		android.util.Log.v("ApnSwitch INFO: ", lmsg);
	}
	
	/*
	 * Returns TRUE if we should use ApnICS();
	*/
	private boolean isICS() {
		return ( android.os.Build.VERSION.SDK_INT >= 14 ? true : false );
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
		if(isICS()) { return (new ApnICS()).getApnStatus(ctx);    }
		else        { return (new ApnLegacy()).getApnStatus(ctx); }
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
		if(isICS()) { (new ApnICS()).setApnStatus(ctx,on);    }
		else        { (new ApnLegacy()).setApnStatus(ctx,on); }
	}
}
