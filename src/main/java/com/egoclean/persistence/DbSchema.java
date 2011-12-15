package com.egoclean.persistence;

public final class DbSchema {
    static final String CREATE_MEDIA_TABLE = "CREATE TABLE IF NOT EXISTS " + Media.TABLE_NAME + " (" +
            Media.ID + " INTEGER PRIMARY KEY, " +
            Media.SEASON + " INTEGER NOT NULL, " +
            Media.EPISODE + " INTEGER NOT NULL, " +
            Media.TITLE + " TEXT, " +
            Media.SUMMARY + " TEXT, " +
            Media.URL + " TEXT, " +
            Media.IMDB + " TEXT, " +
            Media.THUMBNAIL + " TEXT, " +
            Media.POSITIVE + " INTEGER, " +
            Media.NEGATIVE + " INTEGER)";

    static final String CREATE_SHOWS_TABLE = "CREATE TABLE IF NOT EXISTS " + Show.TABLE_NAME + " (" +
            Show.ID + " INTEGER PRIMARY KEY, " +
            Show.NAME + " INTEGER NOT NULL, " +
            Show.DESCRIPTION + " INTEGER NOT NULL, " +
            Show.THUMBNAIL + " TEXT, " +
            Show.MARKET_PRO + " TEXT, " +
            Show.MARKET_FREE + " TEXT," +
            Show.EXTERNAL_URL + " TEXT," +
            Show.PREFIX + " TEXT," +
            Show.GENRE + " TEXT," +
            Show.IMDB + " TEXT)";

    static final String CREATE_LIKES_TABLE = "CREATE TABLE IF NOT EXISTS " + Likes.TABLE_NAME + " (" +
            Likes.ID + " INTEGER PRIMARY KEY, " +
            Likes.EPISODE_ID + " INTEGER NOT NULL, " +
            Likes.LIKED + " INTEGER NOT NULL)";

    public static class Media {
        public static final String TABLE_NAME = "media";

        public static final String ID = "_id";
        public static final String SEASON = "season";
        public static final String EPISODE = "episode";
        public static final String TITLE = "title";
        public static final String SUMMARY = "summary";
        public static final String URL = "url";
        public static final String IMDB = "imdb";
        public static final String THUMBNAIL = "thumbnail";
        public static final String POSITIVE = "positive";
        public static final String NEGATIVE = "negative";

        public static final int ID_IDX = 0;
        public static final int SEASON_IDX = 1;
        public static final int EPISODE_IDX = 2;
        public static final int TITLE_IDX = 3;
        public static final int SUMMARY_IDX = 4;
        public static final int URL_IDX = 5;
        public static final int IMDB_IDX = 6;
        public static final int THUMBNAIL_IDX = 7;
        public static final int POSITIVE_IDX = 8;
        public static final int NEGATIVE_IDX = 9;
    }

    public static class Show {
        public static final String TABLE_NAME = "shows";

        public static final String ID = "_id";
        public static final String NAME = "name";
        public static final String DESCRIPTION = "description";
        public static final String THUMBNAIL = "thumbnail";
        public static final String MARKET_PRO = "market_pro";
        public static final String MARKET_FREE = "market_free";
        public static final String EXTERNAL_URL = "external_url";
        public static final String PREFIX = "prefix";
        public static final String GENRE = "genre";
        public static final String IMDB = "imdb";

        public static final int ID_IDX = 0;
        public static final int NAME_IDX = 1;
        public static final int DESCRIPTION_IDX = 2;
        public static final int THUMBNAIL_IDX = 3;
        public static final int MARKET_PRO_IDX = 4;
        public static final int MARKET_FREE_IDX = 5;
        public static final int EXTERNAL_URL_IDX = 6;
        public static final int PREFIX_IDX = 7;
        public static final int GENRE_IDX = 8;
        public static final int IMDB_IDX = 9;
    }

    public static class Likes {
        public static final String TABLE_NAME = "likes";

        public static final String ID = "_id";
        public static final String EPISODE_ID = "episode_id";
        public static final String LIKED = "liked";

        public static final int ID_IDX = 0;
        public static final int EPISODE_ID_IDX = 1;
        public static final int LIKED_IDX = 2;
    }
}