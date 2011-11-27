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

package kaljurand_at_gmail_dot_com.diktofon;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.util.Log;

/**
 * <p>Returns raw audio using AudioRecord and saves the result as 16-bit RIFF/WAVE.</p>
 * 
 * <p>Some of the code was borrowed from http://sourceforge.net/projects/rehearsalassist/,
 * see
 * http://rehearsalassist.svn.sourceforge.net/viewvc/rehearsalassist/android/trunk/src/urbanstew/RehearsalAssistant/RehearsalAudioRecorder.java?view=markup</p>
 * 
 * @author Kaarel Kaljurand
 */
public class RawRecorder {

	public enum State {
		// recorder is initializing
		INITIALIZING,

		// recorder has been initialized, recorder not yet started
		READY,

		// recording
		RECORDING,

		// reconstruction needed
		ERROR,

		// reset needed
		STOPPED
	};


	// The interval in which the recorded samples are output to the file
	private static final int TIMER_INTERVAL = 120;

	private AudioRecord mRecorder = null;

	// Stores current amplitude
	// TODO: Why isn't it SHORT (16-bit)?!
	private int	mMaxAmplitude = 0;

	// Output file path
	private File mPath = null;

	// Recorder state
	private State mState;

	// File writer
	private RandomAccessFile mRAFile;

	// Number of channels (MONO = 1, STEREO = 2)
	private short mChannels;

	// Sample rate
	private int mRate;

	// Resolution (8000 or 16000 bits)
	private short mResolution;

	// Buffer size
	private int mBufferSize;

	// Audio source
	private int mSource;

	// Sample size
	private int mFormat;

	// Number of frames written to file on each output
	private int	mFramePeriod;

	// Buffer for output
	private byte[] mBuffer;

	// Number of bytes written to file after header
	// after stop() is called, this size is written to the header/data chunk in the wave file
	private int mPayloadSize;

	// Is the recording in progress
	private boolean mIsRecording = false;

	/**
	 * <p>Returns the state of the recorder in a RawAudioRecord.State typed object.
	 * Useful, as no exceptions are thrown.</p>
	 *
	 * @return recorder state
	 */
	public State getState() {
		return mState;
	}


	private AudioRecord.OnRecordPositionUpdateListener mListener = new AudioRecord.OnRecordPositionUpdateListener() {
		public void onPeriodicNotification(AudioRecord recorder) {
			// public int read (byte[] audioData, int offsetInBytes, int sizeInBytes)
			int numberOfBytes = recorder.read(mBuffer, 0, mBuffer.length); // Fill buffer

			// Some error checking
			if (numberOfBytes == AudioRecord.ERROR_INVALID_OPERATION) {
				Log.e(RawRecorder.class.getName(), "The AudioRecord object was not properly initialized");
				stop();
			} else if (numberOfBytes == AudioRecord.ERROR_BAD_VALUE) {
				Log.e(RawRecorder.class.getName(), "The parameters do not resolve to valid data and indexes.");
				stop();
			} else if (numberOfBytes > mBuffer.length) {
				Log.e(RawRecorder.class.getName(), "Read more bytes than is buffer length:" + numberOfBytes + ": " + mBuffer.length);
				stop();
			} else {
				// Everything seems to be OK, writing the buffer into the file.
				try {
					if (mIsRecording) {
						mRAFile.write(mBuffer);
						mPayloadSize += mBuffer.length;
					}

					if (mResolution == 16) {
						for (int i = 0; i < mBuffer.length/2; i++) {
							short curSample = getShort(mBuffer[i*2], mBuffer[i*2+1]);
							if (curSample > mMaxAmplitude) {
								mMaxAmplitude = curSample;
							}
						}
					} else {
						for (int i = 0; i < mBuffer.length; i++) {
							if (mBuffer[i] > mMaxAmplitude) {
								mMaxAmplitude = mBuffer[i];
							}
						}
					}
				} catch (IOException e) {
					Log.e(RawRecorder.class.getName(), "I/O error occured in OnRecordPositionUpdateListener, recording is aborted");
					stop();
				}
			}
		}

		public void onMarkerReached(AudioRecord recorder) {
			// BUG: NOT USED
		}
	};


