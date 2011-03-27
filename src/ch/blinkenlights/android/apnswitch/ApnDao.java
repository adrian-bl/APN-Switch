/*
 * This file contains code of the APNdroid project.
 * Changes:
 *   - Change package name
 *   - Merge ApnDao, ApplicationConstants and NameUtil into
 *     a single file
*/

/*
 * This file is part of APNdroid.
 *
 * APNdroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * APNdroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with APNdroid. If not, see <http://www.gnu.org/licenses/>.
 */

package ch.blinkenlights.android.apnswitch;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Martin Adamek <martin.adamek@gmail.com>
 * @author Pavlov Dmitry <pavlov.dmitry.n@gmail.com>
 */
public final class ApnDao {

    private static final String ID = "_id";
    private static final String APN = "apn";
    private static final String TYPE = "type";

    // from frameworks/base/core/java/android/provider/Telephony.java
    static final Uri CONTENT_URI = Uri.parse("content://telephony/carriers");

    // from packages/providers/TelephonyProvider/TelephonyProvider.java
    static final Uri PREFERRED_APN_URI = Uri.parse("content://telephony/carriers/preferapn");
    private static final String PREFER_APN_ID_KEY = "apn_id";

    private static final String DB_LIKE_SUFFIX = "%" + NameUtil.SUFFIX;

    private ContentResolver contentResolver;

    private int mmsTarget = ApplicationConstants.State.ON;
    private boolean disableAll = false;
    private static final String[] MMS_SUFFIX = new String[]{"mms" + NameUtil.SUFFIX};

    public ApnDao(ContentResolver contentResolver, int mmsTarget) {
        this.contentResolver = contentResolver;
        this.mmsTarget = mmsTarget;
    }

    public ApnDao(ContentResolver contentResolver) {
        this.contentResolver = contentResolver;
    }

    List<ApnInfo> getEnabledApnsMap() {
        String query;
        boolean disableAll = this.disableAll;
        String disableAllQuery = disableAll ? null : "current is not null";
        if (mmsTarget == ApplicationConstants.State.OFF) {
            query = disableAllQuery;
        } else {
            query = "(not lower(type)='mms' or type is null)";
            if (!disableAll){
                query += " and " +disableAllQuery;
            }
        }
        return selectApnInfo(query, null);
    }

    List<ApnInfo> getDisabledApnsMap() {
        String suffix = DB_LIKE_SUFFIX;
        return selectApnInfo("apn like ? or type like ?", new String[]{suffix, suffix});
    }

    long getPreferredApnId(){
        Cursor cursor = contentResolver.query(PREFERRED_APN_URI, new String[]{ID}, null, null, null);
        cursor.moveToFirst();
        if (!cursor.isAfterLast()){
            return cursor.getLong(0);
        }
        return -1L;
    }

    void restorePreferredApn(long id){
        ContentValues cv = new ContentValues();
        cv.put(PREFER_APN_ID_KEY, id);
        contentResolver.insert(PREFERRED_APN_URI, cv);
    }

