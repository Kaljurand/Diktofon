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

package kaljurand_at_gmail_dot_com.diktofon.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import kaljurand_at_gmail_dot_com.diktofon.R;

public class SpeakerListAdapter extends BaseAdapter {

	private final LayoutInflater mInflater;
	private final List<String> mSpeakerIds = new ArrayList<String>();

	public SpeakerListAdapter(Context context, Set<String> speakerIds) {
		mInflater = LayoutInflater.from(context);
		mSpeakerIds.addAll(speakerIds);
	}


	public View getView(final int position, View convertView, ViewGroup parent) {
		ViewHolder holder;

		if (convertView == null) {
			convertView = mInflater.inflate(R.layout.list_item_speaker, null);

			holder = new ViewHolder();
			holder.list_item_speaker_name = (TextView) convertView.findViewById(R.id.list_item_speaker_name);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		final String speakerId = mSpeakerIds.get(position);
		holder.list_item_speaker_name.setText(speakerId);
		return convertView;
	}


	static class ViewHolder {
		TextView list_item_speaker_name;
	}


	@Override
	public long getItemId(int position) {
		return position;
	}


	@Override
	public Object getItem(int position) {
		return mSpeakerIds.get(position);
	}


	@Override
	public int getCount() {
		return mSpeakerIds.size();
	}
}