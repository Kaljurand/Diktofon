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

package kaljurand_at_gmail_dot_com.diktofon.activity;

import android.app.ListActivity;
import android.content.SharedPreferences;
import android.widget.Toast;

/**
 * Every Diktofon activity that wants to extend ListActivity
 * should extend this class instead.
 *
 * @author Kaarel Kaljurand
 */
public abstract class AbstractDiktofonListActivity extends ListActivity {

    void toast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }


    void set(SharedPreferences prefs, String key, boolean b) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(key, b);
        editor.apply();
    }
}