/*
 * Copyright 2011-2012, Institute of Cybernetics at Tallinn University of Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kaljurand_at_gmail_dot_com.diktofon.service;

import kaljurand_at_gmail_dot_com.diktofon.Log;
import kaljurand_at_gmail_dot_com.diktofon.R;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;

public abstract class DiktofonService extends Service {

	private static final String LOG_TAG = DiktofonService.class.getName();

	private NotificationManager mNotificationMngr;
	private SharedPreferences mPreferences;

	private boolean mNotificationIsShowing = false;

	public abstract void cancelNotification();
	protected abstract void saveState();
	protected abstract void releaseResources();


	@Override
	public void onCreate() {
		Log.i(LOG_TAG, "onCreate");
		mPreferences = getSharedPreferences(getString(R.string.file_preferences), 0);
		mNotificationMngr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
	}


	/**
	 * <p>If the service is destroyed (incl. via the general Android
	 * service-management menu), then</p>
	 * 
	 * <ul>
	 * <li>cancel the notifications</li>
	 * <li>save the state</li>
	 * <li>release the resources (e.g. Media Player, Audio Recorder)</li>
	 * </ul>
	 */
	@Override
	public void onDestroy() {
		Log.i(LOG_TAG, "onDestroy");
		cancelNotification();
		saveState();
		releaseResources();
	}


	public void cancelNotification(int notificationId) {
		mNotificationMngr.cancel(notificationId);
		mNotificationIsShowing = false;
	}


	protected SharedPreferences getPreferences() {
		return mPreferences;
	}


	protected boolean isNotificationShowing() {
		return mNotificationIsShowing;
	}


	protected void publishNotification(Notification notification, int notificationId) {
		mNotificationMngr.notify(notificationId, notification);
		mNotificationIsShowing = true;
	}


	/**
	 * @param tickerText Ticker text (shown briefly)
	 * @param contentText Content text (shown when the notification is pulled down)
	 * @param intent Intent to be launched if notification is clicked
	 * @param flags Notification flags
	 * @return Notification object
	 */
	protected Notification createNotification(int icon, CharSequence tickerText, CharSequence contentText, Intent intent, int flags) {
		CharSequence contentTitle = getString(R.string.app_name);
		Notification notification = new Notification(icon, tickerText, System.currentTimeMillis());
		// TODO: are there default flags which we should preserve?
		//notification.flags |= flags;
		notification.flags = flags;
		// TODO: is the this-context good here?
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		notification.setLatestEventInfo(this, contentTitle, contentText, contentIntent);
		return notification;
	}
}