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

package kaljurand_at_gmail_dot_com.diktofon;

import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class BackgroundTranscriber {

	private final Resources mRes;
	private final Handler mHandler;
	private final Recording mRecording;
	private final String mEmail;
	private final int mPollingAmount;
	private final int mInitialWaitLength; // in milliseconds
	private final int mPollingPause; // in milliseconds

	public BackgroundTranscriber(Resources res, Handler handler, Recording recording, String email, int initialWaitLength, int pollingPause, int pollingAmount) {
		mRes = res;
		mHandler = handler;
		mRecording = recording;
		mEmail = email;
		mPollingAmount = pollingAmount;
		mInitialWaitLength = initialWaitLength;
		mPollingPause = pollingPause;
	}


	/**
	 * <p>Transcribes the recording in an other thread.</p>
	 */
	public void transcribeInBackground() {
		Thread t = new Thread() {
			public void run() {
				String message = null;
				try {
					refreshRecording();
					message = String.format(mRes.getString(R.string.message_transcribed), mRecording);
				} catch (TransException e) {
					setStatusAndSendMessage(Recording.State.FAILURE);
					message = e.getMessage();
					mRecording.addMessage(message);
				}
				Bundle b = new Bundle();
				b.putString("message", message);
				Message m = Message.obtain();
				m.setData(b);
				mHandler.sendMessage(m);
			}
		};
		t.start();
	}


	private void refreshRecording() throws TransException {
		String token = mRecording.getToken();
		if (token == null) {
			setStatusAndSendMessage(Recording.State.UPLOADING);
			token = NetSpeechApiUtils.noteToToken(mRecording.getAudioFile(), mRecording.getMime(), mEmail);
			if (token == null) {
				setStatusAndSendMessage(Recording.State.FAILURE);
				throw new TransException(mRes.getString(R.string.error_failed_obtain_token));
			} else {
				setStatusAndSendMessage(Recording.State.WAITING);
				mRecording.setToken(token);
				pauseThread(mInitialWaitLength);
			}
		}
		String trs = NetSpeechApiUtils.tokenToTrans(token);
		int tries = 0;
		mRecording.resetPollCount();
		while (trs == null && tries < mPollingAmount) {
			tries++;
			mRecording.incPollCount();
			setStatusAndSendMessage(Recording.State.POLLING);
			pauseThread(mPollingPause);
			trs = NetSpeechApiUtils.tokenToTrans(token);
		}
		if (trs == null) {
			setStatusAndSendMessage(Recording.State.FAILURE);
			throw new TransException(String.format(mRes.getString(R.string.error_failed_obtain_trans), mPollingAmount));
		}
		setStatusAndSendMessage(Recording.State.SUCCESS);
		mRecording.setTrans(trs);
	}


	private void pauseThread(int duration) throws TransException {
		try {
			Thread.sleep(duration);
		} catch (InterruptedException e) {
			setStatusAndSendMessage(Recording.State.FAILURE);
			throw new TransException(e.getMessage());
		}
	}


	private void setStatusAndSendMessage(Recording.State state) {
		mRecording.setState(state);
		mHandler.sendEmptyMessage(0);
	}
}
