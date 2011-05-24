/*
 * Copyright (C) 2011 Kaarel Kaljurand
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

package kaljurand_at_gmail_dot_com.diktofon.activity;

import kaljurand_at_gmail_dot_com.diktofon.Dirs;
import kaljurand_at_gmail_dot_com.diktofon.Executable;
import kaljurand_at_gmail_dot_com.diktofon.GuiUtils;
import kaljurand_at_gmail_dot_com.diktofon.MyFileUtils;
import kaljurand_at_gmail_dot_com.diktofon.R;
import kaljurand_at_gmail_dot_com.diktofon.Utils;
import kaljurand_at_gmail_dot_com.diktofon.service.RecorderService;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioFormat;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;


public class RecorderActivity extends AbstractDiktofonActivity {

	// Base directory (String, default: BUG)
	public static final String EXTRA_BASE_DIR = "BASE_DIR";
	// Recording resolution (boolean, default: true, i.e. 16 bit)
	public static final String EXTRA_HIGH_RESOLUTION = "HIGH_RESOLUTION";
	// Recording sample rate (int, default: 16000, i.e. 16 kHz)
	public static final String EXTRA_SAMPLE_RATE = "SAMPLE_RATE";

	// The amplitude is either short (16-bit) or byte (8-bit)
	private static final double LOG_OF_MAX_VOLUME = Math.log10((double) Short.MAX_VALUE);
	private static final String MAX_BAR = "||||||||||||||||||||";

	private File mRecordingsDir = null;

	private ImageButton mButtonPauseResumeRecorder;
	private TextView mVolume;
	private TextView mStatusbar;
	private Chronometer mChronometer;

	private Handler mStatusHandler = new Handler();
	private Handler mVolumeHandler = new Handler();
	private Runnable mShowStatusTask;
	private Runnable mShowVolumeTask;

	private boolean mHighResolution = true;
	private int mResolution = AudioFormat.ENCODING_PCM_16BIT;
	private int mSampleRate = 16000;

	private RecorderService mService;
	private boolean mIsBound = false;

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			Log.i(RecorderActivity.class.getName(), "Service connected");
			mService = ((RecorderService.RecorderBinder) service).getService();

			try {
				mService.startRecording(mSampleRate, mResolution, getRecordingFile());
				setRecorderStyle(getResources().getColor(R.color.processing));
				startTasks();
			} catch (IOException e) {
				toast(e.getMessage());
				// TODO: check if the SD card is mounted and writable,
				// if not then tell that to the user.
				doUnbindService();
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			// Because it is running in our same process, we should never
			// see this happen.
			mService = null;
			Log.i(RecorderActivity.class.getName(), "Service disconnected");
		}
	};


	void doBindService() {
		Log.i(RecorderActivity.class.getName(), "Binding to RecorderService");
		bindService(new Intent(this, RecorderService.class), mConnection, Context.BIND_AUTO_CREATE);
		mIsBound = true;
	}


	void doUnbindService() {
		if (mIsBound) {
			unbindService(mConnection);
			mIsBound = false;
			mService = null;
		}
	}


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.recorder);

		// Don't shut down the screen.
		// TODO: Think about it.
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		mButtonPauseResumeRecorder = (ImageButton) findViewById(R.id.buttonPauseResumeRecording);

		mVolume = (TextView) findViewById(R.id.volume);
		mStatusbar = (TextView) findViewById(R.id.statusbar);
		mChronometer = (Chronometer) findViewById(R.id.chronometer);

		mVolume.setText("");
		mStatusbar.setText("");

		String baseDir = null;
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			baseDir = extras.getString(EXTRA_BASE_DIR);
			mHighResolution = extras.getBoolean(EXTRA_HIGH_RESOLUTION);
			mSampleRate = extras.getInt(EXTRA_SAMPLE_RATE);
		}

		if (baseDir == null) {
			Dirs.setBaseDir(getPackageName());
			mRecordingsDir = Dirs.getRecorderDir();
		} else {
			mRecordingsDir = new File(baseDir);
		}

		if (! mHighResolution) {
			mResolution = AudioFormat.ENCODING_PCM_8BIT;
		}
	}


	@Override
	public void onStart() {
		super.onStart();

		// Show the file size every second
		mShowStatusTask = new Runnable() {
			public void run() {
				if (mService != null) {
					mStatusbar.setText(MyFileUtils.getSizeInKbAsString((long) mService.getLength()));
					mStatusHandler.postDelayed(this, 1000);
				}
			}
		};

		// Show the max volume 10 times in a second.
		mShowVolumeTask = new Runnable() {
			public void run() {
				if (mService != null) {
					mVolume.setText(makeBar(scaleVolume(mService.getMaxAmplitude())));
					mVolumeHandler.postDelayed(this, 100);
				}
			}
		};

		mButtonPauseResumeRecorder.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (mService == null) {
					doBindService();

				} else {
					if (mService.isRecording()) {
						mService.pause();
						setGuiPausing();
					} else {
						mService.resume();
						setGuiRecording();
					}
				}
			}
		});


		if (mService != null) {
			mService.cancelNotification();
			if (mService.isRecording()) {
				setGuiRecording();
			} else {
				setGuiPausing();
			}
		}
	}


	/**
	 * The activity is going to be hidden. We tear down all the GUI.
	 * If the the activity is not finishing and is thus not going to be destroyed
	 * (e.g. because the HOME-key was pressed), then we put up a notification on the status bar,
	 * so that one can easily return to the activity. The Recorder-service will keep running.
	 */
	@Override
	public void onStop() {
		super.onStop();
		mStatusHandler.removeCallbacks(mShowStatusTask);
		mVolumeHandler.removeCallbacks(mShowVolumeTask);
		mButtonPauseResumeRecorder.setOnClickListener(null);
		stopChronometer();

		if (mService != null && ! isFinishing()) {
			String recorderText = getString(R.string.notification_text_recorder_pausing);
			if (mService.isRecording()) {
				recorderText = getString(R.string.notification_text_recorder_recording);
			}
			// BUG: Think about that:
			// Note that we do not set the extras as we hope to access via onStart.
			// In case we happen to enter via onCreate then the activity will immediately
			// call finish.
			Intent notificationIntent = new Intent(this, RecorderActivity.class);
			// The intent is to destroy all the activities that happen to be on
			// top of the existing Recorder-activity. E.g. if the R&L activity was launched
			// during recording to change the playing audio, then upon re-entry into the
			// Recorder-activity R&L will be closed (of course, the audio will still remain playing).
			notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP| Intent.FLAG_ACTIVITY_SINGLE_TOP);
			mService.showNotification(notificationIntent, recorderText);
		}
	}


	@Override
	public void onDestroy() {
		super.onDestroy();
		doUnbindService();
	}


	/**
	 * If the BACK-key is pressed but we are connected to
	 * the Recorder-service then we ask for a confirmation.
	 * If the user really wants to quit then we close the
	 * service properly.
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (mService != null && keyCode == KeyEvent.KEYCODE_BACK) {
			GuiUtils.getYesNoDialog(
					this,
					getString(R.string.confirm_finish_activity_recorder),
					new Executable() {
						public void execute() {
							if (mService != null) {
								Intent intent = new Intent();
								intent.setData(Uri.fromFile(mService.getRecordingFile()));
								setResult(Activity.RESULT_OK, intent);
								mService.stop();
							}
							finish();
						}
					}
			).show();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}


	private void setRecorderStyle(int color) {
		mVolume.setTextColor(color);
		mStatusbar.setTextColor(color);
		mChronometer.setTextColor(color);
	}


	private void startTasks() {
		mStatusHandler.postDelayed(mShowStatusTask, 100);
		mVolumeHandler.postDelayed(mShowVolumeTask, 100);
		startChronometer();
	}


	// BUG: experimenting with 200
	// How to do it properly?
	private int scaleVolume(int volume) {
		if (volume <= 200) return 0;
		return (int) (MAX_BAR.length() * Math.log10((double) volume) / LOG_OF_MAX_VOLUME);
	}


	private String makeBar(int len) {
		if (len <= 0) return "";
		if (len >= MAX_BAR.length()) return MAX_BAR;
		return MAX_BAR.substring(0, len);
	}


	private void setGuiPausing() {
		stopChronometer();
		setRecorderStyle(getResources().getColor(R.color.d_fg_text_faded));
		mStatusHandler.removeCallbacks(mShowStatusTask);
	}


	private void setGuiRecording() {
		startChronometer();
		setRecorderStyle(getResources().getColor(R.color.processing));
		mStatusHandler.postDelayed(mShowStatusTask, 100);
	}


	private void stopChronometer() {
		mChronometer.stop();
	}


	private void startChronometer() {
		mChronometer.setBase(Utils.getTimestamp() - mService.getRecordingTime());
		mChronometer.start();
	}


	private File getRecordingFile() throws IOException {
		if (!mRecordingsDir.exists() && !mRecordingsDir.mkdirs()) {
			throw new IOException(getString(R.string.error_cant_create_dir) + ": " + mRecordingsDir);
		}
		String path = mRecordingsDir.getAbsolutePath() + "/" + String.valueOf(System.currentTimeMillis()) + ".wav";
		return new File(path);
	}
}