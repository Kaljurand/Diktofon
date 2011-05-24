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
import android.widget.CheckBox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import kaljurand_at_gmail_dot_com.diktofon.R;

public class TagSelectorAdapter extends BaseAdapter {

	private final LayoutInflater mInflater;
	private final List<String> mTags = new ArrayList<String>();
	private final Set<String> mSelectedTags = new HashSet<String>();

	public TagSelectorAdapter(Context context, List<String> tags, Set<String> selectedTags) {
		mInflater = LayoutInflater.from(context);
		mTags.addAll(tags);
		mSelectedTags.addAll(selectedTags);
		Collections.sort(mTags, new TagComparator(selectedTags));
	}


	public View getView(final int position, View convertView, ViewGroup parent) {
		ViewHolder holder;

		if (convertView == null) {
			convertView = mInflater.inflate(R.layout.list_item_edittag, null);

			holder = new ViewHolder();
			holder.list_item_edittag_title = (TextView) convertView.findViewById(R.id.list_item_edittag_title);
			holder.list_item_edittag_state = (CheckBox) convertView.findViewById(R.id.list_item_edittag_state);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		final String tag = mTags.get(position);
		holder.list_item_edittag_title.setText(tag);
		holder.list_item_edittag_state.setChecked(mSelectedTags.contains(tag));
		holder.list_item_edittag_state.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (((CheckBox) v).isChecked()) {
					mSelectedTags.add(tag);
				} else {
					mSelectedTags.remove(tag);
				}
			}
		});
		return convertView;
	}


	static class ViewHolder {
		TextView list_item_edittag_title;
		CheckBox list_item_edittag_state;
	}


	@Override
	public long getItemId(int position) {
		return position;
	}


	@Override
	public Object getItem(int position) {
		return mTags.get(position);
	}


	@Override
	public int getCount() {
		return mTags.size();
	}


	public int getSelectedCount() {
		return mSelectedTags.size();
	}


	public void addAll(Iterable<String> iterable) {
		for (String tag : iterable) {
			if (tag.length() > 0) {
				mSelectedTags.add(tag);
				if (! mTags.contains(tag)) {
					// TODO: instead add to the place where the comparator requires
					mTags.add(0, tag);
				}
			}
		}
		notifyDataSetChanged();
	}


	public Set<String> getSelectedTags() {
		return mSelectedTags;
	}


	private static class TagComparator implements Comparator<String> {
		private final Set<String> selected;

		public TagComparator(Set<String> selected) {
			this.selected = selected;
		}

		public int compare(String t1, String t2) {
			boolean b1 = selected.contains(t1);
			boolean b2 = selected.contains(t2);
			if (b1 && ! b2) return -1;
			if (! b1 && b2) return 1;
			return t1.compareToIgnoreCase(t2);
		}
	}
}
