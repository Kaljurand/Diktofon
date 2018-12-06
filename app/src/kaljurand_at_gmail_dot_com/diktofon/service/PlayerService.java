/*
 * Copyright 2011, Institute of Cybernetics at Tallinn University of Technology
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
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;

import java.io.IOException;

import kaljurand_at_gmail_dot_com.diktofon.Log;
import kaljurand_at_gmail_dot_com.diktofon.R;

/**
 * <p>This service controls the Media Player and handles status bar notifications.
 * Activities can request a notification to be put up when they lose
 * focus. The Service can in addition update the notification, e.g. to show
 * how much longer the playback will take place.</p>
 *
 * @author Kaarel Kaljurand
 */
public class PlayerService extends DiktofonService {

    // Unique Identification Number for the Notification.
    // We use it on Notification start, and to cancel it.
    private static final int NOTIFICATION_ID = R.string.notification_ticker_player;

    private final IBinder mBinder = new PlayerBinder();

    // TODO: not sure Intent should be stored in this service
    private Intent mIntent;
    private String mAudioPath;
    private MediaPlayer mMediaPlayer;

    public class PlayerBinder extends Binder {
        public PlayerService getService() {
            return PlayerService.this;
        }
    }


    /**
     * Note: if binding is used then onStartCommand is not called
     * onStartCommand is available only in API level 5.
     * It is possible to use it in a backwards compatible way, but maybe we don't
     * need it anyway.
     */
	/*
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i("Received start id " + startId + ": " + intent);
		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		return START_STICKY;
	}
	 */
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }


    /**
     * <p>We first try to set up a new Media Player to play the given audio.
     * If this succeeds then we kill an existing Media Player (if there is one)
     * and save its state. We then start using the new Media Player, first by
     * restoring the state of the audio (seek position and playing state).</p>
     * <p>
     * TODO: If the audioPath would not change then don't create a new Media Player
     */
    public void setAudioPath(Intent intent, final String title, String audioPath) throws IOException {
        MediaPlayer newMp = new MediaPlayer();

        Log.i("Setting Media Player data source: " + audioPath);
        newMp.setDataSource(audioPath);
        newMp.prepare();

        newMp.setOnCompletionListener(mp -> {
            Log.i("Media Player completed");
            // We are now in the state PlaybackCompleted.
            // Here we can call seekTo() and later start()
            seekTo(0);
            if (isNotificationShowing()) {
                showNotification(
                        String.format(getString(R.string.notification_text_player_completed), title),
                        Notification.FLAG_AUTO_CANCEL
                );
            }
        });

        if (mMediaPlayer != null) {
            Log.i("Releasing existing Media Player");
            saveState(mAudioPath);
            mMediaPlayer.release();
        }

        // The switch to the new audio path succeeded, we will update the fields now.
        mMediaPlayer = newMp;
        mIntent = intent;
        mAudioPath = audioPath;

        // Restore seek-position and playing-state.
        seekTo(getPreferences().getInt("position_" + audioPath, 0));
        if (getPreferences().getBoolean("isPlaying_" + audioPath, false)) {
            start();
        }
    }


    public void start() {
        if (mMediaPlayer != null) {
            mMediaPlayer.start();
        }
    }


    public void togglePlay() {
        if (isPlaying()) {
            pause();
        } else {
            start();
        }
    }


    public boolean isPlaying() {
        if (mMediaPlayer == null) {
            return false;
        }
        return mMediaPlayer.isPlaying();
    }


    /**
     * BUG: It's probably more resource-friendly
     * if we released the Media Player here and
     * saved the seek position. And Player.play() would
     * recreate the Media Player and seek to the stored position.
     */
    public void pause() {
        if (mMediaPlayer != null) {
            mMediaPlayer.pause();
        }
    }


    public void seekTo(int millis) {
        if (mMediaPlayer != null) {
            try {
                mMediaPlayer.seekTo(millis);
            } catch (IllegalStateException e) {
                Log.e("IllegalStateException: seekTo: " + millis);
            }
        }
    }


    public void seekToByPercentage(int positionAsPercentage) {
        if (mMediaPlayer != null) {
            int position = (positionAsPercentage * mMediaPlayer.getDuration()) / 100;
            seekTo(position);
        }
    }


    // BUG: if mp is not prepared then getCurrentPosition() will cause an error state,
    // not sure though what will be returned here...
    // IllegalStateException will be thrown?
    public int getCurrentPosition() {
        if (mMediaPlayer == null) return 0;
        int position = 0;
        try {
            position = mMediaPlayer.getCurrentPosition();
        } catch (IllegalStateException e) {
            // Ignoring the situation where the Media Player state is wrong
            Log.e("IllegalStateException: getCurrentPosition");
        }
        return position;
    }


    // Returns a number between 0 and 100
    // obtaining the max from the MediaPlayer duration.
    public int getProgress() {
        int value = getCurrentPosition();
        if (value == 0) return 0;
        // BUG: if mp is not prepared then getDuration() will cause an error state,
        // not sure though what will max be...
        int max = mMediaPlayer.getDuration();
        if (max == 0) return 0;
        return (value * 100) / max;
    }


    /**
     * <p>Show a notification while the activity is hidden but the service is running.</p>
     */
    public void showNotification(CharSequence title, int flags) {
        Notification notification = createNotification(
                R.drawable.ic_stat_notify_player,
                getString(R.string.notification_ticker_player),
                title,
                mIntent,
                flags
        );

        publishNotification(notification, NOTIFICATION_ID);
    }


    @Override
    public void cancelNotification() {
        cancelNotification(NOTIFICATION_ID);
    }


    @Override
    protected void saveState() {
        if (mAudioPath != null) {
            saveState(mAudioPath);
        }
    }


    @Override
    protected void releaseResources() {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
        }
    }


    private void saveState(String audioPath) {
        SharedPreferences.Editor editor = getPreferences().edit();
        editor.putInt("position_" + audioPath, getCurrentPosition());
        editor.putBoolean("isPlaying_" + audioPath, isPlaying());
        editor.apply();
    }
}