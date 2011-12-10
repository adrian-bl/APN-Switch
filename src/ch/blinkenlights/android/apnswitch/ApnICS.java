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

import android.content.Context;
import android.net.ConnectivityManager;
import java.lang.reflect.Method;

/* IceCreamSandwich method -> use setMobileDataEnabled */

public class ApnICS {
	
	public ApnICS() {
	}
	
	public boolean getApnStatus(Context ctx) {
		boolean result = false;
		try {
			ConnectivityManager cmgr = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
			Method method = cmgr.getClass().getMethod("getMobileDataEnabled");
			method.setAccessible(true);
			result = (Boolean)method.invoke(cmgr);
		}
		catch(Exception e) {
			log("Error: "+e);
		}
		log("API returns: "+result);
		return result;
	}
	
	public void setApnStatus(Context ctx, boolean on) {
		try {
			ConnectivityManager cmgr = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
			Method method = cmgr.getClass().getMethod("setMobileDataEnabled", boolean.class);
			method.invoke(cmgr, on);
			Thread.sleep(250); /* fixme: we should listen for updates */
		}
		catch(Exception e) {
			log("Error: "+e);
		}
	}
	
	private void log(String lmsg) {
		android.util.Log.d("ApnICS: ", lmsg);
	}
	
}
