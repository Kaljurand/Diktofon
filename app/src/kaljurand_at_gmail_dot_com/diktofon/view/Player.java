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

package kaljurand_at_gmail_dot_com.diktofon.view;

import android.content.res.Resources;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import kaljurand_at_gmail_dot_com.diktofon.R;
import kaljurand_at_gmail_dot_com.diktofon.Utils;
import kaljurand_at_gmail_dot_com.diktofon.service.PlayerService;

/**
 * Player is a simple audio player widget with a play/pause button,
 * timer, and seekbar. It connects to an existing PlayerService and
 * controls it via these buttons.
 * 
 * One can only call two methods on the Player object: init and release.
 * 
 * TODO: extend View
 * http://developer.android.com/guide/topics/ui/custom-components.html
 * 
 * @author Kaarel Kaljurand
 */
public class Player {

	private final Resources mRes;
	private final Button mButtonPlaypause;
	private final TextView mTvTimer;
	private final SeekBar mSbSeek;

	private final Handler mHandlerTimer = new Handler();
	private final Runnable mRunnableTimer = new Runnable() {
		public void run() {
			updateTimeControls();
			mHandlerTimer.postDelayed(this, 1000);
		}
	};

	private PlayerService mService;

	public Player(Resources res, Button buttonPlaypause, TextView tvTimer, SeekBar sbSeek) {
		this.mRes = res;
		this.mButtonPlaypause = buttonPlaypause;
		this.mTvTimer = tvTimer;
		this.mSbSeek = sbSeek;
	}


	/**
	 * <p>Sets up the GUI listeners and sets the GUI into playing mode
	 * in case the Service is playing.</p>
	 * 
	 * TODO: Throw IllegalArgumentException if PlayerService == null,
	 * in the rest of the code assume that mService is not null.
	 */
	public void init(PlayerService service) {
		mService = service;
		mSbSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				// We only check the user-initiated changes in the seekbar
				if (fromUser) {
					seekToByPercentage(progress);
				}
			}

			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			public void onStopTrackingTouch(SeekBar seekBar) {
			}
		});


		mButtonPlaypause.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				togglePlayPause();
			}
		});

		if (mService != null && mService.isPlaying()) {
			start();
		} else {
			pause();
			updateTimeControls();
		}
	}


	/**
	 * <p>Removes all the listeners. Call this once you are done
	 * with using the Player.</p>
	 */
	public void release() {
		mSbSeek.setOnSeekBarChangeListener(null);
		mButtonPlaypause.setOnClickListener(null);
		mHandlerTimer.removeCallbacks(mRunnableTimer);
	}


	/**
	 * <p>Toggles the service state and the GUI state between
	 * playing and pausing.</p>
	 */
	private void togglePlayPause() {
		if (mService != null) {
			if (mService.isPlaying()) {
				mService.pause();
				pause();
			} else {
				mService.start();
				start();
			}
		}
	}


	private void seekToByPercentage(int progress) {
		if (mService != null) {
			mService.seekToByPercentage(progress);
			mTvTimer.setText(Utils.formatMillis((long) mService.getCurrentPosition()));
		}
	}


	/**
	 * <p>Sets the GUI into playing mode.</p>
	 */
	public void start() {
		mHandlerTimer.removeCallbacks(mRunnableTimer);
		mHandlerTimer.postDelayed(mRunnableTimer, 100);
		mButtonPlaypause.setBackgroundDrawable(mRes.getDrawable(R.drawable.button_pause));
	}


	/**
	 * <p>Sets the GUI into the pausing mode.</p>
	 */
	private void pause() {
		mHandlerTimer.removeCallbacks(mRunnableTimer);
		mButtonPlaypause.setBackgroundDrawable(mRes.getDrawable(R.drawable.button_play));
	}


	/**
	 * <p>This method is a bit complicated because we want to handle the
	 * situation when the playback is completed, after which a seekTo(0)
	 * is made by the Service.</p>
	 */
	private void updateTimeControls() {
		if (mService == null) {
			mSbSeek.setProgress(0);
			mTvTimer.setText(Utils.formatMillis((long) 0L));
		} else {
			int position = mService.getCurrentPosition();
			if (position == 0) {
				mSbSeek.setProgress(0);
				if (! mService.isPlaying()) {
					pause();
				}
			} else {
				mSbSeek.setProgress(mService.getProgress());
			}
			mTvTimer.setText(Utils.formatMillis((long) position));
		}
	}
}