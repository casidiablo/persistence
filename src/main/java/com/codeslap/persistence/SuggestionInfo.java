package com.codeslap.persistence;

/**
 * Keeps the suggestion information
 * @author cristian
 */
public class SuggestionInfo {
    private long mId;
    private String mText;
    private String mDescription;

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
