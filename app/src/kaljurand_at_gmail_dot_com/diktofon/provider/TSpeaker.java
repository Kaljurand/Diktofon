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

package kaljurand_at_gmail_dot_com.diktofon.provider;

import android.net.Uri;
import android.provider.BaseColumns;

public class TSpeaker {

	public TSpeaker() {
	}

	public static final class Columns implements BaseColumns {
		private Columns() {
		}

		public static final Uri CONTENT_URI = Uri.parse("content://" + SpeakersContentProvider.AUTHORITY + "/tspeakers");

		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.kaljurand_at_gmail_dot_com.diktofon";

		public static final String SPEAKER_ID = "SPEAKER_ID";
	}
}