	/**
	 * <p>Instantiates a new recorder and sets the state to INITIALIZING.
	 * In case of errors, no exception is thrown, but the state is set to ERROR.</p>
	 */
	public RawRecorder(int audioSource, int sampleRate, int channelConfig, int audioFormat) {
		try {
			if (audioFormat == AudioFormat.ENCODING_PCM_16BIT) {
				mResolution = 16;
			} else {
				mResolution = 8;
			}

			if (channelConfig == AudioFormat.CHANNEL_CONFIGURATION_MONO) {
				mChannels = 1;
			} else {
				mChannels = 2;
			}

			mSource = audioSource;
			mRate = sampleRate;
			mFormat = audioFormat;

			mFramePeriod = sampleRate * TIMER_INTERVAL / 1000;
			mBufferSize = mFramePeriod * 2 * mResolution * mChannels / 8;

			// Check to make sure buffer size is not smaller than the smallest allowed one
			if (mBufferSize < AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)) {
				mBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
				// Set frame period and timer interval accordingly
				mFramePeriod = mBufferSize / ( 2 * mResolution * mChannels / 8 );
				Log.w(RawRecorder.class.getName(), "Increasing buffer size to " + Integer.toString(mBufferSize));
			}

			mRecorder = new AudioRecord(audioSource, sampleRate, channelConfig, audioFormat, mBufferSize);
			if (mRecorder.getState() != AudioRecord.STATE_INITIALIZED) {
				throw new Exception("AudioRecord initialization failed");
			}
			mRecorder.setRecordPositionUpdateListener(mListener);
			mRecorder.setPositionNotificationPeriod(mFramePeriod);

			mMaxAmplitude = 0;
			mPath = null;
			mState = State.INITIALIZING;
		} catch (Exception e) {
			if (e.getMessage() == null) {
				Log.e(RawRecorder.class.getName(), "Unknown error occured while initializing recording");
			} else {
				Log.e(RawRecorder.class.getName(), e.getMessage());
			}
			mState = State.ERROR;
		}
	}


	// returns the filesize (without the header)
	// headerSize = 8 + 36
	public int getLength() {
		return mPayloadSize;
	}


	/**
	 * <p>Sets output file, call directly after construction/reset.</p>
	 */
	public void setOutputFile(File path) {
		if (mState == State.INITIALIZING) {
			mPath = path;
		}
	}


	/**
	 * <p>Returns the largest amplitude sampled since the last call to this method.</p>
	 *
	 * @return The largest amplitude since the last call, or 0 when not in recording state.
	 */
	public int getMaxAmplitude() {
		if (mState == State.RECORDING) {
			int result = mMaxAmplitude;
			mMaxAmplitude = 0;
			return result;
		}
		return 0;
	}


	/**
	 * <p>Prepares the recorder for recording and sets the state to READY.
	 * In case the recorder is not in the INITIALIZING state
	 * and the file path was not set
	 * the recorder is set to the ERROR state, which makes a reconstruction necessary.
	 * The header of the wave file is written.
	 * In case of an exception, the state is changed to ERROR.</p>
	 */
	public void prepare() {
		try {
			if (mState == State.INITIALIZING) {
				if ((mRecorder.getState() == AudioRecord.STATE_INITIALIZED) && (mPath != null)) {
					// write file header

					mRAFile = new RandomAccessFile(mPath, "rw");

					// Set file length to 0, to prevent unexpected behavior in case the file already existed
					mRAFile.setLength(0);
					mRAFile.writeBytes("RIFF");
					mRAFile.writeInt(0); // Final file size not known yet, write 0
					mRAFile.writeBytes("WAVE");
					mRAFile.writeBytes("fmt ");
					mRAFile.writeInt(Integer.reverseBytes(16)); // Sub-chunk size, 16 for PCM
					mRAFile.writeShort(Short.reverseBytes((short) 1)); // AudioFormat, 1 for PCM
					mRAFile.writeShort(Short.reverseBytes(mChannels));// Number of channels, 1 for mono, 2 for stereo
					mRAFile.writeInt(Integer.reverseBytes(mRate)); // Sample rate
					mRAFile.writeInt(Integer.reverseBytes(mRate * (mResolution/8) * mChannels)); // Byte rate, SampleRate*NumberOfChannels*BitsPerSample/8
					mRAFile.writeShort(Short.reverseBytes((short)(mChannels * mResolution/8))); // Block align, NumberOfChannels*BitsPerSample/8
					mRAFile.writeShort(Short.reverseBytes(mResolution)); // Bits per sample
					mRAFile.writeBytes("data");
					mRAFile.writeInt(0); // Data chunk size not known yet, write 0

					mBuffer = new byte[mFramePeriod * (mResolution/8) * mChannels];
					mState = State.READY;
				} else {
					Log.e(RawRecorder.class.getName(), "prepare() method called on uninitialized recorder");
					mState = State.ERROR;
				}
			} else {
				Log.e(RawRecorder.class.getName(), "prepare() method called on illegal state");
				release();
				mState = State.ERROR;
			}
		} catch(Exception e) {
			if (e.getMessage() == null) {
				Log.e(RawRecorder.class.getName(), "Unknown error occured in prepare()");
			} else {
				Log.e(RawRecorder.class.getName(), e.getMessage());
			}
			mState = State.ERROR;
		}
	}


	/**
	 * <p>Releases the resources associated with this class, and removes the unnecessary files, when necessary.</p>
	 */
	public void release() {
		if (mState == State.RECORDING) {
			stop();
		} else {
			if (mState == State.READY) {
				try {
					mRAFile.close(); // Remove prepared file
				} catch (IOException e) {
					Log.e(RawRecorder.class.getName(), "I/O exception occured while closing output file");
				}
				mPath.delete();
			}
		}

		if (mRecorder != null) {
			mRecorder.release();
		}
	}


	/**
	 * @deprecated Just use release, not sure reset() works as it doesn't restore the listener
	 * 
	 * Resets the recorder to the INITIALIZING state, as if it was just created.
	 * In case the class was in RECORDING state, the recording is stopped.
	 * In case of exceptions the class is set to the ERROR state.
	 */
	public void reset()
	{
		try {
			if (mState != State.ERROR) {
				release();
				mPath = null; // Reset file path
				mMaxAmplitude = 0; // Reset amplitude
				mRecorder = new AudioRecord(mSource, mRate, mChannels+1, mFormat, mBufferSize);
				mState = State.INITIALIZING;
			}
		} catch (Exception e) {
			Log.e(RawRecorder.class.getName(), e.getMessage());
			mState = State.ERROR;
		}
	}


	/**
	 * <p>Starts the recording, and sets the state to RECORDING.
	 * Call after prepare().</p>
	 */
	public void start() {
		if (mState == State.READY) {
			mPayloadSize = 0;
			mRecorder.startRecording();
			mRecorder.read(mBuffer, 0, mBuffer.length);
			mState = State.RECORDING;
			mIsRecording = true;
		} else {
			Log.e(RawRecorder.class.getName(), "start() called on illegal state");
			mState = State.ERROR;
		}
	}


	public void resume() {
		mIsRecording = true;
	}


	public void pause() {
		mIsRecording = false;
	}


	public boolean isRecording() {
		return mIsRecording;
	}


	/**
	 * <p>Stops the recording, and sets the state to STOPPED.
	 * Also finalizes the wave file.</p>
	 */
	public void stop() {
		if (mState == State.RECORDING) {
			Log.e(RawRecorder.class.getName(), "Stopping the recorder...");
			// TODO: not sure if we need to set the listener to null
			mRecorder.setRecordPositionUpdateListener(null);
			mRecorder.stop();
			try {
				mRAFile.seek(4); // Write size to RIFF header
				mRAFile.writeInt(Integer.reverseBytes(36+mPayloadSize));

				mRAFile.seek(40); // Write size to Subchunk2Size field
				mRAFile.writeInt(Integer.reverseBytes(mPayloadSize));

				mRAFile.close();
				mState = State.STOPPED;
			} catch (IOException e) {
				Log.e(RawRecorder.class.getName(), "I/O exception occured while closing output file");
				mState = State.ERROR;
			}
		} else {
			Log.e(RawRecorder.class.getName(), "stop() called in illegal state: " + mState);
			mState = State.ERROR;
		}
	}

	/*
	 * <p>Converts two bytes to a short, in LITTLE_ENDIAN format</p>
	 */
	private short getShort(byte argB1, byte argB2) {
		return (short)(argB1 | (argB2 << 8));
	}
}