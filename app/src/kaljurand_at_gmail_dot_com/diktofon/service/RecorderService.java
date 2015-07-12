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

import java.io.File;
import java.io.IOException;

import kaljurand_at_gmail_dot_com.diktofon.R;
import kaljurand_at_gmail_dot_com.diktofon.RawRecorder;
import kaljurand_at_gmail_dot_com.diktofon.Utils;
import android.app.Notification;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.os.Binder;
import android.os.IBinder;

/**
 * <p>This service controls the Audio Recorder and handles status bar notifications.</p>
 * 
 * @author Kaarel Kaljurand
 */
public class RecorderService extends DiktofonService {

	// Unique Identification Number for the Notification.
	// We use it on Notification start, and to cancel it.
	private static final int NOTIFICATION_ID = R.string.notification_ticker_recorder;

	// This is the object that receives interactions from clients.
	// See RemoteService for a more complete example.
	private final IBinder mBinder = new RecorderBinder();

	private RawRecorder mRecorder;

	// Total duration of the actual recording
	// (i.e. not considering the pauses) in milliseconds.
	private long mTotalRecordingTime;

	// Timestamp of the last start of recording (after a pause)
	private long mStartRecordingTimestamp;

	private File mRecordingFile;

	/**
	 * <p>Class for clients to access.  Because we know this service always
	 * runs in the same process as its clients, we don't need to deal with
	 * IPC.</p>
	 */
	public class RecorderBinder extends Binder {
		public RecorderService getService() {
			return RecorderService.this;
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
		Log.i(RecorderService.class.getName(), "Received start id " + startId + ": " + intent);
		// We want this service to continue running until it is explicitly stopped, so return sticky.
		return START_STICKY;
	}
	 */


	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}


	/**
	 * @return Duration of recording (not considering the pauses) in milliseconds
	 */
	public long getRecordingTime() {
		return mTotalRecordingTime + (Utils.getTimestamp() - mStartRecordingTimestamp);
	}


	/**
	 * @return <code>true</code> if recording is in progress, <code>false</code> otherwise
	 */
	public boolean isRecording() {
		if (mRecorder == null) {
			return false;
		}
		return mRecorder.isRecording();
	}


	/**
	 * <p>Resumes recording</p>
	 */
	public void resume() {
		if (mRecorder != null) {
			mRecorder.resume();
			mStartRecordingTimestamp = Utils.getTimestamp();
		}
	}

	/**
	 * <p>Pauses recording</p>
	 */
	public void pause() {
		if (mRecorder != null) {
			mRecorder.pause();
			mTotalRecordingTime += (Utils.getTimestamp() - mStartRecordingTimestamp);
		}
	}


	/**
	 * <p>Stops recording</p>
	 */
	public void stop() {
		if (mRecorder != null) {
			mRecorder.stop();
			mTotalRecordingTime += (Utils.getTimestamp() - mStartRecordingTimestamp);
		}
	}


	/**
	 * @return Max amplitude since the last measurement
	 */
	public float getRmsdb() {
		if (mRecorder == null) {
			return 0;
		}
		return mRecorder.getRmsdb();
	}


	/**
	 * @return Length of the current recording
	 */
	public int getLength() {
		if (mRecorder == null) {
			return 0;
		}
		return mRecorder.getLength();
	}


	/**
	 * <p>Show a notification while this service is running.</p>
	 */
	public void showNotification(Intent intent, CharSequence title) {
		Notification notification = createNotification(
				R.drawable.ic_stat_notify_recorder,
				getString(R.string.notification_ticker_recorder),
				title,
				intent,
				Notification.FLAG_AUTO_CANCEL | Notification.FLAG_ONGOING_EVENT
				);

		publishNotification(notification, NOTIFICATION_ID);
	}


	@Override
	public void cancelNotification() {
		cancelNotification(NOTIFICATION_ID);
	}


	/**
	 * <p>Starts recording with the given sample rate and given
	 * resolution into the given file.</p>
	 * 
	 * @param sampleRate Sample rate, e.g. 16000 (Hz)
	 * @param resolution Resolution, e.g. AudioFormat.ENCODING_PCM_16BIT
	 * @param recordingFile File to store the raw audio into
	 * @throws IOException if recorder could not be created or file is not writable
	 */
	public void startRecording(int microphoneMode, int sampleRate, int resolution, File recordingFile) throws IOException {
		mRecordingFile = recordingFile;
		// RawRecorder(int audioSource, int sampleRate, int channelConfig, int audioFormat)
		mRecorder = new RawRecorder(microphoneMode, sampleRate, AudioFormat.CHANNEL_CONFIGURATION_MONO, resolution);
		if (mRecorder.getState() == RawRecorder.State.ERROR) {
			mRecorder = null;
			throw new IOException(getString(R.string.error_cant_create_recorder));
		}

		mRecorder.setOutputFile(recordingFile);
		mRecorder.prepare();

		if (mRecorder.getState() != RawRecorder.State.READY) {
			releaseResources();
			throw new IOException(getString(R.string.error_cant_create_recorder));
		}

		mRecorder.start();

		if (mRecorder.getState() != RawRecorder.State.RECORDING) {
			releaseResources();
			throw new IOException(getString(R.string.error_cant_create_recorder));
		}

		mTotalRecordingTime = 0L;
		mStartRecordingTimestamp = Utils.getTimestamp();
	}


	public File getRecordingFile() {
		return mRecordingFile;
	}


	protected void saveState() {
		// TODO: maybe to something here
	}


	/**
	 * <p>Note that release() can be called in any state.
	 * After that the recorder object is no longer available,
	 * so we should set it to <code>null</code>.</p>
	 */
	protected void releaseResources() {
		if (mRecorder != null) {
			mRecorder.release();
			mRecorder = null;
		}
	}
}