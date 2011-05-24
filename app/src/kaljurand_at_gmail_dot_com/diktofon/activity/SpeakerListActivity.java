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

package kaljurand_at_gmail_dot_com.diktofon.activity;

import kaljurand_at_gmail_dot_com.diktofon.Executable;
import kaljurand_at_gmail_dot_com.diktofon.ExecutableString;
import kaljurand_at_gmail_dot_com.diktofon.GuiUtils;
import kaljurand_at_gmail_dot_com.diktofon.R;
import kaljurand_at_gmail_dot_com.diktofon.provider.Speaker;
import android.app.Activity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

public class SpeakerListActivity extends AbstractDiktofonListActivity {

	public static final String EXTRA_SPEAKERS = "EXTRA_SPEAKERS";
	private static final Uri CONTENT_URI = Speaker.Columns.CONTENT_URI;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		ListView lv = getListView();
		lv.setFastScrollEnabled(true);
		GuiUtils.setEmptyView(this, lv, getString(R.string.emptyview_speakers));

		// BUG: this should depend on the input intent, sometimes
		// we wan to select only a single item (i.e. when establishing
		// a mapping between two IDs), sometimes we want to select several
		// (i.e. when ranking the recording list)
		//lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

		/*
		Bundle extras = getIntent().getExtras();
		if (extras == null) {
			finish();
		}

		Set<String> ids = new HashSet<String>();
		List<String> idsAsList = extras.getStringArrayList(EXTRA_SPEAKERS);
		if (idsAsList != null) {
			ids.addAll(idsAsList);
		}
		 */

		String[] columns = new String[] {
				Speaker.Columns._ID,
				Speaker.Columns.NAME
		};

		int[] to = new int[] {
				R.id.list_item_speaker_id,
				R.id.list_item_speaker_name
		};


		Cursor managedCursor = managedQuery(
				CONTENT_URI,
				columns, 
				// Which rows to return (null = "all rows")
				// BUG: Constrain this using the input extra
				null,
				null,
				// Put the results in ascending order by name
				Speaker.Columns.NAME + " ASC"
		);

		SimpleCursorAdapter mAdapter = new SimpleCursorAdapter(
				this,
				R.layout.list_item_speaker,
				managedCursor,
				columns,
				to
		);
		lv.setAdapter(mAdapter);

		registerForContextMenu(lv);

		// Onclick returns the URI of the speaker
		lv.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Cursor cursor = (Cursor) parent.getItemAtPosition(position); 
				final long key = cursor.getLong(cursor.getColumnIndex(Speaker.Columns._ID));
				Uri selectedSpeakerUri = ContentUris.withAppendedId(CONTENT_URI, key);
				Intent intent = new Intent();
				intent.setData(selectedSpeakerUri);
				setResult(Activity.RESULT_OK, intent);
				finish();
			}
		});
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.speakers, menu);
		return true;
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_speakers_add:
			GuiUtils.getTextEntryDialog(
					this,
					getString(R.string.dialog_title_new_speaker),
					"",
					new ExecutableString() {
						public void execute(String str) {
							addSpeaker(str, "", "");
						}
					}
			).show();
			return true;
			// TODO: sorting
		default:
			return super.onContextItemSelected(item);
		}
	}


	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.cm_speakers, menu);
	}


	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		Cursor cursor = (Cursor) getListView().getItemAtPosition(info.position);
		final long key = cursor.getLong(cursor.getColumnIndex(Speaker.Columns._ID));
		String name = cursor.getString(cursor.getColumnIndex(Speaker.Columns.NAME));

		switch (item.getItemId()) {
		case R.id.cm_speakers_edit:
			GuiUtils.getTextEntryDialog(
					this,
					getString(R.string.dialog_title_new_name),
					name,
					new ExecutableString() {
						public void execute(String newName) {
							updateSpeakerName(key, newName);
						}
					}
			).show();
			return true;
		case R.id.cm_speakers_delete:
			GuiUtils.getYesNoDialog(
					this,
					String.format(getString(R.string.confirm_delete_speaker), name),
					new Executable() {
						public void execute() {
							deleteSpeaker(key);
						}
					}
			).show();
			return true;
			// TODO: show relations to recordings and co-speakers
		default:
			return super.onContextItemSelected(item);
		}
	}


	private void updateSpeakerName(long key, String name) {
		if (name.length() > 0) {
			ContentValues values = new ContentValues();
			values.put(Speaker.Columns.NAME, name);
			Uri speakerUri = ContentUris.withAppendedId(CONTENT_URI, key);
			getContentResolver().update(speakerUri, values, null, null);
			toast(String.format(getString(R.string.toast_speaker_change_name), name));
		}
	}


	private void deleteSpeaker(long key) {
		Uri speakerUri = ContentUris.withAppendedId(CONTENT_URI, key);
		getContentResolver().delete(speakerUri, null, null);
	}


	private void addSpeaker(String name, String gender, String desc) {
		if (name.length() > 0) {
			ContentValues values = new ContentValues();
			values.put(Speaker.Columns.NAME, name);
			values.put(Speaker.Columns.GENDER, gender);
			values.put(Speaker.Columns.DESC, desc);
			getContentResolver().insert(CONTENT_URI, values);
			toast(String.format(getString(R.string.toast_speaker_add_new), name));
		}
	}
}