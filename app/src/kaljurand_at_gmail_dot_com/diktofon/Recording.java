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

import android.text.TextUtils;
import android.text.format.DateFormat;

import kaljurand_at_gmail_dot_com.estspeechapi.trans.Transcription;

import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

/**
TODO: re-think the exception handling in this class
TODO: localize details and error/warning messages
 */
public class Recording {

	public static enum State {
		INITIAL,
		UPLOADING,
		WAITING,
		POLLING,
		SUCCESS,
		FAILURE;
	}

	public static final String TOKENS = "/tokens/";
	public static final String TRANS = "/trans/";
	public static final String TAGS = "/tags/";
	public static final String TAG_NOTRANS = ":notrans";

	public static final DurationComparator DURATION_COMPARATOR = new DurationComparator();
	public static final SizeComparator SIZE_COMPARATOR = new SizeComparator();
	public static final TimestampComparator TIMESTAMP_COMPARATOR = new TimestampComparator();
	public static final WordcountComparator WORDCOUNT_COMPARATOR = new WordcountComparator();
	public static final SpeakercountComparator SPEAKERCOUNT_COMPARATOR = new SpeakercountComparator();

	// TODO: pass in the current Locale
	// TODO: think about timezones
	//private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss (E)");
	//private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss (EEE)");
	//private static final CharSequence dateFormat = "yyyy-MM-dd (EEE) kk:mm:ss";
	private static final CharSequence dateFormat = "yyyy-MM-dd (EEE) kk:mm";
	private static final int MAX_EXCERPT_LENGTH = 500;

	// TODO: enforce this limit in the GUI
	public static final int MAX_TAG_COUNT = 100;

	private final File mAudioFile;
	private final String mId;
	private final String mMime;
	private final long mTimestamp;
	private final int mDuration;

	private final Map<State, Long> mStateToTime = new HashMap<State, Long>();
	private final Map<String, Integer> mMatchCache = new HashMap<String, Integer>();
	private final List<String> mMessages = new ArrayList<String>();

	private String mExcerpt = null;
	private String mSearchData = null;
	private String mToken = null;
	private int mPollCount = 0;
	private State mState;
	//private Set tags = Collections.synchronizedSet(new HashSet<String>());
	private Set<String> mTags = null;
	private int mWordCount = -1;
	private int mWaitingTime = 0;
	private Transcription mTranscription = null;


	// Guessing the mime from the extension
	public Recording(File voiceFile) {
		mAudioFile = voiceFile;
		mTimestamp = voiceFile.lastModified();
		mId = voiceFile.getName();
		mMime = MyFileUtils.guessMime(voiceFile.toURI().toString());
		mDuration = Utils.getDuration(voiceFile.getAbsolutePath());
		init();
	}


	private void init() {
		getToken();
		initTrans();
		getTags();
		if (hasTrans()) {
			setState(State.SUCCESS);
		} else if (hasToken()) {
			setState(State.INITIAL);
		} else {
			// BUG: possibly also FAILURE
			setState(State.INITIAL);
		}
	}


	public int getDuration() {
		return mDuration;
	}


	public Transcription getTranscription() {
		return mTranscription;
	}


	public String getDurationAsString() {
		return Utils.formatMillis((long) mDuration);
	}


	public File getAudioFile() {
		return mAudioFile;
	}


	public String getAudioFilePath() {
		return mAudioFile.getAbsolutePath();
	}


	public String getTransPath() {
		if (hasTrans()) {
			return Dirs.getBaseDir() + TRANS + mId + ".xml";
		}
		return null;
	}


	public String getId() {
		return mId;
	}


	public String getMime() {
		return mMime;
	}


	public String getMimePart() {
		int slash = mMime.indexOf('/');
		if (slash == -1 || slash == mMime.length() - 1) return mMime;
		return mMime.substring(slash + 1);
	}


	public CharSequence getTimestampAsString() {
		return getTimestampAsString(mTimestamp);
	}


	public String getSizeAsString() {
		return MyFileUtils.getSizeAsString(mAudioFile.length());
	}


	public String toString() {
		return (String) getTimestampAsString();
	}

	// TODO: localize
	public String[] getDetails() {
		return new String[] {
				//"GENERAL",
				"ID: " + getId(),
				"Tags: " + getTagsAsString(),
				//"AUDIO",
				"Audio file: " + getAudioFilePath(),
				"Content type: " + getMime(),
				"Size in bytes: " + mAudioFile.length(),
				"Duration: " + getDurationAsString(),
				//"TRANSCRIPTION",
				"Has transcription: " + hasTrans(),
				"Server token: " + getToken(),
				"Word count: " + getWordCount(),
				"Speaker count: " + getSpeakerCount(),
				"Speakers: " + getSpeakersAsString(),
				//"Sync points: " + getSyncPoints(),
				//"NETWORKING",
				"Poll count: " + getPollCount(),
				//"OTHER",
				"Messages: " + TextUtils.join("\n", getMessages()),
				"States: " + formatStates(mStateToTime)
		};
	}


