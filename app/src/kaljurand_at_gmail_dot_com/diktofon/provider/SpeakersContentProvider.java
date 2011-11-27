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

import java.util.HashMap;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public class SpeakersContentProvider extends ContentProvider {

	private static final String TAG = "SpeakersContentProvider";

	private static final String DATABASE_NAME = "speakers.db";

	private static final int DATABASE_VERSION = 5;

	private static final String SPEAKERS_TABLE_NAME = "speakers";
	private static final String TSPEAKERS_TABLE_NAME = "tspeakers";

	public static final String AUTHORITY = "kaljurand_at_gmail_dot_com.diktofon.provider.SpeakersContentProvider";

	private static final UriMatcher sUriMatcher;

	private static final int SPEAKERS = 1;
	private static final int SPEAKER_ID = 2;
	private static final int TSPEAKERS = 3;


	private static HashMap<String, String> speakersProjectionMap;
	private static HashMap<String, String> tspeakersProjectionMap;

	private static class DatabaseHelper extends SQLiteOpenHelper {

		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		/**
		 * Foreign keys are not available e.g. in Android 1.6,
		 * so we try to do without them for the time being.
		 * "FOREIGN KEY(" + TSpeaker.Columns.SPEAKER_ID + ")" + "REFERENCES " + SPEAKERS_TABLE_NAME + "(" + Speaker.Columns._ID + ") ON DELETE CASCADE,"
		 */
		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE " + SPEAKERS_TABLE_NAME + " ("
					+ Speaker.Columns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
					+ Speaker.Columns.NAME + " VARCHAR(255),"
					+ Speaker.Columns.GENDER + " VARCHAR(10),"
					+ Speaker.Columns.DESC + " TEXT"
					+ ");");

			db.execSQL("CREATE TABLE " + TSPEAKERS_TABLE_NAME + " ("
					+ TSpeaker.Columns._ID + " VARCHAR(255) PRIMARY KEY ON CONFLICT REPLACE,"
					+ TSpeaker.Columns.SPEAKER_ID + " INTEGER"
					+ ");");
		}


		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, "Upgrading database v" + oldVersion + " -> v" + newVersion + ", which will destroy all old data.");
			db.execSQL("DROP TABLE IF EXISTS " + SPEAKERS_TABLE_NAME);
			db.execSQL("DROP TABLE IF EXISTS " + TSPEAKERS_TABLE_NAME);
			onCreate(db);
		}
	}

	private DatabaseHelper dbHelper;

	@Override
	public int delete(Uri uri, String where, String[] whereArgs) {
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		int count;
		switch (sUriMatcher.match(uri)) {
		case SPEAKERS:
			count = db.delete(SPEAKERS_TABLE_NAME, where, whereArgs);
			break;

		case TSPEAKERS:
			count = db.delete(TSPEAKERS_TABLE_NAME, where, whereArgs);
			break;

		case SPEAKER_ID:
			String speakerId = uri.getPathSegments().get(1);
			count = db.delete(
					SPEAKERS_TABLE_NAME,
					Speaker.Columns._ID + "=" + speakerId + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""),
					whereArgs);
			break;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}


	@Override
	public String getType(Uri uri) {
		switch (sUriMatcher.match(uri)) {
		case SPEAKERS:
			return Speaker.Columns.CONTENT_TYPE;

		case TSPEAKERS:
			return TSpeaker.Columns.CONTENT_TYPE;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}


	@Override
	public Uri insert(Uri uri, ContentValues initialValues) {
		ContentValues values;
		if (initialValues != null) {
			values = new ContentValues(initialValues);
		} else {
			values = new ContentValues();
		}

		SQLiteDatabase db = dbHelper.getWritableDatabase();
		long rowId = 0;
		Uri returnUri = null;

		switch (sUriMatcher.match(uri)) {
		case SPEAKERS:
			rowId = db.insert(SPEAKERS_TABLE_NAME, Speaker.Columns.DESC, values);
			if (rowId <= 0) {
				throw new SQLException("Failed to insert row into " + uri);
			}
			returnUri = ContentUris.withAppendedId(Speaker.Columns.CONTENT_URI, rowId);
			getContext().getContentResolver().notifyChange(returnUri, null);
			return returnUri;

		case TSPEAKERS:
			rowId = db.insert(TSPEAKERS_TABLE_NAME, TSpeaker.Columns.SPEAKER_ID, values);
			if (rowId <= 0) {
				throw new SQLException("Failed to insert row into " + uri);
			}
			returnUri = Uri.withAppendedPath(TSpeaker.Columns.CONTENT_URI, "BUG: this should be tspeaker ID");
			getContext().getContentResolver().notifyChange(returnUri, null);
			return returnUri;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}


	@Override
	public boolean onCreate() {
		dbHelper = new DatabaseHelper(getContext());
		return true;
	}


	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

		switch (sUriMatcher.match(uri)) {
		case SPEAKERS:
			qb.setTables(SPEAKERS_TABLE_NAME);
			qb.setProjectionMap(speakersProjectionMap);
			break;

		case TSPEAKERS:
			qb.setTables(TSPEAKERS_TABLE_NAME);
			qb.setProjectionMap(tspeakersProjectionMap);
			break;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		SQLiteDatabase db = dbHelper.getReadableDatabase();
		Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);

		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}


	@Override
	public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		int count;
		switch (sUriMatcher.match(uri)) {
		case SPEAKERS:
			count = db.update(SPEAKERS_TABLE_NAME, values, where, whereArgs);
			break;

		case SPEAKER_ID:
			String speakerId = uri.getPathSegments().get(1);
			count = db.update(
					SPEAKERS_TABLE_NAME,
					values,
					Speaker.Columns._ID + "=" + speakerId + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""),
					whereArgs);
			break;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}


	static {
		sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		sUriMatcher.addURI(AUTHORITY, SPEAKERS_TABLE_NAME, SPEAKERS);
		sUriMatcher.addURI(AUTHORITY, SPEAKERS_TABLE_NAME + "/#", SPEAKER_ID);	
		sUriMatcher.addURI(AUTHORITY, TSPEAKERS_TABLE_NAME, TSPEAKERS);

		speakersProjectionMap = new HashMap<String, String>();
		speakersProjectionMap.put(Speaker.Columns._ID, Speaker.Columns._ID);
		speakersProjectionMap.put(Speaker.Columns.NAME, Speaker.Columns.NAME);
		speakersProjectionMap.put(Speaker.Columns.GENDER, Speaker.Columns.GENDER);
		speakersProjectionMap.put(Speaker.Columns.DESC, Speaker.Columns.DESC);

		tspeakersProjectionMap = new HashMap<String, String>();
		tspeakersProjectionMap.put(TSpeaker.Columns._ID, TSpeaker.Columns._ID);
		tspeakersProjectionMap.put(TSpeaker.Columns.SPEAKER_ID, TSpeaker.Columns.SPEAKER_ID);
	}
}