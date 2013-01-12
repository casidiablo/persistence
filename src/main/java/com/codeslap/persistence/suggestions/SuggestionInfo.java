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

/**
 * Keeps the suggestion information
 * @author cristian
 */
public class SuggestionInfo {
    private final long mId;
    private final String mText;
    private final String mDescription;

    private SuggestionInfo(long id, String text, String description) {
        mId = id;
        mText = text;
        mDescription = description;
    }

    public long getId() {
        return mId;
    }

    public String getText() {
        return mText;
    }

    public String getDescription() {
        return mDescription;
    }

    public static class Builder {
        private long id = -1;
        private String suggestColumn1 = null;
        private String suggestColumn2 = null;

        public Builder setId(long id) {
            this.id = id;
            return this;
        }

        public Builder setText(String suggestColumn1) {
            this.suggestColumn1 = suggestColumn1;
            return this;
        }

        public Builder setDescription(String suggestColumn2) {
            this.suggestColumn2 = suggestColumn2;
            return this;
        }

        public SuggestionInfo build() {
            if (id == -1) {
                throw new IllegalStateException("You did not set the ID in a suggestion");
            }
            if (suggestColumn1 == null) {
                throw new IllegalStateException("You did not set the text in a suggestion.");
            }
            return new SuggestionInfo(id, suggestColumn1, suggestColumn2);
        }
    }
}
