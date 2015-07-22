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

package kaljurand_at_gmail_dot_com.diktofon;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import android.media.AudioFormat;
import android.media.AudioRecord;

/**
 * <p>Returns raw audio using AudioRecord and saves the result as 16-bit RIFF/WAVE.</p>
 * 
 * @author Kaarel Kaljurand
 */
public class RawRecorder {

	private static final String LOG_TAG = RawRecorder.class.getName();

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

	// Sample rate  (8000 or 16000 samples per second)
	private int mRate;

	// Resolution (8 or 16 bits, maybe 24 with Android 5?)
	private short mResolution;

	// Buffer size
	private int mBufferSize;

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

			mRate = sampleRate;

			mFramePeriod = sampleRate * TIMER_INTERVAL / 1000;
			mBufferSize = mFramePeriod * 2 * mResolution * mChannels / 8;

			// Check to make sure buffer size is not smaller than the smallest allowed one
			if (mBufferSize < AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)) {
				mBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
				// Set frame period and timer interval accordingly
				mFramePeriod = mBufferSize / ( 2 * mResolution * mChannels / 8 );
				Log.i(LOG_TAG, "Increasing buffer size to " + Integer.toString(mBufferSize));
			}

			mRecorder = new AudioRecord(audioSource, sampleRate, channelConfig, audioFormat, mBufferSize);
			if (mRecorder.getState() != AudioRecord.STATE_INITIALIZED) {
				throw new Exception("AudioRecord initialization failed");
			}

			mMaxAmplitude = 0;
			mPath = null;
			mState = State.INITIALIZING;
		} catch (Exception e) {
			if (e.getMessage() == null) {
				Log.e(LOG_TAG, "Unknown error occured while initializing recording");
			} else {
				Log.e(LOG_TAG, e.getMessage());
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
					Log.e(LOG_TAG, "prepare() method called on uninitialized recorder");
					mState = State.ERROR;
				}
			} else {
				Log.e(LOG_TAG, "prepare() method called on illegal state");
				release();
				mState = State.ERROR;
			}
		} catch(Exception e) {
			if (e.getMessage() == null) {
				Log.e(LOG_TAG, "Unknown error occured in prepare()");
			} else {
				Log.e(LOG_TAG, e.getMessage());
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
					Log.e(LOG_TAG, "I/O exception occured while closing output file");
				}
				mPath.delete();
			}
		}

		if (mRecorder != null) {
			mRecorder.release();
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
			new Thread() {
				public void run() {
					while (mRecorder != null && mRecorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
						int status = read(mRecorder);
						if (status < 0) {
							break;
						}
					}
				}
			}.start();
		} else {
			Log.e(LOG_TAG, "start() called on illegal state");
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
			Log.e(LOG_TAG, "Stopping the recorder...");
			mRecorder.stop();
			try {
				mRAFile.seek(4); // Write size to RIFF header
				mRAFile.writeInt(Integer.reverseBytes(36+mPayloadSize));

				mRAFile.seek(40); // Write size to Subchunk2Size field
				mRAFile.writeInt(Integer.reverseBytes(mPayloadSize));

				mRAFile.close();
				mState = State.STOPPED;
			} catch (IOException e) {
				Log.e(LOG_TAG, "I/O exception occured while closing output file");
				mState = State.ERROR;
			}
		} else {
			Log.e(LOG_TAG, "stop() called in illegal state: " + mState);
			mState = State.ERROR;
		}
	}


	/*
	 * <p>Converts two bytes to a short, assuming that the 2nd byte is
	 * more significant (LITTLE_ENDIAN format).</p>
	 * 
	 * <pre>
	 * 255 | (255 << 8)
	 * 65535
	 * </pre>
	 */
	private static short getShort(byte argB1, byte argB2) {
		return (short) (argB1 | (argB2 << 8));
	}


	private int read(AudioRecord recorder) {
		// public int read (byte[] audioData, int offsetInBytes, int sizeInBytes)
		int numberOfBytes = recorder.read(mBuffer, 0, mBuffer.length); // Fill buffer

		// Some error checking
		if (numberOfBytes == AudioRecord.ERROR_INVALID_OPERATION) {
			Log.e(LOG_TAG, "The AudioRecord object was not properly initialized");
			return -1;
		} else if (numberOfBytes == AudioRecord.ERROR_BAD_VALUE) {
			Log.e(LOG_TAG, "The parameters do not resolve to valid data and indexes.");
			return -2;
		} else if (numberOfBytes > mBuffer.length) {
			Log.e(LOG_TAG, "Read more bytes than is buffer length:" + numberOfBytes + ": " + mBuffer.length);
			return -3;
		} else if (numberOfBytes == 0) {
			Log.e(LOG_TAG, "Read zero bytes");
			return -4;
		} else {
			try {
				if (mIsRecording) {
					mRAFile.write(mBuffer);
					mPayloadSize += mBuffer.length;
				}

				// FIXME - needs to be adapted for 24-bit recording
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
				Log.e(LOG_TAG, "I/O error occured in OnRecordPositionUpdateListener, recording is aborted");
				stop();
			}
		}
		return 0;
	}


	/**
	 * @return volume indicator that shows the average volume of the last read buffer
	 */
	public float getRmsdb() {
		if (mState != State.RECORDING) {
			return 0;
		}
		long sumOfSquares = 0;
		for (int i = 0; i < mBuffer.length; i += 2) {
			short curSample = getShort(mBuffer[i], mBuffer[i+1]);
			sumOfSquares += curSample * curSample;
		}
		double rootMeanSquare = Math.sqrt(sumOfSquares / (mBuffer.length / 2));
		if (rootMeanSquare > 1) {
			// TODO: why 10?
			return (float) (10 * Math.log10(rootMeanSquare));
		}
		return 0;
	}
}