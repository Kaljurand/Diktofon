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

package kaljurand_at_gmail_dot_com.diktofon.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.SearchRecentSuggestions;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map.Entry;

import ee.ioc.phon.netspeechapi.trans.Transcription;

import kaljurand_at_gmail_dot_com.diktofon.BackgroundTranscriber;
import kaljurand_at_gmail_dot_com.diktofon.Dirs;
import kaljurand_at_gmail_dot_com.diktofon.Executable;
import kaljurand_at_gmail_dot_com.diktofon.GuiUtils;
import kaljurand_at_gmail_dot_com.diktofon.Log;
import kaljurand_at_gmail_dot_com.diktofon.MyFileUtils;
import kaljurand_at_gmail_dot_com.diktofon.Recording;
import kaljurand_at_gmail_dot_com.diktofon.RecordingList;
import kaljurand_at_gmail_dot_com.diktofon.R;
import kaljurand_at_gmail_dot_com.diktofon.SearchSuggestionsProvider;
import kaljurand_at_gmail_dot_com.diktofon.Utils;
import kaljurand_at_gmail_dot_com.diktofon.adapter.RecordingListAdapter;
import kaljurand_at_gmail_dot_com.diktofon.provider.Speaker;

/**
 * <p>Main activity of the Diktofon app. Displays the list of recordings, allows them
 * to be viewed and tagged. Allows new recordings to be added.</p>
 * 
 * <p>Title can change (1) on create, (2) after adding a recording, (3) after transcribing a recording,
 * (4) after deleting a recording, after reloading all recordings, and after adding/removing the :notrans-tag.</p>
 * 
 * @author Kaarel Kaljurand
 */
public class RecordingListActivity extends AbstractDiktofonListActivity {

	private static final int MY_ACTIVITY_RECORD_SOUND = 1;
	private static final int ACTIVITY_RECORD_SOUND = 2;
	private static final int ACTIVITY_SELECT_TAGS = 3;
	private static final int ACTIVITY_SELECT_TAGS_FOR_SORT = 4;
	private static final int ACTIVITY_PICK_AUDIO = 5;

	private static final String LOG_TAG = RecordingListActivity.class.getName();

	private SharedPreferences mPrefs;
	private ListView mListView;
	private RecordingList mRecordings;

	private TransHandler mHandler = null;

	private String mQuery;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mHandler = new TransHandler(this);

		mPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		MyFileUtils.createNomedia();

		mListView = getListView();
		mListView.setFastScrollEnabled(true);

		GuiUtils.setDivider(mListView);

