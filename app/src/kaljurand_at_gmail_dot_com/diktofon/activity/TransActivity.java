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

import kaljurand_at_gmail_dot_com.diktofon.Executable;
import kaljurand_at_gmail_dot_com.diktofon.ExecutableSpan;
import kaljurand_at_gmail_dot_com.diktofon.GuiUtils;
import kaljurand_at_gmail_dot_com.diktofon.Log;
import kaljurand_at_gmail_dot_com.diktofon.R;
import kaljurand_at_gmail_dot_com.diktofon.SearchSuggestionsProvider;
import kaljurand_at_gmail_dot_com.diktofon.SpeakerColor;
import kaljurand_at_gmail_dot_com.diktofon.provider.TSpeaker;
import kaljurand_at_gmail_dot_com.diktofon.service.PlayerService;
import kaljurand_at_gmail_dot_com.diktofon.view.Player;

import android.app.Activity;
import android.app.Notification;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.SearchRecentSuggestions;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.BackgroundColorSpan;
import android.text.style.StyleSpan;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ScrollView;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import ee.ioc.phon.netspeechapi.trans.Speaker;
import ee.ioc.phon.netspeechapi.trans.Transcription;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * TODO: maybe rename it to ReadAndListenActivity
 * 
 * startActivity called from non-Activity context; forcing Intent.FLAG_ACTIVITY_NEW_TASK
 * 
 * @author Kaarel Kaljurand
 */
public class TransActivity extends AbstractDiktofonActivity {

	// TITLE is shown on the title bar, in status bar notifications,
	// and is used in the subject line when sharing.
	public static final String EXTRA_TITLE = "EXTRA_TITLE";

	// TRANS_PATH is a path to the Trans-file on the SD card, it is also used
	// as a label in the preferences DB to store the scroll position.
	// TODO: use URI instead?
	public static final String EXTRA_TRANS_PATH = "EXTRA_TRANS_PATH";

	// AUDIO_PATH is the SD card path to the audio file, it is also used
	// as a label in the preferences DB to store the seek position and the fact
	// that the audio is playing.
	// TODO: use URI instead?
	public static final String EXTRA_AUDIO_PATH = "EXTRA_AUDIO_PATH";

	// QUERY is the search query, the matches of which will be highlighted
	// in the transcription. It make sense only if the transcription is present.
	public static final String EXTRA_QUERY = "EXTRA_QUERY";

	private static final int ACTIVITY_SELECT_SPEAKER = 1;

	private static final Uri tspeakersTableUri = kaljurand_at_gmail_dot_com.diktofon.provider.TSpeaker.Columns.CONTENT_URI;

	private static final String colSpeakerName = kaljurand_at_gmail_dot_com.diktofon.provider.Speaker.Columns.NAME;
	private static final String colSpeakerId = kaljurand_at_gmail_dot_com.diktofon.provider.TSpeaker.Columns.SPEAKER_ID;

	// Input parameters
	private String mTitle;
	private String mTransPath;
	private String mAudioPath;
	private String mQuery;

	private Transcription mTranscription;
	private Player mPlayer;

	private ScrollView mTransScrollView;
	private TextView mTransView;
	private SharedPreferences mSettings;
	private Resources mRes;

	private String mClickedSpeakerId;
	private PlayerService mService;
	private boolean mIsBound = false;


	public static Intent createIntent(Context context, CharSequence title, String audioPath, String transPath, String query) {
		Intent intent = new Intent(context, TransActivity.class);
		intent.putExtra(TransActivity.EXTRA_TITLE, title);
		intent.putExtra(TransActivity.EXTRA_AUDIO_PATH, audioPath);
		intent.putExtra(TransActivity.EXTRA_TRANS_PATH, transPath);
		intent.putExtra(TransActivity.EXTRA_QUERY, query);
		return intent;
	}


	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			// This is called when the connection with the service has been
			// established, giving us the service object we can use to
			// interact with the service.
			mService = ((PlayerService.PlayerBinder) service).getService();

