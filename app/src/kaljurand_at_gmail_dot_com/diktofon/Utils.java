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

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.TextUtils.SimpleStringSplitter;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>A collection of static convenience methods.</p>
 *
 * @author Kaarel Kaljurand
 */
public class Utils {

    public static final String[] EMPTY_STRING_ARRAY = new String[0];


    /**
     * <p>Returns the number of case insensitive regexp matches in
     * the given string using the given regexp.</p>
     */
    public static int countRe(String data, String re) {
        Matcher m = Pattern.compile(re, Pattern.CASE_INSENSITIVE).matcher(data);
        int count = 0;
        while (m.find()) {
            count++;
        }
        return count;
    }


    // Returns milliseconds since boot, including time spent in sleep.
    public static long getTimestamp() {
        return SystemClock.elapsedRealtime();
    }


    /**
     * Another option: return str.split("\\s+");
     */
    public static Iterable<String> parseTagString(String str) {
        SimpleStringSplitter splitter = new SimpleStringSplitter(' ');
        splitter.setString(Utils.normalizeWhitespace(str));
        return splitter;
    }


    public static String normalizeWhitespace(String str) {
        return str.replaceAll("\\s+", " ");
    }


    // sorts a set of Strings and returns it as an array
    public static String[] setToArray(Set<String> setString) {
        List<String> list = new ArrayList<String>(setString);
        Collections.sort(list);
        return (String[]) list.toArray(EMPTY_STRING_ARRAY);
    }


    public static ArrayList<String> setToArrayList(Set<String> setOfStrings) {
        ArrayList<String> list = new ArrayList<String>(setOfStrings);
        Collections.sort(list);
        return list;
    }


    /**
     * <p>Returns the duration (in milliseconds) of the audio file located at the given path.
     * If something happens, e.g. the audio file is not there, the MediaPlayer
     * could not be started, etc., then returns 0.</p>
     */
    public static int getDuration(String path) {
        MediaPlayer mp = new MediaPlayer();
        int duration = 0;
        try {
            mp.setDataSource(path);
            mp.prepare();
            duration = mp.getDuration();
        } catch (IOException e) {
            Log.e(Utils.class.getName(), e.getMessage());
        } finally {
            mp.release();
        }
        return duration;
    }


    public static String formatMillis(long millis) {
        int seconds = (int) (millis / 1000);
        int minutes = (int) (seconds / 60);
        seconds = seconds % 60;

        if (seconds < 10) {
            return minutes + ":0" + seconds;
        }
        return minutes + ":" + seconds;
    }


    /**
     * If the URI has the form file:///path/to/file.wav then we return its
     * path, otherwise we hope to find the path in a content provider.
     * <p>
     * TODO: support also HTTP URIs
     */
    public static String getAudioFilenameFromUri(Activity activity, Uri uri) {
        if (uri == null) {
            return null;
        }
        if ("file".equals(uri.getScheme())) {
            return uri.getPath();
        }
        dumpMetaData(activity, uri);
        Cursor c = activity.managedQuery(uri, null, "", null, null);

        if (c == null) {
            return null;
        }

        String filename = null;

        try {
            if (c.moveToFirst()) {
                filename = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
            }
        } catch (IllegalArgumentException e) {
            Log.i("ERROR: no such column: MediaStore.Audio.Media.DATA");
        } finally {
            c.close();
        }
        return filename;
    }

    private static void dumpMetaData(Activity activity, Uri uri) {
        Cursor cursor = activity.getContentResolver()
                .query(uri, null, null, null, null, null);

        try {
            if (cursor != null && cursor.moveToFirst()) {

                String displayName = cursor.getString(
                        cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                Log.i("Display Name: " + displayName);

                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                String size;
                if (!cursor.isNull(sizeIndex)) {
                    size = cursor.getString(sizeIndex);
                } else {
                    size = "Unknown";
                }
                Log.i("Size: " + size);
            }
        } finally {
            cursor.close();
        }
    }

    private static String getName(Activity activity, Uri uri) {
        Cursor cursor = activity.getContentResolver()
                .query(uri, null, null, null, null, null);

        try {
            if (cursor != null && cursor.moveToFirst()) {

                String displayName = cursor.getString(
                        cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                Log.i("Display Name: " + displayName);
                return displayName;
            }
        } finally {
            cursor.close();
        }
        return null;
    }

    public static File copyUriToRecordingsDir(Activity activity, Uri uri) {
        try {
            InputStream is = activity.getContentResolver().openInputStream(uri);
            String newFileName = String.valueOf(System.currentTimeMillis()) + "_" + getName(activity, uri);
            File newFile = Dirs.getRecordingsFile(activity, newFileName);
            Log.i("File: " + newFile);
            OutputStream os = new FileOutputStream(newFile.getAbsolutePath());
            byte[] buf = new byte[1024]; // optimize the size of buffer to your need
            int num;
            while ((num = is.read(buf)) != -1) {
                os.write(buf, 0, num);
            }
            //byte[] data = new byte[is.available()];
            //is.read(data);
            //os.write(data);
            is.close();
            os.close();
            return newFile;
        } catch (IOException e) {
            Log.i("IOException: " + e.getMessage());
            toast(activity, activity.getString(R.string.error_failed_copy_external_file));
        }
        return null;
    }


    private static void toast(Context c, String message) {
        Toast.makeText(c, message, Toast.LENGTH_LONG).show();
    }
}