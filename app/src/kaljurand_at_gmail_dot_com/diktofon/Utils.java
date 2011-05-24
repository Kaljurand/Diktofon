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

import android.media.MediaPlayer;
import android.os.Environment;
import android.os.SystemClock;
import android.text.TextUtils.SimpleStringSplitter;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>A collection of static convenience methods.</p>
 * 
 * @author Kaarel Kaljurand
 */
public class Utils {

	public static final String[] EMPTY_STRING_ARRAY = new String[0];


	/**
	 * <p>Returns the number of case insensitive regexp matches in
	 * the given string using the given regexp.</p>
	 */
	public static int countRe(String data, String re) {
		Matcher m = Pattern.compile(re, Pattern.CASE_INSENSITIVE).matcher(data);
		int count = 0;
		while (m.find()) {
			count++;
		}
		return count;
	}


	// Returns milliseconds since boot, including time spent in sleep.
	public static long getTimestamp() {
		return SystemClock.elapsedRealtime();
	}


	/**
	 * Another option: return str.split("\\s+");
	 */
	public static Iterable<String> parseTagString(String str) {
		SimpleStringSplitter splitter = new SimpleStringSplitter(' ');
		splitter.setString(Utils.normalizeWhitespace(str));
		return splitter;
	}


	public static String normalizeWhitespace(String str) {
		return str.replaceAll("\\s+", " ");
	}


	// sorts a set of Strings and returns it as an array
	public static String[] setToArray(Set<String> setString) {
		List<String> list = new ArrayList<String>(setString);
		Collections.sort(list);
		return (String []) list.toArray(EMPTY_STRING_ARRAY);
	}


	public static ArrayList<String> setToArrayList(Set<String> setOfStrings) {
		ArrayList<String> list = new ArrayList<String>(setOfStrings);
		Collections.sort(list);
		return list;
	}


	/**
	 * <p>Returns the duration (in milliseconds) of the audio file located at the given path.
	 * If something happens, e.g. the audio file is not there, the MediaPlayer
	 * could not be started, etc., then returns 0.</p>
	 */
	public static int getDuration(String path) {
		MediaPlayer mp = new MediaPlayer();
		if (mp == null) return 0;
		int duration = 0;
		try {
			mp.setDataSource(path);
			mp.prepare();
			duration = mp.getDuration();
		} catch (IOException e) {
			Log.e(Utils.class.getName(), e.getMessage());
		} finally {
			mp.release();
		}
		return duration;
	}


	public static String formatMillis(long millis) {
		int seconds = (int) (millis / 1000);
		int minutes = (int) (seconds / 60);
		seconds = seconds % 60;

		if (seconds < 10) {
			return minutes + ":0" + seconds;
		}
		return minutes + ":" + seconds;
	}


	public static boolean isStorageWritable() {
		boolean mExternalStorageAvailable = false;
		boolean mExternalStorageWriteable = false;
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) {
			// We can read and write the media
			mExternalStorageAvailable = mExternalStorageWriteable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			// We can only read the media
			mExternalStorageAvailable = true;
			mExternalStorageWriteable = false;
		} else {
			// Something else is wrong. It may be one of many other states, but all we need
			// to know is we can neither read nor write
			mExternalStorageAvailable = mExternalStorageWriteable = false;
		}
		return (mExternalStorageAvailable && mExternalStorageWriteable);
	}


	public static boolean isStorageReadable() {
		boolean mExternalStorageAvailable = false;
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) {
			mExternalStorageAvailable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			mExternalStorageAvailable = true;
		}
		return mExternalStorageAvailable;
	}
}