			Log.i(TransActivity.class.getName(), "Service connected");
			try {
				mService.setAudioPath(getIntent(), mTitle, mAudioPath);
			} catch (IOException e) {
				doUnbindService();
				toast(e.getMessage());
				return;
			}

			mPlayer.init(mService);
			mService.cancelNotification();

			// Since the transcription view accesses the Player-service (clicking
			// on a transcription paragraph will perform a seekTo in the service)
			// it makes sense to set it up once the binding to the service is established.
			// Note that this seems to happen after onResume, i.e. we should not assume
			// in onResume that the transcription has been set up.
			setUpTranscription(mService);
			highlightMatches();
		}

		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			// Because it is running in our same process, we should never
			// see this happen.
			mService = null;
			Log.i(TransActivity.class.getName(), "Service disconnected");
		}
	};


	void doBindService() {
		// Establish a connection with the service.  We use an explicit
		// class name because we want a specific service implementation that
		// we know will be running in our own process (and thus won't be
		// supporting component replacement by other applications).
		bindService(new Intent(this, PlayerService.class), mConnection, Context.BIND_AUTO_CREATE);
		mIsBound = true;
	}


	void doUnbindService() {
		if (mIsBound) {
			unbindService(mConnection);
			mIsBound = false;
		}
	}


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TransActivity.class.getName(), "onCreate");
		setContentView(R.layout.transcription);

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mTitle = extras.getString(EXTRA_TITLE);
			mTransPath = extras.getString(EXTRA_TRANS_PATH);
			mAudioPath = extras.getString(EXTRA_AUDIO_PATH);
			mQuery = extras.getString(EXTRA_QUERY);
		}

		// If certain inputs are missing then we just stop.
		if (mAudioPath == null) {
			Log.i(TransActivity.class.getName(), "Finishing because input is not defined: " + EXTRA_AUDIO_PATH);
			finish();
		}

		mTransScrollView = (ScrollView) findViewById(R.id.scrollViewTrs);
		mTransView = (TextView) findViewById(R.id.transcription);
		mRes = getResources();
		mSettings = getSharedPreferences(getString(R.string.file_preferences), 0);

		mPlayer = new Player(
				mRes,
				(Button) findViewById(R.id.button_trans_playpause),
				(TextView) findViewById(R.id.tv_trans_timer),
				(SeekBar) findViewById(R.id.seekbar_trans_seek)
				);


		// Starting the service, obtaining bindings later
		startService(new Intent(this, PlayerService.class));
		doBindService();
	}


	/**
	 * Normally, onStart is called after onCreate. However, onStart is called immediately
	 * when returning from HOME, because going HOME does not destroy the activity.
	 */
	@Override
	public void onStart() {
		super.onStart();
		Log.i(TransActivity.class.getName(), "onStart");

		if (mService != null) {
			mPlayer.init(mService);
			// Cancel any notifications that the service has possibly put up,
			// because the activity is now visible and it would
			// be confusing if the activity could be relaunched via the status bar.
			mService.cancelNotification();
		}
	}


	/**
	 * onResume is called when the search dialog finishes, we need to show the search results now
	 */
	@Override
	public void onResume() {
		super.onResume();
		Log.i(TransActivity.class.getName(), "onResume");
		highlightMatches();
	}


	/**
	 * onPause is called only when the search dialog comes on: don't do anything.
	 */
	@Override
	public void onPause() {
		super.onPause();
		Log.i(TransActivity.class.getName(), "onPause");
	}


	/**
	 * Save the Player state. If the Player is playing then
	 * put a note up to the status bar to indicate that some
	 * aspects of the TransActivity are going on in the background.
	 * 
	 * onStop is called in these cases:
	 * 1. BACK-key was pressed or finish() was called, i.e. after onStop(), also onDestroy() will be called
	 * 2. HOME-key was pressed, i.e. onDestroy() is not going to be called unless Android needs
	 *    more resources
	 * 3. A new activity is launched as a child (e.g. Sharing, Speaker list). In this case
	 * everything will keep running without any additional work needed. In this case we
	 * do not want to show the statusbar notification. The preferred re-rentry into R&L is
	 * BACK-key in the child activity.
	 */
	@Override
	public void onStop() {
		super.onStop();
		Log.i(TransActivity.class.getName(), "onStop");

		SharedPreferences.Editor editor = mSettings.edit();
		// Log.i(TransActivity.class.getName(), "Saving scroll Y = " + scrollViewTrs.getScrollY());
		editor.putInt("scrollY_" + mTransPath, mTransScrollView.getScrollY());
		editor.commit();

		if (mPlayer != null) {
			mPlayer.release();
		}

		// When we get hidden but the service is playing then
		// we put up a notification on the statusbar to provide
		// an easy access back into the activity.
		// We reuse the same intent that was used to launch this activity.
		// GuiUtils.createTransIntent(this, mTitle, mAudioPath, mTransPath, mQuery),
		if (mService.isPlaying()) {
			mService.showNotification(
					String.format(getString(R.string.notification_text_player_playing), mTitle),
					Notification.FLAG_AUTO_CANCEL | Notification.FLAG_ONGOING_EVENT
					);
		}
	}


	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.i(TransActivity.class.getName(), "onDestroy");

		if (mService != null && mService.isPlaying()) {
			doUnbindService();
		} else {
			doUnbindService();
			boolean succeeded = stopService(new Intent(this, PlayerService.class));
			if (succeeded) {
				Log.i(TransActivity.class.getName(), "Successfully shut down PlayerService");
			} else {
				Log.i(TransActivity.class.getName(), "Failed to shut down PlayerService: there must be bound clients");
			}
		}
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_trans, menu);

		if (mTranscription == null) {
			menu.findItem(R.id.menu_trans_search).setEnabled(false);
			//menu.findItem(R.id.menu_trans_speakers).setEnabled(false);
			menu.findItem(R.id.menu_trans_share_trans).setEnabled(false);
			menu.findItem(R.id.menu_trans_share_all).setEnabled(false);
		}

		return true;
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_trans_search:
			onSearchRequested();
			return true;
			/*		case R.id.menu_trans_speakers:
			if (transcription != null) {
				Intent speakersIntent = new Intent(this, SpeakerListActivity.class);
				speakersIntent.putExtra(SpeakerListActivity.EXTRA_SPEAKERS, Utils.setToArrayList(transcription.getSpeakerIds()));
				startActivity(speakersIntent);
			}
			return true;*/
		case R.id.menu_trans_share_trans:
			share(mTitle, mTransView.getText().toString());
			return true;
		case R.id.menu_trans_share_audio:
			shareAudio();
			return true;
		case R.id.menu_trans_share_all:
			shareAll(mTitle, mTransView.getText().toString());
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}


	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode != Activity.RESULT_OK) {
			return;
		}
		switch (requestCode) {
		case ACTIVITY_SELECT_SPEAKER:
			Uri speakerUri = data.getData();
			if (speakerUri == null) {
				toast(getString(R.string.error_failed_pick_speaker));
			} else {
				addSpeakerIdMapping(mClickedSpeakerId, speakerUri);
				refreshDisplay(mService);
			}
			break;
		}
	}


	@Override
	protected void onNewIntent(Intent intent) {
		Log.i(TransActivity.class.getName(), "onNewIntent: " + intent);
		setIntent(intent);
		handleIntent(intent);
	}


	private void share(String title, String str) {
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.putExtra(Intent.EXTRA_SUBJECT, String.format(getString(R.string.subject_share), title));
		intent.setType("text/plain");
		intent.putExtra(Intent.EXTRA_TEXT, str);
		startActivity(Intent.createChooser(intent, getString(R.string.chooser_share)));
	}


	private void shareAudio() {
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setType("audio/*");
		intent.putExtra(android.content.Intent.EXTRA_STREAM, Uri.fromFile(new File(mAudioPath)));
		startActivity(Intent.createChooser(intent, getString(R.string.chooser_share)));
	}


	/**
	 * Shares the content of the textview, the audio file and the transcription XML file.
	 * Seems to work with GMmail. GMail puts the latter two as attachments and the textview
	 * content into the content of the email. The subject of the email is the title of
	 * the recording.
	 * EverNote required premium account to allow for audio file attachment.
	 * 
	 * @param title
	 * @param str
	 */
	private void shareAll(String title, String str) {
		Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
		intent.putExtra(Intent.EXTRA_SUBJECT, String.format(getString(R.string.subject_share), title));
		intent.putExtra(Intent.EXTRA_TEXT, str);

		ArrayList<Uri> uris = new ArrayList<Uri>();
		uris.add(Uri.fromFile(new File(mAudioPath)));
		if (mTransPath != null) {
			uris.add(Uri.fromFile(new File(mTransPath)));
		}
		intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
		intent.setType("text/plain");
		startActivity(Intent.createChooser(intent, getString(R.string.chooser_share)));
	}


	private void handleIntent(Intent intent) {
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			String query = intent.getStringExtra(SearchManager.QUERY);
			if (query != null) {
				// BUG: a bit of a hack: on the Wildfire keyboard
				// I can't enter the correct vertical bar but only
				// the broken bar (\u00a6)
				query = query.replace('\u00a6', '|');
				SearchRecentSuggestions suggestions =
						new SearchRecentSuggestions(this, SearchSuggestionsProvider.AUTHORITY, SearchSuggestionsProvider.MODE);
				suggestions.saveRecentQuery(query, null);
				mQuery = query;
			}
		}
	}


	private Spannable getSpannable(SpeakerColor speakerColor, Transcription transcription, final PlayerService service) {
		final Map<String, Speaker> idToSpeaker = transcription.getIdToSpeaker();
		Map<String, String> idToLabel = getIdToLabel(idToSpeaker);
		NodeList turns = transcription.getTurns();
		SpannableStringBuilder ssb = new SpannableStringBuilder();
		String currentSpeakerLocalId = null;
		for (int i = 0; i < turns.getLength(); i++) {
			Node turn = turns.item(i);

			// Creating a speaker label
			final String speakerLocalId = transcription.getTurnSpeakerId(turn);
			if (speakerLocalId != null && ! speakerLocalId.equals(currentSpeakerLocalId)) {

				if (currentSpeakerLocalId != null) {
					ssb.append('\n');
				}
				currentSpeakerLocalId = speakerLocalId;
				int labelBegin = ssb.length();
				String label = idToLabel.get(speakerLocalId);
				if (label == null) {
					// BUG: shouldn't be needed
					label = "???";
				}
				ssb.append(label);
				int labelEnd = ssb.length();
				ssb.setSpan(new BackgroundColorSpan(speakerColor.getColor(speakerLocalId)), labelBegin, labelEnd, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
				//ssb.setSpan(new UnderlineSpan(), labelBegin, labelEnd, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
				ssb.setSpan(new StyleSpan(Typeface.BOLD), labelBegin, labelEnd, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
				ssb.setSpan(new ExecutableSpan(new Executable() {
					@Override
					public void execute() {
						Speaker speaker = idToSpeaker.get(speakerLocalId);
						if (speaker != null) {
							Intent pickSpeakerIntent = new Intent(TransActivity.this, SpeakerListActivity.class);
							startActivityForResult(pickSpeakerIntent, ACTIVITY_SELECT_SPEAKER);
							mClickedSpeakerId = speaker.getId();
						}
					}
				}), labelBegin, labelEnd, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
			}

			int textBegin = ssb.length();

			ssb.append(' ');
			ssb.append(transcription.getTurnText(turn));
			int textEnd = ssb.length();

			// Creating click-to-sync
			final int startTime = transcription.getTurnStartTime(turn);
			if (startTime >= 0 && textBegin < textEnd) {
				ssb.setSpan(new ExecutableSpan(new Executable() {
					public void execute() {
						if (service != null) {
							service.seekTo(startTime);
							service.start();
							mPlayer.start();
						}
					}
				}), textBegin, textEnd, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
			}

			ssb.append('\n');
		}
		return ssb;
	}


	Map<String, String> getIdToLabel(Map<String, Speaker> idToSpeaker) {
		Map<String, String> idToLabel = new HashMap<String, String>();
		for (Map.Entry<String, Speaker> entry : idToSpeaker.entrySet()) {
			String tspeakerLocalId = entry.getKey();
			Speaker speaker = entry.getValue();
			String speakerLabel = null;
			if (speaker != null) {
				Cursor cur1 = managedQuery(
						tspeakersTableUri,
						new String[] { colSpeakerId },
						"_id" + "='" + speaker.getId() + "'",
						null,
						null);

				if (cur1.moveToFirst()) {
					int col = cur1.getColumnIndex(colSpeakerId);
					long speakerId = cur1.getLong(col);

					Cursor cur2 = managedQuery(
							SPEAKERS_CONTENT_URI,
							new String[] { colSpeakerName },
							"_id" + "=" + speakerId + "",
							null,
							null);

					if (cur2.moveToFirst()) { 
						int col2 = cur2.getColumnIndex(colSpeakerName);
						speakerLabel = cur2.getString(col2);
					}
				}
			}
			if (speakerLabel == null) {
				if (speaker != null) {
					speakerLabel = speaker.getScreenName();
				} else {
					speakerLabel = tspeakerLocalId + "?";
				}
			}
			idToLabel.put(tspeakerLocalId, speakerLabel);
		}
		return idToLabel;
	}


	private void addSpeakerIdMapping(String tspeakerId, Uri speakerUri) {
		long speakerId = Long.parseLong(speakerUri.getPathSegments().get(1));
		ContentValues values = new ContentValues();
		values.put(TSpeaker.Columns._ID, tspeakerId);
		values.put(TSpeaker.Columns.SPEAKER_ID, speakerId);
		getContentResolver().insert(TSpeaker.Columns.CONTENT_URI, values);
	}


	private void refreshDisplay(PlayerService service) {
		Spannable spannable = getSpannable(new SpeakerColor(mRes), mTranscription, service);
		mTransView.setText(spannable, TextView.BufferType.SPANNABLE);		
	}


	/**
	 * Parsing the transcription file. Basically 3 things can happen:
	 * 1. The Trans-file is successfully parsed and its content can be shown.
	 * 2. SAXException: The Trans-file is readable but contains XML errors:
	 *    the error message will be shown in red.
	 * 3. IOException: The Trans-file is not there (or not readable)
	 */
	private void setUpTranscription(PlayerService service) {
		if (mTransPath == null) {
			mTransView.setText(getString(R.string.message_no_transcription));
		} else {
			try {
				mTranscription = new Transcription(new File(mTransPath));
				mTransView.setMovementMethod(LinkMovementMethod.getInstance());
				refreshDisplay(service);
				final int scrollY = mSettings.getInt("scrollY_" + mTransPath, 0);
				//Log.i(TransActivity.class.getName(), "Scrolling to Y = " + scrollY);
				// Scrolling to the remembered position.
				// We have to post a Runnable to wait until the layout is complete?
				mTransScrollView.post(new Runnable() {
					@Override
					public void run() {
						mTransScrollView.scrollTo(0, scrollY);
					}
				});
			} catch (SAXException e) {
				// Showing the XML parsing error to the user.
				mTransView.setText(getString(R.string.error_failed_parse_trans) + ":\n\n" + e.getMessage());
				mTransView.setTextColor(mRes.getColor(R.color.processing));
			} catch (IOException e) {
				// TRS file was not found, i.e. the audio has not been transcribed, this is not really an error.
				mTransView.setText(getString(R.string.error_failed_load_trans) + ":\n\n" + e.getMessage());
				mTransView.setTextColor(mRes.getColor(R.color.processing));
			}
		}
	}


	private void highlightMatches() {
		// TODO: Don't do anything if the query has not changed
		if (mQuery == null || mTranscription == null) {
			setTitle(mTitle);
		} else {
			Spannable spannable = (Spannable) mTransView.getText();
			GuiUtils.removeHighlight(spannable);
			int count = GuiUtils.highlightRe(spannable, mQuery, mRes.getColor(R.color.highlight));
			setTitle(String.format(getString(R.string.title_note_view), mTitle, count, mQuery));
		}
	}
}