		mListView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Recording note = (Recording) mListView.getItemAtPosition(position);
				viewTrans(note);
			}
		});

		handleIntent(getIntent());
		registerForContextMenu(mListView);
	}

	@Override
	public void onStart() {
		super.onStart();
		loadRecordingsInBackground();
	}


	@Override
	protected void onDestroy() {
		super.onDestroy();
	}



	/*
	// TODO: would it be better to use this?
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		toast("Click-" + String.valueOf(position));
	}
	 */


	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.cm_notes, menu);

		// Disable some menu items if they do not make sense in this context.
		// We could also remove them but this might be confusing for the user.
		final AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
		final Recording note = (Recording) mListView.getItemAtPosition(info.position);
		if (! note.needsTrans()) {
			MenuItem menuItem = menu.findItem(R.id.cm_notes_transcribe);
			menuItem.setEnabled(false);
		}
	}


	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		final Recording note = (Recording) mListView.getItemAtPosition(info.position);
		mRecordings.setCurrentRecording(note);
		switch (item.getItemId()) {
		case R.id.cm_notes_view:
			viewTrans(note);
			return true;
		case R.id.cm_notes_tags:
			Intent editTags = new Intent(this, TagSelectorActivity.class);
			editTags.putExtra(TagSelectorActivity.EXTRA_TAGS, Utils.setToArrayList(mRecordings.getTags()));
			editTags.putExtra(TagSelectorActivity.EXTRA_TAGS_SELECTED, Utils.setToArrayList(note.getTags()));
			editTags.putExtra(TagSelectorActivity.EXTRA_ADD_ENABLED, true);
			startActivityForResult(editTags, ACTIVITY_SELECT_TAGS);
			return true;
		case R.id.cm_notes_transcribe:
			transcribeInBackground(note);
			return true;
		case R.id.cm_notes_properties:
			Intent intent1 = new Intent(this, DetailsActivity.class);
			intent1.putExtra(DetailsActivity.EXTRA_TITLE, note.getTimestampAsString());
			intent1.putExtra(DetailsActivity.EXTRA_STRING_ARRAY, note.getDetails());
			startActivity(intent1);
			return true;
		case R.id.cm_notes_delete:
			GuiUtils.getYesNoDialog(
					this,
					String.format(getString(R.string.confirm_delete_recording), note.toString()),
					new Executable() {
						public void execute() {
							mRecordings.remove(note);
							note.delete();
							refreshGui();
						}
					}
					).show();
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_notes, menu);
		return true;
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_notes_add:
			// When recording sounds we want to make sure if the activity stack
			// already contains a recorder then it is reused, i.e. that we
			// do not have several recorders running in parallel.
			if (mPrefs.getBoolean(getString(R.string.keyUseInternalRecorder), getResources().getBoolean(R.bool.defaultUseInternalRecorder))) {
				Intent intent = new Intent(this, RecorderActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
				intent.putExtra(RecorderActivity.EXTRA_BASE_DIR, Dirs.getRecordingsDir().getAbsolutePath());
				if (mPrefs.getString("recordingResolution", "16").equals("16")) {
					intent.putExtra(RecorderActivity.EXTRA_HIGH_RESOLUTION, true);
				} else {
					intent.putExtra(RecorderActivity.EXTRA_HIGH_RESOLUTION, false);
				}
				intent.putExtra(RecorderActivity.EXTRA_SAMPLE_RATE, Integer.parseInt(mPrefs.getString("recordingRate", getString(R.string.defaultRecordingRate))));
				intent.putExtra(RecorderActivity.EXTRA_MICROPHONE_MODE, mPrefs.getString("microphoneMode", getString(R.string.defaultMicrophoneMode)));
				startActivityForResult(intent, MY_ACTIVITY_RECORD_SOUND);
			} else {
				Intent intent = new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
				startActivityForResult(intent, ACTIVITY_RECORD_SOUND);
			}
			return true;
		case R.id.menu_notes_search:
			onSearchRequested();
			return true;
		case R.id.menu_notes_sort_by_timestamp:
			mRecordings.sort(Recording.TIMESTAMP_COMPARATOR);
			refreshAdapter();
			return true;
		case R.id.menu_notes_sort_by_size:
			mRecordings.sort(Recording.SIZE_COMPARATOR);
			refreshAdapter();
			return true;
		case R.id.menu_notes_sort_by_duration:
			mRecordings.sort(Recording.DURATION_COMPARATOR);
			refreshAdapter();
			return true;
		case R.id.menu_notes_sort_by_wordcount:
			mRecordings.sort(Recording.WORDCOUNT_COMPARATOR);
			refreshAdapter();
			return true;
		case R.id.menu_notes_sort_by_speakercount:
			mRecordings.sort(Recording.SPEAKERCOUNT_COMPARATOR);
			refreshAdapter();
			return true;
			/*		case R.id.menu_recordings_speakers:
			Intent speakersIntent = new Intent(this, SpeakerListActivity.class);
			startActivity(speakersIntent);
			return true;*/
		case R.id.menu_notes_tags:
			Intent pickTagsIntent = new Intent(this, TagSelectorActivity.class);
			pickTagsIntent.putExtra(TagSelectorActivity.EXTRA_TAGS, Utils.setToArrayList(mRecordings.getTags()));
			pickTagsIntent.putExtra(TagSelectorActivity.EXTRA_ADD_ENABLED, false);
			startActivityForResult(pickTagsIntent, ACTIVITY_SELECT_TAGS_FOR_SORT);
			return true;
		case R.id.menu_settings:
			startActivity(new Intent(this, Preferences.class));
			return true;
		case R.id.menu_notes_transcribe:
			int needsTransCount = mRecordings.getNeedsTransCount();
			if (needsTransCount == 0) {
				toast(getString(R.string.message_nothing_to_transcribe));
			} else {
				AlertDialog ad = GuiUtils.getYesNoDialog(
						this,
						String.format(getString(R.string.confirm_transcribe_all), needsTransCount),
						new Executable() {
							public void execute() {
								transcribeAll();
							}
						}
						);
				ad.show();
			}
			return true;
		case R.id.menu_recordings_import:
			Intent audioPicker = new Intent(Intent.ACTION_GET_CONTENT);
			audioPicker.setType("audio/*");
			startActivityForResult(Intent.createChooser(audioPicker, getString(R.string.chooser_import)), ACTIVITY_PICK_AUDIO);
			return true;
		case R.id.menu_notes_reload:
			loadRecordingsInBackground();
			return true;
		case R.id.menu_about:
			startActivity(new Intent(this, AboutActivity.class));
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}


	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		if (resultCode != Activity.RESULT_OK || intent == null) {
			return;
		}
		switch (requestCode) {
		case ACTIVITY_SELECT_TAGS:
			String[] selectedTags = intent.getStringArrayExtra(TagSelectorActivity.EXTRA_TAGS_SELECTED);
			mRecordings.setTags(new HashSet<String>(Arrays.asList(selectedTags)));
			refreshGui();
			break;
		case ACTIVITY_SELECT_TAGS_FOR_SORT:
			String[] selectedTagsForSort = intent.getStringArrayExtra(TagSelectorActivity.EXTRA_TAGS_SELECTED);
			mRecordings.sort(new Recording.TagComparator(new HashSet<String>(Arrays.asList(selectedTagsForSort))));
			refreshAdapter();
			break;
		case ACTIVITY_PICK_AUDIO:
			copyIntentDataToRecordingsDir(intent);
			break;
		case ACTIVITY_RECORD_SOUND:
			Uri uri = intent.getData();
			String path1 = Utils.getAudioFilenameFromUri(this, uri);
			if (path1 == null) {
				toast(getString(R.string.error_failed_make_recording));
			} else {
				//String mime = getContentResolver().getType(uri);
				try {
					File newFile = MyFileUtils.moveFileToRecordingsDir(new File(path1));
					addRecording(newFile);
				} catch (IOException e) {
					toast(getString(R.string.error_failed_move_external_file));
				}
			}
			break;
		case MY_ACTIVITY_RECORD_SOUND:
			String filename1 = Utils.getAudioFilenameFromUri(this, intent.getData());
			if (filename1 == null) {
				toast(getString(R.string.error_failed_make_recording));
			} else {
				addRecording(new File(filename1));
			}
			break;
		}
	}


	/**
	 * <p>This is called if the activity is called as singleTop.
	 * An activity will always be paused before receiving a new intent,
	 * so we can count on onResume() being called after this method.</p>
	 */
	@Override
	protected void onNewIntent(Intent intent) {
		Log.i(LOG_TAG, "onNewIntent: " + intent);
		setIntent(intent);
		handleIntent(intent);
	}


	private void copyIntentDataToRecordingsDir(Intent intent) {
		Uri audioUri = intent.getData();
		File file = Utils.copyUriToRecordingsDir(this, audioUri);
		if (file != null) {
			addRecording(file);
		}
	}


	/**
	 * <p>Turns the given file into a Recording-object, adds it to
	 * the list of recordings, updates the GUI and optionally starts
	 * transcribing it in the background.</p>
	 * 
	 * @param file audio file
	 */
	private void addRecording(File file) {
		Recording recording = new Recording(file);
		mRecordings.add(0, recording);
		toast(String.format(getString(R.string.toast_add_recording), recording));
		refreshGui();
		if (mPrefs.getBoolean("autotranscribe", false)) {
			transcribeInBackground(recording);
		}
	}


	private void transcribeInBackground(Recording recording) {
		recording.setWaitingTime(Float.parseFloat(mPrefs.getString("transcribingWaitLength", getString(R.string.defaultTranscribingWaitLength))));
		BackgroundTranscriber bt = new BackgroundTranscriber(
				getResources(),
				mHandler,
				recording,
				mPrefs.getString("email", getString(R.string.defaultEmail)),
				recording.getWaitingTime(),
				Integer.parseInt(mPrefs.getString("transcribingPollPause", getString(R.string.defaultTranscribingPollPause))) * 1000,
				Integer.parseInt(mPrefs.getString("transcribingPollAmount", getString(R.string.defaultTranscribingPollAmount)))
				);
		bt.transcribeInBackground();
	}


	private void viewTrans(Recording note) {
		startActivity(TransActivity.createIntent(
				this,
				note.getTimestampAsString(),
				note.getAudioFilePath(),
				note.getTransPath(),
				mQuery
				));
	}


	private void refreshGui() {
		refreshTitle();
		refreshAdapter();
	}


	/**
	 * Call this whenever the list of recordings changes.
	 */
	private void refreshAdapter() {
		ListAdapter adapter = mListView.getAdapter();
		if (adapter == null) {
			// This should never happen
			toast("ERROR: refreshAdapter() failed: mListView.getAdapter() == null");
		} else {
			RecordingListAdapter recordingListAdapter = (RecordingListAdapter) adapter;
			recordingListAdapter.setSearchQuery(mQuery);
			recordingListAdapter.refresh();
		}
	}


	// TODO: add search query match count
	private void refreshTitle() {
		if (mRecordings != null) {
            String info = String.format(getString(R.string.title_notelist_view),
                    mRecordings.size(), mRecordings.getTransCount(), mRecordings.getNeedsTransCount());
            ActionBar ab = getActionBar();
            ab.setSubtitle(info);
		}
	}


	/**
	 * <p>This is called from onCreate which means that the list adapter is
	 * not set yet. So we need to check if the adapter is null.</p>
	 */
	private void handleIntent(Intent intent) {
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			mQuery = intent.getStringExtra(SearchManager.QUERY);
			if (mQuery == null) {
				toast(getString(R.string.message_no_query));
			} else {
				// On the HTC Wildfire keyboard
				// I can't enter the regular vertical bar, but only the
				// broken vertical bar (\u00a6), so we replace it here with
				// the regular vertical bar.
				mQuery = mQuery.replace('\u00a6', '|');
				SearchRecentSuggestions suggestions =
						new SearchRecentSuggestions(this, SearchSuggestionsProvider.AUTHORITY, SearchSuggestionsProvider.MODE);
				suggestions.saveRecentQuery(mQuery, null);

				Log.i(LOG_TAG, "Query: " + mQuery);
				mRecordings.sort(new Recording.MatchComparator(mQuery));
				refreshAdapter();
			}
		} else {
			Log.i(LOG_TAG, "Intent not handled:" + intent);
		}
	}


	private void transcribeAll() {
		int count = 0;
		for (Recording note : mRecordings.list()) {
			if (note.needsTrans()) {
				count++;
				transcribeInBackground(note);
			}
		}
		if (count > 0) {
			toast(String.format(getString(R.string.message_transcribing), count));
		} else {
			toast(getString(R.string.message_nothing_to_transcribe));
		}
	}


	public static class TransHandler extends Handler {
		private final WeakReference<RecordingListActivity> mRef;
		public TransHandler(RecordingListActivity activity) {
			mRef = new WeakReference<RecordingListActivity>(activity);
		}
		public void handleMessage(Message m) {
			RecordingListActivity activity = mRef.get();
			if (activity != null) {
				String message = m.getData().getString("message");
				if (message != null) {
					activity.toast(message);
					// on final transcription result we update the title
					activity.refreshTitle();
				}
				activity.refreshAdapter();
			}
		}
	}


	/**
	 * <p>The list adapter is set when all the recordings have been
	 * loaded. If there is a search query then we sort the recordings
	 * according to the query, otherwise we sort the in temporal order.
	 * Search query is set e.g. when this activity is launched via the QSB.</p>
	 */
	private void setNewListAdapter() {
		if (mQuery == null) {
			mRecordings.sort(Recording.TIMESTAMP_COMPARATOR);
		} else {
			mRecordings.sort(new Recording.MatchComparator(mQuery));
		}
		setListAdapter(new RecordingListAdapter(this, mRecordings));
	}


	/**
	 * This should update two tables: add a speaker into the speakers table
	 * and add a mapping from Transcription-server speaker ID to the speaker table ID.
	 */
	private int addNewSpeakersFromTranscription(Transcription transcription) {
		int count = 0;
		ContentValues values = new ContentValues();
		for (Entry<String, ee.ioc.phon.netspeechapi.trans.Speaker> entry : transcription.getIdToSpeaker().entrySet()) {
			// BUG: Here we should check that the given speaker is not already in the database
			if (! entry.getKey().equals("")) {
				values.put(Speaker.Columns.NAME, entry.getValue().getScreenName());
				values.put(Speaker.Columns.GENDER, entry.getValue().getGender());
				values.put(Speaker.Columns.DESC, "empty");
				getContentResolver().insert(Speaker.Columns.CONTENT_URI, values);
				values.clear();
				count++;
			}
		}
		return count;
	}


	void loadRecordingsInBackground() {
		final File[] files = Dirs.getRecordingsDir().listFiles(Dirs.FILENAME_FILTER);
		if (files == null) {
			toast(getString(R.string.error_cant_read_dir) + ": " + Dirs.getRecordingsDir());
		} else {
			new LoadRecordings(this, files.length).execute(files);
		}
	}


	private class LoadRecordings extends AsyncTask<File[], String, RecordingList> {

		private final ProgressDialog mProgressDialog;

		LoadRecordings(Context context, int maxProgress) {
			mProgressDialog = new ProgressDialog(context);
			mProgressDialog.setTitle(getString(R.string.message_loading_recordings));
			mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			mProgressDialog.setCancelable(false);
			mProgressDialog.setProgress(0);
			mProgressDialog.setMax(maxProgress);
		}

		protected void onPreExecute() {
			mProgressDialog.show();
		}

		protected RecordingList doInBackground(File[]... files) {
			RecordingList recList = new RecordingList();
			int numberOfFiles = files.length;
			if (numberOfFiles > 0) {
				for (File file : files[0]) {
					recList.add(new Recording(file));
					publishProgress(file.toString());
				}
			}
			return recList;
		}

		protected void onProgressUpdate(final String... progress) {
			if (mProgressDialog != null) {
				if (mProgressDialog.getProgress() >= mProgressDialog.getMax()) {
					mProgressDialog.dismiss();
				} else {
					mProgressDialog.incrementProgressBy(1);
				}
			}
		}

		protected void onPostExecute(RecordingList recList) {
			mRecordings = recList;
			mProgressDialog.cancel();
			GuiUtils.setEmptyView(RecordingListActivity.this, mListView, getString(R.string.emptyview_recordings));
			setNewListAdapter();
			refreshTitle();
		}
	}
}