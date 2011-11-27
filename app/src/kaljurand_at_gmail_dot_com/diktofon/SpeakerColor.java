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

import android.content.res.Resources;

import java.util.HashMap;
import java.util.Map;

public class SpeakerColor {
	// BUG: Is it possible to put this array into the resources
	// otherwise we have to sync it with the color resource file every time
	// a speaker-color is added/removed.
	private static final int[] SPEAKER_COLORS = {
		R.color.speaker1,
		R.color.speaker2,
		R.color.speaker3,
		R.color.speaker4,
		R.color.speaker5,
		R.color.speaker6,
	};

	private final Map<String, Integer> mIdToColor = new HashMap<String, Integer>();
	private final Resources mRes;

	public SpeakerColor(Resources res) {
		mRes = res;
	}


	public int getColor(String id) {
		Integer color = mIdToColor.get(id);

		if (color == null) {
			int size = mIdToColor.size();
			if (size >= SPEAKER_COLORS.length) {
				color = mRes.getColor(R.color.speakerN);
			} else {
				color = mRes.getColor(SPEAKER_COLORS[size]);
			}
			mIdToColor.put(id, color);
		}
		return color;
	}
}