	public String getSpeakersAsString() {
		if (mTranscription == null) {
			return "";
		}
		return mTranscription.getIdToSpeaker().toString();
	}


	private String getTagsAsString() {
		if (mTags == null) {
			return "";
		}
		return TextUtils.join(", ", mTags);
	}


	private void initTags() {
		mTags = new HashSet<String>();
		File f = makeFile(TAGS, mId);
		try {
			String tagsAsString = MyFileUtils.loadFile(f);
			for (String tag : Utils.parseTagString(tagsAsString)) {
				mTags.add(tag);
			}
		} catch (IOException e) {
			addMessage("getTags: I/O error: " + e.getMessage());
		}
	}


	public Set<String> getTags() {
		if (mTags == null) {
			initTags();
		}
		// return a shallow copy (because the set elements are immutable Strings)
		return (Set<String>) ((HashSet<String>) mTags).clone();
	}


	public SortedSet<String> getTagsAsSortedSet() {
		if (mTags == null) {
			initTags();
		}
		return new TreeSet<String>(mTags);
	}


	public String getToken() {
		if (mToken == null) {
			File f = makeFile(TOKENS, mId);
			try {
				mToken = MyFileUtils.loadFile(f);
			} catch (IOException e) {
				addMessage("getToken: I/O error: " + e.getMessage());
			}
		}
		return mToken;
	}


	/**
	 * <p>Returns a number how many times the given regular expression
	 * occurs in the transcription:</p>
	 * <pre>
	 *   n if there are n matches
	 *   0 if there are no matches
	 *  -1 if search data doesn't exist (i.e. there is no transcription) and :notrans-tag is on
	 *  -2 if search data doesn't exist and :notrans-tag is off
	 * </pre>
	 * <p>-1 and -2 group the notrans-recordings and simply untranscribed recordings into two
	 * separate groups thus simplifying the locating the recordings that _need_ transcription.</p>
	 */
	public int getMatchCount(String query) {
		if (mSearchData == null) {
			if (hasTag(TAG_NOTRANS)) {
				return -1;
			} else {
				return -2;
			}
		}

		Integer matchCount = mMatchCache.get(query);
		if (matchCount == null) {
			int count = Utils.countRe(mSearchData, query);
			mMatchCache.put(query, count);
			return count;
		}
		return matchCount;
	}


	public int getSpeakerCount() {
		if (mTranscription == null) {
			return -1;
		}
		return mTranscription.getIdToSpeaker().size();
	}


	public int getWordCount() {
		return mWordCount;
	}


	public String getExcerpt() {
		return mExcerpt;
	}


	private void initTrans() {
		File f = makeFile(TRANS, mId, ".xml");
		try {
			initTrans(f);
		} catch (SAXException e) {
			addMessage("initTrans: XML error: " + e.getMessage());
		} catch (IOException e) {
			addMessage("initTrans: I/O error: " + e.getMessage());
		}
	}


	private void initTrans(File xmlFile) throws SAXException, IOException {
		mTranscription = new Transcription(xmlFile);
		mSearchData = mTranscription.getPlainText();

		int end = mSearchData.length();
		if (end > MAX_EXCERPT_LENGTH) {
			end = MAX_EXCERPT_LENGTH;
		}
		// Doing "new String" here allows the large string to be garbage collected.
		// But we need to large string anyway, it should stay around as long as the excerpt.
		mExcerpt = new String(mSearchData.substring(0, end));

		// We normalize the whitespace.
		mExcerpt = mExcerpt.replaceAll("\\s+", " ").trim();

		// TODO: would it be faster (or space efficient) to do it with Matcher/Pattern/find,
		// i.e. look for all whitespace sequences, the "whitespace" could also include punctuation
		// To make it faster, this counting can be done at the same time when plain text is generated.
		//wordCount = searchData.split("\\s+").length;
		mWordCount = Utils.countRe(mSearchData, "\\s+");

		// TODO: implement the usage of sync points to map seek points to scroll offsets.
		// syncPoints.addAll(TransUtils.getSyncPoints(f));
	}


	private void storeTags() {
		if (mTags != null && ! mTags.isEmpty()) {
			File f = makeFile(TAGS, mId);
			try {
				MyFileUtils.saveFile(f, TextUtils.join(" ", mTags));
			} catch (IOException e) {
				addMessage("storeTags: I/O error: " + e.getMessage());
			}
		}
	}


	public void setToken(String token) {
		mToken = token;
		if (token != null) {
			File f = makeFile(TOKENS, mId);
			try {
				MyFileUtils.saveFile(f, token);
			} catch (IOException e) {
				addMessage("setToken: I/O error: " + e.getMessage());
			}
		}
	}


	public void setTrans(String xmlString) {
		if (xmlString != null) {
			File f = makeFile(TRANS, mId, ".xml");
			try {
				MyFileUtils.saveFile(f, xmlString);
				initTrans(f);
			} catch (SAXException e) {
				addMessage("setTrans: XML error: " + e.getMessage());
			} catch (IOException e) {
				addMessage("setTrans: I/O error: " + e.getMessage());
			}
		}
	}


