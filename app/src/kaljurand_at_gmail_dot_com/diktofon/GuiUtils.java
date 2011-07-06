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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.GradientDrawable.Orientation;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GuiUtils {

	private static final int[] DIVIDER_COLORS = {0xFFeeeee0, 0xFFbfbfbf, 0xFFeeeee0};

	/**
	 * <p>Sets the (text)view that is shown when the list is empty.</p>
	 * 
	 * @param activity
	 * @param lv
	 * @param text
	 */
	public static void setEmptyView(Activity activity, ListView lv, String text) {
		TextView emptyView = (TextView) activity.getLayoutInflater().inflate(R.layout.empty_list, null);
		emptyView.setText(text);
		emptyView.setVisibility(View.GONE);
		((ViewGroup) lv.getParent()).addView(emptyView);
		lv.setEmptyView(emptyView);
	}


	public static AlertDialog getYesNoDialog(Context context, String confirmationMessage, final Executable ex) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder
		.setMessage(confirmationMessage)
		.setCancelable(false)
		.setPositiveButton(context.getString(R.string.b_yes), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				ex.execute();
			}
		})
		.setNegativeButton(context.getString(R.string.b_no), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();
			}
		});
		return builder.create();
	}


	/*
	public static AlertDialog getMultiChoiceDialog(final NoteListView context, String title, final TagChanger tagChanger) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder
		.setTitle(title)
		.setCancelable(false)
		.setMultiChoiceItems(tagChanger.getItems(), tagChanger.getCheckedItems(), new DialogInterface.OnMultiChoiceClickListener() {
			public void onClick(DialogInterface dialog, int which, boolean isChecked) {
				tagChanger.check(which, isChecked);
			}
		})
		.setPositiveButton(context.getString(R.string.b_ok), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				tagChanger.changeTags();
				context.refreshGui();
			}
		})
		.setNegativeButton(context.getString(R.string.b_cancel), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();
			}
		});
		return builder.create();
	}
	 */


	public static AlertDialog getTextEntryDialog(Context context, String title, String initialText, final ExecutableString ex) {
		LayoutInflater factory = LayoutInflater.from(context);
		final View textEntryView = factory.inflate(R.layout.alert_dialog_text_entry, null);
		final EditText et = (EditText) textEntryView.findViewById(R.id.tagname_edit);
		et.setText(initialText);
		return new AlertDialog.Builder(context)
		.setTitle(title)
		.setView(textEntryView)
		.setPositiveButton(R.string.b_ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				ex.execute(et.getText().toString());
			}
		})
		.setNegativeButton(R.string.b_cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				dialog.cancel();
			}
		})
		.create();
	}


	public static void removeHighlight(Spannable spannable) {
		for (HighlightSpan span : spannable.getSpans(0, spannable.length(), HighlightSpan.class)) {
			spannable.removeSpan(span);
		}
	}


	// @deprecated
	public static int highlight(Spannable spannable, String substr, int color) {
		String str = spannable.toString();
		int ind = str.indexOf(substr, 0);
		int substrLen = substr.length();
		int count = 0;
		while (ind != -1) {
			int end = ind + substrLen;
			spannable.setSpan(new HighlightSpan(color), ind, end, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
			count++;
			ind = str.indexOf(substr, end);
		}
		return count;
	}


	// Note that on Android, UNICODE_CASE is always on: case-insensitive matching will always be Unicode-aware.
	// Note that we cannot reuse the same HighlightSpan-object everywhere, because every setSpan
	// would move the style-object to the new location.
	public static int highlightRe(Spannable spannable, String re, int color) {
		Matcher m = Pattern.compile(re, Pattern.CASE_INSENSITIVE).matcher(spannable);
		int count = 0;
		while (m.find()) {
			int start = m.start();
			int end = m.end();
			spannable.setSpan(new HighlightSpan(color), start, end, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
			count++;
		}
		return count;
	}


	public static Spannable highlightCollection(Collection<String> collection, int color) {
		SpannableStringBuilder ssb = new SpannableStringBuilder();
		for (String str : collection) {
			ssb.append(' ');
			int start = ssb.length();
			ssb.append(str);
			int end = ssb.length();
			ssb.setSpan(new HighlightSpan(color), start, end, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
		}
		return ssb;
	}


	public static void setDivider(ListView lv) {
		lv.setDivider(new GradientDrawable(Orientation.LEFT_RIGHT, DIVIDER_COLORS));
		lv.setDividerHeight(1);
	}
}