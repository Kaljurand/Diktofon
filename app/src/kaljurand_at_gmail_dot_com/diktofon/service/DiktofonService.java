/*
 * Copyright 2011-2013, Institute of Cybernetics at Tallinn University of Technology
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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import kaljurand_at_gmail_dot_com.diktofon.Log;

public abstract class DiktofonService extends Service {

    private NotificationManager mNotificationMngr;
    private SharedPreferences mPreferences;

    private boolean mNotificationIsShowing = false;

    public abstract void cancelNotification();

    protected abstract void saveState();

    protected abstract void releaseResources();


    @Override
    public void onCreate() {
        Log.i("onCreate");
        mPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
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
        Log.i("onDestroy");
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
}