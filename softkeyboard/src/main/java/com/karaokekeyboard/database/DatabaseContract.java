package com.karaokekeyboard.database;

import android.provider.BaseColumns;
import android.util.Log;

/**
 * This file was created by Jew on 1/17/2016.
 */
public final class DatabaseContract {
    public static final String TAG = "db";
    public DatabaseContract() {
        Log.d(TAG, "Why construct DatabaseContract ?");
    }
    public static abstract class Frequency implements BaseColumns {
        public static final String TABLE_NAME = "freq";
        public static final String COLUMN_NAME_WORD = "word";
        public static final String COLUMN_NAME_FREQ = "freq";
        public static final String SQL_CREATE_TABLE =
                "CREATE TABLE " + Frequency.TABLE_NAME + " (" +
                        Frequency._ID + " INTEGER PRIMARY KEY," +
                        Frequency.COLUMN_NAME_WORD + " TEXT NOT NULL," +
                        Frequency.COLUMN_NAME_FREQ + " INT NOT NULL" + " )";
        public static final String SQL_UPDATE =
                "INSERT";
    }
}
