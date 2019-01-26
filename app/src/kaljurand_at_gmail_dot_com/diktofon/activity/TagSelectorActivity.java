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

package kaljurand_at_gmail_dot_com.diktofon.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import kaljurand_at_gmail_dot_com.diktofon.GuiUtils;
import kaljurand_at_gmail_dot_com.diktofon.R;
import kaljurand_at_gmail_dot_com.diktofon.Utils;
import kaljurand_at_gmail_dot_com.diktofon.adapter.TagSelectorAdapter;

public class TagSelectorActivity extends Activity {

    public static final String EXTRA_ADD_ENABLED = "EXTRA_ADD_ENABLED";
    public static final String EXTRA_TAGS = "EXTRA_TAGS";
    public static final String EXTRA_TAGS_SELECTED = "EXTRA_TAGS_SELECTED";

    private TagSelectorAdapter mAdapter = null;
    private boolean mAddEnabled = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tagselector);

        ListView lv = findViewById(R.id.list_edittag);
        lv.setFastScrollEnabled(true);
        GuiUtils.setDivider(lv);

        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            finish();
            return;
        }

        List<String> tags = extras.getStringArrayList(EXTRA_TAGS);
        if (tags == null) {
            tags = new ArrayList<>();
        }

        mAddEnabled = extras.getBoolean(EXTRA_ADD_ENABLED);
        List<String> selectedTagsAsList = extras.getStringArrayList(EXTRA_TAGS_SELECTED);
        if (selectedTagsAsList == null) {
            mAdapter = new TagSelectorAdapter(this, tags, new HashSet<>());
        } else {
            mAdapter = new TagSelectorAdapter(this, tags, new HashSet<>(selectedTagsAsList));
        }
        lv.setAdapter(mAdapter);
        updateTitle();

        findViewById(R.id.b_apply_tags).setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.putExtra(EXTRA_TAGS_SELECTED, Utils.setToArray(mAdapter.getSelectedTags()));
            setResult(Activity.RESULT_OK, intent);
            finish();
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (mAddEnabled) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.tagselector, menu);
        }
        return mAddEnabled;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_edittags_add:
                GuiUtils.getTextEntryDialog(
                        this,
                        getString(R.string.dialog_title_new_tags),
                        "",
                        str -> {
                            mAdapter.addAll(Utils.parseTagString(str));
                            updateTitle();
                        }
                ).show();
                return true;
            // TODO: sorting
            default:
                return super.onContextItemSelected(item);
        }
    }


    // TODO: provide a useful title that shows the total number of tags,
    // and also the number of selected tags
    private void updateTitle() {
        if (mAdapter != null) {
            //setTitle(String.format(getString(R.string.titleTagSelector), adapter.getCount(), adapter.getSelectedCount()));
            String info = String.format(getString(R.string.titleTagSelector), mAdapter.getCount());
            ActionBar ab = getActionBar();
            if (ab != null) {
                ab.setSubtitle(info);
            }
        }
    }
}