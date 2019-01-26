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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RecordingList {

    // List of recordings
    private final List<Recording> mRecordings = new ArrayList<>();
    // Map of tag -> frequency (the frequency information is not really used)
    private final Map<String, Integer> mTags = new HashMap<>();

    private Recording mCurrentRecording;

    public RecordingList() {
        // Don't transcribe
        mTags.put(Recording.TAG_NOTRANS, 1);
    }


    public void setCurrentRecording(Recording recording) {
        mCurrentRecording = recording;
    }


    public List<Recording> list() {
        return mRecordings;
    }


    public int size() {
        return mRecordings.size();
    }


    public Recording get(int index) {
        return mRecordings.get(index);
    }


    public void add(int index, Recording rec) {
        mRecordings.add(index, rec);
        addRecordingTags(rec);
    }

    public void add(Recording rec) {
        mRecordings.add(rec);
        addRecordingTags(rec);
    }

    public void remove(Recording rec) {
        mRecordings.remove(rec);
        // TODO: remove tags
    }

    public void sort(Comparator<Recording> c) {
        Collections.sort(mRecordings, c);
    }

    public int getNeedsTransCount() {
        int needsTransCount = 0;
        for (Recording rec : mRecordings) {
            if (rec.needsTrans()) {
                needsTransCount++;
            }
        }
        return needsTransCount;
    }

    public int getTransCount() {
        int transCount = 0;
        for (Recording rec : mRecordings) {
            if (rec.hasTrans()) {
                transCount++;
            }
        }
        return transCount;
    }

    /**
     * @return Copy of the tags set
     */
    public Set<String> getTags() {
        return new HashSet<>(mTags.keySet());
    }


    /**
     * <p>Adds the given tags to the "current" recording.</p>
     */
    public boolean setTags(Set<String> tags) {
        if (mCurrentRecording == null) {
            return false;
        }
        mCurrentRecording.setTags(tags);
        addRecordingTags(mCurrentRecording);
        return true;
    }

    private void addRecordingTags(Recording rec) {
        for (String tag : rec.getTags()) {
            Integer count = mTags.get(tag);
            mTags.put(tag, count == null ? 1 : count + 1);
        }
    }
}