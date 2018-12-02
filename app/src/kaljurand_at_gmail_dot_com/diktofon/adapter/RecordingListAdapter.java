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

package kaljurand_at_gmail_dot_com.diktofon.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import kaljurand_at_gmail_dot_com.diktofon.GuiUtils;
import kaljurand_at_gmail_dot_com.diktofon.HighlightSpan;
import kaljurand_at_gmail_dot_com.diktofon.R;
import kaljurand_at_gmail_dot_com.diktofon.Recording;
import kaljurand_at_gmail_dot_com.diktofon.RecordingList;
import kaljurand_at_gmail_dot_com.diktofon.Utils;

public class RecordingListAdapter extends BaseAdapter {

    private final Context mContext;
    private final RecordingList mRecordings;
    private final LayoutInflater mInflater;
    private final Resources mRes;

    private String mSearchQuery = null;


    public RecordingListAdapter(Context context, RecordingList recordings) {
        mContext = context;
        mRecordings = recordings;
        mInflater = LayoutInflater.from(context);
        mRes = context.getResources();
    }


    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.list_item_recording, null);

            holder = new ViewHolder();
            holder.list_item_position = convertView.findViewById(R.id.list_item_position);
            holder.list_item_title = convertView.findViewById(R.id.list_item_title);
            holder.list_item_meta = convertView.findViewById(R.id.list_item_meta);
            holder.list_item_trans = convertView.findViewById(R.id.list_item_trans);
            holder.list_item_tags = convertView.findViewById(R.id.list_item_tags);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        if (position >= mRecordings.size()) {
            // This cannot probably happen
            return convertView;
        }

        // Title
        Recording rec = mRecordings.get(position);
        int index = position + 1;
        holder.list_item_position.setText(index + ".");
        holder.list_item_title.setText(rec.getTimestampAsString());

        // Meta info (duration, word count, ...)
        holder.list_item_meta.setText(getMetaInfo(mContext, rec));

        // Transcription or state info
        if (rec.hasTrans()) {
            holder.list_item_trans.setTextColor(mRes.getColor(R.color.l_fg_text));
            holder.list_item_trans.setVisibility(View.VISIBLE);
            if (mSearchQuery != null && rec.getMatchCount(mSearchQuery) > 0) {
                SpannableStringBuilder ssb = new SpannableStringBuilder();
                ssb.append(rec.getMatchCount(mSearchQuery) + " x");
                int labelEnd = ssb.length();
                ssb.setSpan(new HighlightSpan(mRes.getColor(R.color.highlight)), 0, labelEnd, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                ssb.append(' ');
                ssb.append(rec.getExcerpt());
                holder.list_item_trans.setText(ssb, TextView.BufferType.SPANNABLE);
            } else {
                holder.list_item_trans.setText(rec.getExcerpt());
            }
        } else {
            String stateText = getStateText(rec);
            if (stateText == null) {
                holder.list_item_trans.setVisibility(View.GONE);
            } else {
                holder.list_item_trans.setTextColor(mRes.getColor(R.color.processing));
                holder.list_item_trans.setVisibility(View.VISIBLE);
                holder.list_item_trans.setText(stateText);
            }
        }

        // Tags
        if (rec.hasTags()) {
            holder.list_item_tags.setVisibility(View.VISIBLE);
            holder.list_item_tags.setText(GuiUtils.highlightCollection(rec.getTagsAsSortedSet(), mRes.getColor(R.color.l_bg_tags)), TextView.BufferType.SPANNABLE);
        } else {
            holder.list_item_tags.setVisibility(View.GONE);
        }

        return convertView;
    }


    static class ViewHolder {
        TextView list_item_position;
        TextView list_item_title;
        TextView list_item_meta;
        TextView list_item_trans;
        TextView list_item_tags;
    }


    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getCount() {
        return mRecordings.size();
    }

    @Override
    public Object getItem(int position) {
        return mRecordings.get(position);
    }

    public void refresh() {
        notifyDataSetChanged();
    }

    public void setSearchQuery(String searchQuery) {
        mSearchQuery = searchQuery;
    }

    private String getStateText(Recording rec) {
        switch (rec.getState()) {
            case INITIAL:
                return null;
            case UPLOADING:
                return mContext.getString(R.string.progress_uploading);
            case WAITING:
                long remainingTime = rec.getWaitingTime() - Utils.getTimestamp() + rec.getStateTime(Recording.State.WAITING);
                if (remainingTime < 0) {
                    // BUG: This shouldn't really happen, as the state should change
                    // when the waiting time runs out
                    return mContext.getString(R.string.progress_waiting_unknown);
                } else {
                    return String.format(mContext.getString(R.string.progress_waiting), Utils.formatMillis(remainingTime));
                }
            case POLLING:
                return String.format(mContext.getString(R.string.progress_polling), rec.getPollCount());
            case SUCCESS:
                // BUG: this shouldn't be possible (i.e. the transcription gets printed instead)
                // It does sometimes happen though...
                return mContext.getString(R.string.progress_success);
            case FAILURE:
                return mContext.getString(R.string.progress_failure);
            default:
                return mContext.getString(R.string.submit_bug_report);
        }
    }


    // TODO: probably the users don't care about the MIME type, it would
    // save some space not to print it.
    private String getMetaInfo(Context context, Recording note) {
        String metaText = note.getDurationAsString() + "  " + note.getMimePart() + "  " + note.getSizeAsString();
        int wordCount = note.getWordCount();
        if (wordCount >= 0) {
            metaText += "  " + wordCount + " " + context.getString(R.string.words);
        }
        int speakerCount = note.getSpeakerCount();
        if (speakerCount >= 0) {
            metaText += "  " + speakerCount + " " + context.getString(R.string.speakers);
        }
        return metaText;
    }
}