    private List<ApnInfo> selectApnInfo(String whereQuery, String[] whereParams) {
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(CONTENT_URI, new String[]{ID, APN, TYPE}, whereQuery, whereParams, null);
            return createApnList(cursor);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    boolean enableAllInDb() {
        List<ApnInfo> apns = getDisabledApnsMap();
        return enableApnList(apns);
    }

    /**
     * Creates list of apn dtos from a DB cursor
     *
     * @param mCursor db cursor with select result set
     * @return list of APN dtos
     */
    private List<ApnInfo> createApnList(Cursor mCursor) {
        List<ApnInfo> result = new ArrayList<ApnInfo>();
        mCursor.moveToFirst();
        while (!mCursor.isAfterLast()) {
            String id = mCursor.getString(0);
            String apn = mCursor.getString(1);
            String type = mCursor.getString(2);
            result.add(new ApnInfo(id, apn, type));
            mCursor.moveToNext();
        }
        return result;
    }

    /**
     * Tries to disable apn's according to user preferences.
     *
     * @return {@code true} if one o more apns changed and {@code false} if all APNs did not changed their states
     */
    boolean disableAllInDb() {
        List<ApnInfo> apns = getEnabledApnsMap();

        //when selected apns is empty
        if (apns.isEmpty()) {
            return countDisabledApns() > 0;
        }

        return disableApnList(apns);
    }

    /**
     * Use this one if you have fresh list of APNs already and you can save one query to DB
     *
     * @param apns list of apns data to modify
     * @return {@code true} if switch was successfull and {@code false} otherwise
     */
    private boolean enableApnList(List<ApnInfo> apns) {
        final ContentResolver contentResolver = this.contentResolver;
        for (ApnInfo apnInfo : apns) {
            ContentValues values = new ContentValues();
            String newApnName = NameUtil.removeSuffix(apnInfo.apn);
            values.put(APN, newApnName);
            String newApnType = NameUtil.removeSuffix(apnInfo.type);
            if ("".equals(newApnType)) {
                values.putNull(TYPE);
            } else {
                values.put(TYPE, newApnType);
            }
            contentResolver.update(CONTENT_URI, values, ID + "=?", new String[]{apnInfo.id});

        }
        return true;//we always return true because in any situation we can reset all apns to initial state
    }

    private boolean disableApnList(List<ApnInfo> apns) {
        final ContentResolver contentResolver = this.contentResolver;
        for (ApnInfo apnInfo : apns) {
            ContentValues values = new ContentValues();
            String newApnName = NameUtil.addSuffix(apnInfo.apn);
            values.put(APN, newApnName);
            String newApnType = NameUtil.addSuffix(apnInfo.type);
            values.put(TYPE, newApnType);
            contentResolver.update(CONTENT_URI, values, ID + "=?", new String[]{apnInfo.id});
        }
        return true;
    }

    /**
     * Performs switching apns work state according to passed state parameter
     *
     * @param targetState apn state. this method tries to make a switch passed target state
     * @return {@code true} if switch was successfull (apn state changed) and {@code false} if apn state was not changed
     */
    boolean switchApnState(int targetState) {
        if (targetState == ApplicationConstants.State.OFF) {
            return disableAllInDb();
        } else {
            return enableAllInDb();
        }
    }

    /**
     * Performs switching apns with 'mms' type according to passed state parameter
     *
     * @param targetState apn state. this method tries to passed target state
     * @return {@code true} if switch was successfull (apn state changed) and {@code false} if apn state was not changed
     */
    boolean switchMmsState(int targetState) {
        if (targetState == ApplicationConstants.State.OFF) {
            final List<ApnInfo> mmsList = selectEnabledMmsApns();
            return mmsList.size() != 0 && disableApnList(mmsList);
        } else {
            return enableApnList(selectDisabledMmsApns());
        }
    }

    /**
     * Calculates current apn state
     *
     * @return current apn state;
     */
    int getApnState() {
        return countDisabledApns() == 0
                ? ApplicationConstants.State.ON
                : ApplicationConstants.State.OFF;
    }

    int countDisabledApns() {
        return executeCountQuery("apn like ? or type like ?", new String[]{DB_LIKE_SUFFIX, DB_LIKE_SUFFIX});
    }

    public int countMmsApns() {
        return executeCountQuery("(type like ? or type like 'mms')"+getCurrentCriteria(), MMS_SUFFIX);
    }

    public int countDisabledMmsApns() {
        return executeCountQuery("type like ?", MMS_SUFFIX);
    }

    private int executeCountQuery(String whereQuery, String[] whereParams) {
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(CONTENT_URI, new String[]{"count(*)"}, whereQuery, whereParams, null);
            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            } else {
                return -1;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public List<ApnInfo> selectDisabledMmsApns() {
        return selectApnInfo("type like ?", MMS_SUFFIX);
    }

    public List<ApnInfo> selectEnabledMmsApns() {
        return selectApnInfo("type like ?"+getCurrentCriteria(), new String[]{"mms"});
    }

    public String getCurrentCriteria(){
        return disableAll ? "" : " and current is not null";
    }

    /**
     * @return current mms state
     */
    public int getMmsState() {
        return countMmsApns() > 0 && countDisabledMmsApns() > 0
                ? ApplicationConstants.State.OFF
                : ApplicationConstants.State.ON;
    }

    public void setMmsTarget(int mmsTarget) {
        this.mmsTarget = mmsTarget;
    }

    public boolean getDisableAllApns(){
        return disableAll;
    }

    public void setDisableAllApns(boolean disableAll){
        this.disableAll = disableAll;
    }

    public long getRandomCurrentDataApn() {
        Cursor cursor = null;
        try{
            cursor = contentResolver.query(CONTENT_URI, new String[]{ID}, "(not lower(type)='mms' or type is null) and current is not null", null, null);
            cursor.moveToFirst();
            if (!cursor.isAfterLast()) {
                return cursor.getLong(0);
            }
        }finally{
            if (cursor != null){
                cursor.close();
            }
        }
        return -1;
    }

    /**
     * Selection of few interesting columns from APN table
     */
    static final class ApnInfo {

        final String id;
        final String apn;
        final String type;

        public ApnInfo(String id, String apn, String type) {
            this.id = id;
            this.apn = apn;
            this.type = type;
        }
    }

}

/* COPY-N-PASTE CODE FROM NameUtil.java and ApplicationConstants.java follows (adrian 20110326) */

/**
 * @author Martin Adamek <martin.adamek@gmail.com>
 */
final class NameUtil {

    public static final String SUFFIX = "apndroid";

    static String addSuffix(String currentName) {
        String result;
        if (currentName == null) {
            result = SUFFIX;
        } else {
            result = currentName + SUFFIX;
        }
        return result;
    }

    static String removeSuffix(String currentName) {        
        return currentName.substring(0, currentName.length() - SUFFIX.length());
    }

}

/**
 * @author Pavlov Dmitry
 * @since 03.10.2009
 */
final class ApplicationConstants {

    public static final String STATUS_EXTRA = "com.google.code.apndroid.intent.extra.STATUS";
    /**
     * Intent name for send to request current apn state.
     */
    public static final String STATUS_REQUEST = "com.google.code.apndroid.intent.action.STATUS_REQUEST";
    /**
     * Intent name for send to switch apn state.
     */
    public static final String CHANGE_STATUS_REQUEST = "com.google.code.apndroid.intent.action.CHANGE_REQUEST";


    public static final String SHOW_NOTIFICATION = "com.google.code.apndroid.intent.extra.SHOW_NOTIFICATION";

    public static final String TARGET_MMS_STATE = "com.google.code.apndroid.intent.extra.TARGET_MMS_STATE";

    public static final String TARGET_APN_STATE = "com.google.code.apndroid.intent.extra.TARGET_STATE";

    /**
     * Intent name for returned result
     */
    public static final String APN_DROID_RESULT = "com.google.code.apndroid.intent.REQUEST_RESULT";
    /**
     * Extra name that holds main apn state
     */
    public static final String RESPONSE_APN_STATE = "APN_STATE";
    /**
     * Extra name that holds mms state. This extra set only if main apn state is 'off' (result == false)
     */
    public static final String RESPONSE_MMS_STATE = "MMS_STATE";
    /**
     * Intent name that holds switch result. It is {@code true} if the resulted switched request performed succesfully
     * and {@code false} otherwise.
     */
    public static final String RESPONSE_SWITCH_SUCCESS = "SWITCH_SUCCESS";

    public static final class State{
        public static final int OFF = 0;
        public static final int ON = 1;
    }

    static final String STATUS_CHANGED_MESSAGE = "com.google.code.apndroid.intent.action.STATUS_CHANGED";

    static final String SETTINGS_TOGGLE_BUTTON = "toggle_preference";

    static final String SETTINGS_KEEP_MMS_ACTIVE = "com.google.code.apndroid.preferences.KEEP_MMS_ENABLED";
    static final String SETTINGS_SHOW_NOTIFICATION = "com.google.code.apndroid.preferences.SHOW_NOTIFICATION";
    static final String SETTINGS_DISABLE_ALL = "com.google.code.apndroid.preferences.DISABLE_ALL";

    static final String SETTING_PREFERRED_APN= "preferred_apn_id";

    /**
     * Common log tag
     */
    static final String APP_LOG = "apndroid.log";

}
