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

package kaljurand_at_gmail_dot_com.diktofon.activity;

import kaljurand_at_gmail_dot_com.diktofon.provider.Speaker;
import android.app.Activity;
import android.net.Uri;
import android.widget.Toast;

/**
 * <p>Every Diktofon activity that wants to extend Activity should
 * extend this class instead.</p>
 * 
 * @author Kaarel Kaljurand
 *
 */
public abstract class AbstractDiktofonActivity extends Activity {

	protected static final Uri SPEAKERS_CONTENT_URI = Speaker.Columns.CONTENT_URI;

	void toast(String message) {
		Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
	}

}