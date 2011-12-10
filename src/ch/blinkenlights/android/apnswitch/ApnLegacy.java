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

/* ApnDAO method */

public class ApnLegacy {
	
	public ApnLegacy() {
	}
	
	
	public boolean getApnStatus(Context ctx) {
		ApnDao apn = new ApnDao(ctx.getContentResolver());
		return ( apn.getApnState() == ApplicationConstants.State.ON ? true : false );
	}
	
	public void setApnStatus(Context ctx, boolean on) {
		ApnDao apn = new ApnDao(ctx.getContentResolver());
		apn.switchApnState( on ? ApplicationConstants.State.ON : ApplicationConstants.State.OFF );
	}
	
	private void log(String lmsg) {
		android.util.Log.d("ApnLegacy: ", lmsg);
	}
	
}
