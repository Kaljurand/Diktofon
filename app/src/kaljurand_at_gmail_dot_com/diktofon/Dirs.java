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

package kaljurand_at_gmail_dot_com.diktofon;

import android.os.Environment;

import java.io.File;
import java.io.FilenameFilter;

public class Dirs {

	// This filter excludes files whose name starts with a dot, e.g. ".nomedia" (which
	// is created by Diktofon), ".DS_Store" which is created by Mac OS X, etc.
	// In other words: no recording can have a name that starts with a dot.
	public static final FilenameFilter FILENAME_FILTER = new FilenameFilter() {
		public boolean accept(File dir, String name) {
			return ! name.startsWith(".");
		}
	};


	private static final String RECORDINGS = "/recordings/";

	// This directory is used for the files that have been recorded
	// using the RecorderActivity but that are not part of the Diktofon
	// collection.
	private static final String RECORDER = "/recorder/";

	private static File sBaseDir;
	private static File sRecordingsDir;
	private static File sNomediaFile;

	static {
		String baseDirAsString = Environment.getExternalStorageDirectory().getAbsolutePath() +
				"/Android/data/kaljurand_at_gmail_dot_com.diktofon/files/";

		sBaseDir = new File(baseDirAsString);
		sRecordingsDir = new File(baseDirAsString + RECORDINGS);
		sNomediaFile = new File(baseDirAsString + RECORDINGS + ".nomedia");
	}

	public static File getBaseDir() {
		return sBaseDir;
	}

	public static File getRecordingsDir() {
		return sRecordingsDir;
	}

	public static File getNomediaFile() {
		return sNomediaFile;
	}

	public static File getRecorderDir() {
		return new File(sBaseDir.getAbsolutePath() + RECORDER);
	}
}