	public boolean hasToken() {
		return (mToken != null);
	}


	public boolean hasTrans() {
		return (mWordCount != -1);
	}


	public void delete() {
		mAudioFile.delete();
		makeFile(TOKENS, mId).delete();
		makeFile(TRANS, mId, ".xml").delete();
		makeFile(TAGS, mId).delete();
	}


	public void setState(State state) {
		mState = state;
		mStateToTime.put(state, Utils.getTimestamp());
	}


	public long getStateTime(State state) {
		Long time = mStateToTime.get(state);
		if (time == null) {
			return 0;
		}
		return time;
	}


	public State getState() {
		return mState;
	}


	public void resetPollCount() {
		mPollCount = 0;
	}


	public void incPollCount() {
		mPollCount++;
	}


	public int getPollCount() {
		return mPollCount;
	}


	public void addMessage(String message) {
		mMessages.add(message);
	}


	public List<String> getMessages() {
		return mMessages;
	}


	public boolean hasTags() {
		return (mTags != null && ! mTags.isEmpty());
	}


	public boolean hasTag(String tag) {
		return (mTags != null && mTags.contains(tag));
	}


	// tagValue = 0 if this note has no tags
	// tagValue is the largest if this note has only the given tags (= query tags)
	// tagValue is the lowest if this notes has many tags none of which matches the given tags
	public int getTagValue(Set<String> queryTags) {
		if (mTags == null || mTags.isEmpty()) {
			return 0;
		}

		int matchCount = 0;
		for (String queryTag : queryTags) {
			if (mTags.contains(queryTag)) {
				matchCount++;
			}
		}

		return matchCount * MAX_TAG_COUNT - mTags.size();
	}


	public boolean addTag(String tag) {
		boolean b = mTags.add(tag);
		if (b) {
			storeTags();
		}
		return b;
	}


	public boolean removeTag(String tag) {
		boolean b = mTags.remove(tag);
		if (b) {
			storeTags();
		}
		return b;
	}


	public void setTags(Set<String> newTags) {
		mTags.clear();
		mTags.addAll(newTags);
		storeTags();
	}


	public void clearTags() {
		mTags.clear();
		storeTags();
	}


	public boolean needsTrans() {
		return ! (mState == State.UPLOADING || mState == State.POLLING || hasTrans() || hasTag(TAG_NOTRANS));
	}


	public void setWaitingTime(float multiplier) {
		mWaitingTime = (int) (multiplier * mDuration);
	}


	public int getWaitingTime() {
		return mWaitingTime;
	}


	public static class TimestampComparator implements Comparator<Recording> {
		public int compare(Recording n1, Recording n2) {
			return numberCompare(n1.getAudioFile().lastModified(), n2.getAudioFile().lastModified());
		}
	}


	public static class DurationComparator implements Comparator<Recording> {
		public int compare(Recording n1, Recording n2) {
			return numberCompare(n1.getDuration(), n2.getDuration());
		}
	}


	public static class SizeComparator implements Comparator<Recording> {
		public int compare(Recording n1, Recording n2) {
			return numberCompare(n1.getAudioFile().length(), n2.getAudioFile().length());
		}
	}


	public static class MatchComparator implements Comparator<Recording> {
		final String query;

		public MatchComparator(String query) {
			this.query = query;
		}

		public int compare(Recording n1, Recording n2) {
			return numberCompare(n1.getMatchCount(query), n2.getMatchCount(query));
		}

		public String getQuery() {
			return query;
		}
	}


	public static class TagComparator implements Comparator<Recording> {
		final Set<String> tags;

		public TagComparator(Set<String> tags) {
			this.tags = tags;
		}

		public int compare(Recording n1, Recording n2) {
			return numberCompare(n1.getTagValue(tags), n2.getTagValue(tags));
		}
	}


	public static class WordcountComparator implements Comparator<Recording> {
		public int compare(Recording n1, Recording n2) {
			return numberCompare(n1.getWordCount(), n2.getWordCount());
		}
	}


	public static class SpeakercountComparator implements Comparator<Recording> {
		public int compare(Recording n1, Recording n2) {
			return numberCompare(n1.getSpeakerCount(), n2.getSpeakerCount());
		}
	}


	private static int numberCompare(long n1, long n2) {
		if (n1 < n2) return 1;
		if (n1 > n2) return -1;
		return 0;
	}

	private static int numberCompare(int n1, int n2) {
		if (n1 < n2) return 1;
		if (n1 > n2) return -1;
		return 0;
	}


	private static File makeFile(String dirName, String id, String ext) {
		return new File(Dirs.getBaseDir() + dirName + id + ext);
	}


	private static File makeFile(String dirName, String id) {
		return makeFile(dirName, id, ".txt");
	}


	private static String formatStates(Map<State, Long> states) {
		String str = "\n";
		for (State state : states.keySet()) {
			str += state + ": " + Utils.formatMillis(states.get(state)) + "\n";
		}
		return str;
	}


	private static CharSequence getTimestampAsString(long timestamp) {
		return DateFormat.format(dateFormat, timestamp);
	}
}
