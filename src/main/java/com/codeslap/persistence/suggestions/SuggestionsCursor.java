/*
 * Copyright 2013 CodeSlap
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.codeslap.persistence.suggestions;

import android.app.SearchManager;
import android.database.AbstractCursor;
import android.provider.BaseColumns;

import java.util.ArrayList;
import java.util.List;

/**
 * This is a {@link android.database.Cursor} implementation that works as a facade to the suggestions table.
 * It is used by the search widget to show suggestions while searching something
 * @author cristian
 */
class SuggestionsCursor extends AbstractCursor {

    static final int COLUMN_INDEX_ID = 0;
    private static final int COLUMN_INDEX_INTENT_ID = 1;
    static final int COLUMN_INDEX_TEXT = 2;
    static final int COLUMN_INDEX_DESCRIPTION = 3;

    private final String[] COLUMNS;
    private final List<SuggestionInfo> mSuggestionInfos = new ArrayList<SuggestionInfo>();

    SuggestionsCursor(List<SuggestionInfo> suggestionInfos) {
        mSuggestionInfos.addAll(suggestionInfos);
        // if the suggestion contains a description, add description column
        if (suggestionInfos.size() > 0 && suggestionInfos.get(0).getDescription() != null) {
            COLUMNS = new String[]{BaseColumns._ID, SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID,
                    SearchManager.SUGGEST_COLUMN_TEXT_1, SearchManager.SUGGEST_COLUMN_TEXT_2};
        } else {
            COLUMNS = new String[]{BaseColumns._ID, SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID,
                    SearchManager.SUGGEST_COLUMN_TEXT_1};
        }
    }

    @Override
    public int getCount() {
        return mSuggestionInfos.size();
    }

    @Override
    public String[] getColumnNames() {
        return COLUMNS;
    }

    @Override
    public long getLong(int i) {
        if (i == COLUMN_INDEX_ID) {
            return mSuggestionInfos.get(getPosition()).getId();
        }
        return 0;
    }

    @Override
    public String getString(int i) {
        if (i == COLUMN_INDEX_INTENT_ID) {
            return String.valueOf(mSuggestionInfos.get(getPosition()).getId());
        }
        if (i == COLUMN_INDEX_TEXT) {
            return mSuggestionInfos.get(getPosition()).getText();
        }
        if (i == COLUMN_INDEX_DESCRIPTION) {
            return mSuggestionInfos.get(getPosition()).getDescription();
        }
        return null;
    }

    @Override
    public short getShort(int i) {
        return 0;
    }

    @Override
    public int getInt(int i) {
        return 0;
    }

    @Override
    public float getFloat(int i) {
        return 0;
    }

    @Override
    public double getDouble(int i) {
        return 0;
    }

    @Override
    public boolean isNull(int i) {
        return false;
    }
}