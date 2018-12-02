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

import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.View;

public class ExecutableSpan extends ClickableSpan {

    private final Executable mExecutable;

    public ExecutableSpan(Executable ex) {
        if (ex == null) {
            throw new IllegalArgumentException("Executable == null");
        }
        mExecutable = ex;
    }

    @Override
    public void onClick(View widget) {
        mExecutable.execute();
    }

    public void updateDrawState(TextPaint ds) {
        // Don't do any decoration
    }
}