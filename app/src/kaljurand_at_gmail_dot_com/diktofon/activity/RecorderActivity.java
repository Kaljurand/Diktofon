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

package kaljurand_at_gmail_dot_com.diktofon.activity;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;

import kaljurand_at_gmail_dot_com.diktofon.Dirs;
import kaljurand_at_gmail_dot_com.diktofon.Executable;
import kaljurand_at_gmail_dot_com.diktofon.GuiUtils;
import kaljurand_at_gmail_dot_com.diktofon.Log;
import kaljurand_at_gmail_dot_com.diktofon.MyFileUtils;
import kaljurand_at_gmail_dot_com.diktofon.R;
import kaljurand_at_gmail_dot_com.diktofon.Utils;
import kaljurand_at_gmail_dot_com.diktofon.service.RecorderService;


public class RecorderActivity extends AbstractDiktofonActivity {

    // Base directory (String, default: BUG)
    public static final String EXTRA_BASE_DIR = "BASE_DIR";
    // Recording resolution (boolean, default: true, i.e. 16 bit)
    public static final String EXTRA_HIGH_RESOLUTION = "HIGH_RESOLUTION";
    // Recording sample rate (int, default: 16000, i.e. 16 kHz)
    public static final String EXTRA_SAMPLE_RATE = "SAMPLE_RATE";
    // Recording Microphone Mode (String, default: VOICE_RECOGNITION)
    public static final String EXTRA_MICROPHONE_MODE = "VOICE_RECOGNITION";

    private File mRecordingsDir = null;

    private Button mButtonPauseResumeRecorder;
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
    private int mMicrophoneMode = MediaRecorder.AudioSource.VOICE_RECOGNITION;

    private String mVolumeBar;

    private RecorderService mService;
    private boolean mIsBound = false;

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.i(RecorderActivity.class.getName(), "Service connected");
            mService = ((RecorderService.RecorderBinder) service).getService();

            try {
                mService.startRecording(mMicrophoneMode, mSampleRate, mResolution, getRecordingFile());
                setRecorderStyle(getResources().getColor(R.color.processing));
                setButtonRecording();
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
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mVolumeBar = getString(R.string.volumeBar);

        mButtonPauseResumeRecorder = (Button) findViewById(R.id.buttonPauseResumeRecording);

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
            String microphoneModeName = extras.getString(EXTRA_MICROPHONE_MODE);
            if (microphoneModeName.equals("VOICE_RECOGNITION")) {
                mMicrophoneMode = MediaRecorder.AudioSource.VOICE_RECOGNITION;
            } else if (microphoneModeName.equals("MIC")) {
                mMicrophoneMode = MediaRecorder.AudioSource.MIC;
            } else {
                Log.e("Invalid microphoneMode: " + microphoneModeName + ", using default");
                mMicrophoneMode = MediaRecorder.AudioSource.VOICE_RECOGNITION;
            }
        }

        if (baseDir == null) {
            mRecordingsDir = Dirs.getRecorderDir();
        } else {
            mRecordingsDir = new File(baseDir);
        }

        if (!mHighResolution) {
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
                    mStatusbar.setText(MyFileUtils.getSizeAsStringExact((long) mService.getLength()));
                    mStatusHandler.postDelayed(this, 1000);
                }
            }
        };

        // Show the DB level 10 times in a second.
        mShowVolumeTask = new Runnable() {
            public void run() {
                if (mService != null) {
                    mVolume.setText(makeBar(scaleVolume(mService.getRmsdb())));
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
            mVolumeHandler.postDelayed(mShowVolumeTask, 100);
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

        if (mService != null && !isFinishing()) {
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
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
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


    private int scaleVolume(float db) {
        // TODO: take these from some configuration
        float min = 15.f;
        float max = 30.f;
        int maxLevel = mVolumeBar.length();
        int index = (int) ((db - min) / (max - min) * maxLevel);
        return Math.min(Math.max(0, index), maxLevel);
    }


    private String makeBar(int len) {
        if (len <= 0) return "";
        if (len >= mVolumeBar.length()) return mVolumeBar;
        return mVolumeBar.substring(0, len);
    }


    private void setGuiPausing() {
        Resources res = getResources();
        stopChronometer();
        setRecorderStyle(res.getColor(R.color.d_fg_text_faded));
        mStatusHandler.removeCallbacks(mShowStatusTask);

        mButtonPauseResumeRecorder.setText(getString(R.string.b_recorder_resume));
        mButtonPauseResumeRecorder.setBackgroundDrawable(res.getDrawable(R.drawable.button_record_pause));
        mButtonPauseResumeRecorder.setTextColor(res.getColor(R.color.grey3));
        mButtonPauseResumeRecorder.setShadowLayer(0f, 0f, 0f, res.getColor(R.color.shadow));
    }


    private void setGuiRecording() {
        startChronometer();
        setRecorderStyle(getResources().getColor(R.color.processing));
        mStatusHandler.postDelayed(mShowStatusTask, 100);
        setButtonRecording();
    }


    private void setButtonRecording() {
        Resources res = getResources();
        mButtonPauseResumeRecorder.setText(getString(R.string.b_recorder_pause));
        mButtonPauseResumeRecorder.setBackgroundDrawable(res.getDrawable(R.drawable.button_record));
        mButtonPauseResumeRecorder.setTextColor(res.getColor(R.color.l_bg));
        mButtonPauseResumeRecorder.setShadowLayer(1.6f, 1.5f, 1.3f, res.getColor(R.color.shadow));
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