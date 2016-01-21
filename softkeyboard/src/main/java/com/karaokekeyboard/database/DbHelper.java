package com.karaokekeyboard.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * This file was created by Jew on 1/19/2016.
 */
public class DbHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 5;
    public static final String DATABASE_NAME = "Database.db";
    public DbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    @Override
    public void onCreate(SQLiteDatabase db){
        db.execSQL(DatabaseContract.Frequency.SQL_CREATE_TABLE);
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
        //db.execSQL(DatabaseContract.Frequency.DELETE_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + DatabaseContract.Frequency.TABLE_NAME);
        onCreate(db);
    }
}
